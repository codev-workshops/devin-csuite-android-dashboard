package com.devin.csuite.di

import com.devin.csuite.data.remote.MetricsRepositoryImpl
import com.devin.csuite.domain.repository.MetricsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindMetricsRepository(impl: MetricsRepositoryImpl): MetricsRepository
}
