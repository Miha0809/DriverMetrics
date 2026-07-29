package com.apexcode.drivermetrics.core.parser

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Driver apps rarely expose stable resource IDs for order details, and their layouts change
 * often. These helpers walk the tree defensively by visible text instead of by ID, so parsers
 * degrade to "no order found" rather than crashing when the UI shifts.
 */

private const val MAX_DEPTH = 40

fun AccessibilityNodeInfo.collectAllText(): List<String> {
    val result = mutableListOf<String>()
    fun visit(node: AccessibilityNodeInfo, depth: Int) {
        if (depth > MAX_DEPTH) return
        node.text?.toString()?.takeIf { it.isNotBlank() }?.let(result::add)
        node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let(result::add)
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { visit(it, depth + 1) }
        }
    }
    visit(this, 0)
    return result
}

fun AccessibilityNodeInfo.findAllMatching(predicate: (String) -> Boolean): List<String> =
    collectAllText().filter(predicate)
