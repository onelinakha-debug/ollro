package com.example.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.common.ConnectionStatusBadge
import com.example.presentation.common.MetricPill
import com.example.presentation.dashboard.components.OrderBookPreview
import com.example.presentation.dashboard.components.SignalSummaryCard
import com.example.presentation.dashboard.components.TickerCard
import com.example.ui.theme.TerminalBackground
import com.example.ui.theme.TerminalBullishGreen
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceBorder
import com.example.ui.theme.TerminalSurfaceElevated
import com.example.ui.theme.TerminalTextMuted
import com.example.ui.theme.TerminalTextPrimary
import com.example.ui.theme.TerminalTextSecondary
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToChart: (String) -> Unit,
    onOpenAlerts: () -> Unit,
    onOpenHoldings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val selectedSymbol by viewModel.selectedSymbol.collectAsState()
    val currentTicker by viewModel.currentTicker.collectAsState()
    val topTickers by viewModel.topTickers.collectAsState()
    val orderBook by viewModel.orderBook.collectAsState()
    val indicators by viewModel.indicators.collectAsState()
    val signal by viewModel.signal.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()

    val availableSymbols = listOf("BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "DOGEUSDT")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBackground)
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
    ) {
        // 1. Terminal Top Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "OLRO",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = TerminalCyan
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TERMINAL",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TerminalTextMuted
                        )
                    }
                    Text(
                        text = "Real-Time Surveillance Engine",
                        fontSize = 11.sp,
                        color = TerminalTextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    ConnectionStatusBadge(status = connectionStatus)
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onOpenHoldings,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(TerminalSurfaceElevated)
                            .testTag("open_holdings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Holdings",
                            tint = TerminalBullishGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onOpenAlerts,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(TerminalSurfaceElevated)
                            .testTag("open_alerts_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Alerts",
                            tint = TerminalCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 2. Symbol Selector Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(availableSymbols) { sym ->
                    val isSelected = sym.equals(selectedSymbol, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) TerminalCyan else TerminalSurfaceElevated)
                            .border(1.dp, if (isSelected) TerminalCyan else TerminalSurfaceBorder, RoundedCornerShape(8.dp))
                            .clickable { viewModel.selectSymbol(sym) }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                            .testTag("symbol_chip_${sym.lowercase()}")
                    ) {
                        Text(
                            text = sym.replace("USDT", ""),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSelected) Color.Black else TerminalTextPrimary
                        )
                    }
                }
            }
        }

        // 3. Main Selected Ticker Card
        item {
            currentTicker?.let { ticker ->
                TickerCard(
                    ticker = ticker,
                    isSelected = true,
                    onClick = { onNavigateToChart(ticker.symbol) }
                )
            }
        }

        // 4. Quick Indicator Telemetry Metric Pills
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricPill(
                    label = "RSI (14)",
                    value = indicators.rsi14?.let { String.format(Locale.US, "%.1f", it) } ?: "--",
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = "ATR (14)",
                    value = indicators.atr14?.let { String.format(Locale.US, "%.2f", it) } ?: "--",
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = "RVOL 24H",
                    value = indicators.rvol?.let { String.format(Locale.US, "%.2fx", it) } ?: "--",
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = "VWAP",
                    value = indicators.vwap?.let { String.format(Locale.US, "$%,.0f", it) } ?: "--",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 5. Active Signal Summary Card
        item {
            SignalSummaryCard(signal = signal)
        }

        // 6. Order Book Depth Preview
        item {
            OrderBookPreview(orderBook = orderBook)
        }

        // 7. Market Overview Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WATCHLIST ASSETS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TerminalTextMuted,
                    fontFamily = FontFamily.Monospace
                )
                IconButton(
                    onClick = { viewModel.refreshTickers() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = TerminalTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        items(topTickers.filter { it.symbol != selectedSymbol }) { ticker ->
            TickerCard(
                ticker = ticker,
                isSelected = false,
                onClick = {
                    viewModel.selectSymbol(ticker.symbol)
                    onNavigateToChart(ticker.symbol)
                }
            )
        }
    }
}
