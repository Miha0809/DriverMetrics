package com.apexcode.drivermetrics.routing

import com.apexcode.drivermetrics.core.model.LatLng
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private class FakeOsrmApi(private val response: OsrmRouteResponse) : OsrmApi {
    override suspend fun getRoute(coordinates: String, overview: String, geometries: String) = response
}

class OsrmRoutingRepositoryTest {

    @Test
    fun `getRoute converts meters and seconds to km and minutes`() = runTest {
        val api = FakeOsrmApi(
            OsrmRouteResponse(
                code = "Ok",
                routes = listOf(OsrmRoute(distance = 5000.0, duration = 600.0)),
            ),
        )
        val repository = OsrmRoutingRepository(api)

        val route = repository.getRoute(LatLng(59.43, 24.74), LatLng(59.44, 24.75))

        requireNotNull(route)
        assertEquals(5.0, route.distanceKm, 0.0001)
        assertEquals(10.0, route.durationMin, 0.0001)
    }

    @Test
    fun `getRoute maps geojson coordinates from lon,lat to LatLng`() = runTest {
        val api = FakeOsrmApi(
            OsrmRouteResponse(
                code = "Ok",
                routes = listOf(
                    OsrmRoute(
                        distance = 1000.0,
                        duration = 60.0,
                        geometry = OsrmGeometry(coordinates = listOf(listOf(24.74, 59.43), listOf(24.75, 59.44))),
                    ),
                ),
            ),
        )
        val repository = OsrmRoutingRepository(api)

        val route = repository.getRoute(LatLng(59.43, 24.74), LatLng(59.44, 24.75))

        requireNotNull(route)
        assertEquals(listOf(LatLng(59.43, 24.74), LatLng(59.44, 24.75)), route.geometry)
    }

    @Test
    fun `getRoute returns null when there are no routes`() = runTest {
        val api = FakeOsrmApi(OsrmRouteResponse(code = "NoRoute", routes = emptyList()))
        val repository = OsrmRoutingRepository(api)

        assertNull(repository.getRoute(LatLng(0.0, 0.0), LatLng(1.0, 1.0)))
    }
}
