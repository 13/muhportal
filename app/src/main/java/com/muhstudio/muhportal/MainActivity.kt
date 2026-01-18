package com.muhstudio.muhportal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.muhstudio.muhportal.ui.theme.MuhportalTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MuhportalTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        val context = LocalContext.current
                        var isRefreshing by remember { mutableStateOf(false) }
                        var connState by remember { mutableStateOf(ConnState.DISCONNECTED) }
                        val portalStates = remember { mutableStateMapOf<String, PortalUpdate>() }

                        val mqtt = remember {
                            GarageMqttClient(
                                context = context,
                                onConnState = {
                                    connState = it
                                    if (it != ConnState.CONNECTING) {
                                        isRefreshing = false
                                    }
                                },
                                onPortalUpdate = { update ->
                                    portalStates[update.id] = update
                                }
                            )
                        }

                        val isPreview = androidx.compose.ui.platform.LocalInspectionMode.current
                        DisposableEffect(Unit) {
                            if (!isPreview) mqtt.connect()
                            onDispose { if (!isPreview) mqtt.disconnect() }
                        }

                        PortalScreen(
                            connState = connState,
                            portalStates = portalStates,
                            isRefreshing = isRefreshing,
                            onRefresh = {
                                isRefreshing = true
                                mqtt.reconnect()
                            },
                            onToggle = { key -> mqtt.toggle(key) },
                            snackbarHostState = snackbarHostState,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }

                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 8.dp)
                    ) { data ->
                        Snackbar(
                            containerColor = Color(0xFF4CAF50),
                            contentColor = Color.White,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(data.visuals.message)
                        }
                    }
                }
            }
        }
    }
}

fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val pattern = if (diff < 24 * 60 * 60 * 1000L) "HH:mm" else "dd.MM. HH:mm"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestamp))
}
