package com.apexcode.drivermetrics.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.apexcode.drivermetrics.core.model.LatLng
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Plain LocationManager (no Play Services dependency) — used only to draw the driver -> pickup
 * leg on the mini-map (see OrderOrchestrator, RouteDisplayMode.DRIVER_TO_PICKUP_AND_DROPOFF).
 * Reads cached last-known fixes rather than requesting a fresh one, since this only runs on the
 * already-async, best-effort map path and a live GPS request would add real latency there.
 *
 * Only ACCESS_COARSE_LOCATION used to be requested, which made GPS_PROVIDER (fine accuracy)
 * permanently unusable — every reading came from the network/cell-based provider, which can be
 * off by hundreds of meters to a few kilometers, making the drawn "доїзд" leg start from visibly
 * the wrong place. Now both permissions are requested and read here, and among whatever fixes are
 * actually available, the most *accurate* one recent enough to still be trustworthy wins — not
 * just the most recent one, which could easily be a fresh-but-imprecise network fix from another
 * app while a much better GPS fix from a few minutes ago goes unused.
 */
class SystemLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : CurrentLocationProvider {

    override fun getLastKnownLocation(): LatLng? {
        val hasPermission = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (!hasPermission) return null

        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        val now = System.currentTimeMillis()
        return locationManager.allProviders
            .mapNotNull { provider ->
                try {
                    locationManager.getLastKnownLocation(provider)
                } catch (e: SecurityException) {
                    null
                }
            }
            .filter { now - it.time <= MAX_FIX_AGE_MS }
            .minByOrNull { it.accuracyOrWorstCase() }
            ?.let { LatLng(it.latitude, it.longitude) }
    }

    private fun hasPermission(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /** Locations without a reported accuracy are treated as the least trustworthy, not the most. */
    private fun Location.accuracyOrWorstCase(): Float = if (hasAccuracy()) accuracy else Float.MAX_VALUE

    private companion object {
        // Older than this and a fix is more likely to actively mislead (driver has moved on)
        // than to help, given this app never requests an active/fresh update.
        const val MAX_FIX_AGE_MS = 5 * 60 * 1000L
    }
}
