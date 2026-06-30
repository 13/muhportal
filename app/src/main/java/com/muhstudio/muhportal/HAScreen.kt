package com.muhstudio.muhportal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HAScreen(
    connState: ConnState,
    sensorStates: Map<String, SensorUpdate>,
    switchStates: Map<String, SwitchUpdate>,
    pvStates: Map<String, PvUpdate>,
    energyStates: Map<String, EnergyUpdate>,
    deviceConfig: HADeviceConfig,
    onSwitchAction: (String, Boolean) -> Unit,
    onRefresh: () -> Unit,
    isBlackWhiteMode: Boolean,
    onOpenSettings: () -> Unit,
    alarmState: AlarmState? = null,
    alarmAlerts: List<AlarmAlert> = emptyList(),
    onAlarmSet: (AlarmState) -> Unit = {},
    awaySimManualActive: Boolean = false,
    awaySimScheduleEnabled: Boolean = false,
    awaySimScheduleStart: String = "22:00",
    awaySimScheduleEnd: String = "06:00",
    onAwaySimManualSet: (Boolean) -> Unit = {},
    onAwaySimScheduleSet: (Boolean) -> Unit = {},
    onAwaySimScheduleStart: (String) -> Unit = {},
    onAwaySimScheduleEnd: (String) -> Unit = {},
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
                    val sensor = sensorStates[deviceConfig.tempSensorId]
                    
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
                    val pv = pvStates[deviceConfig.pvId]
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
                    val pv = pvStates[deviceConfig.pvId]
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
                    val energy = energyStates[deviceConfig.energyId]
                    val pv = pvStates[deviceConfig.pvId]
                    
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
                            value1 = energy?.let { "%.1f kWh".format(java.util.Locale.US,it.todayImport) },
                            value2 = null,
                            switchState = false,
                            onSwitchChange = {},
                            isBlackWhiteMode = isBlackWhiteMode,
                            showSwitch = false
                        )
                        HASection(
                            title = "Export",
                            value1 = energy?.let { "%.1f kWh".format(java.util.Locale.US,it.todayExport) },
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
                            value1 = pvVerbrauch?.let { "%.1f kWh".format(java.util.Locale.US,it) },
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
                    val sensor = sensorStates[deviceConfig.kommerSensorId]
                    val sw = switchStates[deviceConfig.kommerSwitchId]

                    HASection(
                        title = "Kommer",
                        value1 = sensor?.temp?.let { "%.1f°".format(java.util.Locale.US,it) },
                        value2 = sensor?.humidity?.let { "%.0f%%".format(it) },
                        switchState = sw?.state ?: false,
                        onSwitchChange = { onSwitchAction(deviceConfig.kommerSwitchId, it) },
                        isBlackWhiteMode = isBlackWhiteMode
                    )
                }
                item {
                    val s1 = sensorStates[deviceConfig.brennerSensor1Id]
                    val s2 = sensorStates[deviceConfig.brennerSensor2Id]
                    val sw = switchStates[deviceConfig.brennerSwitchId]

                    HASection(
                        title = "Brenner",
                        value1 = s1?.temp?.let { "%.0f°".format(it) },
                        value2 = s2?.temp?.let { "%.0f°".format(it) },
                        switchState = sw?.state ?: false,
                        onSwitchChange = { onSwitchAction(deviceConfig.brennerSwitchId, it) },
                        isBlackWhiteMode = isBlackWhiteMode
                    )
                    HorizontalDivider(modifier = Modifier.padding(bottom = 24.dp))
                }
                item {
                    AlarmSection(
                        alarmState = alarmState,
                        alarmAlerts = alarmAlerts,
                        onAlarmSet = onAlarmSet,
                        isBlackWhiteMode = isBlackWhiteMode
                    )
                }
                item {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 24.dp))
                    AwaySimSection(
                        manualActive = awaySimManualActive,
                        scheduleEnabled = awaySimScheduleEnabled,
                        scheduleStart = awaySimScheduleStart,
                        scheduleEnd = awaySimScheduleEnd,
                        onManualSet = onAwaySimManualSet,
                        onScheduleSet = onAwaySimScheduleSet,
                        onScheduleStart = onAwaySimScheduleStart,
                        onScheduleEnd = onAwaySimScheduleEnd,
                        isBlackWhiteMode = isBlackWhiteMode
                    )
                }
            }
        }
    }
}

@Composable
private fun AlarmRow(
    title: String,
    checked: Boolean,
    isBlackWhiteMode: Boolean,
    activeColor: AppColor,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontSize = 24.sp, color = Color.Gray, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = getAppColor(activeColor, isBlackWhiteMode),
                checkedTrackColor = getAppColor(activeColor, isBlackWhiteMode).copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun AlarmSection(
    alarmState: AlarmState?,
    alarmAlerts: List<AlarmAlert>,
    onAlarmSet: (AlarmState) -> Unit,
    isBlackWhiteMode: Boolean
) {
    val armed  = alarmState == AlarmState.ARM_AWAY || alarmState == AlarmState.ARM_HOME
    val atHome = alarmState == AlarmState.ARM_HOME

    Column(modifier = Modifier.fillMaxWidth()) {
        AlarmRow(
            title = "Alarm",
            checked = armed,
            isBlackWhiteMode = isBlackWhiteMode,
            activeColor = AppColor.RED
        ) { on ->
            onAlarmSet(if (on) AlarmState.ARM_AWAY else AlarmState.DISARM)
        }
        AlarmRow(
            title = "Alarm @Home",
            checked = atHome,
            isBlackWhiteMode = isBlackWhiteMode,
            activeColor = AppColor.ORANGE
        ) { on ->
            onAlarmSet(if (on) AlarmState.ARM_HOME else if (armed) AlarmState.ARM_AWAY else AlarmState.DISARM)
        }

        if (alarmAlerts.isNotEmpty()) {
            Text(
                text = "Alerts",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            alarmAlerts.take(10).forEach { alert ->
                val ts = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(alert.ts))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = alert.label.ifEmpty { alert.device },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = alert.alarmState,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Text(
                        text = alert.time.ifEmpty { ts },
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AwaySimSection(
    manualActive: Boolean,
    scheduleEnabled: Boolean,
    scheduleStart: String,
    scheduleEnd: String,
    onManualSet: (Boolean) -> Unit,
    onScheduleSet: (Boolean) -> Unit,
    onScheduleStart: (String) -> Unit,
    onScheduleEnd: (String) -> Unit,
    isBlackWhiteMode: Boolean
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        AlarmRow(
            title = "Away Sim",
            checked = manualActive,
            isBlackWhiteMode = isBlackWhiteMode,
            activeColor = AppColor.GREEN,
            onCheckedChange = onManualSet
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Away Schedule", fontSize = 24.sp, color = Color.Gray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = scheduleStart,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { showStartPicker = true }
                    )
                    Text(
                        text = " – ",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = scheduleEnd,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { showEndPicker = true }
                    )
                }
            }
            Switch(
                checked = scheduleEnabled,
                onCheckedChange = onScheduleSet,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = getAppColor(AppColor.GREEN, isBlackWhiteMode),
                    checkedTrackColor = getAppColor(AppColor.GREEN, isBlackWhiteMode).copy(alpha = 0.5f)
                )
            )
        }
    }

    if (showStartPicker) {
        val parts = scheduleStart.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 22
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val state = rememberTimePickerState(initialHour = h, initialMinute = m, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showStartPicker = false },
            title = { Text("Start Time") },
            text = { TimeInput(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    onScheduleStart("%02d:%02d".format(state.hour, state.minute))
                    showStartPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text("Cancel") }
            }
        )
    }

    if (showEndPicker) {
        val parts = scheduleEnd.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 6
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val state = rememberTimePickerState(initialHour = h, initialMinute = m, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showEndPicker = false },
            title = { Text("End Time") },
            text = { TimeInput(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    onScheduleEnd("%02d:%02d".format(state.hour, state.minute))
                    showEndPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) { Text("Cancel") }
            }
        )
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
