package com.apexcode.drivermetrics.ui.settings

import com.apexcode.drivermetrics.metrics.StandardCriteria
import com.apexcode.drivermetrics.metrics.StandardFilterRules

/**
 * Presentation metadata (label/unit) for the settings screen only — deliberately kept out of the
 * `metrics` package so that package stays pure business logic. Adding a new criterion/filter rule
 * (9.6) means adding one entry here alongside the corresponding object in StandardCriteria/
 * StandardFilterRules; the settings screen itself needs no other changes since it iterates these
 * lists generically.
 */
data class CriterionDescriptor(val id: String, val label: String, val unit: String)

val EVALUATION_CRITERIA_DESCRIPTORS = listOf(
    CriterionDescriptor(StandardCriteria.PRICE_PER_HOUR, "Оплата за годину", "€/год"),
    CriterionDescriptor(StandardCriteria.PRICE_PER_KM, "Оплата за кілометр", "€/км"),
    CriterionDescriptor(StandardCriteria.PASSENGER_RATING, "Рейтинг пасажира", "★"),
    CriterionDescriptor(StandardCriteria.ORDER_PRICE, "Вартість замовлення", "€"),
)

val FILTER_RULE_DESCRIPTORS = listOf(
    CriterionDescriptor(StandardFilterRules.MIN_HOURLY_RATE, "Мінімальна погодинна ставка", "€/год"),
    CriterionDescriptor(StandardFilterRules.MIN_PRICE_PER_KM, "Мінімальна оплата за кілометр", "€/км"),
    CriterionDescriptor(StandardFilterRules.MIN_ORDER_PRICE, "Мінімальна вартість замовлення", "€"),
    CriterionDescriptor(StandardFilterRules.MIN_PASSENGER_RATING, "Мінімальний рейтинг пасажира", "★"),
    CriterionDescriptor(StandardFilterRules.MAX_DISTANCE_TO_CLIENT_KM, "Максимальна відстань до клієнта", "км"),
    CriterionDescriptor(StandardFilterRules.MAX_TIME_TO_CLIENT_MIN, "Максимальний час доїзду до клієнта", "хв"),
)
