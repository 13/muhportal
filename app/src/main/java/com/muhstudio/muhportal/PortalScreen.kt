package com.muhstudio.muhportal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muhstudio.muhportal.ui.theme.MuhportalTheme

@Composable
fun PortalScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        TopBar()
        TitleBar()
        HorizontalDivider(color = Color(0xFF4CAF50), thickness = 4.dp)
        PortalContent()
    }
}

@Composable
private fun TopBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color.Black)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Icon(
            painter = painterResource(id = android.R.drawable.ic_menu_sort_by_size),
            contentDescription = stringResource(R.string.menu_content_description),
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun TitleBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = android.R.drawable.ic_lock_idle_lock),
            contentDescription = stringResource(R.string.lock_content_description),
            tint = Color.Gray,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(32.dp))
        Text(
            text = stringResource(R.string.portal_title),
            fontSize = 24.sp,
            color = Color.Black
        )
    }
}

@Composable
private fun PortalContent() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp)
    ) {
        item {
            PortalSection(
                title = stringResource(R.string.haustuer),
                times = listOf("14:13", "09:54"),
                rows = listOf(
                    StatusRowData(stringResource(R.string.geschlossen), Color.Red),
                    StatusRowData(stringResource(R.string.entriegelt), Color(0xFF4CAF50))
                )
            )
        }
        item {
            PortalSection(
                title = stringResource(R.string.garagentuer),
                times = listOf("16.01. 07:50", "16.01. 08:00"),
                rows = listOf(
                    StatusRowData(stringResource(R.string.geschlossen), Color.Red),
                    StatusRowData(stringResource(R.string.verriegelt), Color.Red)
                )
            )
        }
        item {
            PortalSection(
                title = stringResource(R.string.garage),
                times = listOf("09:53"),
                rows = listOf(
                    StatusRowData(stringResource(R.string.geschlossen), Color.Red)
                )
            )
        }
    }
}

@Composable
private fun PortalSection(
    title: String,
    times: List<String>,
    rows: List<StatusRowData>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 28.sp,
                color = Color.LightGray
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                times.forEach { time ->
                    TimeBadge(time)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(row.color, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(32.dp))
                Text(
                    text = row.text,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
private fun TimeBadge(time: String) {
    Surface(
        color = Color(0xFFE0E0E0),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = time,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 14.sp,
            color = Color.Black
        )
    }
}

private data class StatusRowData(val text: String, val color: Color)

@Preview(showBackground = true)
@Composable
fun PortalScreenPreview() {
    MuhportalTheme {
        PortalScreen()
    }
}
