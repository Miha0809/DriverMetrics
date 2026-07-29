package com.apexcode.drivermetrics.routing

import android.util.Log
import com.apexcode.drivermetrics.core.model.LatLng
import com.apexcode.drivermetrics.core.model.RouteInfo
import javax.inject.Inject

/**
 * Default RoutingRepository backed by the public OSRM demo server (router.project-osrm.org).
 * That server is best-effort/rate-limited and explicitly NOT meant for production load — for
 * real usage, point this at a self-hosted OSRM instance (change the base URL in NetworkModule
 * or move it to SettingsRepository once that exists).
 */
class OsrmRoutingRepository @Inject constructor(
    private val api: OsrmApi,
) : RoutingRepository {

    override suspend fun getRoute(from: LatLng, to: LatLng): RouteInfo? {
        val coordinates = "${from.longitude},${from.latitude};${to.longitude},${to.latitude}"
        return try {
            val route = api.getRoute(coordinates).routes.firstOrNull() ?: return null
            RouteInfo(
                distanceKm = route.distance / 1000.0,
                durationMin = route.duration / 60.0,
                geometry = route.geometry?.coordinates.orEmpty().map { LatLng(it[1], it[0]) },
            )
        } catch (e: Exception) {
            Log.w(TAG, "Routing failed for $coordinates", e)
            null
        }
    }

    private companion object {
        const val TAG = "Routing"
    }
}
