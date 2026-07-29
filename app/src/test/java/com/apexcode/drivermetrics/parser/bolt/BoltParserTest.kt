package com.apexcode.drivermetrics.parser.bolt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class BoltParserTest {

    private val parser = BoltParser()

    @Test
    fun `parses a real Bolt order screen where each leg is one combined text node`() {
        val order = parser.parseFromTexts(
            listOf(
                "Bolt",
                "zł 39.28 (NET, tax included)",
                "Magdalena • 4.9 ★",
                "5 min • 2 km",
                "Herbu Janina 3, Warszawa 02-972",
                "30 min • 13.3 km",
                "Podwale 25 Kompania Piwna, Podwale 25, Warszawa 00-261",
                "Decline",
                "Confirm",
            ),
        )
        requireNotNull(order)
        assertEquals(BigDecimal("39.28"), order.price)
        assertEquals("PLN", order.currency)
        assertEquals("Herbu Janina 3, Warszawa 02-972", order.pickupAddress)
        assertEquals("Podwale 25 Kompania Piwna, Podwale 25, Warszawa 00-261", order.dropoffAddress)
        assertEquals(2.0, order.pickupEta?.distanceKm ?: -1.0, 0.0001)
        assertEquals(5.0, order.pickupEta?.durationMin ?: -1.0, 0.0001)
        assertEquals(13.3, order.trip?.distanceKm ?: -1.0, 0.0001)
        assertEquals(30.0, order.trip?.durationMin ?: -1.0, 0.0001)
        assertEquals(4.9, order.passengerRating ?: -1.0, 0.0001)
    }

    @Test
    fun `parses the same screen when duration and distance are split across sibling nodes`() {
        val order = parser.parseFromTexts(
            listOf(
                "Bolt", "zł 39.28", "(NET, tax included)", "Magdalena", "4.9 ★",
                "5 min", "2 km", "Herbu Janina 3, Warszawa 02-972",
                "30 min", "13.3 km", "Podwale 25 Kompania Piwna, Podwale 25, Warszawa 00-261",
                "Decline", "Confirm",
            ),
        )
        requireNotNull(order)
        assertEquals(BigDecimal("39.28"), order.price)
        assertEquals("PLN", order.currency)
        assertEquals("Herbu Janina 3, Warszawa 02-972", order.pickupAddress)
        assertEquals("Podwale 25 Kompania Piwna, Podwale 25, Warszawa 00-261", order.dropoffAddress)
        assertEquals(2.0, order.pickupEta?.distanceKm ?: -1.0, 0.0001)
        assertEquals(13.3, order.trip?.distanceKm ?: -1.0, 0.0001)
    }

    @Test
    fun `extracts a EUR price with symbol before amount`() {
        val order = parser.parseFromTexts(
            listOf("New order", "€12.50", "Vabaduse väljak 7, Tallinn", "Tartu mnt 47, Tallinn"),
        )
        requireNotNull(order)
        assertEquals(BigDecimal("12.50"), order.price)
        assertEquals("EUR", order.currency)
    }

    @Test
    fun `returns null when no price token is present`() {
        val order = parser.parseFromTexts(listOf("Narva mnt 5, Tallinn", "Pärnu mnt 102, Tallinn"))
        assertNull(order)
    }

    @Test
    fun `button labels and driver rating are not mistaken for addresses`() {
        val order = parser.parseFromTexts(
            listOf("€8.00", "Magdalena • 4.9 ★", "Accept order 1", "Narva mnt 5, Tallinn"),
        )
        requireNotNull(order)
        assertEquals("Narva mnt 5, Tallinn", order.pickupAddress)
        assertNull(order.dropoffAddress)
    }
}
