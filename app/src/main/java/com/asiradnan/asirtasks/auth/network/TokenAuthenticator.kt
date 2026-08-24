package com.asiradnan.asirtasks.auth.network

import android.util.Log
import com.asiradnan.asirtasks.auth.data.TokenManager
import com.asiradnan.asirtasks.auth.models.Tokens
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Modular Authenticator that automatically refreshes JWT tokens when a 401 is received.
 * [refreshAction] is a lambda that calls your refresh API. This keeps the class
 * independent of any specific Retrofit instance.
 */
class TokenAuthenticator(
    private val tokenManager: TokenManager,
    private val refreshAction: suspend (String) -> Tokens?
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        Log.d(
            "TokenAuthenticator",
            "authenticate() called, priorResponse=${response.priorResponse != null}"
        )
        if (responseCount(response) >= 3) return null

        val refreshToken = runBlocking { tokenManager.refreshToken.first() }
        Log.d("TokenAuthenticator", "refreshToken from storage: $refreshToken")
        if (refreshToken == null) return null

        // 3. Use synchronized to ensure multiple threads don't refresh at the same time
        synchronized(this) {
            // 4. Re-check the current access token.
            // If it's different from the one that failed, another thread already refreshed it!
            val currentToken = runBlocking { tokenManager.accessToken.first() }
            val failedToken = response.request.header("Authorization")?.removePrefix("Bearer ")

            if (currentToken != failedToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            // 5. Try to refresh the token
            val newTokens = try {
                runBlocking { refreshAction(refreshToken) }
            } catch (e: retrofit2.HttpException) {
                // If the refresh token itself is rejected (400, 401, 403), log out
                if (e.code() in 400..403) {
                    Log.e("TokenAuthenticator", "Refresh token expired/invalid. Forcing logout.")
                    runBlocking { tokenManager.clearTokens() }
                } else {
                    Log.e("TokenAuthenticator", "Server error during refresh: ${e.code()}")
                }
                return null
            } catch (e: Exception) {
                Log.e("TokenAuthenticator", "Network error during refresh", e)
                return null // Return null to let the original 401 fail, but DON'T clear tokens (e.g. offline)
            }

            return if (newTokens != null) {
                // 6. Save new tokens and retry the request
                // If the server didn't return a new refresh token, use the one we already have
                val nextRefreshToken = newTokens.refreshToken ?: refreshToken

                runBlocking {
                    tokenManager.saveTokens(newTokens.accessToken, nextRefreshToken)
                }

                response.request.newBuilder()
                    .header("Authorization", "Bearer ${newTokens.accessToken}")
                    .build()
            } else {
                // 7. Refresh failed (e.g., refresh token expired). Clear data and force logout.
                runBlocking { tokenManager.clearTokens() }
                null
            }
        }
    }

    private fun responseCount(response: Response?): Int {
        var result = 1
        var current = response
        while (current?.priorResponse != null) {
            result++
            current = current.priorResponse
        }
        return result
    }
}
