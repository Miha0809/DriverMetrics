package com.apexcode.drivermetrics.routing

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** http://project-osrm.org/docs/v5.24.0/api/#route-service */
interface OsrmApi {
    @GET("route/v1/driving/{coordinates}")
    suspend fun getRoute(
        @Path("coordinates", encoded = true) coordinates: String,
        // "simplified" (not "full") — this only ever feeds a small preview map, and a much
        // shorter point list downloads/parses/renders faster with no visible loss of accuracy
        // at that scale.
        @Query("overview") overview: String = "simplified",
        @Query("geometries") geometries: String = "geojson",
    ): OsrmRouteResponse
}

@Serializable
data class OsrmRouteResponse(
    val code: String,
    val routes: List<OsrmRoute> = emptyList(),
)

@Serializable
data class OsrmRoute(
    val distance: Double,
    val duration: Double,
    val geometry: OsrmGeometry? = null,
)

@Serializable
data class OsrmGeometry(
    val coordinates: List<List<Double>> = emptyList(),
)
