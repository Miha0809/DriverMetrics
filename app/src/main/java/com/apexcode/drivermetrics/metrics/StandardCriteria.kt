package com.apexcode.drivermetrics.metrics

/** The evaluation criteria (9.3) shipped today. Extend by adding another object to [ALL]. */
object StandardCriteria {
    const val PRICE_PER_HOUR = "price_per_hour"
    const val PRICE_PER_KM = "price_per_km"
    const val PASSENGER_RATING = "passenger_rating"
    const val ORDER_PRICE = "order_price"

    val ALL: List<EvaluationCriterion> = listOf(
        object : EvaluationCriterion {
            override val id = PRICE_PER_HOUR
            override val direction = CriterionDirection.HIGHER_IS_BETTER
            override fun valueOf(context: EvaluationContext) = context.pricePerHour
        },
        object : EvaluationCriterion {
            override val id = PRICE_PER_KM
            override val direction = CriterionDirection.HIGHER_IS_BETTER
            override fun valueOf(context: EvaluationContext) = context.pricePerKm
        },
        object : EvaluationCriterion {
            override val id = PASSENGER_RATING
            override val direction = CriterionDirection.HIGHER_IS_BETTER
            override fun valueOf(context: EvaluationContext) = context.order.passengerRating
        },
        object : EvaluationCriterion {
            override val id = ORDER_PRICE
            override val direction = CriterionDirection.HIGHER_IS_BETTER
            override fun valueOf(context: EvaluationContext) = context.effectivePrice
        },
    )
}
