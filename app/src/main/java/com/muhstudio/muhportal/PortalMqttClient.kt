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

class GarageMqttClient(
    context: Context,
    private val onConnState: (ConnState) -> Unit,
    private val onPortalUpdate: (PortalUpdate) -> Unit,
) {
    private val serverUri = "ws://192.168.22.5:1884"
    private val clientId = "muhportal-" + UUID.randomUUID().toString()
    private val persistence = MemoryPersistence()
    private val client = MqttAsyncClient(serverUri, clientId, persistence)

    // Portal configurations: Map of suffix to display name
    private val portals = mapOf(
        "G" to "Garage",
        "GD" to "Garage Door",
        "GDL" to "Garage Door Lock",
        "HD" to "House Door",
        "HDL" to "House Door Lock"
    )

    private val topicPrefix = "muh/portal/"
    private val topicSuffix = "/json"
    private val topicToggleTopic = "muh/portal/RLY/cmnd"

    fun connect() {
        if (client.isConnected) return
        
        onConnState(ConnState.CONNECTING)

        client.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                onConnState(ConnState.CONNECTED)
                try {
                    portals.keys.forEach { key ->
                        client.subscribe("$topicPrefix$key$topicSuffix", 0)
                    }
                } catch (e: MqttException) {
                    e.printStackTrace()
                }
            }

            override fun connectionLost(cause: Throwable?) {
                onConnState(ConnState.DISCONNECTED)
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                if (topic != null && message != null) {
                    val key = topic.removePrefix(topicPrefix).removeSuffix(topicSuffix)
                    if (portals.containsKey(key)) {
                        val payload = message.payload.toString(StandardCharsets.UTF_8)
                        parsePortalUpdate(key, payload)?.let { update ->
                            onPortalUpdate(update)
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

    fun toggle(key: String) {
        if (!client.isConnected) {
            connect()
            return
        }
        val command = when(key) {
            "G", "G_T" -> "G_T"
            "GDL_O" -> "GD_O"
            "GDL_U" -> "GD_U"
            "GDL_L" -> "GD_L"
            "HDL_O" -> "HD_O"
            "HDL_U" -> "HD_U"
            "HDL_L" -> "HD_L"
            else -> return
        }
        val msg = MqttMessage(command.toByteArray(StandardCharsets.UTF_8)).apply { qos = 0 }
        try {
            client.publish(topicToggleTopic, msg)
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

            var timestamp = System.currentTimeMillis()
            val timeKeys = listOf("time", "Time", "timestamp", "ts")
            for (tk in timeKeys) {
                if (json.has(tk)) {
                    val timeValue = json.getString(tk)
                    val parsed = tryParseTime(timeValue)
                    if (parsed != null) {
                        timestamp = parsed
                        break
                    }
                }
            }
            PortalUpdate(key, state, timestamp)
        } catch (e: Exception) {
            // Fallback for non-JSON or missing fields
            val s = jsonStr.replace(" ", "")
            val state = when {
                s.contains("\"state\":0") -> DoorState.OPEN
                s.contains("\"state\":1") -> DoorState.CLOSED
                else -> return null
            }
            PortalUpdate(key, state, System.currentTimeMillis())
        }
    }

    private fun tryParseTime(timeStr: String): Long? {
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy/MM/dd HH:mm:ss",
            "HH:mm:ss dd.MM.yyyy",
            "dd.MM.yyyy HH:mm:ss"
        )
        for (fmt in formats) {
            try {
                return SimpleDateFormat(fmt, Locale.getDefault()).parse(timeStr)?.time
            } catch (e: Exception) {}
        }
        return timeStr.toLongOrNull()?.let {
            if (it < 10000000000L) it * 1000 else it
        }
    }

    fun getPortalName(key: String) = portals[key] ?: key
    fun getPortalKeys() = portals.keys.toList()
}
