package com.apexcode.drivermetrics.geocoding

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/** https://nominatim.org/release-docs/latest/api/Search/ */
interface NominatimApi {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "jsonv2",
        @Query("limit") limit: Int = 1,
    ): List<NominatimResult>
}

@Serializable
data class NominatimResult(
    val lat: String,
    val lon: String,
)
