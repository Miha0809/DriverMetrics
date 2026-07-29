package com.apexcode.drivermetrics.ui.settings

import com.apexcode.drivermetrics.core.model.settings.StandardDisplayOptions

/** Presentation metadata for [StandardDisplayOptions] — same reasoning as CriterionDescriptors. */
data class DisplayOptionDescriptor(val id: String, val label: String)

val DISPLAY_OPTION_DESCRIPTORS = listOf(
    DisplayOptionDescriptor(StandardDisplayOptions.SHOW_STATS, "Показувати статистику (€/год, €/км, час)"),
    DisplayOptionDescriptor(StandardDisplayOptions.SHOW_MAP, "Показувати карту маршруту"),
)
