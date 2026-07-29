package com.apexcode.drivermetrics.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.graphics.vector.ImageVector
import com.apexcode.drivermetrics.metrics.StandardCriteria
import com.apexcode.drivermetrics.metrics.StandardFilterRules

/**
 * Presentation metadata (label/unit/icon) for the settings screen only — deliberately kept out of
 * the `metrics` package so that package stays pure business logic. Adding a new criterion/filter
 * rule (9.6) means adding one entry here alongside the corresponding object in StandardCriteria/
 * StandardFilterRules; the settings screen itself needs no other changes since it iterates these
 * lists generically.
 */
data class CriterionDescriptor(val id: String, val label: String, val unit: String, val icon: ImageVector)

val EVALUATION_CRITERIA_DESCRIPTORS = listOf(
    CriterionDescriptor(StandardCriteria.PRICE_PER_HOUR, "Оплата за годину", "€/год", Icons.Filled.Schedule),
    CriterionDescriptor(StandardCriteria.PRICE_PER_KM, "Оплата за кілометр", "€/км", Icons.Filled.Speed),
    CriterionDescriptor(StandardCriteria.PASSENGER_RATING, "Рейтинг пасажира", "★", Icons.Filled.Star),
    CriterionDescriptor(StandardCriteria.ORDER_PRICE, "Вартість замовлення", "€", Icons.Filled.Payments),
)

val FILTER_RULE_DESCRIPTORS = listOf(
    CriterionDescriptor(StandardFilterRules.MIN_HOURLY_RATE, "Мінімальна погодинна ставка", "€/год", Icons.Filled.Schedule),
    CriterionDescriptor(StandardFilterRules.MIN_PRICE_PER_KM, "Мінімальна оплата за кілометр", "€/км", Icons.Filled.Speed),
    CriterionDescriptor(StandardFilterRules.MIN_ORDER_PRICE, "Мінімальна вартість замовлення", "€", Icons.Filled.Payments),
    CriterionDescriptor(StandardFilterRules.MIN_PASSENGER_RATING, "Мінімальний рейтинг пасажира", "★", Icons.Filled.Star),
    CriterionDescriptor(
        StandardFilterRules.MAX_DISTANCE_TO_CLIENT_KM,
        "Максимальна відстань до клієнта",
        "км",
        Icons.Filled.NearMe,
    ),
    CriterionDescriptor(
        StandardFilterRules.MAX_TIME_TO_CLIENT_MIN,
        "Максимальний час доїзду до клієнта",
        "хв",
        Icons.Filled.Timer,
    ),
)
