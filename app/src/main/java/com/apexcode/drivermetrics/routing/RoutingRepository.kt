package com.apexcode.drivermetrics.routing

import com.apexcode.drivermetrics.core.model.LatLng
import com.apexcode.drivermetrics.core.model.RouteInfo

/**
 * Behind an interface so a self-hosted or paid routing provider can replace the public OSRM
 * demo server later via DI without touching callers.
 */
interface RoutingRepository {
    /** Returns null if no route could be found or the request failed. */
    suspend fun getRoute(from: LatLng, to: LatLng): RouteInfo?
}
