package com.apexcode.drivermetrics.geocoding

import android.os.SystemClock
import android.util.Log
import com.apexcode.drivermetrics.core.model.LatLng
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * Free/default GeocodingRepository backed by the public Nominatim instance. Nominatim's usage
 * policy recommends ~1 request/sec for sustained traffic; [MIN_INTERVAL_MS] is set lower than
 * that on purpose — this app only ever fires a couple of requests per order (a few times an
 * hour, not sustained), so a short burst well under the strict 1/sec spacing is a reasonable
 * trade for a map that doesn't take seconds to appear. It's the dominant cause of mini-map
 * latency (it gates the two geocode calls a single order needs — pickup and dropoff — even when
 * they're fired concurrently), so keep this as low as still feels like a good-faith reading of
 * "not sustained traffic" rather than raising it back up by default. Bump it back toward 1000+ms
 * if usage patterns change (e.g. many orders/minute) or Nominatim starts rate-limiting responses.
 * Results are cached for the process lifetime since a driver re-sees the same handful of
 * pickup/dropoff addresses repeatedly during a shift.
 */
class NominatimGeocodingRepository @Inject constructor(
    private val api: NominatimApi,
) : GeocodingRepository {

    private val cache = ConcurrentHashMap<String, LatLng>()
    private val throttleMutex = Mutex()
    private var lastRequestElapsedMs = 0L

    override suspend fun geocode(address: String): LatLng? {
        cache[address]?.let { return it }

        for (candidate in candidateQueries(address)) {
            val result = search(candidate)
            if (result != null) {
                cache[address] = result
                return result
            }
        }
        return null
    }

    /**
     * Real captures showed two ways driver apps' address text trips up Nominatim's free-text
     * search, both worked around here as fallback queries tried only when the previous one
     * fails (so the extra throttled requests are only paid for addresses that actually need
     * them):
     *  - Uber includes a middle initial ("aleja Władysława S. Reymonta, Варшава") that
     *    Nominatim can't match; dropping single-letter+period tokens fixes it.
     *  - Bolt prefixes the address with a venue name that repeats the street further along
     *    ("Podwale 25 Kompania Piwna, Podwale 25, Warszawa 00-261"); dropping the leading
     *    comma-separated segment(s) fixes it.
     */
    private fun candidateQueries(address: String): List<String> {
        val candidates = LinkedHashSet<String>()
        candidates += address

        val initialsStripped = INITIAL_REGEX.replace(address, "").replace(EXTRA_SPACES_REGEX, " ").trim()
        if (initialsStripped.isNotBlank()) candidates += initialsStripped

        val segments = address.split(",").map { it.trim() }
        for (dropCount in 1 until segments.size) {
            val dropped = segments.drop(dropCount).joinToString(", ")
            if (dropped.isNotBlank()) candidates += dropped
        }

        return candidates.toList()
    }

    private suspend fun search(query: String): LatLng? {
        return try {
            throttle()
            val result = api.search(query = query).firstOrNull() ?: return null
            LatLng(latitude = result.lat.toDouble(), longitude = result.lon.toDouble())
        } catch (e: Exception) {
            Log.w(TAG, "Geocoding failed for \"$query\"", e)
            null
        }
    }

    private suspend fun throttle() = throttleMutex.withLock {
        val now = SystemClock.elapsedRealtime()
        val wait = MIN_INTERVAL_MS - (now - lastRequestElapsedMs)
        if (wait > 0) delay(wait)
        lastRequestElapsedMs = SystemClock.elapsedRealtime()
    }

    private companion object {
        const val TAG = "Geocoding"
        const val MIN_INTERVAL_MS = 200L
        val INITIAL_REGEX = Regex("""\b\p{L}\.\s*""")
        val EXTRA_SPACES_REGEX = Regex("""\s{2,}""")
    }
}
