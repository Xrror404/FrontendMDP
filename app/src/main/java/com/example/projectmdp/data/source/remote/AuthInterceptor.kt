package com.example.projectmdp.data.source.remote

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Refresh token dari Firebase secara sinkron (dengan runBlocking)
        val newToken = runBlocking {
            FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.await()?.token
        }

        val newRequest = originalRequest.newBuilder().apply {
            newToken?.let {
                addHeader("Authorization", "Bearer $it")
            }
        }.build()

        return chain.proceed(newRequest)
    }
}