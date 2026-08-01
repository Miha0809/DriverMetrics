package com.apexcode.drivermetrics.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.apexcode.drivermetrics.core.model.settings.AggregatorId
import com.apexcode.drivermetrics.core.model.settings.AggregatorSettings
import com.apexcode.drivermetrics.core.model.settings.CriterionThreshold
import com.apexcode.drivermetrics.core.model.settings.RouteDisplayMode
import com.apexcode.drivermetrics.core.model.settings.isDisplayOptionEnabled
import com.apexcode.drivermetrics.metrics.MetricsCalculator
import com.apexcode.drivermetrics.settings.AggregatorSettingsRepository
import com.apexcode.drivermetrics.ui.common.SectionCard
import kotlinx.coroutines.launch

/**
 * The "окремий модуль налаштувань" (9). Lets a driver personalize, per aggregator or synced
 * across all of them (9.1), how the route is drawn (9.2), which criteria drive the green/yellow/
 * red indicator (9.3) and which hard filters force it to red (9.5). New criteria/filters show up
 * automatically since this screen renders [EVALUATION_CRITERIA_DESCRIPTORS] and
 * [FILTER_RULE_DESCRIPTORS] generically (9.6) rather than having one field per criterion.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    aggregatorSettingsRepository: AggregatorSettingsRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val syncEnabled by aggregatorSettingsRepository.syncEnabled.collectAsState(initial = true)
    var selectedAggregator by remember { mutableStateOf(AggregatorId.entries.first()) }
    // While synced there's only one bucket, so which aggregator we read/write through is
    // irrelevant to the result but still has to be a concrete id — settingsFor's sync branch
    // ignores it.
    val editingAggregator = if (syncEnabled) AggregatorId.entries.first() else selectedAggregator

    val settings by aggregatorSettingsRepository.settingsFor(editingAggregator)
        .collectAsState(initial = AggregatorSettings())

    fun update(transform: (AggregatorSettings) -> AggregatorSettings) {
        coroutineScope.launch { aggregatorSettingsRepository.updateSettings(editingAggregator, transform) }
    }

    Column(modifier = modifier) {
        TopAppBar(
            title = { Text("Налаштування оцінки замовлень") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
            },
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard(title = "Синхронізація", icon = Icons.Filled.Sync) {
                    SyncToggleRow(
                        enabled = syncEnabled,
                        onChange = { enabled ->
                            coroutineScope.launch { aggregatorSettingsRepository.setSyncEnabled(enabled) }
                        },
                    )
                    if (!syncEnabled) {
                        AggregatorSelectorRow(selected = selectedAggregator, onSelect = { selectedAggregator = it })
                    }
                }
            }

            item {
                SectionCard(title = "Відображення маршруту", icon = Icons.Filled.Route) {
                    RouteDisplaySection(
                        mode = settings.routeDisplayMode,
                        onChange = { mode -> update { it.copy(routeDisplayMode = mode) } },
                    )
                }
            }

            item {
                SectionCard(title = "Показ на екрані", icon = Icons.Filled.Insights) {
                    DISPLAY_OPTION_DESCRIPTORS.forEachIndexed { index, descriptor ->
                        if (index > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        DisplayOptionRow(
                            descriptor = descriptor,
                            enabled = settings.isDisplayOptionEnabled(descriptor.id),
                            onChange = { enabled ->
                                update { current ->
                                    current.copy(displayOptions = current.displayOptions + (descriptor.id to enabled))
                                }
                            },
                        )
                    }
                }
            }

            item {
                SectionCard(title = "Час очікування", icon = Icons.Filled.Timer) {
                    FreeWaitingTimeToggleRow(
                        enabled = settings.includeFreeWaitingTime,
                        onChange = { enabled -> update { it.copy(includeFreeWaitingTime = enabled) } },
                    )
                }
            }

            item {
                SectionCard(title = "Критерії оцінки вигідності", icon = Icons.Filled.Tune) {
                    EVALUATION_CRITERIA_DESCRIPTORS.forEachIndexed { index, descriptor ->
                        if (index > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        EvaluationCriterionRow(
                            descriptor = descriptor,
                            threshold = settings.evaluationCriteria[descriptor.id] ?: CriterionThreshold(),
                            resetKey = editingAggregator,
                            onChange = { threshold ->
                                update { current ->
                                    val criteria = if (threshold.redBelow == null && threshold.greenAtOrAbove == null) {
                                        current.evaluationCriteria - descriptor.id
                                    } else {
                                        current.evaluationCriteria + (descriptor.id to threshold)
                                    }
                                    current.copy(evaluationCriteria = criteria)
                                }
                            },
                        )
                    }
                }
            }

            item {
                SectionCard(title = "Правила фільтрації", icon = Icons.Filled.FilterAlt) {
                    FILTER_RULE_DESCRIPTORS.forEachIndexed { index, descriptor ->
                        if (index > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        FilterRuleRow(
                            descriptor = descriptor,
                            limit = settings.filterRules[descriptor.id],
                            resetKey = editingAggregator,
                            onChange = { limit ->
                                update { current ->
                                    val rules = if (limit == null) {
                                        current.filterRules - descriptor.id
                                    } else {
                                        current.filterRules + (descriptor.id to limit)
                                    }
                                    current.copy(filterRules = rules)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncToggleRow(enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Синхронізувати між агрегаторами", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (enabled) {
                    "Зміна параметра застосовується до всіх агрегаторів"
                } else {
                    "Кожен агрегатор має власні налаштування"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = enabled, onCheckedChange = onChange)
    }
}

@Composable
private fun FreeWaitingTimeToggleRow(enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Чи враховувати безплатний час очікування до статистики?", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "Додає ${formatNumber(MetricsCalculator.FREE_WAITING_TIME_MINUTES)} хв до загального часу " +
                    "замовлення перед розрахунком €/год",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = enabled, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AggregatorSelectorRow(selected: AggregatorId, onSelect: (AggregatorId) -> Unit) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        AggregatorId.entries.forEachIndexed { index, aggregator ->
            SegmentedButton(
                selected = aggregator == selected,
                onClick = { onSelect(aggregator) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = AggregatorId.entries.size),
            ) {
                Text(aggregator.displayName)
            }
        }
    }
}

@Composable
private fun RouteDisplaySection(mode: RouteDisplayMode, onChange: (RouteDisplayMode) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RouteDisplayOptionCard(
            selected = mode == RouteDisplayMode.CLIENT_TO_DROPOFF_ONLY,
            label = "Тільки від клієнта до кінцевої точки",
            helper = "Як зараз",
            onClick = { onChange(RouteDisplayMode.CLIENT_TO_DROPOFF_ONLY) },
        )
        RouteDisplayOptionCard(
            selected = mode == RouteDisplayMode.DRIVER_TO_PICKUP_AND_DROPOFF,
            label = "Від поточного місця водія до клієнта і далі до кінцевої точки",
            helper = "Повний маршрут",
            onClick = { onChange(RouteDisplayMode.DRIVER_TO_PICKUP_AND_DROPOFF) },
        )
    }
}

/** A selectable option styled as its own outlined surface (highlighted border when selected) rather than a bare radio row. */
@Composable
private fun RouteDisplayOptionCard(selected: Boolean, label: String, helper: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(text = label, style = MaterialTheme.typography.bodyMedium)
                Text(text = helper, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DisplayOptionRow(descriptor: DisplayOptionDescriptor, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = descriptor.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = descriptor.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = enabled, onCheckedChange = onChange)
    }
}

/**
 * Both bounds of one criterion, edited together and committed as a single [CriterionThreshold]
 * built from local text state (not the [threshold] parameter) so that editing one field right
 * after the other can't silently drop the first edit while the settings Flow round-trips.
 */
@Composable
private fun EvaluationCriterionRow(
    descriptor: CriterionDescriptor,
    threshold: CriterionThreshold,
    resetKey: Any,
    onChange: (CriterionThreshold) -> Unit,
) {
    var redText by remember(resetKey, descriptor.id) { mutableStateOf(threshold.redBelow?.let(::formatNumber) ?: "") }
    var greenText by remember(resetKey, descriptor.id) {
        mutableStateOf(threshold.greenAtOrAbove?.let(::formatNumber) ?: "")
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = descriptor.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "${descriptor.label} (${descriptor.unit})", style = MaterialTheme.typography.titleSmall)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = redText,
                onValueChange = { value ->
                    redText = value
                    onChange(CriterionThreshold(redBelow = value.toDoubleOrNull(), greenAtOrAbove = greenText.toDoubleOrNull()))
                },
                label = { Text("Червоний нижче") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = greenText,
                onValueChange = { value ->
                    greenText = value
                    onChange(CriterionThreshold(redBelow = redText.toDoubleOrNull(), greenAtOrAbove = value.toDoubleOrNull()))
                },
                label = { Text("Зелений від") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun FilterRuleRow(
    descriptor: CriterionDescriptor,
    limit: Double?,
    resetKey: Any,
    onChange: (Double?) -> Unit,
) {
    var text by remember(resetKey, descriptor.id) { mutableStateOf(limit?.let(::formatNumber) ?: "") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = descriptor.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "${descriptor.label} (${descriptor.unit})",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = text,
            onValueChange = { value ->
                text = value
                onChange(value.toDoubleOrNull())
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(110.dp),
        )
    }
}

private fun formatNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
