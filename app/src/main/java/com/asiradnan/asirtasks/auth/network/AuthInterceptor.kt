package com.asiradnan.asirtasks.auth.network

import com.asiradnan.asirtasks.auth.data.TokenManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenManager.accessToken.first() }
        // Log.d("AuthInterceptor", "Sending token: $token")

        val request = chain.request().newBuilder().apply {
            // Use .header() to replace any existing Authorization header
            token?.let { header("Authorization", "Bearer $it") }
        }.build()

        return chain.proceed(request)
    }
}