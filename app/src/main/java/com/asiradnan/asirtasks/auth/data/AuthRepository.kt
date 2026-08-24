package com.asiradnan.asirtasks.auth.data

import com.asiradnan.asirtasks.auth.models.LoginRequest
import com.asiradnan.asirtasks.auth.network.AuthApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

interface AuthRepository {
    val isLoggedIn: Flow<Boolean>
    suspend fun login(loginRequest: LoginRequest): Result<Unit>
    suspend fun logout()
}

class DefaultAuthRepository(
    private val authApiService: AuthApiService,
    private val tokenManager: TokenManager
) : AuthRepository {
    override val isLoggedIn: Flow<Boolean> =
        tokenManager.accessToken.map { it != null }.distinctUntilChanged()

    override suspend fun login(loginRequest: LoginRequest): Result<Unit> {
        return try {
            val tokens = authApiService.login(loginRequest)
            // Log.d("asiradnan", tokens.toString())
            tokenManager.saveTokens(tokens.accessToken, tokens.refreshToken)
            // Log.d(
            //     "AuthRepository",
            //     "Saving tokens: access=${tokens.accessToken}, refresh=${tokens.refreshToken}"
            // )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        tokenManager.clearTokens()
    }
}