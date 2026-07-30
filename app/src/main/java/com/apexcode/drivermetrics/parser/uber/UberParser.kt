package com.apexcode.drivermetrics.parser.uber

import com.apexcode.drivermetrics.core.model.TaxiOrder
import com.apexcode.drivermetrics.core.parser.OrderParser
import com.apexcode.drivermetrics.core.parser.PriceExtractor
import com.apexcode.drivermetrics.core.parser.RatingExtractor
import com.apexcode.drivermetrics.core.parser.RouteLegExtractor
import com.apexcode.drivermetrics.core.parser.containsActionButton
import com.apexcode.drivermetrics.core.parser.isAddressLike
import java.time.Instant
import javax.inject.Inject

/**
 * Heuristic, resource-ID-free parser for the Uber Driver app (package com.ubercab.driver).
 *
 * Tuned against real order-screen captures in multiple device locales (RU/UK observed): price
 * as "49,30 PLN" (amount + currency-code word, comma decimal), then a rating, then "X min (Y
 * km)"/"X хв (Y км)" leg summaries for pickup and trip, each followed by its address. Shares
 * extraction logic with BoltParser via core/parser — the two screens turned out to have almost
 * the same shape, just with different price/leg text formats.
 */
class UberParser @Inject constructor() : OrderParser {

    override val sourcePackageNames = setOf("com.ubercab.driver")

    override fun canParse(texts: List<String>): Boolean {
        // Requiring the Accept/Decline buttons (not just a price) means canParse() — and so the
        // overlay — goes false the moment the driver accepts/declines and Uber moves off this
        // screen, rather than lingering on whatever screen comes next.
        return texts.any { PriceExtractor.extract(it) != null } && containsActionButton(texts)
    }

    override fun parse(texts: List<String>): TaxiOrder? = parseFromTexts(texts)

    internal fun parseFromTexts(texts: List<String>): TaxiOrder? {
        val (price, currency) = texts.firstNotNullOfOrNull { PriceExtractor.extract(it) } ?: return null

        val legs = RouteLegExtractor.extractLegs(texts, ::isAddressLike)
        val pickupLeg = legs.getOrNull(0)
        val tripLeg = legs.getOrNull(1)

        val addressFallback = texts.filter(::isAddressLike)

        return TaxiOrder(
            pickupAddress = pickupLeg?.address ?: addressFallback.getOrNull(0),
            pickupLatLng = null,
            dropoffAddress = tripLeg?.address ?: addressFallback.getOrNull(1),
            dropoffLatLng = null,
            price = price,
            currency = currency,
            sourceApplication = "uber",
            timestamp = Instant.now(),
            pickupEta = pickupLeg?.leg,
            trip = tripLeg?.leg,
            passengerRating = texts.firstNotNullOfOrNull(RatingExtractor::extract),
        )
    }
}
