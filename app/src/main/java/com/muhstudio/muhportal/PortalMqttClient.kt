package com.muhstudio.muhportal

import android.content.Context
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.nio.charset.StandardCharsets
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
    private val topicTogglePrefix = "muh/portal/RLY/cmnd/"

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
                        parseDoorState(payload)?.let { state ->
                            onPortalUpdate(PortalUpdate(key, state))
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
            if (client.isConnected) client.disconnect()
        } catch (_: Throwable) {}
        onConnState(ConnState.DISCONNECTED)
    }

    fun toggle(key: String) {
        if (!client.isConnected) return
        val toggleTopic = when(key) {
            "G" -> "${topicTogglePrefix}G_TOGGLE"
            "GD" -> "${topicTogglePrefix}GD_TOGGLE"
            "GDL" -> "${topicTogglePrefix}GDL_TOGGLE"
            "HD" -> "${topicTogglePrefix}HD_TOGGLE"
            "HDL" -> "${topicTogglePrefix}HDL_TOGGLE"
            else -> return
        }
        val msg = MqttMessage("TOGGLE".toByteArray()).apply { qos = 0 }
        try {
            client.publish(toggleTopic, msg)
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    private fun parseDoorState(json: String): DoorState? {
        val s = json.replace(" ", "")
        return when {
            s.contains("\"state\":0") -> DoorState.OPEN
            s.contains("\"state\":1") -> DoorState.CLOSED
            else -> null
        }
    }

    fun getPortalName(key: String) = portals[key] ?: key
    fun getPortalKeys() = portals.keys.toList()
}