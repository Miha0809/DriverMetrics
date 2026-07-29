package com.apexcode.drivermetrics.core.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class PriceExtractorTest {

    @Test
    fun `symbol before amount with dot decimal`() {
        assertEquals(BigDecimal("39.28") to "PLN", PriceExtractor.extract("zł 39.28 (NET, tax included)"))
    }

    @Test
    fun `symbol before amount euro`() {
        assertEquals(BigDecimal("12.50") to "EUR", PriceExtractor.extract("€12.50"))
    }

    @Test
    fun `amount before currency code with comma decimal`() {
        assertEquals(BigDecimal("49.30") to "PLN", PriceExtractor.extract("49,30 PLN"))
    }

    @Test
    fun `amount before symbol with comma decimal`() {
        assertEquals(BigDecimal("15.90") to "PLN", PriceExtractor.extract("15,90 zł"))
    }

    @Test
    fun `returns null for text without a price`() {
        assertNull(PriceExtractor.extract("Herbu Janina 3, Warszawa 02-972"))
    }
}
