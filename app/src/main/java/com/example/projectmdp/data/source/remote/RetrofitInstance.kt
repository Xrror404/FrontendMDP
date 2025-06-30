package com.example.projectmdp.data.source.remote

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.moshi.MoshiConverterFactory


object RetrofitInstance {

    private var token: String? = null

    fun setToken(newToken: String){
        token = newToken
    }

    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val builder = originalRequest.newBuilder()

        token?.let {
            builder.addHeader("Authorization", "Bearer $it")
        }

        val newRequest = builder.build()
        chain.proceed(newRequest)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .build()

//    https://pouncing-rune-lord.glitch.me
//    http://10.0.2.2:3000/
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://pouncing-rune-lord.glitch.me")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val Authapi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }

    val Productapi: ProductApi by lazy {
        retrofit.create(ProductApi::class.java)
    }

    val Chatapi: ChatApi by lazy {
        retrofit.create(ChatApi::class.java)
    }

    val Transactionapi: TransactionApi by lazy {
        retrofit.create(TransactionApi::class.java)
    }

    val Userapi: UserApi by lazy {
        retrofit.create(UserApi::class.java)
    }



}
