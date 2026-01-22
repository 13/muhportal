package com.muhstudio.muhportal

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.muhstudio.muhportal.ui.theme.MuhportalTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
            val systemDark = isSystemInDarkTheme()
            var isDarkMode by remember { 
                mutableStateOf(prefs.getBoolean("dark_mode", systemDark)) 
            }
            var showSettings by remember { mutableStateOf(false) }

            DisposableEffect(isDarkMode) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { isDarkMode },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.argb(0xe6, 0xFF, 0xFF, 0xFF),
                        android.graphics.Color.argb(0x80, 0x1b, 0x1b, 0x1b)
                    ) { isDarkMode }
                )
                onDispose {}
            }

            val updateDarkMode: (Boolean) -> Unit = { newValue ->
                isDarkMode = newValue
                prefs.edit().putBoolean("dark_mode", newValue).apply()
            }

            MuhportalTheme(darkTheme = isDarkMode) {
                val snackbarHostState = remember { SnackbarHostState() }
                
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    AnimatedContent(
                        targetState = showSettings,
                        transitionSpec = {
                            if (targetState) {
                                slideInHorizontally { it } + fadeIn() togetherWith
                                        slideOutHorizontally { -it } + fadeOut()
                            } else {
                                slideInHorizontally { -it } + fadeIn() togetherWith
                                        slideOutHorizontally { it } + fadeOut()
                            }
                        },
                        label = "navigation"
                    ) { settingsVisible ->
                        if (settingsVisible) {
                            BackHandler { showSettings = false }
                            SettingsScreen(
                                isDarkMode = isDarkMode,
                                onDarkModeChange = updateDarkMode,
                                onBack = { showSettings = false }
                            )
                        } else {
                            MainContent(
                                isDarkMode = isDarkMode,
                                onDarkModeChange = updateDarkMode,
                                onOpenSettings = { showSettings = true },
                                snackbarHostState = snackbarHostState
                            )
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

@Composable
fun MainContent(
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 3 })

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                    icon = { Icon(Icons.Default.Lock, contentDescription = "Portal") },
                    label = { 
                        Text(
                            text = "Portal",
                            fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Normal
                        ) 
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    icon = { Icon(Icons.Default.Lan, contentDescription = "WOL") },
                    label = { 
                        Text(
                            text = "WOL",
                            fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Normal
                        ) 
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 2,
                    onClick = { scope.launch { pagerState.animateScrollToPage(2) } },
                    icon = { Icon(Icons.Default.Lightbulb, contentDescription = "HA") },
                    label = { 
                        Text(
                            text = "HA",
                            fontWeight = if (pagerState.currentPage == 2) FontWeight.Bold else FontWeight.Normal
                        ) 
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                    snackbarHostState = snackbarHostState,
                    isDarkMode = isDarkMode,
                    onDarkModeChange = onDarkModeChange,
                    onOpenSettings = onOpenSettings
                )
                1 -> WolScreen(
                    connState = connState,
                    wolStates = wolStates,
                    onRefresh = { mqtt.reconnect() },
                    onWolAction = { mac, action -> mqtt.wolAction(mac, action) },
                    snackbarHostState = snackbarHostState,
                    isDarkMode = isDarkMode,
                    onDarkModeChange = onDarkModeChange,
                    onOpenSettings = onOpenSettings
                )
                2 -> HAScreen(
                    connState = connState,
                    onRefresh = { mqtt.reconnect() },
                    isDarkMode = isDarkMode,
                    onDarkModeChange = onDarkModeChange,
                    onOpenSettings = onOpenSettings
                )
            }
        }
    }
}
