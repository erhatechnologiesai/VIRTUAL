package com.virtual.core.network

import android.content.Context
import com.virtual.core.storage.SecureStorageManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // Default emulator or local IP; configurable at runtime
    var baseUrl: String = "http://10.0.2.2:4000/"

    private var retrofit: Retrofit? = null
    private var apiService: ApiService? = null

    fun getService(context: Context): ApiService {
        if (apiService == null) {
            val storage = SecureStorageManager.getInstance(context)

            val authInterceptor = Interceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()

                // Attach JWT Access Token
                storage.accessToken?.let { token ->
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }

                // Attach Device UUID
                storage.deviceUuid?.let { uuid ->
                    requestBuilder.addHeader("X-Device-Id", uuid)
                }

                val request = requestBuilder.build()
                chain.proceed(request)
            }

            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            apiService = retrofit!!.create(ApiService::class.java)
        }
        return apiService!!
    }

    fun updateBaseUrl(newUrl: String, context: Context): ApiService {
        var formatted = newUrl.trim()
        if (!formatted.endsWith("/")) {
            formatted += "/"
        }
        baseUrl = formatted
        apiService = null
        return getService(context)
    }
}
