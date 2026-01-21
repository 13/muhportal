package com.muhstudio.muhportal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.muhstudio.muhportal.ui.theme.MuhportalTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MuhportalTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                val pagerState = rememberPagerState(pageCount = { 3 })
                
                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            NavigationBar(
                                containerColor = Color.White,
                                tonalElevation = 8.dp
                            ) {
                                NavigationBarItem(
                                    selected = pagerState.currentPage == 0,
                                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                                    icon = { Icon(Icons.Default.Lock, contentDescription = "Portal") },
                                    label = { Text("Portal") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.Black,
                                        selectedTextColor = Color.Black,
                                        indicatorColor = Color(0xFFE0E0E0),
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray
                                    )
                                )
                                NavigationBarItem(
                                    selected = pagerState.currentPage == 1,
                                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                                    icon = { Icon(Icons.Default.Lan, contentDescription = "WOL") },
                                    label = { Text("WOL") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.Black,
                                        selectedTextColor = Color.Black,
                                        indicatorColor = Color(0xFFE0E0E0),
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray
                                    )
                                )
                                NavigationBarItem(
                                    selected = pagerState.currentPage == 2,
                                    onClick = { scope.launch { pagerState.animateScrollToPage(2) } },
                                    icon = { Icon(Icons.Default.Lightbulb, contentDescription = "HA") },
                                    label = { Text("HA") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.Black,
                                        selectedTextColor = Color.Black,
                                        indicatorColor = Color(0xFFE0E0E0),
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray
                                    )
                                )
                            }
                        }
                    ) { innerPadding ->
                        val context = LocalContext.current
                        var connState by remember { mutableStateOf(ConnState.DISCONNECTED) }
                        val portalStates = remember { mutableStateMapOf<String, PortalUpdate>() }
                        val wolStates = remember { mutableStateMapOf<String, WolUpdate>() }

                        val mqtt = remember {
                            GarageMqttClient(
                                context = context,
                                onConnState = { connState = it },
                                onPortalUpdate = { portalStates[it.id] = it },
                                onWolUpdate = { wolStates[it.id] = it }
                            )
                        }

                        val isPreview = androidx.compose.ui.platform.LocalInspectionMode.current
                        DisposableEffect(Unit) {
                            if (!isPreview) mqtt.connect()
                            onDispose { if (!isPreview) mqtt.disconnect() }
                        }

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.padding(innerPadding).fillMaxSize()
                        ) { page ->
                            when (page) {
                                0 -> PortalScreen(
                                    connState = connState,
                                    portalStates = portalStates,
                                    onRefresh = { mqtt.reconnect() },
                                    onToggle = { mqtt.toggle(it) },
                                    snackbarHostState = snackbarHostState
                                )
                                1 -> WolScreen(
                                    connState = connState,
                                    wolStates = wolStates,
                                    onRefresh = { mqtt.reconnect() },
                                    onWolAction = { mac, action -> mqtt.wolAction(mac, action) },
                                    snackbarHostState = snackbarHostState
                                )
                                2 -> GenericPlaceholderScreen("HA", connState) { mqtt.reconnect() }
                            }
                        }
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
    val now = Calendar.getInstance()
    val time = Calendar.getInstance().apply { timeInMillis = timestamp }
    
    val isSameDay = now.get(Calendar.YEAR) == time.get(Calendar.YEAR) &&
                    now.get(Calendar.DAY_OF_YEAR) == time.get(Calendar.DAY_OF_YEAR)
    
    val pattern = if (isSameDay) "HH:mm" else "dd.MM. HH:mm"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestamp))
}
