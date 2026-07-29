package com.apexcode.drivermetrics.core.parser

/**
 * Shared across parsers: order screens show the passenger's rating next to a "★" marker, either
 * on its own line ("4,83 ★", seen on Uber) or after the passenger's name ("Magdalena • 4.9 ★",
 * seen on Bolt). Decimal separator varies (dot or comma) regardless of locale, same as
 * [PriceExtractor].
 */
object RatingExtractor {
    private val RATING_REGEX = Regex("""(\d(?:[.,]\d+)?)\s*★""")

    fun extract(text: String): Double? =
        RATING_REGEX.find(text)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
}
