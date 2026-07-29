package com.apexcode.drivermetrics.parser.bolt

import android.view.accessibility.AccessibilityNodeInfo
import com.apexcode.drivermetrics.core.model.TaxiOrder
import com.apexcode.drivermetrics.core.parser.OrderParser
import com.apexcode.drivermetrics.core.parser.PriceExtractor
import com.apexcode.drivermetrics.core.parser.RatingExtractor
import com.apexcode.drivermetrics.core.parser.RouteLegExtractor
import com.apexcode.drivermetrics.core.parser.collectAllText
import com.apexcode.drivermetrics.core.parser.containsActionButton
import com.apexcode.drivermetrics.core.parser.isAddressLike
import java.time.Instant
import javax.inject.Inject

/**
 * Heuristic, resource-ID-free parser for the Bolt Driver app (package ee.mtakso.driver).
 *
 * Tuned against real order-screen captures: a "zł 39.28 (NET, tax included)" price line, a
 * driver name+rating line, then two "X min • Y km" leg summaries (doїзд, then the trip) each
 * immediately followed by that leg's address, and Decline/Confirm buttons. Still text/regex
 * based rather than resource-ID based, since Bolt doesn't expose stable IDs for these fields and
 * the exact layout may still drift between app versions/screen variants.
 */
class BoltParser @Inject constructor() : OrderParser {

    override val sourcePackageNames = setOf("ee.mtakso.driver")

    override fun canParse(root: AccessibilityNodeInfo): Boolean {
        val texts = root.collectAllText()
        // Requiring the Accept/Decline buttons (not just a price) means canParse() — and so the
        // overlay — goes false the moment the driver accepts/declines and Bolt moves off this
        // screen, rather than lingering on whatever screen comes next.
        return texts.any { PriceExtractor.extract(it) != null } && containsActionButton(texts)
    }

    override fun parse(root: AccessibilityNodeInfo): TaxiOrder? =
        parseFromTexts(root.collectAllText())

    /**
     * Pure text-in, order-out extraction — kept separate from AccessibilityNodeInfo traversal
     * so the regex/heuristics can be unit tested directly against captured text fixtures
     * without needing Robolectric or a fake node tree.
     */
    internal fun parseFromTexts(texts: List<String>): TaxiOrder? {
        val (price, currency) = texts.firstNotNullOfOrNull { PriceExtractor.extract(it) } ?: return null

        val legs = RouteLegExtractor.extractLegs(texts, ::isAddressLike)
        val pickupLeg = legs.getOrNull(0)
        val tripLeg = legs.getOrNull(1)

        // Fallback for screen variants without leg summaries: just take addresses in order.
        val addressFallback = texts.filter(::isAddressLike)

        return TaxiOrder(
            pickupAddress = pickupLeg?.address ?: addressFallback.getOrNull(0),
            pickupLatLng = null,
            dropoffAddress = tripLeg?.address ?: addressFallback.getOrNull(1),
            dropoffLatLng = null,
            price = price,
            currency = currency,
            sourceApplication = "bolt",
            timestamp = Instant.now(),
            pickupEta = pickupLeg?.leg,
            trip = tripLeg?.leg,
            passengerRating = texts.firstNotNullOfOrNull(RatingExtractor::extract),
        )
    }
}
