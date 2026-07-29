package com.apexcode.drivermetrics.di

import com.apexcode.drivermetrics.geocoding.GeocodingRepository
import com.apexcode.drivermetrics.geocoding.NominatimGeocodingRepository
import com.apexcode.drivermetrics.routing.OsrmRoutingRepository
import com.apexcode.drivermetrics.routing.RoutingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {
    @Binds
    fun bindGeocodingRepository(impl: NominatimGeocodingRepository): GeocodingRepository

    @Binds
    fun bindRoutingRepository(impl: OsrmRoutingRepository): RoutingRepository
}
