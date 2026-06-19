package com.devin.csuite.di

import com.devin.csuite.data.remote.MetricsRepositoryImpl
import com.devin.csuite.data.remote.SessionsRepositoryImpl
import com.devin.csuite.data.remote.TeamRepositoryImpl
import com.devin.csuite.domain.repository.MetricsRepository
import com.devin.csuite.domain.repository.SessionsRepository
import com.devin.csuite.domain.repository.TeamRepository
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

    @Binds
    @Singleton
    abstract fun bindSessionsRepository(impl: SessionsRepositoryImpl): SessionsRepository

    @Binds
    @Singleton
    abstract fun bindTeamRepository(impl: TeamRepositoryImpl): TeamRepository
}
