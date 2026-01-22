package com.muhstudio.muhportal

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TitleBar(
    connState: ConnState,
    onRefresh: () -> Unit,
    title: String,
    icon: ImageVector,
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onOpenSettings: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (connState == ConnState.CONNECTED) MaterialTheme.colorScheme.primary else Color.LightGray,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(32.dp))
        Text(text = title, fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))

        IconButton(onClick = onRefresh) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.onBackground)
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onBackground)
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Settings") },
                    onClick = {
                        showMenu = false
                        onOpenSettings()
                    },
                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Dark Mode")
                            Spacer(Modifier.weight(1f))
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = {
                                    onDarkModeChange(it)
                                }
                            )
                        }
                    },
                    onClick = { }
                )
            }
        }
    }
}

@Composable
fun ActionButton(
    text: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    contentColor: Color? = null,
    elevation: androidx.compose.ui.unit.Dp = 2.dp,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "scale")

    val finalContainerColor = containerColor ?: MaterialTheme.colorScheme.secondaryContainer
    val finalContentColor = contentColor ?: MaterialTheme.colorScheme.onSecondaryContainer

    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(56.dp).graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        interactionSource = interactionSource,
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = finalContainerColor),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = elevation)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, tint = finalContentColor, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = text, color = finalContentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

fun getConnColor(connState: ConnState) = when (connState) {
    ConnState.CONNECTED -> Color(0xFF4CAF50)
    ConnState.CONNECTING -> Color.Yellow
    ConnState.DISCONNECTED -> Color.Red
}

fun formatTime(timestamp: Long): String {
    val now = Calendar.getInstance()
    val time = Calendar.getInstance().apply { timeInMillis = timestamp }
    
    val isSameDay = now.get(Calendar.YEAR) == time.get(Calendar.YEAR) &&
                    now.get(Calendar.DAY_OF_YEAR) == time.get(Calendar.DAY_OF_YEAR)
    
    val pattern = if (isSameDay) "HH:mm" else "dd.MM. HH:mm"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestamp))
}
