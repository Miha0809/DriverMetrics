package com.apexcode.drivermetrics

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.apexcode.drivermetrics.accessibility.DriverMetricsAccessibilityService
import com.apexcode.drivermetrics.core.model.TaxiOrder
import com.apexcode.drivermetrics.debug.DebugOrderFixtures
import com.apexcode.drivermetrics.pipeline.CurrentOrderRepository
import com.apexcode.drivermetrics.pipeline.PipelineStatus
import com.apexcode.drivermetrics.pipeline.PipelineStatusRepository
import com.apexcode.drivermetrics.settings.AggregatorSettingsRepository
import com.apexcode.drivermetrics.settings.MapSettingsRepository
import com.apexcode.drivermetrics.ui.common.SectionCard
import com.apexcode.drivermetrics.ui.settings.SettingsScreen
import com.apexcode.drivermetrics.ui.theme.DriverMetricsTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import javax.inject.Inject

/**
 * Onboarding + debug status screen: the accessibility service and overlay permissions can't be
 * requested as normal runtime permissions, so this walks the driver through Settings, and shows
 * the last event/order the pipeline has seen so it's obvious it's actually running.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var pipelineStatusRepository: PipelineStatusRepository
    @Inject lateinit var currentOrderRepository: CurrentOrderRepository
    @Inject lateinit var mapSettingsRepository: MapSettingsRepository
    @Inject lateinit var aggregatorSettingsRepository: AggregatorSettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DriverMetricsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    var showAggregatorSettings by remember { mutableStateOf(false) }
                    if (showAggregatorSettings) {
                        SettingsScreen(
                            aggregatorSettingsRepository = aggregatorSettingsRepository,
                            onBack = { showAggregatorSettings = false },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                        )
                    } else {
                        OnboardingScreen(
                            pipelineStatusRepository = pipelineStatusRepository,
                            currentOrderRepository = currentOrderRepository,
                            mapSettingsRepository = mapSettingsRepository,
                            onOpenAggregatorSettings = { showAggregatorSettings = true },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberRefreshOnResume(): Int {
    var trigger by remember { mutableStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) trigger++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return trigger
}

@Composable
private fun OnboardingScreen(
    pipelineStatusRepository: PipelineStatusRepository,
    currentOrderRepository: CurrentOrderRepository,
    mapSettingsRepository: MapSettingsRepository,
    onOpenAggregatorSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val refreshKey = rememberRefreshOnResume()
    val accessibilityEnabled = remember(refreshKey) { isAccessibilityServiceEnabled(context) }
    val overlayGranted = remember(refreshKey) { Settings.canDrawOverlays(context) }

    var locationGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> locationGranted = granted }

    val status by pipelineStatusRepository.status.collectAsState()
    val wideMapZoom by mapSettingsRepository.wideMapZoom.collectAsState(initial = true)

    Column(modifier = modifier) {
        AppHeader()
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard(title = "Дозволи", icon = Icons.Filled.Shield) {
                    PermissionListItem(
                        icon = Icons.Filled.Accessibility,
                        title = "Accessibility Service",
                        description = "Читання екранів замовлень у Bolt Driver",
                        granted = accessibilityEnabled,
                        actionLabel = "Налаштування",
                        onAction = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    PermissionListItem(
                        icon = Icons.Filled.Layers,
                        title = "Показ поверх інших додатків",
                        description = "Потрібно для overlay з показниками",
                        granted = overlayGranted,
                        actionLabel = "Дозволити",
                        onAction = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}"),
                                ),
                            )
                        },
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    PermissionListItem(
                        icon = Icons.Filled.LocationOn,
                        title = "Місцезнаходження (опційно)",
                        description = "Для розрахунку часу доїзду до точки посадки",
                        granted = locationGranted,
                        actionLabel = "Дозволити",
                        onAction = {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                        },
                    )
                }
            }
            item {
                SectionCard(title = "Карта", icon = Icons.Filled.Map) {
                    MapZoomListItem(
                        wideMapZoom = wideMapZoom,
                        onWideMapZoomChange = { enabled ->
                            coroutineScope.launch { mapSettingsRepository.setWideMapZoom(enabled) }
                        },
                    )
                }
            }
            item { SettingsEntryCard(onClick = onOpenAggregatorSettings) }
            item {
                SectionCard(title = "Статус pipeline", icon = Icons.Filled.History) {
                    StatusSection(status = status)
                }
            }
            if (BuildConfig.DEBUG) {
                item {
                    DebugSectionCard(
                        onTrigger = { order -> currentOrderRepository.update(order) },
                        onHide = { currentOrderRepository.update(null) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppHeader() {
    Surface(tonalElevation = 2.dp, shadowElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.LocalTaxi,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = "DriverMetrics", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Оцінка вигідності замовлень у реальному часі",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The prominent entry point into the aggregator settings module — styled like a menu item, not a button. */
@Composable
private fun SettingsEntryCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            leadingContent = {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            },
            headlineContent = {
                Text(
                    text = "Налаштування оцінки замовлень",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            },
            supportingContent = {
                Text(
                    text = "Bolt · Uber · FreeNow",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            },
            trailingContent = {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            },
        )
    }
}

/**
 * TEMPORARY, debug-build-only: fires the real pipeline (geocode -> route -> metrics -> overlay)
 * with TaxiOrder objects parsed from the sample screenshots, so the overlay can be exercised
 * without needing to catch a live order in Bolt/Uber. Remove alongside DebugOrderFixtures once
 * no longer needed. Styled distinctly (tertiary container + BugReport icon) so it visibly reads
 * as a developer-only area, not part of the normal driver-facing UI.
 */
@Composable
private fun DebugSectionCard(onTrigger: (TaxiOrder) -> Unit, onHide: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.BugReport,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DEBUG · тестові замовлення",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            Text(
                text = "Запускає весь pipeline на даних, розпарсених із ui/examples_screen",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            DebugOrderFixtures.all.forEach { fixture ->
                FilledTonalButton(
                    onClick = { onTrigger(fixture.order) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Text(fixture.label)
                }
            }
            OutlinedButton(
                onClick = onHide,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            ) {
                Text("Сховати overlay")
            }
        }
    }
}

@Composable
private fun MapZoomListItem(wideMapZoom: Boolean, onWideMapZoomChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text("Ширший зум карти") },
        supportingContent = {
            Text(
                text = if (wideMapZoom) {
                    "Увімкнено — по боках карти лишається трохи запасу"
                } else {
                    "Вимкнено — маршрут займає всю карту, як було раніше"
                },
            )
        },
        trailingContent = { Switch(checked = wideMapZoom, onCheckedChange = onWideMapZoomChange) },
    )
}

@Composable
private fun PermissionListItem(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
) {
    ListItem(
        leadingContent = {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                StatusChip(granted = granted)
                if (!granted) {
                    TextButton(
                        onClick = onAction,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) {
                        Text(actionLabel, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        },
    )
}

@Composable
private fun StatusChip(granted: Boolean) {
    val containerColor = if (granted) Color(0xFFDCF3E1) else Color(0xFFFBE1DF)
    val contentColor = if (granted) Color(0xFF1B5E20) else Color(0xFFB3261E)
    val icon = if (granted) Icons.Filled.CheckCircle else Icons.Filled.Cancel
    val label = if (granted) "Дозволено" else "Не дозволено"
    Surface(shape = RoundedCornerShape(50), color = containerColor) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, color = contentColor, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun StatusSection(status: PipelineStatus) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault()) }

    ListItem(
        headlineContent = {
            Text(
                status.lastEventPackage?.let { pkg -> "Останній пакет: $pkg" } ?: "Подій ще не було",
            )
        },
        supportingContent = {
            status.lastEventAt?.let { Text(timeFormatter.format(it)) }
        },
    )
    val order = status.lastOrder
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    ListItem(
        headlineContent = {
            Text(
                text = if (order != null) {
                    "${order.price} ${order.currency} · ${order.pickupAddress ?: "?"} → ${order.dropoffAddress ?: "?"}"
                } else {
                    "Замовлень ще не розпізнано"
                },
            )
        },
        supportingContent = {
            status.lastOrderAt?.let { Text(timeFormatter.format(it)) }
        },
    )
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expectedComponentName = ComponentName(context, DriverMetricsAccessibilityService::class.java)
    val enabledServicesSetting = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ) ?: return false

    val splitter = TextUtils.SimpleStringSplitter(':')
    splitter.setString(enabledServicesSetting)
    while (splitter.hasNext()) {
        if (ComponentName.unflattenFromString(splitter.next()) == expectedComponentName) {
            return true
        }
    }
    return false
}
