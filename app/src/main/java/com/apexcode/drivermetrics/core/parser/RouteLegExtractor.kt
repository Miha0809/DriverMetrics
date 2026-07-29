package com.apexcode.drivermetrics.core.parser

import com.apexcode.drivermetrics.core.model.RouteLeg

data class ParsedLeg(val leg: RouteLeg, val address: String?)

/**
 * Shared across parsers: Bolt/Uber order screens show "X min • Y km"-style summaries for both
 * the pickup leg ("доїзд") and the trip itself, in that order, each immediately followed by its
 * address. The unit labels are locale-dependent (min/хв/мин), so this matches on digits+unit
 * rather than any fixed word, and tolerates duration/distance being either one combined text
 * node or two separate sibling nodes (both were observed across the two apps' screenshots).
 */
object RouteLegExtractor {
    private val DURATION_REGEX = Regex("""(\d+)\s*(?:min|хв|мин)\.?""", RegexOption.IGNORE_CASE)
    private val DISTANCE_REGEX = Regex("""(\d+(?:[.,]\d+)?)\s*(?:km|км)""", RegexOption.IGNORE_CASE)

    fun containsLegToken(text: String): Boolean =
        DURATION_REGEX.containsMatchIn(text) || DISTANCE_REGEX.containsMatchIn(text)

    /** @param isAddressLike used to find each leg's address among the texts that follow it. */
    fun extractLegs(texts: List<String>, isAddressLike: (String) -> Boolean): List<ParsedLeg> {
        val legs = mutableListOf<ParsedLeg>()
        var pendingDuration: Double? = null
        var pendingDistance: Double? = null

        var index = 0
        while (index < texts.size) {
            val text = texts[index]
            DURATION_REGEX.find(text)?.groupValues?.get(1)?.toDoubleOrNull()?.let { pendingDuration = it }
            DISTANCE_REGEX.find(text)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
                ?.let { pendingDistance = it }

            val duration = pendingDuration
            val distance = pendingDistance
            if (duration != null && distance != null) {
                val remaining = texts.subList(index + 1, texts.size)
                val address = remaining.firstOrNull(isAddressLike)
                legs.add(ParsedLeg(RouteLeg(distanceKm = distance, durationMin = duration), address))
                pendingDuration = null
                pendingDistance = null
                if (address != null) {
                    index += 1 + remaining.indexOf(address)
                }
            }
            index++
        }
        return legs
    }
}
