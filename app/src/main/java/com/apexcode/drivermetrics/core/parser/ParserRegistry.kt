package com.apexcode.drivermetrics.core.parser

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.apexcode.drivermetrics.core.model.TaxiOrder
import javax.inject.Inject

class ParserRegistry @Inject constructor(
    private val parsers: Set<@JvmSuppressWildcards OrderParser>,
) {
    /** Never throws — a misbehaving parser is logged and treated as "no order". */
    fun parseOrder(packageName: String, root: AccessibilityNodeInfo): TaxiOrder? {
        val parser = parsers.firstOrNull { packageName in it.sourcePackageNames } ?: return null
        return try {
            val texts = root.collectAllText()
            if (parser.canParse(texts)) parser.parse(texts) else null
        } catch (e: Exception) {
            Log.w(TAG, "${parser::class.simpleName} failed to parse $packageName", e)
            null
        }
    }

    private companion object {
        const val TAG = "ParserRegistry"
    }
}
