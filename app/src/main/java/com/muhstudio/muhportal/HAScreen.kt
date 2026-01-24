package com.muhstudio.muhportal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HAScreen(
    connState: ConnState,
    sensorStates: Map<String, SensorUpdate>,
    switchStates: Map<String, SwitchUpdate>,
    pvStates: Map<String, PvUpdate>,
    energyStates: Map<String, EnergyUpdate>,
    onSwitchAction: (String, Boolean) -> Unit,
    onRefresh: () -> Unit,
    isBlackWhiteMode: Boolean,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TitleBar(
            connState = connState,
            onRefresh = onRefresh,
            title = "HA",
            icon = Icons.Default.Lightbulb,
            isBlackWhiteMode = isBlackWhiteMode,
            onOpenSettings = onOpenSettings
        )
        HorizontalDivider(
            color = getConnColor(connState, isBlackWhiteMode),
            thickness = 4.dp
        )
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    onRefresh()
                    delay(1000)
                    isRefreshing = false
                }
            },
            modifier = Modifier.weight(1f)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    val sensor = sensorStates["B327"]
                    
                    HASection(
                        title = "Temperatur",
                        value1 = sensor?.temp?.let { "%.1f°".format(java.util.Locale.US, it) },
                        value2 = sensor?.humidity?.let { "%.0f%%".format(java.util.Locale.US, it) },
                        switchState = false,
                        onSwitchChange = { },
                        isBlackWhiteMode = isBlackWhiteMode,
                        showSwitch = false
                    )
                    HorizontalDivider(modifier = Modifier.padding(bottom = 24.dp))
                }
                item {
                    val pv = pvStates["E07000055917"]
                    HASection(
                        title = "PV",
                        value1 = pv?.let { "%.0f W".format(it.p1 + it.p2) },
                        value2 = pv?.let { "%.0f/%.0f".format(it.p1, it.p2) },
                        switchState = false,
                        onSwitchChange = { },
                        isBlackWhiteMode = isBlackWhiteMode,
                        showSwitch = false
                    )
                }
                item {
                    val pv = pvStates["E07000055917"]
                    HASection(
                        title = "PV Produktion",
                        value1 = pv?.let { "%.1f kWh".format(java.util.Locale.US, it.e1 + it.e2) },
                        value2 = pv?.let { "%.1f/%.1f".format(java.util.Locale.US, it.e1, it.e2) },
                        switchState = false,
                        onSwitchChange = { },
                        isBlackWhiteMode = isBlackWhiteMode,
                        showSwitch = false
                    )
                    HorizontalDivider(modifier = Modifier.padding(bottom = 24.dp))
                }
                
                item {
                    val energy = energyStates["tasmota_5FF8B2"]
                    val pv = pvStates["E07000055917"]
                    
                    Column(modifier = Modifier.fillMaxWidth()) {
                        HASection(
                            title = "Verbrauch",
                            value1 = energy?.let { "%.0f W".format(it.activePower) },
                            value2 = null,
                            switchState = false,
                            onSwitchChange = {},
                            isBlackWhiteMode = isBlackWhiteMode,
                            showSwitch = false
                        )
                        HASection(
                            title = "Import",
                            value1 = energy?.let { "%.1f kWh".format(it.todayImport) },
                            value2 = null,
                            switchState = false,
                            onSwitchChange = {},
                            isBlackWhiteMode = isBlackWhiteMode,
                            showSwitch = false
                        )
                        HASection(
                            title = "Export",
                            value1 = energy?.let { "%.1f kWh".format(it.todayExport) },
                            value2 = null,
                            switchState = false,
                            onSwitchChange = {},
                            isBlackWhiteMode = isBlackWhiteMode,
                            showSwitch = false
                        )
                        
                        val pvProd = pv?.let { it.e1 + it.e2 }
                        val pvVerbrauch = if (pvProd != null && energy != null) {
                            (pvProd - energy.todayExport).coerceAtLeast(0f)
                        } else null
                        
                        HASection(
                            title = "PV Verbrauch",
                            value1 = pvVerbrauch?.let { "%.1f kWh".format(it) },
                            value2 = null,
                            switchState = false,
                            onSwitchChange = {},
                            isBlackWhiteMode = isBlackWhiteMode,
                            showSwitch = false
                        )
                        HorizontalDivider(modifier = Modifier.padding(bottom = 24.dp))
                    }
                }
                
                item {
                    val sensor = sensorStates["87"]
                    val sw = switchStates["tasmota_BDC5E0"]
                    
                    HASection(
                        title = "Kommer",
                        value1 = sensor?.temp?.let { "%.1f°".format(it) },
                        value2 = sensor?.humidity?.let { "%.0f%%".format(it) },
                        switchState = sw?.state ?: false,
                        onSwitchChange = { onSwitchAction("tasmota_BDC5E0", it) },
                        isBlackWhiteMode = isBlackWhiteMode
                    )
                }
                item {
                    val s1 = sensorStates["DS18B20-3628FF"]
                    val s2 = sensorStates["DS18B20-1C16E1"]
                    val sw = switchStates["tasmota_A7EEA3"]

                    HASection(
                        title = "Brenner",
                        value1 = s1?.temp?.let { "%.0f°".format(it) },
                        value2 = s2?.temp?.let { "%.0f°".format(it) },
                        switchState = sw?.state ?: false,
                        onSwitchChange = { onSwitchAction("tasmota_A7EEA3", it) },
                        isBlackWhiteMode = isBlackWhiteMode
                    )
                }
            }
        }
    }
}

@Composable
private fun HASection(
    title: String,
    value1: String?,
    value2: String?,
    switchState: Boolean,
    onSwitchChange: (Boolean) -> Unit,
    isBlackWhiteMode: Boolean,
    showSwitch: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 24.sp,
            color = Color.Gray,
            modifier = Modifier.weight(1f)
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp)
        ) {
            Text(
                text = value1 ?: "--.-",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (value2 != null) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = value2,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (showSwitch) {
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = switchState,
                onCheckedChange = onSwitchChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = getAppColor(AppColor.GREEN, isBlackWhiteMode),
                    checkedTrackColor = getAppColor(AppColor.GREEN, isBlackWhiteMode).copy(alpha = 0.5f)
                )
            )
        }
    }
}
