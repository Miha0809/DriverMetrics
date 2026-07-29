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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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

    LazyColumn(modifier = modifier.padding(horizontal = 16.dp)) {
        item {
            Text(
                text = "DriverMetrics",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }
        item {
            PermissionRow(
                title = "Accessibility Service",
                description = "Читання екранів замовлень у Bolt Driver",
                granted = accessibilityEnabled,
                actionLabel = "Налаштування",
                onAction = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
            )
        }
        item {
            PermissionRow(
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
        }
        item {
            PermissionRow(
                title = "Місцезнаходження (опційно)",
                description = "Для розрахунку часу доїзду до точки посадки",
                granted = locationGranted,
                actionLabel = "Дозволити",
                onAction = {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                },
            )
        }
        item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }
        item {
            Text(text = "Налаштування", style = MaterialTheme.typography.titleMedium)
        }
        item {
            MapZoomSetting(
                wideMapZoom = wideMapZoom,
                onWideMapZoomChange = { enabled ->
                    coroutineScope.launch { mapSettingsRepository.setWideMapZoom(enabled) }
                },
            )
        }
        item {
            Button(
                onClick = onOpenAggregatorSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                Text("Налаштування оцінки замовлень (Bolt / Uber / FreeNow)")
            }
        }
        item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }
        item {
            Text(text = "Статус pipeline", style = MaterialTheme.typography.titleMedium)
        }
        item { StatusSection(status = status) }
        if (BuildConfig.DEBUG) {
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }
            item {
                DebugSection(
                    onTrigger = { order -> currentOrderRepository.update(order) },
                    onHide = { currentOrderRepository.update(null) },
                )
            }
        }
    }
}

/**
 * TEMPORARY, debug-build-only: fires the real pipeline (geocode -> route -> metrics -> overlay)
 * with TaxiOrder objects parsed from the sample screenshots, so the overlay can be exercised
 * without needing to catch a live order in Bolt/Uber. Remove alongside DebugOrderFixtures once
 * no longer needed.
 */
@Composable
private fun DebugSection(onTrigger: (TaxiOrder) -> Unit, onHide: () -> Unit) {
    Column {
        Text(text = "Debug: тестові замовлення", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Запускає весь pipeline на даних, розпарсених із ui/examples_screen",
            style = MaterialTheme.typography.bodySmall,
        )
        DebugOrderFixtures.all.forEach { fixture ->
            Button(
                onClick = { onTrigger(fixture.order) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                Text(fixture.label)
            }
        }
        Button(
            onClick = onHide,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        ) {
            Text("Сховати overlay")
        }
    }
}

@Composable
private fun MapZoomSetting(wideMapZoom: Boolean, onWideMapZoomChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Ширший зум карти", style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (wideMapZoom) {
                    "Увімкнено — по боках карти лишається трохи запасу"
                } else {
                    "Вимкнено — маршрут займає всю карту, як було раніше"
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Checkbox(checked = wideMapZoom, onCheckedChange = onWideMapZoomChange)
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = description, style = MaterialTheme.typography.bodySmall)
            Text(
                text = if (granted) "Дозволено" else "Не дозволено",
                color = if (granted) Color(0xFF2E7D32) else Color(0xFFC62828),
                style = MaterialTheme.typography.labelMedium,
            )
        }
        if (!granted) {
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun StatusSection(status: PipelineStatus) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault()) }

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = status.lastEventPackage?.let { pkg ->
                "Останній пакет: $pkg (${status.lastEventAt?.let(timeFormatter::format)})"
            } ?: "Подій ще не було",
            style = MaterialTheme.typography.bodyMedium,
        )
        val order = status.lastOrder
        Text(
            text = if (order != null) {
                "Останнє замовлення: ${order.price} ${order.currency}, " +
                    "${order.pickupAddress ?: "?"} → ${order.dropoffAddress ?: "?"} " +
                    "(${status.lastOrderAt?.let(timeFormatter::format)})"
            } else {
                "Замовлень ще не розпізнано"
            },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
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
