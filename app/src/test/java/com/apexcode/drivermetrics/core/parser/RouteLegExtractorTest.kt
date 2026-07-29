package com.apexcode.drivermetrics.core.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RouteLegExtractorTest {

    @Test
    fun `reads two legs with combined duration-distance nodes and following addresses`() {
        val legs = RouteLegExtractor.extractLegs(
            listOf(
                "5 min • 2 km", "Herbu Janina 3, Warszawa 02-972",
                "30 min • 13.3 km", "Podwale 25, Warszawa 00-261",
            ),
            ::isAddressLike,
        )
        assertEquals(2, legs.size)
        assertEquals(2.0, legs[0].leg.distanceKm, 0.0001)
        assertEquals(5.0, legs[0].leg.durationMin, 0.0001)
        assertEquals("Herbu Janina 3, Warszawa 02-972", legs[0].address)
        assertEquals(13.3, legs[1].leg.distanceKm, 0.0001)
        assertEquals("Podwale 25, Warszawa 00-261", legs[1].address)
    }

    @Test
    fun `reads legs when duration and distance are split across sibling nodes`() {
        val legs = RouteLegExtractor.extractLegs(
            listOf("8 min", "3.1 km", "Some Address, City"),
            ::isAddressLike,
        )
        assertEquals(1, legs.size)
        assertEquals(3.1, legs[0].leg.distanceKm, 0.0001)
        assertEquals(8.0, legs[0].leg.durationMin, 0.0001)
        assertEquals("Some Address, City", legs[0].address)
    }

    @Test
    fun `supports localized unit labels хв and км`() {
        val legs = RouteLegExtractor.extractLegs(
            listOf("За 10 хв (3.7 км)", "aleja Reymonta, Варшава"),
            ::isAddressLike,
        )
        assertEquals(1, legs.size)
        assertEquals(3.7, legs[0].leg.distanceKm, 0.0001)
        assertEquals(10.0, legs[0].leg.durationMin, 0.0001)
    }

    @Test
    fun `leg address is null when nothing address-like follows it`() {
        val legs = RouteLegExtractor.extractLegs(listOf("5 min • 2 km", "Confirm"), ::isAddressLike)
        assertEquals(1, legs.size)
        assertNull(legs[0].address)
    }
}
