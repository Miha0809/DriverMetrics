package com.apexcode.drivermetrics.pipeline

import android.util.Log
import com.apexcode.drivermetrics.core.model.MapRoute
import com.apexcode.drivermetrics.core.model.RouteInfo
import com.apexcode.drivermetrics.core.model.TaxiOrder
import com.apexcode.drivermetrics.core.model.settings.AggregatorId
import com.apexcode.drivermetrics.core.model.settings.AggregatorSettings
import com.apexcode.drivermetrics.core.model.settings.RouteDisplayMode
import com.apexcode.drivermetrics.core.model.settings.StandardDisplayOptions
import com.apexcode.drivermetrics.core.model.settings.isDisplayOptionEnabled
import com.apexcode.drivermetrics.geocoding.GeocodingRepository
import com.apexcode.drivermetrics.location.CurrentLocationProvider
import com.apexcode.drivermetrics.metrics.MetricsCalculator
import com.apexcode.drivermetrics.overlay.OverlayController
import com.apexcode.drivermetrics.routing.RoutingRepository
import com.apexcode.drivermetrics.settings.AggregatorSettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TEMPORARY: distance/duration for the €/km and €/hr metrics come straight off the order screen
 * (order.trip = the course itself, order.pickupEta = doїзд to the client) rather than an
 * external routing service — geocoding proved too slow/unreliable to drive the numbers
 * themselves. Geocoding + OSRM are still used, but only to draw the route on the mini-map, which
 * doesn't affect the metrics already shown. Swap back to a routed source of truth for the
 * metrics once a paid/self-hosted routing service is wired in.
 *
 * Collects [CurrentOrderRepository.currentOrder] with collectLatest rather than reacting to
 * one-off calls: the moment the order changes (accepted, declined, expired, or a different order
 * replaces it), any in-flight geocode/route work for the previous order is cancelled outright, so
 * it can never call OverlayController.show() again after the overlay was already told to hide —
 * the overlay only ever exists while there is a current order.
 */
@Singleton
class OrderOrchestrator @Inject constructor(
    private val currentOrderRepository: CurrentOrderRepository,
    private val geocodingRepository: GeocodingRepository,
    private val routingRepository: RoutingRepository,
    private val overlayController: OverlayController,
    private val aggregatorSettingsRepository: AggregatorSettingsRepository,
    private val currentLocationProvider: CurrentLocationProvider,
) {

    fun start(scope: CoroutineScope) {
        scope.launch {
            currentOrderRepository.currentOrder.collectLatest { order ->
                if (order == null) {
                    overlayController.hide()
                } else {
                    present(order)
                }
            }
        }
    }

    private suspend fun present(order: TaxiOrder) {
        try {
            val trip = order.trip
            if (trip == null) {
                Log.w(TAG, "No trip distance/duration found on screen for ${order.sourceApplication} order")
                overlayController.hide()
                return
            }

            val aggregatorId = AggregatorId.fromSourceApplication(order.sourceApplication)
            val settings = aggregatorId?.let { aggregatorSettingsRepository.settingsFor(it).first() }
                ?: AggregatorSettings()

            val tripRoute = RouteInfo(distanceKm = trip.distanceKm, durationMin = trip.durationMin)
            val pickupRoute = order.pickupEta?.let { RouteInfo(it.distanceKm, it.durationMin) }
            val metrics = MetricsCalculator.compute(
                order = order,
                tripRoute = tripRoute,
                pickupRoute = pickupRoute,
                evaluationCriteria = settings.evaluationCriteria,
                filterRules = settings.filterRules,
            )

            // Displayed distance/duration are the whole order — the доїзд to the client plus the
            // paid trip — not just the trip, matching how €/год and €/км are already computed.
            val displayRoute = RouteInfo(
                distanceKm = (pickupRoute?.distanceKm ?: 0.0) + tripRoute.distanceKm,
                durationMin = (pickupRoute?.durationMin ?: 0.0) + tripRoute.durationMin,
            )

            val showStats = settings.isDisplayOptionEnabled(StandardDisplayOptions.SHOW_STATS)
            val showMap = settings.isDisplayOptionEnabled(StandardDisplayOptions.SHOW_MAP)

            overlayController.show(order, metrics, displayRoute, showStats = showStats, showMap = showMap)

            // Best-effort, map only — doesn't touch the metrics already shown above. Skipped
            // outright when the driver turned the map off, so it doesn't geocode/route for
            // nothing.
            if (showMap) {
                val mapRoute = fetchMapRoute(order, settings.routeDisplayMode)
                overlayController.show(order, metrics, displayRoute, mapRoute, showStats, showMap)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed to process order from ${order.sourceApplication}", e)
            overlayController.hide()
        }
    }

    /**
     * Pickup -> dropoff by default — the "доїзд" to the client is counted in the €/год metric via
     * [TaxiOrder.pickupEta] regardless, but only routed/drawn here too when [routeDisplayMode] is
     * DRIVER_TO_PICKUP_AND_DROPOFF (9.2), and only if the driver's current location is known.
     *
     * Pickup/dropoff geocoding and the two OSRM legs are each independent of one another, so
     * they're fired concurrently via [async] rather than awaited one at a time — sequentially
     * they added up to a very noticeable 2-3s before the map appeared, since every step is a real
     * network round trip.
     */
    private suspend fun fetchMapRoute(order: TaxiOrder, routeDisplayMode: RouteDisplayMode): MapRoute? = coroutineScope {
        val pickupDeferred = async { order.pickupLatLng ?: order.pickupAddress?.let { geocodingRepository.geocode(it) } }
        val dropoffDeferred = async { order.dropoffLatLng ?: order.dropoffAddress?.let { geocodingRepository.geocode(it) } }
        val pickup = pickupDeferred.await()
        val dropoff = dropoffDeferred.await()
        if (pickup == null || dropoff == null) return@coroutineScope null

        val driverLocation = if (routeDisplayMode == RouteDisplayMode.DRIVER_TO_PICKUP_AND_DROPOFF) {
            currentLocationProvider.getLastKnownLocation()
        } else {
            null
        }

        val tripGeometryDeferred = async { routingRepository.getRoute(pickup, dropoff)?.geometry.orEmpty() }
        val pickupLegGeometryDeferred = driverLocation?.let { loc ->
            async { routingRepository.getRoute(loc, pickup)?.geometry.orEmpty() }
        }

        MapRoute(
            pickup = pickup,
            dropoff = dropoff,
            geometry = pickupLegGeometryDeferred?.await().orEmpty() + tripGeometryDeferred.await(),
            driverLocation = driverLocation,
        )
    }

    private companion object {
        const val TAG = "OrderOrchestrator"
    }
}
