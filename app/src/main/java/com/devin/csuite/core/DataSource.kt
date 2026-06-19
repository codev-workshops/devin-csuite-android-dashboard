package com.devin.csuite.core

interface DataSource<T> {
    suspend fun get(): Result<T>
}

interface RemoteDataSource<T> : DataSource<T>

interface LocalDataSource<T> : DataSource<T> {
    suspend fun save(data: T)
    suspend fun clear()
}
