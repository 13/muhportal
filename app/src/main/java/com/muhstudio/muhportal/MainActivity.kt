package com.muhstudio.muhportal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muhstudio.muhportal.ui.theme.MuhportalTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MuhportalTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PortalListScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun PortalListScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var connState by remember { mutableStateOf(ConnState.DISCONNECTED) }
    val portalStates = remember { mutableStateMapOf<String, PortalUpdate>() }

    val mqtt = remember {
        GarageMqttClient(
            context = context,
            onConnState = { connState = it },
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

    Column(modifier = modifier.fillMaxSize()) {
        StatusHeader(connState)
        
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val keys = mqtt.getPortalKeys()
            items(keys) { key ->
                val update = portalStates[key]
                PortalItem(
                    name = mqtt.getPortalName(key),
                    state = update?.state ?: DoorState.UNKNOWN,
                    timestamp = update?.timestamp,
                    onToggle = { mqtt.toggle(key) },
                    enabled = connState == ConnState.CONNECTED
                )
            }
        }
    }
}

@Composable
fun StatusHeader(state: ConnState) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Status: $state",
            modifier = Modifier.padding(16.dp, 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = when(state) {
                ConnState.CONNECTED -> MaterialTheme.colorScheme.primary
                ConnState.CONNECTING -> MaterialTheme.colorScheme.secondary
                ConnState.DISCONNECTED -> MaterialTheme.colorScheme.error
            }
        )
    }
}

@Composable
fun PortalItem(
    name: String,
    state: DoorState,
    timestamp: Long?,
    onToggle: () -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    text = state.name,
                    color = when(state) {
                        DoorState.OPEN -> MaterialTheme.colorScheme.error
                        DoorState.CLOSED -> MaterialTheme.colorScheme.primary
                        DoorState.UNKNOWN -> MaterialTheme.colorScheme.outline
                    }
                )
                if (timestamp != null) {
                    Text(
                        text = formatTime(timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            Button(onClick = onToggle, enabled = enabled) {
                Text("Toggle")
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

@Preview(showBackground = true)
@Composable
fun PortalPreview() {
    MuhportalTheme {
        PortalListScreen()
    }
}