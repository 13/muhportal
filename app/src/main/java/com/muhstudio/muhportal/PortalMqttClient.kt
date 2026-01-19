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
    val timestamp: Long = System.currentTimeMillis()
)

class GarageMqttClient(
    context: Context,
    private val onConnState: (ConnState) -> Unit,
    private val onPortalUpdate: (PortalUpdate) -> Unit,
    private val onWolUpdate: (WolUpdate) -> Unit,
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
                    if (topic.startsWith(portalTopicPrefix)) {
                        val key = topic.removePrefix(portalTopicPrefix).removeSuffix(portalTopicSuffix)
                        parsePortalUpdate(key, payload)?.let { onPortalUpdate(it) }
                    } else if (topic.startsWith(wolTopicPrefix)) {
                        val key = topic.removePrefix(wolTopicPrefix)
                        if (key != "cmnd") {
                            parseWolUpdate(key, payload)?.let { onWolUpdate(it) }
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
                timestamp = tryParseTime(json) ?: System.currentTimeMillis()
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
