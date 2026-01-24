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
import kotlinx.coroutines.delay
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
    // Easter egg state
    var tapCount by remember { mutableStateOf(0) }
    var showEasterEgg by remember { mutableStateOf(false) }
    
    // Rotation animation for easter egg
    val rotation by animateFloatAsState(
        targetValue = if (showEasterEgg) 360f else 0f,
        label = "rotation"
    )
    
    // Auto-reset tap counter if user doesn't complete 7 taps within 2 seconds
    LaunchedEffect(tapCount) {
        if (tapCount > 0 && tapCount < 7) {
            delay(2000)
            tapCount = 0
        }
    }
    
    // Auto-reset easter egg after 3 seconds
    LaunchedEffect(showEasterEgg) {
        if (showEasterEgg) {
            delay(3000)
            showEasterEgg = false
            tapCount = 0
        }
    }
    
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Clickable title area (icon + text) with rotation animation
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (!showEasterEgg) {
                        tapCount++
                        if (tapCount >= 7) {
                            showEasterEgg = true
                        }
                    }
                }
                .graphicsLayer {
                    rotationZ = rotation
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (showEasterEgg) Color(0xFFFF6B35) 
                      else if (connState == ConnState.CONNECTED) getAppColor(AppColor.GREEN, isBlackWhiteMode) 
                      else Color.LightGray,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(32.dp))
            Text(
                text = if (showEasterEgg) "🎉 MUH SECRET! 🎉" else title,
                fontSize = 24.sp,
                color = if (showEasterEgg) Color(0xFFFF6B35) else MaterialTheme.colorScheme.onBackground
            )
        }

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
