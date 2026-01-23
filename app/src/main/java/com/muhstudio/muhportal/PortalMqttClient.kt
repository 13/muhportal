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

class GarageMqttClient(
    context: Context,
    private val onConnState: (ConnState) -> Unit,
    private val onPortalUpdate: (PortalUpdate) -> Unit,
    private val onWolUpdate: (WolUpdate) -> Unit,
    private val onSensorUpdate: (SensorUpdate) -> Unit,
    private val onSwitchUpdate: (SwitchUpdate) -> Unit,
    private val onPvUpdate: (PvUpdate) -> Unit,
) {
    private val serverUri = "ws://192.168.22.5:1884"
    private val clientId = "muhportal-" + UUID.randomUUID().toString()
    private val persistence = MemoryPersistence()
    private val client = MqttAsyncClient(serverUri, clientId, persistence)

    private val portalTopicPrefix = "muh/portal/"
    private val wolTopicPrefix = "muh/pc/"
    private val portalTopicSuffix = "/json"
    
    private val topicToggleTopic = "muh/portal/RLY/cmnd"
    private val wolWakeTopic = "muh/wol"
    private val wolShutdownTopic = "muh/poweroff"

    fun connect() {
        if (client.isConnected) return
        
        onConnState(ConnState.CONNECTING)

        client.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                onConnState(ConnState.CONNECTED)
                try {
                    client.subscribe("muh/portal/+/json", 0)
                    client.subscribe("muh/pc/+", 0)
                    client.subscribe("muh/sensors/#", 0)
                    client.subscribe("muh/wst/data/+", 0)
                    client.subscribe("muh/pv/+/json", 0)
                    client.subscribe("tasmota/tele/+/STATE", 0)
                    client.subscribe("tasmota/stat/+/RESULT", 0)
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
                    when {
                        topic.startsWith(portalTopicPrefix) -> {
                            val key = topic.removePrefix(portalTopicPrefix).removeSuffix(portalTopicSuffix)
                            parsePortalUpdate(key, payload)?.let { onPortalUpdate(it) }
                        }
                        topic.startsWith(wolTopicPrefix) -> {
                            val key = topic.removePrefix(wolTopicPrefix)
                            if (key != "cmnd") {
                                parseWolUpdate(key, payload)?.let { onWolUpdate(it) }
                            }
                        }
                        topic.startsWith("muh/sensors/") -> {
                            val parts = topic.split("/")
                            if (parts.size >= 3) {
                                val id = if (parts.last() == "json") parts[parts.size - 2] else parts.last()
                                parseSensorUpdate(id, payload)?.let { onSensorUpdate(it) }
                            }
                        }
                        topic.startsWith("muh/wst/data/") -> {
                            val id = topic.removePrefix("muh/wst/data/")
                            parseWstUpdate(id, payload)?.let { onSensorUpdate(it) }
                        }
                        topic.startsWith("muh/pv/") -> {
                            val id = topic.removePrefix("muh/pv/").removeSuffix("/json")
                            parsePvUpdate(id, payload)?.let { onPvUpdate(it) }
                        }
                        topic.startsWith("tasmota/tele/") || topic.startsWith("tasmota/stat/") -> {
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
            try {
                if (client.isConnected) {
                    client.disconnect().waitForCompletion(2000)
                }
            } catch (_: Throwable) {}
            connect()
        }.start()
    }

    fun toggle(command: String) {
        if (!client.isConnected) return
        val msg = MqttMessage(command.toByteArray(StandardCharsets.UTF_8)).apply { qos = 0 }
        try {
            client.publish(topicToggleTopic, msg)
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun wolAction(mac: String, action: String) {
        if (!client.isConnected) return
        val topic = if (action == "WAKE") wolWakeTopic else wolShutdownTopic
        val payload = org.json.JSONObject().apply { put("mac", mac) }.toString()
        val msg = MqttMessage(payload.toByteArray(StandardCharsets.UTF_8)).apply { qos = 0 }
        try {
            client.publish(topic, msg)
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun setPower(deviceId: String, state: Boolean) {
        if (!client.isConnected) return
        val topic = "tasmota/cmnd/$deviceId/POWER"
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
                json.has("T1") -> json.getDouble("T1").toFloat()
                json.has("DS18B20") -> json.getJSONObject("DS18B20").getDouble("Temperature").toFloat()
                else -> return null
            }
            val humidity = if (json.has("H1")) json.getDouble("H1").toFloat() else 0f
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
            val temp = if (json.has("temp_c")) json.getDouble("temp_c").toFloat() else return null
            val humidity = if (json.has("humidity")) json.getDouble("humidity").toFloat() else 0f
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
