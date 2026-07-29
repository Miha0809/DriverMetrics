package com.apexcode.drivermetrics.overlay

import com.apexcode.drivermetrics.core.model.MapRoute
import com.apexcode.drivermetrics.core.model.OrderMetrics
import com.apexcode.drivermetrics.core.model.RouteInfo
import com.apexcode.drivermetrics.core.model.TaxiOrder

internal data class OverlayState(
    val order: TaxiOrder,
    val metrics: OrderMetrics,
    val route: RouteInfo,
    val mapRoute: MapRoute? = null,
    val showStats: Boolean = true,
    val showMap: Boolean = true,
)
