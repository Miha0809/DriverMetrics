package com.apexcode.drivermetrics.core.parser

private const val ADDRESS_MIN_LENGTH = 6
private const val RATING_MARKER = "★"

// Accept/decline/confirm labels across the locales observed in real captures (EN/UK/RU/PL).
// Matched as substrings, so e.g. "akcept" also covers "Akceptuję"/"Akceptować".
private val ACTION_LABELS = listOf(
    "accept", "decline", "confirm",
    "прийняти", "відхилити", "підтвердити",
    "принять", "отклонить", "подтвердить",
    "potwierdź", "odrzuć", "akcept",
)

/**
 * Shared across parsers. Deliberately conservative: a false negative just means an address is
 * missing from the overlay, while a false positive could mislabel a price/rating/button as an
 * address, so every known non-address shape (price, route-leg summary, rating, action button)
 * is excluded explicitly rather than relying on length/digit heuristics alone.
 *
 * Doesn't require a digit: real captures show some addresses without a house number (e.g. Uber
 * showing "aleja Władysława S. Reymonta, Варшава"). A trailing ", <city>" is just as strong a
 * signal, so either a digit or a comma qualifies.
 */
fun isAddressLike(text: String): Boolean {
    if (text.length < ADDRESS_MIN_LENGTH) return false
    if (text.contains(RATING_MARKER)) return false
    if (PriceExtractor.extract(text) != null) return false
    if (RouteLegExtractor.containsLegToken(text)) return false
    if (ACTION_LABELS.any { text.contains(it, ignoreCase = true) }) return false
    if (!text.any(Char::isLetter)) return false
    return text.contains(',') || text.any(Char::isDigit)
}

/**
 * True while an order is actually being offered (Accept/Decline-style buttons visible). Used as
 * part of canParse() so the overlay only shows while the decision is pending: once the driver
 * accepts/declines and the app moves to a different screen, these labels disappear and the
 * order stops being "parseable", which is what makes the overlay disappear with it.
 */
fun containsActionButton(texts: List<String>): Boolean =
    ACTION_LABELS.any { label -> texts.any { it.contains(label, ignoreCase = true) } }
