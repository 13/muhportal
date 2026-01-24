package com.muhstudio.muhportal

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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

val LocalOverlayHost = compositionLocalOf<MutableState<(@Composable () -> Unit)?>> {
    error("No OverlayHost provided")
}

@Composable
fun TitleBar(
    connState: ConnState,
    onRefresh: () -> Unit,
    title: String,
    icon: ImageVector,
    isBlackWhiteMode: Boolean,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (connState == ConnState.CONNECTED) getAppColor(AppColor.GREEN, isBlackWhiteMode) else Color.LightGray,
            modifier = Modifier.size(28.dp)
        )
        // Fixed hardcoded color with MaterialTheme
        Spacer(modifier = Modifier.width(32.dp))
        Text(text = title, fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))

        IconButton(onClick = onRefresh) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.onBackground)
        }

        IconButton(onClick = onOpenSettings) {
            Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onBackground)
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

@Composable
fun ModalOverlay(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.clickable(enabled = false) { }) {
            content()
        }
    }
}

enum class AppColor { GREEN, RED, YELLOW }

fun getAppColor(color: AppColor, isBlackWhiteMode: Boolean): Color = when (color) {
    AppColor.GREEN -> if (isBlackWhiteMode) Color(0xFF7F7F7F) else Color( color = 0xFF4CAF50)
    AppColor.RED -> if (isBlackWhiteMode) Color(0xFF111111) else Color( color = 0xFFF44336)
    AppColor.YELLOW -> if (isBlackWhiteMode) Color(0xFFBBBBBB) else Color( color = 0xFFFFEB3B)
}

fun getConnColor(connState: ConnState, isBlackWhiteMode: Boolean) = when (connState) {
    ConnState.CONNECTED -> getAppColor(AppColor.GREEN, isBlackWhiteMode)
    ConnState.CONNECTING -> getAppColor(AppColor.YELLOW, isBlackWhiteMode)
    ConnState.DISCONNECTED -> getAppColor(AppColor.RED, isBlackWhiteMode)
}

fun formatTime(timestamp: Long): String {
    val now = Calendar.getInstance()
    val time = Calendar.getInstance().apply { timeInMillis = timestamp }
    
    val isSameDay = now.get(Calendar.YEAR) == time.get(Calendar.YEAR) &&
                    now.get(Calendar.DAY_OF_YEAR) == time.get(Calendar.DAY_OF_YEAR)
    
    val pattern = if (isSameDay) "HH:mm" else "dd.MM. HH:mm"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestamp))
}
