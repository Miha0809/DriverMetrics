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

    // Take the already-collected text rather than the raw tree so ParserRegistry can walk the
    // tree (an AccessibilityNodeInfo traversal — cross-process IPC per node) exactly once per
    // event instead of once for canParse and again for parse.
    fun canParse(texts: List<String>): Boolean

    /** Returns null if no order is currently visible in this tree. */
    fun parse(texts: List<String>): TaxiOrder?
}
