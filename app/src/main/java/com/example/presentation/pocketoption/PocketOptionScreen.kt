package com.example.presentation.pocketoption

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.data.remote.pocketoption.PocketOptionSession
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.BinaryOptionAsset
import com.example.domain.model.SignalDirection
import com.example.presentation.common.ConnectionStatusBadge
import com.example.ui.theme.TerminalBackground
import com.example.ui.theme.TerminalBearishRed
import com.example.ui.theme.TerminalBearishRedBg
import com.example.ui.theme.TerminalBullishGreen
import com.example.ui.theme.TerminalBullishGreenBg
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.TerminalGridLine
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceBorder
import com.example.ui.theme.TerminalSurfaceElevated
import com.example.ui.theme.TerminalTextMuted
import com.example.ui.theme.TerminalTextPrimary
import com.example.ui.theme.TerminalTextSecondary
import com.example.ui.theme.TerminalWarningAmber
import com.example.ui.theme.TerminalWarningAmberBg
import java.util.Locale

@Composable
fun PocketOptionScreen(
    viewModel: PocketOptionViewModel,
    modifier: Modifier = Modifier
) {
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val sessionState by viewModel.sessionState.collectAsState()
    val assets by viewModel.assets.collectAsState()
    val selectedAsset by viewModel.selectedAsset.collectAsState()
    val selectedExpirySeconds by viewModel.selectedExpirySeconds.collectAsState()
    val countdownRemaining by viewModel.countdownRemaining.collectAsState()
    val recentTicks by viewModel.recentTicks.collectAsState()
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()
    val isSimulationActive by viewModel.isSimulationActive.collectAsState()

    var showSessionDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBackground)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Header with Mode Badge & Security Disclaimer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "POCKET OPTION",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = TerminalWarningAmber
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(TerminalWarningAmberBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (sessionState.isDemo) "DEMO" else "REAL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TerminalWarningAmber
                        )
                    }
                }
                Text(
                    text = "Binary Options Surveillance Engine",
                    fontSize = 11.sp,
                    color = TerminalTextMuted
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                ConnectionStatusBadge(status = connectionStatus)
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = { showSessionDialog = true },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(TerminalSurfaceElevated)
                        .testTag("po_session_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "Session",
                        tint = TerminalCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (isSimulationActive) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(TerminalWarningAmber.copy(alpha = 0.15f))
                    .border(1.dp, TerminalWarningAmber, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Simulated data",
                    tint = TerminalWarningAmber,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SIMULATED DATA (OTC SANDBOX) — Live binary feed restricted or offline.",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TerminalWarningAmber,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // 2. Data Isolation & Security Warning Notice
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(TerminalSurfaceElevated)
                .border(1.dp, TerminalWarningAmber.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Security Note",
                tint = TerminalWarningAmber,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Isolated stream: Binary ticks are segregated from Binance crypto order books. Session SSID is encrypted with Android Keystore (AES-256-GCM).",
                fontSize = 11.sp,
                color = TerminalTextSecondary,
                lineHeight = 15.sp
            )
        }

        // 3. Asset Selector Chips (Payout Rate shown prominently)
        Text(
            text = "AVAILABLE ASSETS & PAYOUT RATES",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = TerminalTextMuted,
            fontFamily = FontFamily.Monospace
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(assets) { asset ->
                val isSel = asset.id == selectedAsset?.id
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSel) TerminalSurfaceElevated else TerminalSurface)
                        .border(1.dp, if (isSel) TerminalWarningAmber else TerminalSurfaceBorder, RoundedCornerShape(10.dp))
                        .clickable { viewModel.selectAsset(asset) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("po_asset_${asset.id.lowercase()}")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = asset.symbol,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TerminalTextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${asset.payoutPercent}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = TerminalBullishGreen
                        )
                    }
                    Text(
                        text = String.format(Locale.US, "%.5f", asset.currentPrice),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TerminalTextSecondary
                    )
                }
            }
        }

        // 4. Selected Asset Card with Live Tick Graph
        selectedAsset?.let { asset ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(TerminalSurface)
                    .border(1.dp, TerminalSurfaceBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = asset.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TerminalTextPrimary
                        )
                        Text(
                            text = String.format(Locale.US, "Payout: %d%% profit on win", asset.payoutPercent),
                            fontSize = 11.sp,
                            color = TerminalBullishGreen
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = String.format(Locale.US, "%.5f", asset.currentPrice),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = TerminalCyan
                        )
                        Text(
                            text = if (asset.priceChange24h >= 0) "+${String.format(Locale.US, "%.2f", asset.priceChange24h)}%" else "${String.format(Locale.US, "%.2f", asset.priceChange24h)}%",
                            fontSize = 11.sp,
                            color = if (asset.priceChange24h >= 0) TerminalBullishGreen else TerminalBearishRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Live Tick Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(TerminalSurfaceElevated)
                        .padding(8.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        drawLine(
                            color = TerminalGridLine,
                            start = Offset(0f, h / 2f),
                            end = Offset(w, h / 2f),
                            strokeWidth = 1f
                        )

                        if (recentTicks.size >= 2) {
                            val minP = recentTicks.minOrNull() ?: 1.0
                            val maxP = recentTicks.maxOrNull() ?: 1.0
                            val span = (maxP - minP).coerceAtLeast(0.00005)
                            val step = w / (recentTicks.size - 1)
                            val path = Path()

                            for (i in recentTicks.indices) {
                                val p = recentTicks[i]
                                val x = i * step
                                val y = (h * (1.0 - (p - minP) / span)).toFloat()
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }

                            drawPath(
                                path = path,
                                color = TerminalCyan,
                                style = Stroke(width = 2.dp.toPx())
                            )

                            // Pulse on latest tick
                            val lastP = recentTicks.last()
                            val lastY = (h * (1.0 - (lastP - minP) / span)).toFloat()
                            drawCircle(
                                color = TerminalBullishGreen,
                                radius = 4.dp.toPx(),
                                center = Offset(w, lastY)
                            )
                        }
                    }
                }
            }
        }

        // 5. Expiry Selection & Countdown Engine
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(TerminalSurface)
                .border(1.dp, TerminalSurfaceBorder, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Text(
                text = "EXPIRY DURATION & TIMING",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TerminalTextMuted,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))

            val expiryOptions = listOf(30 to "30s", 60 to "1m", 120 to "2m", 300 to "5m")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                expiryOptions.forEach { (sec, label) ->
                    val isSel = sec == selectedExpirySeconds
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) TerminalCyan else TerminalSurfaceElevated)
                            .border(1.dp, if (isSel) TerminalCyan else TerminalSurfaceBorder, RoundedCornerShape(8.dp))
                            .clickable { viewModel.setExpiry(sec) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSel) Color.Black else TerminalTextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Countdown Timer Display & Play Trigger
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(TerminalSurfaceElevated)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "EXPIRY CLOCK",
                        fontSize = 10.sp,
                        color = TerminalTextMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${countdownRemaining / 60}:${String.format(Locale.US, "%02d", countdownRemaining % 60)}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = if (countdownRemaining < 10 && isTimerRunning) TerminalBearishRed else TerminalTextPrimary
                    )
                }

                Button(
                    onClick = { viewModel.startExpiryCountdown() },
                    colors = ButtonDefaults.buttonColors(containerColor = TerminalCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("start_countdown_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start Expiry",
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isTimerRunning) "RESTART" else "TEST EXPIRY",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // 6. Binary Call / Put Sentiment & Recommendation
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(TerminalSurface)
                .border(1.dp, TerminalSurfaceBorder, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Text(
                text = "BINARY OPTIONS CONSENSUS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TerminalTextMuted,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Call Card
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(TerminalBullishGreenBg)
                        .border(1.dp, TerminalBullishGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "Call",
                        tint = TerminalBullishGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("CALL (HIGHER)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TerminalBullishGreen)
                        Text("68% Consensus", fontSize = 11.sp, color = TerminalTextSecondary)
                    }
                }

                // Put Card
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(TerminalBearishRedBg)
                        .border(1.dp, TerminalBearishRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingDown,
                        contentDescription = "Put",
                        tint = TerminalBearishRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("PUT (LOWER)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TerminalBearishRed)
                        Text("32% Consensus", fontSize = 11.sp, color = TerminalTextSecondary)
                    }
                }
            }
        }
    }

    // Session Credentials Dialog
    if (showSessionDialog) {
        SessionCredentialsDialog(
            session = sessionState,
            onSave = { ssid, uid, isDemo ->
                viewModel.saveSessionCredentials(ssid, uid, isDemo)
                showSessionDialog = false
            },
            onClear = {
                viewModel.clearSession()
                showSessionDialog = false
            },
            onDismiss = { showSessionDialog = false }
        )
    }
}

@Composable
private fun SessionCredentialsDialog(
    session: PocketOptionSession,
    onSave: (String, String, Boolean) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var ssid: String by remember { mutableStateOf(session.ssid) }
    var uid: String by remember { mutableStateOf(session.uid) }
    var isDemo: Boolean by remember { mutableStateOf(session.isDemo) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TerminalSurface,
        title = {
            Text(
                text = "Pocket Option Session Security",
                color = TerminalTextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Security Warning: Pocket Option uses an unofficial WebSocket protocol. Credentials are encrypted at rest using Android Keystore (AES-256-GCM).",
                    fontSize = 11.sp,
                    color = TerminalWarningAmber
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Demo Mode (isDemo: 1)", color = TerminalTextPrimary, fontSize = 13.sp)
                    Switch(
                        checked = isDemo,
                        onCheckedChange = { isDemo = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = TerminalCyan)
                    )
                }

                OutlinedTextField(
                    value = ssid,
                    onValueChange = { ssid = it },
                    label = { Text("SSID Token") },
                    placeholder = { Text("Enter extracted browser SSID") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TerminalCyan,
                        unfocusedBorderColor = TerminalSurfaceBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uid,
                    onValueChange = { uid = it },
                    label = { Text("User ID (UID)") },
                    placeholder = { Text("Optional UID") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TerminalCyan,
                        unfocusedBorderColor = TerminalSurfaceBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(ssid, uid, isDemo) },
                colors = ButtonDefaults.buttonColors(containerColor = TerminalCyan)
            ) {
                Text("Save & Encrypt", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onClear) {
                Text("Clear Credentials", color = TerminalBearishRed)
            }
        }
    )
}
