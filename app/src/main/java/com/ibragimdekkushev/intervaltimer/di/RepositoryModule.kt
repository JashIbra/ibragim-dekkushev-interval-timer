package com.ibragimdekkushev.intervaltimer.di

import com.ibragimdekkushev.intervaltimer.data.repository.IntervalTimerRepositoryImpl
import com.ibragimdekkushev.intervaltimer.domain.repository.IntervalTimerRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindIntervalTimerRepository(
        impl: IntervalTimerRepositoryImpl,
    ): IntervalTimerRepository
}
