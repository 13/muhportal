package com.muhstudio.muhportal

import android.content.Context
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

enum class DoorState { OPEN, CLOSED, UNKNOWN }
enum class ConnState { CONNECTING, CONNECTED, DISCONNECTED }
enum class AlarmState { ARM_AWAY, ARM_HOME, DISARM }

data class AlarmAlert(
    val device: String,
    val label: String,
    val alarmState: String,
    val time: String,
    val ts: Long = System.currentTimeMillis()
)

data class AwaySim(
    val active: Boolean,
    val manual_active: Boolean,
    val schedule_enabled: Boolean,
    val schedule_active: Boolean,
    val schedule_start: String,
    val schedule_end: String,
    val current_pool_light: String?
)

data class PortalUpdate(
    val id: String,
    val state: DoorState,
    val timestamp: Long = System.currentTimeMillis()
)

data class WolUpdate(
    val id: String,
    val name: String,
    val ip: String,
    val mac: String,
    val alive: Boolean,
    val priority: Int = 99,
    val timestamp: Long = System.currentTimeMillis()
)

data class SensorUpdate(
    val id: String,
    val temp: Float,
    val humidity: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

data class SwitchUpdate(
    val id: String,
    val state: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class PvUpdate(
    val id: String,
    val p1: Float,
    val p2: Float,
    val e1: Float,
    val e2: Float,
    val timestamp: Long = System.currentTimeMillis()
)

data class EnergyUpdate(
    val id: String,
    val activePower: Float,
    val todayImport: Float,
    val todayExport: Float,
    val timestamp: Long = System.currentTimeMillis()
)

data class MqttConnectionConfig(
    val serverUri: String = "ws://192.168.22.5:1884",
    val username: String = "",
    val password: String = ""
)

data class HADeviceConfig(
    val tempSensorId: String = "B327",
    val pvId: String = "E07000055917",
    val energyId: String = "tasmota_5FF8B2",
    val kommerSensorId: String = "87",
    val kommerSwitchId: String = "tasmota_BDC5E0",
    val brennerSensor1Id: String = "DS18B20-3628FF",
    val brennerSensor2Id: String = "DS18B20-1C16E1",
    val brennerSwitchId: String = "tasmota_A7EEA3",
    val sensorTempKey: String = "T_SI",
    val sensorDs18b20Key: String = "DS18B20",
    val sensorDs18b20TempKey: String = "Temperature",
    val sensorHumidityKey: String = "H_SI",
    val wstTempKey: String = "temp_c",
    val wstHumidityKey: String = "humidity"
)

data class MqttTopicConfig(
    val portalSub: String = "muh/portal/+/json",
    val wolSub: String = "muh/pc/+",
    val sensorsSub: String = "muh/sensors/#",
    val wstSub: String = "muh/wst/data/+",
    val pvSub: String = "muh/pv/+/json",
    val tasmotaStateSub: String = "tasmota/tele/+/STATE",
    val tasmotaSensorSub: String = "tasmota/tele/+/SENSOR",
    val tasmotaResultSub: String = "tasmota/stat/+/RESULT",
    val portalCmndPub: String = "muh/portal/RLY/cmnd",
    val wolWakePub: String = "muh/wol",
    val wolShutdownPub: String = "muh/poweroff",
    val tasmotaCmndPub: String = "tasmota/cmnd/{id}/POWER",
    val alarmStateSub: String = "muh/alarm/state",
    val alarmAlertSub: String = "muh/alarm/alert",
    val alarmSetPub: String = "muh/alarm/set",
    val awaySimStatusSub: String = "muh/awaysim/status",
    val awaySimManualSetPub: String = "muh/awaysim/manual/set",
    val awaySimScheduleEnablePub: String = "muh/awaysim/schedule/set",
    val awaySimScheduleStartPub: String = "muh/awaysim/schedule/start/set",
    val awaySimScheduleEndPub: String = "muh/awaysim/schedule/end/set"
)

class GarageMqttClient(
    context: Context,
    var connectionConfig: MqttConnectionConfig = MqttConnectionConfig(),
    var config: MqttTopicConfig = MqttTopicConfig(),
    var haDeviceConfig: HADeviceConfig = HADeviceConfig(),
    private val onConnState: (ConnState) -> Unit,
    private val onPortalUpdate: (PortalUpdate) -> Unit,
    private val onWolUpdate: (WolUpdate) -> Unit,
    private val onSensorUpdate: (SensorUpdate) -> Unit,
    private val onSwitchUpdate: (SwitchUpdate) -> Unit,
    private val onPvUpdate: (PvUpdate) -> Unit,
    private val onEnergyUpdate: (EnergyUpdate) -> Unit,
    private val onAlarmStateUpdate: (AlarmState) -> Unit = {},
    private val onAlarmAlert: (AlarmAlert) -> Unit = {},
    private val onAwaySimUpdate: (AwaySim) -> Unit = {},
) {
    private val clientId = "muhportal-" + UUID.randomUUID().toString()
    private val persistence = MemoryPersistence()
    private var client = MqttAsyncClient(connectionConfig.serverUri, clientId, persistence)

    private fun topicPrefix(topic: String): String =
        topic.substringBefore("+").substringBefore("#")

    private fun topicSuffix(topic: String): String =
        if (topic.contains("+")) topic.substringAfter("+") else ""

    fun connect() {
        if (client.isConnected) return
        
        onConnState(ConnState.CONNECTING)

        client.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                onConnState(ConnState.CONNECTED)
                try {
                    client.subscribe(config.portalSub, 0)
                    client.subscribe(config.wolSub, 0)
                    client.subscribe(config.sensorsSub, 0)
                    client.subscribe(config.wstSub, 0)
                    client.subscribe(config.pvSub, 0)
                    client.subscribe(config.tasmotaStateSub, 0)
                    client.subscribe(config.tasmotaSensorSub, 0)
                    client.subscribe(config.tasmotaResultSub, 0)
                    client.subscribe(config.alarmStateSub, 1)
                    client.subscribe(config.alarmAlertSub, 1)
                    client.subscribe(config.awaySimStatusSub, 1)
                } catch (e: MqttException) {
                    e.printStackTrace()
                }
            }

            override fun connectionLost(cause: Throwable?) {
                onConnState(ConnState.DISCONNECTED)
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                if (topic != null && message != null) {
                    val payload = message.payload.toString(StandardCharsets.UTF_8)
                    val portalPrefix = topicPrefix(config.portalSub)
                    val portalSuffix = topicSuffix(config.portalSub)
                    val wolPrefix = topicPrefix(config.wolSub)
                    val sensorsPrefix = topicPrefix(config.sensorsSub)
                    val wstPrefix = topicPrefix(config.wstSub)
                    val pvPrefix = topicPrefix(config.pvSub)
                    val pvSuffix = topicSuffix(config.pvSub)
                    val tasmotaSensorSuffix = topicSuffix(config.tasmotaSensorSub)
                    val tasmotaTelePrefix = topicPrefix(config.tasmotaStateSub)
                    val tasmotaStatPrefix = topicPrefix(config.tasmotaResultSub)
                    when {
                        topic == config.alarmStateSub -> {
                            AlarmState.entries.firstOrNull { it.name == payload }
                                ?.let { onAlarmStateUpdate(it) }
                        }
                        topic == config.alarmAlertSub -> {
                            parseAlarmAlert(payload)?.let { onAlarmAlert(it) }
                        }
                        topic == config.awaySimStatusSub -> {
                            parseAwaySim(payload)?.let { onAwaySimUpdate(it) }
                        }
                        topic.startsWith(portalPrefix) -> {
                            val key = topic.removePrefix(portalPrefix).removeSuffix(portalSuffix)
                            parsePortalUpdate(key, payload)?.let { onPortalUpdate(it) }
                        }
                        topic.startsWith(wolPrefix) -> {
                            val key = topic.removePrefix(wolPrefix)
                            if (key != "cmnd") {
                                parseWolUpdate(key, payload)?.let { onWolUpdate(it) }
                            }
                        }
                        topic.startsWith(sensorsPrefix) -> {
                            val parts = topic.split("/")
                            if (parts.size >= 3) {
                                val id = if (parts.last() == "json") parts[parts.size - 2] else parts.last()
                                parseSensorUpdate(id, payload)?.let { onSensorUpdate(it) }
                            }
                        }
                        topic.startsWith(wstPrefix) -> {
                            val id = topic.removePrefix(wstPrefix)
                            parseWstUpdate(id, payload)?.let { onSensorUpdate(it) }
                        }
                        topic.startsWith(pvPrefix) -> {
                            val id = topic.removePrefix(pvPrefix).removeSuffix(pvSuffix)
                            parsePvUpdate(id, payload)?.let { onPvUpdate(it) }
                        }
                        tasmotaSensorSuffix.isNotEmpty() && topic.endsWith(tasmotaSensorSuffix) -> {
                            val key = topic.split("/")[2]
                            parseEnergyUpdate(key, payload)?.let { onEnergyUpdate(it) }
                        }
                        topic.startsWith(tasmotaTelePrefix) || topic.startsWith(tasmotaStatPrefix) -> {
                            val key = topic.split("/")[2]
                            parseTasmotaUpdate(key, payload)?.let { onSwitchUpdate(it) }
                        }
                    }
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) = Unit
        })

        val opts = MqttConnectOptions().apply {
            isAutomaticReconnect = true
            isCleanSession = true
            connectionTimeout = 10
            keepAliveInterval = 60
            if (connectionConfig.username.isNotEmpty()) {
                userName = connectionConfig.username
                password = connectionConfig.password.toCharArray()
            }
        }

        try {
            client.connect(opts, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {}
                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    onConnState(ConnState.DISCONNECTED)
                }
            })
        } catch (e: MqttException) {
            onConnState(ConnState.DISCONNECTED)
        }
    }

    fun disconnect() {
        try {
            if (client.isConnected) {
                client.disconnect().waitForCompletion(1000)
            }
        } catch (_: Throwable) {}
        onConnState(ConnState.DISCONNECTED)
    }

    fun reconnect() {
        onConnState(ConnState.CONNECTING)
        Thread {
            val serverUriChanged = client.serverURI != connectionConfig.serverUri
            try {
                if (client.isConnected) {
                    client.disconnect().waitForCompletion(2000)
                }
                if (serverUriChanged) client.close()
            } catch (_: Throwable) {}
            if (serverUriChanged) {
                client = MqttAsyncClient(connectionConfig.serverUri, clientId, persistence)
            }
            connect()
        }.start()
    }

    fun toggle(command: String) {
        if (!client.isConnected) return
        val msg = MqttMessage(command.toByteArray(StandardCharsets.UTF_8)).apply { qos = 0 }
        try {
            client.publish(config.portalCmndPub, msg)
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun wolAction(mac: String, action: String) {
        if (!client.isConnected) return
        val topic = if (action == "WAKE") config.wolWakePub else config.wolShutdownPub
        val payload = org.json.JSONObject().apply { put("mac", mac) }.toString()
        val msg = MqttMessage(payload.toByteArray(StandardCharsets.UTF_8)).apply { qos = 0 }
        try {
            client.publish(topic, msg)
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun setAlarm(state: AlarmState) {
        if (!client.isConnected) return
        val msg = MqttMessage(state.name.toByteArray(StandardCharsets.UTF_8)).apply {
            qos = 1
            isRetained = false
        }
        try {
            client.publish(config.alarmSetPub, msg)
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun setAwaySimManual(active: Boolean) {
        if (!client.isConnected) return
        val payload = if (active) "ON" else "OFF"
        val msg = MqttMessage(payload.toByteArray(StandardCharsets.UTF_8)).apply {
            qos = 1
            isRetained = false
        }
        try {
            client.publish(config.awaySimManualSetPub, msg)
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun setAwaySimScheduleEnabled(enabled: Boolean) {
        if (!client.isConnected) return
        val payload = if (enabled) "ON" else "OFF"
        val msg = MqttMessage(payload.toByteArray(StandardCharsets.UTF_8)).apply {
            qos = 1
            isRetained = false
        }
        try {
            client.publish(config.awaySimScheduleEnablePub, msg)
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun setAwaySimScheduleStart(time: String) {
        if (!client.isConnected) return
        val msg = MqttMessage(time.toByteArray(StandardCharsets.UTF_8)).apply {
            qos = 1
            isRetained = false
        }
        try {
            client.publish(config.awaySimScheduleStartPub, msg)
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun setAwaySimScheduleEnd(time: String) {
        if (!client.isConnected) return
        val msg = MqttMessage(time.toByteArray(StandardCharsets.UTF_8)).apply {
            qos = 1
            isRetained = false
        }
        try {
            client.publish(config.awaySimScheduleEndPub, msg)
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun setPower(deviceId: String, state: Boolean) {
        if (!client.isConnected) return
        val topic = config.tasmotaCmndPub.replace("{id}", deviceId)
        val payload = if (state) "1" else "0"
        val msg = MqttMessage(payload.toByteArray(StandardCharsets.UTF_8)).apply { qos = 0 }
        try {
            client.publish(topic, msg)
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    private fun parsePortalUpdate(key: String, jsonStr: String): PortalUpdate? {
        return try {
            val json = org.json.JSONObject(jsonStr)
            val stateInt = if (json.has("state")) json.getInt("state") else -1
            val state = when (stateInt) {
                0 -> DoorState.OPEN
                1 -> DoorState.CLOSED
                else -> return null
            }
            PortalUpdate(key, state, tryParseTime(json) ?: System.currentTimeMillis())
        } catch (e: Exception) {
            null
        }
    }

    private fun parseWolUpdate(key: String, jsonStr: String): WolUpdate? {
        return try {
            val json = org.json.JSONObject(jsonStr)
            WolUpdate(
                id = key,
                name = json.getString("name"),
                ip = json.getString("ip"),
                mac = json.getString("mac"),
                alive = json.getBoolean("alive"),
                priority = if (json.has("priority")) json.getInt("priority") else 99,
                timestamp = tryParseTime(json) ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseSensorUpdate(key: String, jsonStr: String): SensorUpdate? {
        return try {
            val json = org.json.JSONObject(jsonStr)
            val temp = when {
                json.has(haDeviceConfig.sensorTempKey) -> json.getDouble(haDeviceConfig.sensorTempKey).toFloat()
                json.has(haDeviceConfig.sensorDs18b20Key) -> json.getJSONObject(haDeviceConfig.sensorDs18b20Key).getDouble(haDeviceConfig.sensorDs18b20TempKey).toFloat()
                else -> return null
            }
            val humidity = if (json.has(haDeviceConfig.sensorHumidityKey)) json.getDouble(haDeviceConfig.sensorHumidityKey).toFloat() else 0f
            SensorUpdate(
                id = key,
                temp = temp,
                humidity = humidity,
                timestamp = tryParseTime(json) ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseWstUpdate(key: String, jsonStr: String): SensorUpdate? {
        return try {
            val json = org.json.JSONObject(jsonStr)
            val temp = if (json.has(haDeviceConfig.wstTempKey)) json.getDouble(haDeviceConfig.wstTempKey).toFloat() else return null
            val humidity = if (json.has(haDeviceConfig.wstHumidityKey)) json.getDouble(haDeviceConfig.wstHumidityKey).toFloat() else 0f
            SensorUpdate(
                id = key,
                temp = temp,
                humidity = humidity,
                timestamp = tryParseTime(json) ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parsePvUpdate(key: String, jsonStr: String): PvUpdate? {
        return try {
            val json = org.json.JSONObject(jsonStr)
            val data = json.getJSONObject("data")
            PvUpdate(
                id = key,
                p1 = data.getDouble("p1").toFloat(),
                p2 = data.getDouble("p2").toFloat(),
                e1 = data.getDouble("e1").toFloat(),
                e2 = data.getDouble("e2").toFloat(),
                timestamp = tryParseTime(json) ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseEnergyUpdate(key: String, jsonStr: String): EnergyUpdate? {
        return try {
            val json = org.json.JSONObject(jsonStr)
            if (json.has("ENERGY")) {
                val energy = json.getJSONObject("ENERGY")
                val activePowerArray = energy.getJSONArray("Power")
                var totalActivePower = 0f
                for (i in 0 until activePowerArray.length()) {
                    totalActivePower += activePowerArray.getDouble(i).toFloat()
                }
                EnergyUpdate(
                    id = key,
                    activePower = totalActivePower,
                    todayImport = energy.getDouble("TodaySumImport").toFloat(),
                    todayExport = energy.getDouble("TodaySumExport").toFloat(),
                    timestamp = tryParseTime(json) ?: System.currentTimeMillis()
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun parseTasmotaUpdate(key: String, jsonStr: String): SwitchUpdate? {
        return try {
            val json = org.json.JSONObject(jsonStr)
            if (json.has("POWER")) {
                val powerStr = json.getString("POWER")
                val state = powerStr == "ON" || powerStr == "1"
                SwitchUpdate(key, state, tryParseTime(json) ?: System.currentTimeMillis())
            } else if (json.has("POWER1")) { // Some tasmota devices use POWER1
                val powerStr = json.getString("POWER1")
                val state = powerStr == "ON" || powerStr == "1"
                SwitchUpdate(key, state, tryParseTime(json) ?: System.currentTimeMillis())
            } else null
        } catch (e: Exception) {
            // Check if it's just a raw "0" or "1"
            if (jsonStr == "0" || jsonStr == "1") {
                SwitchUpdate(key, jsonStr == "1", System.currentTimeMillis())
            } else null
        }
    }

    private fun parseAwaySim(jsonStr: String): AwaySim? {
        return try {
            val json = org.json.JSONObject(jsonStr)
            AwaySim(
                active = json.optBoolean("active", false),
                manual_active = json.optBoolean("manual_active", false),
                schedule_enabled = json.optBoolean("schedule_enabled", false),
                schedule_active = json.optBoolean("schedule_active", false),
                schedule_start = json.optString("schedule_start", "22:00"),
                schedule_end = json.optString("schedule_end", "06:00"),
                current_pool_light = json.optString("current_pool_light", "").takeIf { it.isNotEmpty() }
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseAlarmAlert(jsonStr: String): AlarmAlert? {
        return try {
            val json = org.json.JSONObject(jsonStr)
            AlarmAlert(
                device = json.optString("device", ""),
                label = json.optString("label", ""),
                alarmState = json.optString("alarmState", ""),
                time = json.optString("time", ""),
                ts = tryParseTime(json) ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun tryParseTime(json: org.json.JSONObject): Long? {
        val timeKeys = listOf("timestamp", "time", "Time", "ts", "last_seen")
        for (tk in timeKeys) {
            if (json.has(tk)) {
                val timeStr = json.getString(tk)
                val formats = listOf(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss.SSS",
                    "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm:ss",
                    "yyyy-MM-dd HH:mm:ss", "dd.MM.yyyy HH:mm:ss"
                )
                for (fmt in formats) {
                    try {
                        val sdf = SimpleDateFormat(fmt, Locale.getDefault())
                        if (fmt.endsWith("'Z'")) {
                            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                        }
                        return sdf.parse(timeStr)?.time
                    } catch (e: Exception) {}
                }
                timeStr.toLongOrNull()?.let { return if (it < 10000000000L) it * 1000 else it }
            }
        }
        return null
    }
}
