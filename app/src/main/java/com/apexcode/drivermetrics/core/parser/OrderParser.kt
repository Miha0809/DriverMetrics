package com.apexcode.drivermetrics.core.parser

import android.view.accessibility.AccessibilityNodeInfo
import com.apexcode.drivermetrics.core.model.TaxiOrder

/**
 * One implementation per taxi aggregator (Bolt, Uber, FreeNow, Uklon, ...). Adding a new
 * aggregator means adding a new class here and binding it with @IntoSet in a Hilt module —
 * no other file needs to change.
 */
interface OrderParser {
    val sourcePackageNames: Set<String>

    fun canParse(root: AccessibilityNodeInfo): Boolean

    /** Returns null if no order is currently visible in this tree. */
    fun parse(root: AccessibilityNodeInfo): TaxiOrder?
}
