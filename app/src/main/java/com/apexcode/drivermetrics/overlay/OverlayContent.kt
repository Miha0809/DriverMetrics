package com.apexcode.drivermetrics.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.apexcode.drivermetrics.core.model.MapRoute
import com.apexcode.drivermetrics.core.model.OrderMetrics
import com.apexcode.drivermetrics.core.model.ProfitabilityLevel
import com.apexcode.drivermetrics.core.model.RouteInfo
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlin.math.roundToInt

private val CARD_MARGIN = 12.dp
private val CONTENT_PADDING = 12.dp
private val HEADER_HEIGHT = 28.dp
private val CONTENT_SPACING = 8.dp
private const val DRIVER_MARKER_COLOR = 0xFF1565C0.toInt()
private const val PICKUP_MARKER_COLOR = 0xFF2E7D32.toInt()
private const val DROPOFF_MARKER_COLOR = 0xFFC62828.toInt()

/**
 * Just tall enough for the Card's margin/padding plus one line of stats — no map. Used by
 * OverlayController to shrink the overlay *window* itself (not just its content) when the map is
 * turned off, so the window's bottom edge sits right under the stats instead of leaving empty
 * space; its top edge (set via WindowManager gravity/y, not by this height) never moves.
 */
val COMPACT_OVERLAY_HEIGHT: Dp = CARD_MARGIN * 2 + CONTENT_PADDING * 2 + HEADER_HEIGHT

// zoomToBoundingBox's border, in px, around the route's bounding box — WIDE leaves margin on
// every side, CLOSE fills the map with the route edge-to-edge (how it looked before the map
// zoom setting was added).
private const val WIDE_ZOOM_BORDER_PX = 128
private const val CLOSE_ZOOM_BORDER_PX = 64

@Composable
fun OverlayContent(
    metrics: OrderMetrics,
    route: RouteInfo,
    mapRoute: MapRoute? = null,
    wideMapZoom: Boolean = true,
    showStats: Boolean = true,
    showMap: Boolean = true,
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(CARD_MARGIN),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Spacer(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(profitabilityColor(metrics.profitability)),
            )
            // BoxWithConstraints gives the map an explicit height (maxHeight - header) instead
            // of a Column weight(1f): the embedded osmdroid MapView (a real Android View, not
            // pure Compose) was ending up measured/drawn at the full card height when sized via
            // weight in this WindowManager-hosted (non-Activity) window, covering the header —
            // an explicit height plus clipToBounds keeps it inside its own area.
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(CONTENT_PADDING),
            ) {
                val headerHeight = if (showStats) HEADER_HEIGHT else 0.dp
                val spacing = if (showStats && showMap) CONTENT_SPACING else 0.dp
                val mapHeight = (maxHeight - headerHeight - spacing).coerceAtLeast(0.dp)

                Column(modifier = Modifier.fillMaxSize()) {
                    if (showStats) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(HEADER_HEIGHT)
                                .zIndex(1f),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "%.1f €/год  •  %.2f €/км  •  %.1f км  •  %.0f хв".format(
                                    metrics.pricePerHour,
                                    metrics.pricePerKm,
                                    route.distanceKm,
                                    route.durationMin,
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(modifier = Modifier.height(spacing))
                    }
                    if (showMap) {
                        RouteMiniMap(
                            mapRoute = mapRoute,
                            wideMapZoom = wideMapZoom,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(mapHeight)
                                .clipToBounds(),
                        )
                    }
                }
            }
        }
    }
}

private fun profitabilityColor(level: ProfitabilityLevel): Color = when (level) {
    ProfitabilityLevel.GREEN -> Color(0xFF2E7D32)
    ProfitabilityLevel.YELLOW -> Color(0xFFF9A825)
    ProfitabilityLevel.RED -> Color(0xFFC62828)
}

/**
 * Draws pickup (client's location) -> dropoff by default; also draws the driver's current
 * location -> pickup leg when [MapRoute.driverLocation] is set (9.2, DRIVER_TO_PICKUP_AND_DROPOFF
 * route display mode).
 */
@Composable
private fun RouteMiniMap(mapRoute: MapRoute?, wideMapZoom: Boolean = true, modifier: Modifier = Modifier) {
    val endpoints = listOfNotNull(mapRoute?.driverLocation, mapRoute?.pickup, mapRoute?.dropoff)
    if (mapRoute == null || listOfNotNull(mapRoute.pickup, mapRoute.dropoff).size < 2) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "Карта недоступна",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(false)
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                isClickable = false
            }
        },
        update = { mapView ->
            mapView.overlays.clear()

            val points = mapRoute.geometry.ifEmpty { endpoints }
            mapView.overlays.add(
                Polyline().apply { setPoints(points.map { GeoPoint(it.latitude, it.longitude) }) },
            )

            listOfNotNull(
                mapRoute.driverLocation?.let { "A" to it },
                mapRoute.pickup?.let { "B" to it },
                mapRoute.dropoff?.let { "C" to it },
            ).forEach { (label, point) ->
                mapView.overlays.add(
                    Marker(mapView).apply {
                        position = GeoPoint(point.latitude, point.longitude)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = label
                        val color = when (label) {
                            "A" -> DRIVER_MARKER_COLOR
                            "B" -> PICKUP_MARKER_COLOR
                            else -> DROPOFF_MARKER_COLOR
                        }
                        icon = pinIcon(mapView.context, color)
                    },
                )
            }

            // zoomToBoundingBox needs the view to already have a real size — post so it runs
            // after this layout pass instead of possibly running against a 0x0 view. Framed
            // against points+endpoints (not just points) so the driver marker stays in frame
            // even on the best-effort path where the doїзд leg's geometry failed to fetch.
            mapView.post {
                try {
                    val box = BoundingBox.fromGeoPoints((points + endpoints).map { GeoPoint(it.latitude, it.longitude) })
                    val borderPx = if (wideMapZoom) WIDE_ZOOM_BORDER_PX else CLOSE_ZOOM_BORDER_PX
                    mapView.zoomToBoundingBox(box, false, borderPx)
                } catch (e: Exception) {
                    // Best-effort framing; leave the map at its default view if this fails.
                }
            }
            mapView.invalidate()
        },
    )
}

/**
 * Draws a small solid-filled dot in [colorInt] ourselves rather than tinting osmdroid's built-in
 * default marker drawable — Marker.getIcon() returns null until an icon has been explicitly set,
 * so `icon?.mutate()?.apply { setTint(...) }` silently tinted nothing and both markers fell back
 * to the same untinted default appearance.
 */
private fun pinIcon(context: Context, colorInt: Int): Drawable {
    val diameter = (14 * context.resources.displayMetrics.density).roundToInt()
    val bitmap = Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colorInt }
    Canvas(bitmap).drawCircle(diameter / 2f, diameter / 2f, diameter / 2f, paint)
    return BitmapDrawable(context.resources, bitmap)
}
