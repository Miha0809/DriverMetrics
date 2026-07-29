package com.apexcode.drivermetrics.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.apexcode.drivermetrics.settings.AggregatorSettingsRepository
import kotlinx.coroutines.launch

/**
 * The "окремий модуль налаштувань" (9). Lets a driver personalize, per aggregator or synced
 * across all of them (9.1), how the route is drawn (9.2), which criteria drive the green/yellow/
 * red indicator (9.3) and which hard filters force it to red (9.5). New criteria/filters show up
 * automatically since this screen renders [EVALUATION_CRITERIA_DESCRIPTORS] and
 * [FILTER_RULE_DESCRIPTORS] generically (9.6) rather than having one field per criterion.
 */
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

    LazyColumn(modifier = modifier.padding(horizontal = 16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
                TextButton(onClick = onBack) { Text("← Назад") }
                Text("Налаштування оцінки замовлень", style = MaterialTheme.typography.titleLarge)
            }
        }
        item {
            SyncToggleRow(
                enabled = syncEnabled,
                onChange = { enabled -> coroutineScope.launch { aggregatorSettingsRepository.setSyncEnabled(enabled) } },
            )
        }
        if (!syncEnabled) {
            item {
                AggregatorSelectorRow(selected = selectedAggregator, onSelect = { selectedAggregator = it })
            }
        }

        item { SectionHeader("Відображення маршруту") }
        item {
            RouteDisplaySection(
                mode = settings.routeDisplayMode,
                onChange = { mode -> update { it.copy(routeDisplayMode = mode) } },
            )
        }

        item { SectionHeader("Показ на екрані") }
        items(DISPLAY_OPTION_DESCRIPTORS) { descriptor ->
            DisplayOptionRow(
                descriptor = descriptor,
                enabled = settings.isDisplayOptionEnabled(descriptor.id),
                onChange = { enabled ->
                    update { current -> current.copy(displayOptions = current.displayOptions + (descriptor.id to enabled)) }
                },
            )
        }

        item { SectionHeader("Критерії оцінки вигідності") }
        items(EVALUATION_CRITERIA_DESCRIPTORS) { descriptor ->
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

        item { SectionHeader("Правила фільтрації") }
        items(FILTER_RULE_DESCRIPTORS) { descriptor ->
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

@Composable
private fun SectionHeader(title: String) {
    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
    Text(text = title, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun SyncToggleRow(enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.widthIn(max = 280.dp)) {
            Text(text = "Синхронізувати між агрегаторами", style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (enabled) {
                    "Увімкнено — зміна параметра застосовується до всіх агрегаторів"
                } else {
                    "Вимкнено — кожен агрегатор має власні налаштування"
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Checkbox(checked = enabled, onCheckedChange = onChange)
    }
}

@Composable
private fun AggregatorSelectorRow(selected: AggregatorId, onSelect: (AggregatorId) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AggregatorId.entries.forEach { aggregator ->
            if (aggregator == selected) {
                Button(onClick = { onSelect(aggregator) }) { Text(aggregator.displayName) }
            } else {
                OutlinedButton(onClick = { onSelect(aggregator) }) { Text(aggregator.displayName) }
            }
        }
    }
}

@Composable
private fun RouteDisplaySection(mode: RouteDisplayMode, onChange: (RouteDisplayMode) -> Unit) {
    Column {
        RouteDisplayOptionRow(
            selected = mode == RouteDisplayMode.CLIENT_TO_DROPOFF_ONLY,
            label = "Тільки від клієнта до кінцевої точки (як зараз)",
            onClick = { onChange(RouteDisplayMode.CLIENT_TO_DROPOFF_ONLY) },
        )
        RouteDisplayOptionRow(
            selected = mode == RouteDisplayMode.DRIVER_TO_PICKUP_AND_DROPOFF,
            label = "Від поточного місця водія до клієнта і далі до кінцевої точки",
            onClick = { onChange(RouteDisplayMode.DRIVER_TO_PICKUP_AND_DROPOFF) },
        )
    }
}

@Composable
private fun RouteDisplayOptionRow(selected: Boolean, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DisplayOptionRow(descriptor: DisplayOptionDescriptor, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = descriptor.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Checkbox(checked = enabled, onCheckedChange = onChange)
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

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(text = "${descriptor.label} (${descriptor.unit})", style = MaterialTheme.typography.titleSmall)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
