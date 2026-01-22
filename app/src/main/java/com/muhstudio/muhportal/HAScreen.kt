package com.muhstudio.muhportal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HAScreen(
    connState: ConnState,
    onRefresh: () -> Unit,
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TitleBar(
            connState = connState,
            onRefresh = onRefresh,
            title = "HA",
            icon = Icons.Default.Lightbulb,
            isDarkMode = isDarkMode,
            onDarkModeChange = onDarkModeChange,
            onOpenSettings = onOpenSettings
        )
        HorizontalDivider(
            color = when (connState) {
                ConnState.CONNECTED -> Color(0xFF4CAF50)
                ConnState.CONNECTING -> Color.Yellow
                ConnState.DISCONNECTED -> Color.Red
            },
            thickness = 4.dp
        )
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    onRefresh()
                    delay(1000)
                    isRefreshing = false
                }
            },
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "HA Screen", fontSize = 24.sp, color = Color.Gray)
            }
        }
    }
}
