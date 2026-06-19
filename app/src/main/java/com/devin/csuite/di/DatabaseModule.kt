package com.devin.csuite.di

import android.content.Context
import androidx.room.Room
import com.devin.csuite.data.local.db.AppDatabase
import com.devin.csuite.data.local.db.dao.BillingCacheDao
import com.devin.csuite.data.local.db.dao.MetricsCacheDao
import com.devin.csuite.data.local.db.dao.SessionsCacheDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideMetricsCacheDao(db: AppDatabase): MetricsCacheDao = db.metricsCacheDao()

    @Provides
    fun provideSessionsCacheDao(db: AppDatabase): SessionsCacheDao = db.sessionsCacheDao()

    @Provides
    fun provideBillingCacheDao(db: AppDatabase): BillingCacheDao = db.billingCacheDao()
}
