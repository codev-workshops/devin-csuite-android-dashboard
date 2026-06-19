package com.devin.csuite.di

import com.devin.csuite.BuildConfig
import com.devin.csuite.core.Constants
import com.devin.csuite.data.remote.AuthInterceptor
import com.devin.csuite.data.remote.EnterpriseApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Dispatcher
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val dispatcher = Dispatcher().apply {
            maxRequests = Constants.MAX_CONCURRENT_REQUESTS
            maxRequestsPerHost = Constants.MAX_CONCURRENT_REQUESTS
        }

        return OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .addInterceptor(authInterceptor)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                }
            }
            .connectTimeout(Constants.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(Constants.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(Constants.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideEnterpriseApi(retrofit: Retrofit): EnterpriseApi {
        return retrofit.create(EnterpriseApi::class.java)
    }
}
