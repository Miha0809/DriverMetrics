package com.apexcode.drivermetrics.metrics

/** The filter rules (9.5) shipped today. Extend by adding another object to [ALL]. */
object StandardFilterRules {
    const val MIN_HOURLY_RATE = "min_hourly_rate"
    const val MIN_PRICE_PER_KM = "min_price_per_km"
    const val MIN_ORDER_PRICE = "min_order_price"
    const val MIN_PASSENGER_RATING = "min_passenger_rating"
    const val MAX_DISTANCE_TO_CLIENT_KM = "max_distance_to_client_km"
    const val MAX_TIME_TO_CLIENT_MIN = "max_time_to_client_min"

    val ALL: List<FilterRule> = listOf(
        object : FilterRule {
            override val id = MIN_HOURLY_RATE
            override val direction = CriterionDirection.HIGHER_IS_BETTER
            override fun valueOf(context: EvaluationContext) = context.pricePerHour
        },
        object : FilterRule {
            override val id = MIN_PRICE_PER_KM
            override val direction = CriterionDirection.HIGHER_IS_BETTER
            override fun valueOf(context: EvaluationContext) = context.pricePerKm
        },
        object : FilterRule {
            override val id = MIN_ORDER_PRICE
            override val direction = CriterionDirection.HIGHER_IS_BETTER
            override fun valueOf(context: EvaluationContext) = context.effectivePrice
        },
        object : FilterRule {
            override val id = MIN_PASSENGER_RATING
            override val direction = CriterionDirection.HIGHER_IS_BETTER
            override fun valueOf(context: EvaluationContext) = context.order.passengerRating
        },
        object : FilterRule {
            override val id = MAX_DISTANCE_TO_CLIENT_KM
            override val direction = CriterionDirection.LOWER_IS_BETTER
            override fun valueOf(context: EvaluationContext) = context.pickupDistanceKm
        },
        object : FilterRule {
            override val id = MAX_TIME_TO_CLIENT_MIN
            override val direction = CriterionDirection.LOWER_IS_BETTER
            override fun valueOf(context: EvaluationContext) = context.pickupDurationMin
        },
    )
}
