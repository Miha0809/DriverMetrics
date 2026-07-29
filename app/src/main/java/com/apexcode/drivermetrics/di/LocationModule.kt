package com.apexcode.drivermetrics.di

import com.apexcode.drivermetrics.location.CurrentLocationProvider
import com.apexcode.drivermetrics.location.SystemLocationProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface LocationModule {
    @Binds
    fun bindCurrentLocationProvider(impl: SystemLocationProvider): CurrentLocationProvider
}
