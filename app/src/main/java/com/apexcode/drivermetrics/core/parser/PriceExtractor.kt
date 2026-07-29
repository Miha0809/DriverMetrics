package com.apexcode.drivermetrics.core.parser

import java.math.BigDecimal

/**
 * Shared across parsers: order screens show price as symbol+amount ("zł 39.28", "€12.50"),
 * amount+symbol ("15,90 zł"), or amount+currency-code word ("49,30 PLN", seen on Uber). Decimal
 * separator varies (dot or comma) regardless of locale, so both are accepted everywhere.
 */
object PriceExtractor {
    private val SYMBOL_THEN_AMOUNT = Regex("""(€|\$|£|zł|грн)\s?(\d+(?:[.,]\d{1,2})?)""")
    private val AMOUNT_THEN_SYMBOL = Regex("""(\d+(?:[.,]\d{1,2})?)\s?(€|\$|£|zł|грн)""")
    private val AMOUNT_THEN_CODE = Regex("""(\d+(?:[.,]\d{1,2})?)\s?(PLN|EUR|USD|UAH|GBP)\b""")

    private val CURRENCY_SYMBOLS = mapOf(
        "€" to "EUR",
        "$" to "USD",
        "£" to "GBP",
        "zł" to "PLN",
        "грн" to "UAH",
    )

    fun extract(text: String): Pair<BigDecimal, String>? {
        SYMBOL_THEN_AMOUNT.find(text)?.let { match ->
            val amount = match.groupValues[2].replace(',', '.').toBigDecimalOrNull()
            if (amount != null) return amount to (CURRENCY_SYMBOLS[match.groupValues[1]] ?: match.groupValues[1])
        }
        AMOUNT_THEN_CODE.find(text)?.let { match ->
            val amount = match.groupValues[1].replace(',', '.').toBigDecimalOrNull()
            if (amount != null) return amount to match.groupValues[2].uppercase()
        }
        AMOUNT_THEN_SYMBOL.find(text)?.let { match ->
            val amount = match.groupValues[1].replace(',', '.').toBigDecimalOrNull()
            if (amount != null) return amount to (CURRENCY_SYMBOLS[match.groupValues[2]] ?: match.groupValues[2])
        }
        return null
    }
}
