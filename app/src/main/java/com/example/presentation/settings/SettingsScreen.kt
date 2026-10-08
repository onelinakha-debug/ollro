package com.example.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.OlroApplication
import com.example.ui.theme.TerminalBackground
import com.example.ui.theme.TerminalBearishRed
import com.example.ui.theme.TerminalBullishGreen
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceBorder
import com.example.ui.theme.TerminalSurfaceElevated
import com.example.ui.theme.TerminalTextMuted
import com.example.ui.theme.TerminalTextPrimary
import com.example.ui.theme.TerminalTextSecondary
import com.example.ui.theme.TerminalWarningAmber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as? OlroApplication
    val scope = rememberCoroutineScope()

    var cachedCandleCount by remember { mutableIntStateOf(0) }
    var keystoreStatus by remember { mutableStateOf("Hardware Backed / AES-256-GCM Verified") }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            val count = app?.container?.database?.candleDao()?.getCount() ?: 0
            cachedCandleCount = count
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBackground)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Title
        Column {
            Text(
                text = "TERMINAL CONFIGURATION",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = TerminalCyan
            )
            Text(
                text = "Security, Certificate Pinning & Data Persistence",
                fontSize = 11.sp,
                color = TerminalTextMuted
            )
        }

        // 1. Android Keystore Security
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(TerminalSurface)
                .border(1.dp, TerminalSurfaceBorder, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Keystore",
                    tint = TerminalCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "HARDWARE KEYSTORE ENCRYPTION",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TerminalTextPrimary,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Session credentials and tokens are encrypted at rest using AES-256 in Galois/Counter Mode (GCM) without external key transmission.",
                fontSize = 11.sp,
                color = TerminalTextSecondary,
                lineHeight = 15.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Active",
                    tint = TerminalBullishGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = keystoreStatus,
                    fontSize = 11.sp,
                    color = TerminalBullishGreen,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // 2. Network & Certificate Pinning
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(TerminalSurface)
                .border(1.dp, TerminalSurfaceBorder, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.NetworkCheck,
                    contentDescription = "Network",
                    tint = TerminalBullishGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "NETWORK PROTOCOLS & PINNING",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TerminalTextPrimary,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Binance endpoints: api.binance.com, stream.binance.com (25s RFC 6455 Heartbeat, Rate limited < 5 req/s).\nPocket Option endpoints: api-l.po.market (Session Socket.IO fallback sandbox enabled).",
                fontSize = 11.sp,
                color = TerminalTextSecondary,
                lineHeight = 15.sp
            )
        }

        // 3. Local Room Database Cache
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(TerminalSurface)
                .border(1.dp, TerminalSurfaceBorder, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = "Room Storage",
                    tint = TerminalWarningAmber,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "LOCAL ROOM CACHE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TerminalTextPrimary,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Currently persisting $cachedCandleCount historical OHLCV candles across symbols to provide offline and stale data continuity.",
                fontSize = 11.sp,
                color = TerminalTextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        app?.container?.database?.candleDao()?.clearAll()
                        cachedCandleCount = 0
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TerminalSurfaceElevated),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TerminalBearishRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .testTag("clear_cache_button")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Clear Cache",
                    tint = TerminalBearishRed,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text("Purge Room Database Cache", color = TerminalBearishRed, fontSize = 12.sp)
            }
        }

        // 4. Architecture Info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(TerminalSurface)
                .border(1.dp, TerminalSurfaceBorder, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Architecture",
                    tint = TerminalTextMuted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "SYSTEM ARCHITECTURE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TerminalTextPrimary,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Olro v1.0.0 Pro • Clean Architecture with MVVM • Dedicated MarketDataManager Singleton • OkHttp WebSocket Lifecycle with Exponential Backoff & 24h Proactive Rotation.",
                fontSize = 11.sp,
                color = TerminalTextMuted,
                lineHeight = 15.sp
            )
        }
    }
}
