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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.muhstudio.muhportal.ui.theme.MuhportalTheme
import kotlinx.coroutines.launch

enum class PortalGroup { HAUSTUER, GARAGENTUER, GARAGE }

@Composable
fun PortalScreen(
    connState: ConnState,
    portalStates: Map<String, PortalUpdate>,
    onRefresh: () -> Unit,
    onToggle: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    var selectedGroup by remember { mutableStateOf<PortalGroup?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        TitleBar(connState, onRefresh, stringResource(R.string.portal_title), Icons.Default.Lock)
        HorizontalDivider(
            color = getConnColor(connState),
            thickness = 4.dp
        )
        
        Box(
            modifier = Modifier.weight(1f)
        ) {
            PortalContent(portalStates, onGroupClick = { selectedGroup = it })
        }
    }

    selectedGroup?.let { group ->
        PortalActionDialog(
            group = group,
            portalStates = portalStates,
            onDismiss = { selectedGroup = null },
            onToggle = onToggle,
            snackbarHostState = snackbarHostState
        )
    }
}

@Composable
fun WolScreen(
    connState: ConnState,
    wolStates: Map<String, WolUpdate>,
    onRefresh: () -> Unit,
    onWolAction: (String, String) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    var selectedWol by remember { mutableStateOf<WolUpdate?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        TitleBar(connState, onRefresh, "WOL", Icons.Default.Lan)
        HorizontalDivider(
            color = getConnColor(connState),
            thickness = 4.dp
        )
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(top = 16.dp)
        ) {
            items(wolStates.values.toList().sortedBy { it.name }) { wol ->
                WolItem(wol, onClick = { selectedWol = wol })
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
            Text(text = displayName, fontSize = 24.sp, color = Color.Black)
            StatusBadge(if (wol.alive) "ON" else "OFF", if (wol.alive) Color(0xFF4CAF50) else Color.Red)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = wol.ip, fontSize = 14.sp, color = Color.LightGray)
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
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Box(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = wol.name.substringBefore('.'), fontSize = 24.sp, fontWeight = FontWeight.Medium, color = Color.Black)
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
                            //onDismiss()
                        }
                    )
                    ActionButton(
                        text = "Shutdown",
                        icon = Icons.Default.PowerSettingsNew,
                        onClick = {
                            onAction(wol.mac, "SHUTDOWN")
                            scope.launch { snackbarHostState.showSnackbar("Shutdown ${wol.name.substringBefore('.')}") }
                            //onDismiss()
                        }
                    )
                    ActionButton(
                        text = stringResource(R.string.abbrechen),
                        containerColor = Color.Black,
                        contentColor = Color.White,
                        //elevation = 0.dp,
                        onClick = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
fun GenericPlaceholderScreen(title: String, connState: ConnState, onRefresh: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        val icon = when (title) {
            "HA" -> Icons.Default.Lightbulb
            else -> Icons.Default.Help
        }
        TitleBar(connState, onRefresh, title, icon)
        HorizontalDivider(color = getConnColor(connState), thickness = 4.dp)
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "$title Screen", fontSize = 24.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun getConnColor(connState: ConnState) = when (connState) {
    ConnState.CONNECTED -> Color(0xFF4CAF50)
    ConnState.CONNECTING -> Color.Yellow
    ConnState.DISCONNECTED -> Color.Red
}

@Composable
private fun TitleBar(connState: ConnState, onRefresh: () -> Unit, title: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (connState == ConnState.CONNECTED) Color.Gray else Color.LightGray,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(32.dp))
        Text(text = title, fontSize = 24.sp, color = Color.Black, modifier = Modifier.weight(1f))
        IconButton(onClick = onRefresh) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.Black)
        }
    }
}

@Composable
private fun PortalContent(
    portalStates: Map<String, PortalUpdate>,
    onGroupClick: (PortalGroup) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp)
    ) {
        item {
            val hd = portalStates["HD"]
            val hdl = portalStates["HDL"]
            PortalSection(
                title = stringResource(R.string.haustuer),
                updates = listOfNotNull(hd, hdl),
                rows = listOf(
                    StatusRowData(
                        text = if (hd?.state == DoorState.OPEN) stringResource(R.string.offen) else stringResource(R.string.geschlossen),
                        color = if (hd?.state == DoorState.OPEN) Color(0xFF4CAF50) else Color.Red
                    ),
                    StatusRowData(
                        text = if (hdl?.state == DoorState.OPEN) stringResource(R.string.entriegelt) else stringResource(R.string.verriegelt),
                        color = if (hdl?.state == DoorState.OPEN) Color(0xFF4CAF50) else Color.Red
                    )
                ),
                onClick = { onGroupClick(PortalGroup.HAUSTUER) }
            )
        }
        item {
            val gd = portalStates["GD"]
            val gdl = portalStates["GDL"]
            PortalSection(
                title = stringResource(R.string.garagentuer),
                updates = listOfNotNull(gd, gdl),
                rows = listOf(
                    StatusRowData(
                        text = if (gd?.state == DoorState.OPEN) stringResource(R.string.offen) else stringResource(R.string.geschlossen),
                        color = if (gd?.state == DoorState.OPEN) Color(0xFF4CAF50) else Color.Red
                    ),
                    StatusRowData(
                        text = if (gdl?.state == DoorState.OPEN) stringResource(R.string.entriegelt) else stringResource(R.string.verriegelt),
                        color = if (gdl?.state == DoorState.OPEN) Color(0xFF4CAF50) else Color.Red
                    )
                ),
                onClick = { onGroupClick(PortalGroup.GARAGENTUER) }
            )
        }
        item {
            val g = portalStates["G"]
            PortalSection(
                title = stringResource(R.string.garage),
                updates = listOfNotNull(g),
                rows = listOf(
                    StatusRowData(
                        text = if (g?.state == DoorState.OPEN) stringResource(R.string.offen) else stringResource(R.string.geschlossen),
                        color = if (g?.state == DoorState.OPEN) Color(0xFF4CAF50) else Color.Red
                    )
                ),
                onClick = { onGroupClick(PortalGroup.GARAGE) }
            )
        }
    }
}

@Composable
private fun PortalSection(
    title: String,
    updates: List<PortalUpdate>,
    rows: List<StatusRowData>,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "scale")

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
            .padding(bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, fontSize = 28.sp, color = Color.LightGray)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                updates.forEach { update -> TimeBadge(formatTime(update.timestamp)) }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(18.dp).background(row.color, RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(32.dp))
                Text(text = row.text, fontSize = 24.sp, color = Color.Black)
            }
        }
    }
}

@Composable
private fun TimeBadge(time: String) {
    Surface(color = Color(0xFFE0E0E0), shape = RoundedCornerShape(4.dp)) {
        Text(text = time, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 14.sp, color = Color.Black)
    }
}

@Composable
private fun PortalActionDialog(
    group: PortalGroup,
    portalStates: Map<String, PortalUpdate>,
    onDismiss: () -> Unit,
    onToggle: (String) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    val groupName = when (group) {
        PortalGroup.HAUSTUER -> stringResource(R.string.haustuer)
        PortalGroup.GARAGENTUER -> stringResource(R.string.garagentuer)
        PortalGroup.GARAGE -> stringResource(R.string.garage)
    }

    val labelOeffnen = stringResource(R.string.oeffnen)
    val labelEntriegeln = stringResource(R.string.entriegeln)
    val labelVerriegeln = stringResource(R.string.verriegeln)
    val labelBewegen = stringResource(R.string.bewegen)
    val labelAbbrechen = stringResource(R.string.abbrechen)

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Box(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = groupName, fontSize = 24.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                }

                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Column(horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            when (group) {
                                PortalGroup.HAUSTUER, PortalGroup.GARAGENTUER -> {
                                    val prefix = if (group == PortalGroup.HAUSTUER) "HD" else "GD"
                                    StatusItem(
                                        text = if (portalStates[prefix]?.state == DoorState.OPEN) stringResource(R.string.offen) else stringResource(R.string.geschlossen),
                                        color = if (portalStates[prefix]?.state == DoorState.OPEN) Color(0xFF4CAF50) else Color.Red
                                    )
                                    StatusItem(
                                        text = if (portalStates[prefix + "L"]?.state == DoorState.OPEN) stringResource(R.string.entriegelt) else stringResource(R.string.verriegelt),
                                        color = if (portalStates[prefix + "L"]?.state == DoorState.OPEN) Color(0xFF4CAF50) else Color.Red
                                    )
                                }
                                PortalGroup.GARAGE -> {
                                    StatusItem(
                                        text = if (portalStates["G"]?.state == DoorState.OPEN) stringResource(R.string.offen) else stringResource(R.string.geschlossen),
                                        color = if (portalStates["G"]?.state == DoorState.OPEN) Color(0xFF4CAF50) else Color.Red
                                    )
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        when (group) {
                            PortalGroup.HAUSTUER, PortalGroup.GARAGENTUER -> {
                                val prefix = if (group == PortalGroup.HAUSTUER) "HDL" else "GDL"
                                ActionButton(text = labelOeffnen, icon = Icons.Default.MeetingRoom, onClick = {
                                    onToggle("${prefix}_O"); scope.launch { snackbarHostState.showSnackbar("$groupName $labelOeffnen") }
                                })
                                ActionButton(text = labelEntriegeln, icon = Icons.Default.LockOpen, onClick = {
                                    onToggle("${prefix}_U"); scope.launch { snackbarHostState.showSnackbar("$groupName $labelEntriegeln") }
                                })
                                ActionButton(text = labelVerriegeln, icon = Icons.Default.Lock, onClick = {
                                    onToggle("${prefix}_L"); scope.launch { snackbarHostState.showSnackbar("$groupName $labelVerriegeln") }
                                })
                            }
                            PortalGroup.GARAGE -> {
                                ActionButton(text = labelBewegen, icon = Icons.Default.SwapVert, onClick = {
                                    onToggle("G_T"); scope.launch { snackbarHostState.showSnackbar("$groupName $labelBewegen") }
                                })
                            }
                        }
                    }

                    ActionButton(
                        text = labelAbbrechen,
                        containerColor = Color.Black,
                        contentColor = Color.White,
                        //elevation = 0.dp,
                        onClick = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusItem(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(24.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
    }
}

@Composable
private fun ActionButton(
    text: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    containerColor: Color = Color(0xFFF5F5F5),
    contentColor: Color = Color.Black,
    elevation: androidx.compose.ui.unit.Dp = 2.dp,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "scale")

    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(56.dp).graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        interactionSource = interactionSource,
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = elevation)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = text, color = contentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private data class StatusRowData(val text: String, val color: Color)
