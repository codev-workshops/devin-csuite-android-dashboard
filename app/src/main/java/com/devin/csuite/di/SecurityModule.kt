package com.devin.csuite.di

import com.devin.csuite.data.remote.security.SecurityApi
import com.devin.csuite.data.remote.security.SecurityRepositoryImpl
import com.devin.csuite.domain.repository.security.SecurityRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityNetworkModule {

    @Provides
    @Singleton
    fun provideSecurityApi(retrofit: Retrofit): SecurityApi {
        return retrofit.create(SecurityApi::class.java)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityDataModule {

    @Binds
    @Singleton
    abstract fun bindSecurityRepository(impl: SecurityRepositoryImpl): SecurityRepository
}
