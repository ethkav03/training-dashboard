package com.momentum.android.network

import com.momentum.android.BuildConfig
import com.momentum.android.auth.TokenStore
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object ApiClient {
    private val json = Json { ignoreUnknownKeys = true }

    fun create(tokenStore: TokenStore): MomentumApi {
        // Mirrors the web app's axios request interceptor
        // (frontend/src/api/client.ts) -- attaches the stored JWT to every
        // request the same way.
        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder().apply {
                tokenStore.token?.let { header("Authorization", "Bearer $it") }
            }.build()
            chain.proceed(request)
        }

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(ensureTrailingSlash(BuildConfig.API_BASE_URL))
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(MomentumApi::class.java)
    }

    private fun ensureTrailingSlash(url: String) = if (url.endsWith("/")) url else "$url/"
}
