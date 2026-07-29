package com.apexcode.drivermetrics.core.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RatingExtractorTest {

    @Test
    fun `rating after passenger name with dot decimal`() {
        assertEquals(4.9, RatingExtractor.extract("Magdalena • 4.9 ★") ?: -1.0, 0.0001)
    }

    @Test
    fun `standalone rating with comma decimal`() {
        assertEquals(4.83, RatingExtractor.extract("4,83 ★") ?: -1.0, 0.0001)
    }

    @Test
    fun `returns null for text without a rating marker`() {
        assertNull(RatingExtractor.extract("Herbu Janina 3, Warszawa 02-972"))
    }
}
