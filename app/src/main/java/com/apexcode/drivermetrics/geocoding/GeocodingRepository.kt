package com.apexcode.drivermetrics.geocoding

import com.apexcode.drivermetrics.core.model.LatLng

/**
 * Behind an interface so a paid provider can be swapped in later via DI without touching
 * callers (see NominatimGeocodingRepository for the default free implementation).
 */
interface GeocodingRepository {
    /** Returns null if the address couldn't be resolved or the request failed. */
    suspend fun geocode(address: String): LatLng?
}
