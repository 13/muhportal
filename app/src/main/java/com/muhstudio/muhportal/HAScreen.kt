package com.muhstudio.muhportal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HAScreen(
    connState: ConnState,
    onRefresh: () -> Unit,
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
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
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "HA Screen", fontSize = 24.sp, color = Color.Gray)
        }
    }
}
