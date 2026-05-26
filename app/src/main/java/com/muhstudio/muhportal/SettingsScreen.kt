package com.muhstudio.muhportal

import android.widget.ImageView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    var editServerUri by remember(mqttConnection) { mutableStateOf(mqttConnection.serverUri) }
    var editUsername by remember(mqttConnection) { mutableStateOf(mqttConnection.username) }
    var editPassword by remember(mqttConnection) { mutableStateOf(mqttConnection.password) }
    var passwordVisible by remember { mutableStateOf(false) }

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
        try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (_: Exception) {
            null
        }
    }

    val versionName = packageInfo?.versionName ?: "Unknown"
    val buildDate = remember(packageInfo) {
        if (packageInfo != null) {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(Date(packageInfo.lastUpdateTime))
        } else {
            "Unknown"
        }
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Dark Mode", fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = onDarkModeChange
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Black White Mode", fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
                Switch(
                    checked = isBlackWhiteMode,
                    onCheckedChange = onBlackWhiteModeChange
                )
            }
            
            HorizontalDivider()

            Button(
                onClick = { showClearDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = MaterialTheme.shapes.small
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Clear Local Cache")
            }
            
            HorizontalDivider()

            Text(
                "HA Device IDs",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            OutlinedTextField(
                value = editTempSensorId, onValueChange = { editTempSensorId = it },
                label = { Text("Temperatur sensor") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = editPvId, onValueChange = { editPvId = it },
                label = { Text("PV inverter") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = editEnergyId, onValueChange = { editEnergyId = it },
                label = { Text("Energy meter") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = editKommerSensorId, onValueChange = { editKommerSensorId = it },
                label = { Text("Kommer sensor") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = editKommerSwitchId, onValueChange = { editKommerSwitchId = it },
                label = { Text("Kommer switch") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = editBrennerSensor1Id, onValueChange = { editBrennerSensor1Id = it },
                label = { Text("Brenner sensor 1") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = editBrennerSensor2Id, onValueChange = { editBrennerSensor2Id = it },
                label = { Text("Brenner sensor 2") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = editBrennerSwitchId, onValueChange = { editBrennerSwitchId = it },
                label = { Text("Brenner switch") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val d = HADeviceConfig()
                        editTempSensorId = d.tempSensorId
                        editPvId = d.pvId
                        editEnergyId = d.energyId
                        editKommerSensorId = d.kommerSensorId
                        editKommerSwitchId = d.kommerSwitchId
                        editBrennerSensor1Id = d.brennerSensor1Id
                        editBrennerSensor2Id = d.brennerSensor2Id
                        editBrennerSwitchId = d.brennerSwitchId
                    },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small
                ) { Text("Reset") }
                Button(
                    onClick = {
                        onHADevicesChange(HADeviceConfig(
                            tempSensorId = editTempSensorId,
                            pvId = editPvId,
                            energyId = editEnergyId,
                            kommerSensorId = editKommerSensorId,
                            kommerSwitchId = editKommerSwitchId,
                            brennerSensor1Id = editBrennerSensor1Id,
                            brennerSensor2Id = editBrennerSensor2Id,
                            brennerSwitchId = editBrennerSwitchId
                        ))
                    },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small
                ) { Text("Save") }
            }

            HorizontalDivider()

            Text(
                "MQTT Connection",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

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
                        Text(if (passwordVisible) "Hide" else "Show", fontSize = 12.sp)
                    }
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val d = MqttConnectionConfig()
                        editServerUri = d.serverUri
                        editUsername = d.username
                        editPassword = d.password
                    },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small
                ) { Text("Reset") }
                Button(
                    onClick = {
                        onConnectionChange(MqttConnectionConfig(
                            serverUri = editServerUri,
                            username = editUsername,
                            password = editPassword
                        ))
                    },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small
                ) { Text("Save & Reconnect") }
            }

            HorizontalDivider()

            Text(
                "MQTT Topics",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                "Subscribe",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = editPortalSub, onValueChange = { editPortalSub = it },
                label = { Text("Portal States") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = editWolSub, onValueChange = { editWolSub = it },
                label = { Text("Wake-on-LAN") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = editSensorsSub, onValueChange = { editSensorsSub = it },
                label = { Text("Sensors") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = editWstSub, onValueChange = { editWstSub = it },
                label = { Text("Weather Station") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = editPvSub, onValueChange = { editPvSub = it },
                label = { Text("Solar PV") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = editTasmotaStateSub, onValueChange = { editTasmotaStateSub = it },
                label = { Text("Tasmota State") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = editTasmotaSensorSub, onValueChange = { editTasmotaSensorSub = it },
                label = { Text("Tasmota Sensor") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = editTasmotaResultSub, onValueChange = { editTasmotaResultSub = it },
                label = { Text("Tasmota Result") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )

            Text(
                "Publish",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = editPortalCmndPub, onValueChange = { editPortalCmndPub = it },
                label = { Text("Portal Command") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = editWolWakePub, onValueChange = { editWolWakePub = it },
                label = { Text("WOL Wake") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = editWolShutdownPub, onValueChange = { editWolShutdownPub = it },
                label = { Text("WOL Shutdown") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = editTasmotaCmndPub, onValueChange = { editTasmotaCmndPub = it },
                label = { Text("Tasmota Power") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Text(
                "{id} is replaced with the device ID",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val d = MqttTopicConfig()
                        editPortalSub = d.portalSub
                        editWolSub = d.wolSub
                        editSensorsSub = d.sensorsSub
                        editWstSub = d.wstSub
                        editPvSub = d.pvSub
                        editTasmotaStateSub = d.tasmotaStateSub
                        editTasmotaSensorSub = d.tasmotaSensorSub
                        editTasmotaResultSub = d.tasmotaResultSub
                        editPortalCmndPub = d.portalCmndPub
                        editWolWakePub = d.wolWakePub
                        editWolShutdownPub = d.wolShutdownPub
                        editTasmotaCmndPub = d.tasmotaCmndPub
                    },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Reset")
                }
                Button(
                    onClick = {
                        onTopicsChange(MqttTopicConfig(
                            portalSub = editPortalSub,
                            wolSub = editWolSub,
                            sensorsSub = editSensorsSub,
                            wstSub = editWstSub,
                            pvSub = editPvSub,
                            tasmotaStateSub = editTasmotaStateSub,
                            tasmotaSensorSub = editTasmotaSensorSub,
                            tasmotaResultSub = editTasmotaResultSub,
                            portalCmndPub = editPortalCmndPub,
                            wolWakePub = editWolWakePub,
                            wolShutdownPub = editWolShutdownPub,
                            tasmotaCmndPub = editTasmotaCmndPub
                        ))
                    },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Save & Reconnect")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AndroidView(
                    factory = { ctx ->
                        ImageView(ctx).apply {
                            setImageDrawable(ctx.packageManager.getApplicationIcon(ctx.packageName))
                        }
                    },
                    modifier = Modifier.size(80.dp)
                )

                Text(
                    text = stringResource(id = R.string.app_name),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Monitor and control various portals (doors and locks) via MQTT over WebSockets",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Version $versionName",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Build Date: $buildDate",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Cache") },
            text = { Text("Are you sure you want to delete all cached MQTT data? This will clear all visible states until new updates are received.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearCache()
                        showClearDialog = false
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
