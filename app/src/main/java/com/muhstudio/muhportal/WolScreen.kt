package com.muhstudio.muhportal

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WolScreen(
    connState: ConnState,
    wolStates: Map<String, WolUpdate>,
    onRefresh: () -> Unit,
    onWolAction: (String, String) -> Unit,
    snackbarHostState: SnackbarHostState,
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedWol by remember { mutableStateOf<WolUpdate?>(null) }
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
            title = "WOL",
            icon = Icons.Default.Lan,
            isDarkMode = isDarkMode,
            onDarkModeChange = onDarkModeChange,
            onOpenSettings = onOpenSettings
        )
        HorizontalDivider(
            color = getConnColor(connState),
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 16.dp)
            ) {
                items(
                    wolStates.values.toList()
                        .sortedWith(compareBy<WolUpdate> { it.priority }.thenBy { it.name })
                ) { wol ->
                    WolItem(wol, onClick = { if (wol.mac.isNotBlank()) selectedWol = wol })
                }
            }
        }
    }

    selectedWol?.let { wol ->
        WolActionDialog(
            wol = wol,
            onDismiss = { selectedWol = null },
            onAction = onWolAction,
            snackbarHostState = snackbarHostState
        )
    }
}

@Composable
private fun WolItem(wol: WolUpdate, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "scale")

    val displayName = wol.name.substringBefore('.')

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current
            ) { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = displayName, fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground)
            StatusBadge(if (wol.alive) "ON" else "OFF", if (wol.alive) Color(0xFF4CAF50) else Color.Red)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = wol.ip, fontSize = 14.sp, color = Color.Gray)
    }
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    Surface(color = color, shape = RoundedCornerShape(4.dp), modifier = Modifier.width(48.dp)) {
        Text(
            text = text,
            modifier = Modifier.padding(vertical = 4.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun WolActionDialog(
    wol: WolUpdate,
    onDismiss: () -> Unit,
    onAction: (String, String) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Card(
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Box(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = wol.name.substringBefore('.'), fontSize = 24.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ActionButton(
                        text = "Wake",
                        icon = Icons.Default.PowerSettingsNew,
                        onClick = {
                            onAction(wol.mac, "WAKE")
                            scope.launch { snackbarHostState.showSnackbar("Wake ${wol.name.substringBefore('.')}") }
                        }
                    )
                    ActionButton(
                        text = "Shutdown",
                        icon = Icons.Default.PowerSettingsNew,
                        onClick = {
                            onAction(wol.mac, "SHUTDOWN")
                            scope.launch { snackbarHostState.showSnackbar("Shutdown ${wol.name.substringBefore('.')}") }
                        }
                    )
                    ActionButton(
                        text = stringResource(R.string.abbrechen),
                        containerColor = MaterialTheme.colorScheme.onSurface,
                        contentColor = MaterialTheme.colorScheme.surface,
                        onClick = onDismiss
                    )
                }
            }
        }
    }
}
