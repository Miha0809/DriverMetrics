package com.apexcode.drivermetrics.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Map
import androidx.compose.ui.graphics.vector.ImageVector
import com.apexcode.drivermetrics.core.model.settings.StandardDisplayOptions

/** Presentation metadata for [StandardDisplayOptions] — same reasoning as CriterionDescriptors. */
data class DisplayOptionDescriptor(val id: String, val label: String, val icon: ImageVector)

val DISPLAY_OPTION_DESCRIPTORS = listOf(
    DisplayOptionDescriptor(StandardDisplayOptions.SHOW_STATS, "Показувати статистику (€/год, €/км, час)", Icons.Filled.Insights),
    DisplayOptionDescriptor(StandardDisplayOptions.SHOW_MAP, "Показувати карту маршруту", Icons.Filled.Map),
)
