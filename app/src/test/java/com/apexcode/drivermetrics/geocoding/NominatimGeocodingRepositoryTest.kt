package com.apexcode.drivermetrics.geocoding

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private class FakeNominatimApi(
    private val resultsByQuery: Map<String, List<NominatimResult>>,
) : NominatimApi {
    var callCount = 0
        private set

    override suspend fun search(query: String, format: String, limit: Int): List<NominatimResult> {
        callCount++
        return resultsByQuery[query].orEmpty()
    }
}

class NominatimGeocodingRepositoryTest {

    @Test
    fun `geocode maps the first result to LatLng`() = runTest {
        val api = FakeNominatimApi(
            mapOf("Narva mnt 5, Tallinn" to listOf(NominatimResult(lat = "59.437", lon = "24.745"))),
        )
        val repository = NominatimGeocodingRepository(api)

        val result = repository.geocode("Narva mnt 5, Tallinn")

        requireNotNull(result)
        assertEquals(59.437, result.latitude, 0.0001)
        assertEquals(24.745, result.longitude, 0.0001)
    }

    @Test
    fun `geocode returns null when nothing is found`() = runTest {
        val api = FakeNominatimApi(emptyMap())
        val repository = NominatimGeocodingRepository(api)

        assertNull(repository.geocode("nonexistent address"))
    }

    @Test
    fun `falls back to a middle-initial-stripped query when the full address is not found`() = runTest {
        val api = FakeNominatimApi(
            mapOf(
                "aleja Władysława Reymonta, Warszawa" to
                    listOf(NominatimResult(lat = "52.2829621", lon = "20.9404150")),
            ),
        )
        val repository = NominatimGeocodingRepository(api)

        val result = repository.geocode("aleja Władysława S. Reymonta, Warszawa")

        requireNotNull(result)
        assertEquals(52.2829621, result.latitude, 0.0001)
        assertEquals(2, api.callCount)
    }

    @Test
    fun `falls back to dropping a leading venue-name segment when the full address is not found`() = runTest {
        val api = FakeNominatimApi(
            mapOf(
                "Podwale 25, Warszawa 00-261" to
                    listOf(NominatimResult(lat = "52.2500436", lon = "21.0094026")),
            ),
        )
        val repository = NominatimGeocodingRepository(api)

        val result = repository.geocode("Podwale 25 Kompania Piwna, Podwale 25, Warszawa 00-261")

        requireNotNull(result)
        assertEquals(52.2500436, result.latitude, 0.0001)
        assertEquals(2, api.callCount)
    }

    @Test
    fun `repeated calls for the same address are cached and do not re-hit the api`() = runTest {
        val api = FakeNominatimApi(
            mapOf("Narva mnt 5, Tallinn" to listOf(NominatimResult(lat = "59.437", lon = "24.745"))),
        )
        val repository = NominatimGeocodingRepository(api)

        repository.geocode("Narva mnt 5, Tallinn")
        repository.geocode("Narva mnt 5, Tallinn")

        assertEquals(1, api.callCount)
    }
}
