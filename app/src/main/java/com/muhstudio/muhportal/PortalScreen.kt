package com.muhstudio.muhportal

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class PortalGroup { HAUSTUER, GARAGENTUER, GARAGE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortalScreen(
    connState: ConnState,
    portalStates: Map<String, PortalUpdate>,
    onRefresh: () -> Unit,
    onToggle: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    isColorblind: Boolean,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedGroup by remember { mutableStateOf<PortalGroup?>(null) }
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
            title = stringResource(R.string.portal_title),
            icon = Icons.Default.Lock,
            isColorblind = isColorblind,
            onOpenSettings = onOpenSettings
        )
        HorizontalDivider(
            color = getConnColor(connState, isColorblind),
            thickness = 4.dp
        )
        
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    onRefresh()
                    delay(1000) // Visual feedback
                    isRefreshing = false
                }
            },
            modifier = Modifier.weight(1f)
        ) {
            PortalContent(portalStates, isColorblind, onGroupClick = { selectedGroup = it })
        }
    }

    selectedGroup?.let { group ->
        PortalActionDialog(
            group = group,
            portalStates = portalStates,
            isColorblind = isColorblind,
            onDismiss = { selectedGroup = null },
            onToggle = onToggle,
            snackbarHostState = snackbarHostState
        )
    }
}

@Composable
private fun PortalContent(
    portalStates: Map<String, PortalUpdate>,
    isColorblind: Boolean,
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
                        color = getAppColor(if (hd?.state == DoorState.OPEN) AppColor.GREEN else AppColor.RED, isColorblind)
                    ),
                    StatusRowData(
                        text = if (hdl?.state == DoorState.OPEN) stringResource(R.string.entriegelt) else stringResource(R.string.verriegelt),
                        color = getAppColor(if (hdl?.state == DoorState.OPEN) AppColor.GREEN else AppColor.RED, isColorblind)
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
                        color = getAppColor(if (gd?.state == DoorState.OPEN) AppColor.GREEN else AppColor.RED, isColorblind)
                    ),
                    StatusRowData(
                        text = if (gdl?.state == DoorState.OPEN) stringResource(R.string.entriegelt) else stringResource(R.string.verriegelt),
                        color = getAppColor(if (gdl?.state == DoorState.OPEN) AppColor.GREEN else AppColor.RED, isColorblind)
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
                        color = getAppColor(if (g?.state == DoorState.OPEN) AppColor.GREEN else AppColor.RED, isColorblind)
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
                Text(text = row.text, fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}

@Composable
private fun TimeBadge(time: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(4.dp)) {
        Text(text = time, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PortalActionDialog(
    group: PortalGroup,
    portalStates: Map<String, PortalUpdate>,
    isColorblind: Boolean,
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
                    Text(text = groupName, fontSize = 24.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                        color = getAppColor(if (portalStates[prefix]?.state == DoorState.OPEN) AppColor.GREEN else AppColor.RED, isColorblind)
                                    )
                                    StatusItem(
                                        text = if (portalStates[prefix + "L"]?.state == DoorState.OPEN) stringResource(R.string.entriegelt) else stringResource(R.string.verriegelt),
                                        color = getAppColor(if (portalStates[prefix + "L"]?.state == DoorState.OPEN) AppColor.GREEN else AppColor.RED, isColorblind)
                                    )
                                }
                                PortalGroup.GARAGE -> {
                                    StatusItem(
                                        text = if (portalStates["G"]?.state == DoorState.OPEN) stringResource(R.string.offen) else stringResource(R.string.geschlossen),
                                        color = getAppColor(if (portalStates["G"]?.state == DoorState.OPEN) AppColor.GREEN else AppColor.RED, isColorblind)
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
                                    onDismiss()
                                })
                                ActionButton(text = labelEntriegeln, icon = Icons.Default.LockOpen, onClick = {
                                    onToggle("${prefix}_U"); scope.launch { snackbarHostState.showSnackbar("$groupName $labelEntriegeln") }
                                    onDismiss()
                                })
                                ActionButton(text = labelVerriegeln, icon = Icons.Default.Lock, onClick = {
                                    onToggle("${prefix}_L"); scope.launch { snackbarHostState.showSnackbar("$groupName $labelVerriegeln") }
                                    onDismiss()
                                })
                            }
                            PortalGroup.GARAGE -> {
                                ActionButton(text = labelBewegen, icon = Icons.Default.SwapVert, onClick = {
                                    onToggle("G_T"); scope.launch { snackbarHostState.showSnackbar("$groupName $labelBewegen") }
                                    onDismiss()
                                })
                            }
                        }
                    }

                    ActionButton(
                        text = labelAbbrechen,
                        containerColor = MaterialTheme.colorScheme.onSurface,
                        contentColor = MaterialTheme.colorScheme.surface,
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
        Text(text = text, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

private data class StatusRowData(val text: String, val color: Color)
