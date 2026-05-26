package com.muhstudio.muhportal

import android.widget.ImageView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    isBlackWhiteMode: Boolean,
    onBlackWhiteModeChange: (Boolean) -> Unit,
    onClearCache: () -> Unit,
    mqttConnection: MqttConnectionConfig,
    onConnectionChange: (MqttConnectionConfig) -> Unit,
    mqttTopics: MqttTopicConfig,
    onTopicsChange: (MqttTopicConfig) -> Unit,
    haDevices: HADeviceConfig,
    onHADevicesChange: (HADeviceConfig) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    var editServerUri by remember(mqttConnection) { mutableStateOf(mqttConnection.serverUri) }
    var editUsername by remember(mqttConnection) { mutableStateOf(mqttConnection.username) }
    var editPassword by remember(mqttConnection) { mutableStateOf(mqttConnection.password) }

    var editTempSensorId by remember(haDevices) { mutableStateOf(haDevices.tempSensorId) }
    var editPvId by remember(haDevices) { mutableStateOf(haDevices.pvId) }
    var editEnergyId by remember(haDevices) { mutableStateOf(haDevices.energyId) }
    var editKommerSensorId by remember(haDevices) { mutableStateOf(haDevices.kommerSensorId) }
    var editKommerSwitchId by remember(haDevices) { mutableStateOf(haDevices.kommerSwitchId) }
    var editBrennerSensor1Id by remember(haDevices) { mutableStateOf(haDevices.brennerSensor1Id) }
    var editBrennerSensor2Id by remember(haDevices) { mutableStateOf(haDevices.brennerSensor2Id) }
    var editBrennerSwitchId by remember(haDevices) { mutableStateOf(haDevices.brennerSwitchId) }

    var editPortalSub by remember(mqttTopics) { mutableStateOf(mqttTopics.portalSub) }
    var editWolSub by remember(mqttTopics) { mutableStateOf(mqttTopics.wolSub) }
    var editSensorsSub by remember(mqttTopics) { mutableStateOf(mqttTopics.sensorsSub) }
    var editWstSub by remember(mqttTopics) { mutableStateOf(mqttTopics.wstSub) }
    var editPvSub by remember(mqttTopics) { mutableStateOf(mqttTopics.pvSub) }
    var editTasmotaStateSub by remember(mqttTopics) { mutableStateOf(mqttTopics.tasmotaStateSub) }
    var editTasmotaSensorSub by remember(mqttTopics) { mutableStateOf(mqttTopics.tasmotaSensorSub) }
    var editTasmotaResultSub by remember(mqttTopics) { mutableStateOf(mqttTopics.tasmotaResultSub) }
    var editPortalCmndPub by remember(mqttTopics) { mutableStateOf(mqttTopics.portalCmndPub) }
    var editWolWakePub by remember(mqttTopics) { mutableStateOf(mqttTopics.wolWakePub) }
    var editWolShutdownPub by remember(mqttTopics) { mutableStateOf(mqttTopics.wolShutdownPub) }
    var editTasmotaCmndPub by remember(mqttTopics) { mutableStateOf(mqttTopics.tasmotaCmndPub) }

    val packageInfo = remember(context) {
        try { context.packageManager.getPackageInfo(context.packageName, 0) } catch (_: Exception) { null }
    }
    val versionName = packageInfo?.versionName ?: "Unknown"
    val buildDate = remember(packageInfo) {
        if (packageInfo != null)
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(packageInfo.lastUpdateTime))
        else "Unknown"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Appearance
            SettingsGroupLabel("Appearance")
            SettingsCard {
                SettingsToggle(
                    title = "Dark Mode",
                    subtitle = "Use dark color scheme",
                    checked = isDarkMode,
                    onCheckedChange = onDarkModeChange
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsToggle(
                    title = "High Contrast",
                    subtitle = "Black & white accessibility mode",
                    checked = isBlackWhiteMode,
                    onCheckedChange = onBlackWhiteModeChange
                )
            }

            // Data
            SettingsGroupLabel("Data")
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showClearDialog = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Clear Cache",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "Remove all locally cached MQTT data",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // MQTT
            SettingsGroupLabel("MQTT")
            ExpandableSection(title = "Connection", icon = Icons.Default.Wifi) {
                OutlinedTextField(
                    value = editServerUri, onValueChange = { editServerUri = it },
                    label = { Text("Server URI") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = editUsername, onValueChange = { editUsername = it },
                    label = { Text("Username") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = editPassword, onValueChange = { editPassword = it },
                    label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { passwordVisible = !passwordVisible }) {
                            Text(
                                if (passwordVisible) "Hide" else "Show",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                )
                SaveResetRow(
                    onReset = {
                        val d = MqttConnectionConfig()
                        editServerUri = d.serverUri; editUsername = d.username; editPassword = d.password
                    },
                    onSave = {
                        onConnectionChange(MqttConnectionConfig(editServerUri, editUsername, editPassword))
                    },
                    saveLabel = "Save & Reconnect"
                )
            }

            ExpandableSection(title = "Topics", icon = Icons.Default.Lan) {
                FieldGroupLabel("Subscribe")
                OutlinedTextField(value = editPortalSub, onValueChange = { editPortalSub = it },
                    label = { Text("Portal States") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = editWolSub, onValueChange = { editWolSub = it },
                    label = { Text("Wake-on-LAN") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = editSensorsSub, onValueChange = { editSensorsSub = it },
                    label = { Text("Sensors") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = editWstSub, onValueChange = { editWstSub = it },
                    label = { Text("Weather Station") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = editPvSub, onValueChange = { editPvSub = it },
                    label = { Text("Solar PV") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = editTasmotaStateSub, onValueChange = { editTasmotaStateSub = it },
                    label = { Text("Tasmota State") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = editTasmotaSensorSub, onValueChange = { editTasmotaSensorSub = it },
                    label = { Text("Tasmota Sensor") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = editTasmotaResultSub, onValueChange = { editTasmotaResultSub = it },
                    label = { Text("Tasmota Result") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                FieldGroupLabel("Publish")
                OutlinedTextField(value = editPortalCmndPub, onValueChange = { editPortalCmndPub = it },
                    label = { Text("Portal Command") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = editWolWakePub, onValueChange = { editWolWakePub = it },
                    label = { Text("WOL Wake") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = editWolShutdownPub, onValueChange = { editWolShutdownPub = it },
                    label = { Text("WOL Shutdown") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = editTasmotaCmndPub, onValueChange = { editTasmotaCmndPub = it },
                    label = { Text("Tasmota Power") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Text(
                    "{id} is replaced with the device ID",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                SaveResetRow(
                    onReset = {
                        val d = MqttTopicConfig()
                        editPortalSub = d.portalSub; editWolSub = d.wolSub
                        editSensorsSub = d.sensorsSub; editWstSub = d.wstSub; editPvSub = d.pvSub
                        editTasmotaStateSub = d.tasmotaStateSub; editTasmotaSensorSub = d.tasmotaSensorSub
                        editTasmotaResultSub = d.tasmotaResultSub; editPortalCmndPub = d.portalCmndPub
                        editWolWakePub = d.wolWakePub; editWolShutdownPub = d.wolShutdownPub
                        editTasmotaCmndPub = d.tasmotaCmndPub
                    },
                    onSave = {
                        onTopicsChange(MqttTopicConfig(
                            portalSub = editPortalSub, wolSub = editWolSub,
                            sensorsSub = editSensorsSub, wstSub = editWstSub, pvSub = editPvSub,
                            tasmotaStateSub = editTasmotaStateSub, tasmotaSensorSub = editTasmotaSensorSub,
                            tasmotaResultSub = editTasmotaResultSub, portalCmndPub = editPortalCmndPub,
                            wolWakePub = editWolWakePub, wolShutdownPub = editWolShutdownPub,
                            tasmotaCmndPub = editTasmotaCmndPub
                        ))
                    },
                    saveLabel = "Save & Reconnect"
                )
            }

            // HA Devices
            SettingsGroupLabel("HA")
            ExpandableSection(title = "Device IDs", icon = Icons.Default.Lightbulb) {
                FieldGroupLabel("Temperature")
                OutlinedTextField(value = editTempSensorId, onValueChange = { editTempSensorId = it },
                    label = { Text("Sensor ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                FieldGroupLabel("Solar PV")
                OutlinedTextField(value = editPvId, onValueChange = { editPvId = it },
                    label = { Text("Inverter ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                FieldGroupLabel("Energy")
                OutlinedTextField(value = editEnergyId, onValueChange = { editEnergyId = it },
                    label = { Text("Meter ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                FieldGroupLabel("Kommer")
                OutlinedTextField(value = editKommerSensorId, onValueChange = { editKommerSensorId = it },
                    label = { Text("Sensor ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = editKommerSwitchId, onValueChange = { editKommerSwitchId = it },
                    label = { Text("Switch ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                FieldGroupLabel("Brenner")
                OutlinedTextField(value = editBrennerSensor1Id, onValueChange = { editBrennerSensor1Id = it },
                    label = { Text("Sensor 1 ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = editBrennerSensor2Id, onValueChange = { editBrennerSensor2Id = it },
                    label = { Text("Sensor 2 ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = editBrennerSwitchId, onValueChange = { editBrennerSwitchId = it },
                    label = { Text("Switch ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                SaveResetRow(
                    onReset = {
                        val d = HADeviceConfig()
                        editTempSensorId = d.tempSensorId; editPvId = d.pvId; editEnergyId = d.energyId
                        editKommerSensorId = d.kommerSensorId; editKommerSwitchId = d.kommerSwitchId
                        editBrennerSensor1Id = d.brennerSensor1Id; editBrennerSensor2Id = d.brennerSensor2Id
                        editBrennerSwitchId = d.brennerSwitchId
                    },
                    onSave = {
                        onHADevicesChange(HADeviceConfig(
                            tempSensorId = editTempSensorId, pvId = editPvId, energyId = editEnergyId,
                            kommerSensorId = editKommerSensorId, kommerSwitchId = editKommerSwitchId,
                            brennerSensor1Id = editBrennerSensor1Id, brennerSensor2Id = editBrennerSensor2Id,
                            brennerSwitchId = editBrennerSwitchId
                        ))
                    }
                )
            }

            // About
            SettingsGroupLabel("About")
            SettingsCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            ImageView(ctx).apply {
                                setImageDrawable(ctx.packageManager.getApplicationIcon(ctx.packageName))
                            }
                        },
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Version $versionName  ·  $buildDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Monitor and control portals via MQTT over WebSockets",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Cache") },
            text = { Text("Remove all cached MQTT data? States will show as unknown until new messages arrive.") },
            confirmButton = {
                TextButton(onClick = { onClearCache(); showClearDialog = false }) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsGroupLabel(label: String) {
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ExpandableSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val chevron by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "chevron")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.rotate(chevron),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider()
                    content()
                }
            }
        }
    }
}

@Composable
private fun FieldGroupLabel(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun SaveResetRow(
    onReset: () -> Unit,
    onSave: () -> Unit,
    saveLabel: String = "Save"
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp)
        ) { Text("Reset") }
        Button(
            onClick = onSave,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp)
        ) { Text(saveLabel) }
    }
}
