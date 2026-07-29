package com.apexcode.drivermetrics.parser.uber

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class UberParserTest {

    private val parser = UberParser()

    @Test
    fun `parses a real Uber order screen in Russian locale`() {
        val order = parser.parseFromTexts(
            listOf(
                "UberX",
                "49,30 PLN",
                "4,83 ★",
                "С вычетом сервисного сбора, в т.ч. НДС",
                "В 8 мин. (3.1 км) от вас",
                "aleja Władysława S. Reymonta, Варшава",
                "Поездка: 49 мин. (31.2 км)",
                "ulica gen. Wiktora Thommee 1A, Nowy Dwór Mazowiecki",
                "Долгая поездка (более 35 мин.)",
                "Принять",
            ),
        )
        requireNotNull(order)
        assertEquals(BigDecimal("49.30"), order.price)
        assertEquals("PLN", order.currency)
        assertEquals("aleja Władysława S. Reymonta, Варшава", order.pickupAddress)
        assertEquals("ulica gen. Wiktora Thommee 1A, Nowy Dwór Mazowiecki", order.dropoffAddress)
        assertEquals(3.1, order.pickupEta?.distanceKm ?: -1.0, 0.0001)
        assertEquals(8.0, order.pickupEta?.durationMin ?: -1.0, 0.0001)
        assertEquals(31.2, order.trip?.distanceKm ?: -1.0, 0.0001)
        assertEquals(49.0, order.trip?.durationMin ?: -1.0, 0.0001)
        assertEquals(4.83, order.passengerRating ?: -1.0, 0.0001)
    }

    @Test
    fun `parses the same order in Ukrainian locale with an extra exclusivity badge`() {
        val order = parser.parseFromTexts(
            listOf(
                "UberX",
                "Ексклюзивний",
                "50,67 PLN",
                "4,83 ★",
                "Сервісний збір без податків (зокрема без ПДВ на н",
                "За 10 хв (3.7 км)",
                "aleja Władysława S. Reymonta, Варшава",
                "Поїздка: 49 хв (31.2 км)",
                "ulica gen. Wiktora Thommee 1A, Nowy Dwór Mazowiecki",
                "Тривала поїздка (понад 35 хв)",
                "Прийняти",
            ),
        )
        requireNotNull(order)
        assertEquals(BigDecimal("50.67"), order.price)
        assertEquals("PLN", order.currency)
        assertEquals("aleja Władysława S. Reymonta, Варшава", order.pickupAddress)
        assertEquals("ulica gen. Wiktora Thommee 1A, Nowy Dwór Mazowiecki", order.dropoffAddress)
        assertEquals(3.7, order.pickupEta?.distanceKm ?: -1.0, 0.0001)
        assertEquals(10.0, order.pickupEta?.durationMin ?: -1.0, 0.0001)
        assertEquals(31.2, order.trip?.distanceKm ?: -1.0, 0.0001)
        assertEquals(49.0, order.trip?.durationMin ?: -1.0, 0.0001)
    }
}
