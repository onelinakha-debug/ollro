package com.example.presentation.chart

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Candle
import com.example.presentation.chart.components.BacktestDialog
import com.example.presentation.chart.components.CandleChart
import com.example.presentation.chart.components.MacdPane
import com.example.presentation.chart.components.RsiPane
import com.example.presentation.common.ConnectionStatusBadge
import com.example.presentation.common.PriceChangeChip
import com.example.presentation.common.SignalBadge
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChartScreen(
    viewModel: ChartViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedSymbol by viewModel.selectedSymbol.collectAsState()
    val selectedInterval by viewModel.selectedInterval.collectAsState()
    val candles by viewModel.candles.collectAsState()
    val currentTicker by viewModel.currentTicker.collectAsState()
    val indicators by viewModel.indicators.collectAsState()
    val signal by viewModel.signal.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val showRsi by viewModel.showRsi.collectAsState()
    val showMacd by viewModel.showMacd.collectAsState()
    val hoveredCandle by viewModel.hoveredCandle.collectAsState()
    val rsiHistory by viewModel.rsiHistory.collectAsState()
    val macdHistory by viewModel.macdHistory.collectAsState()

    val intervals = listOf("1m", "5m", "15m", "1h", "4h", "1D")
    val displayCandle = hoveredCandle ?: candles.lastOrNull()
    var showBacktestDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBackground)
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Chart Header with back button & live price
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(TerminalSurfaceElevated)
                        .testTag("chart_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TerminalCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = selectedSymbol,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TerminalTextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "($selectedInterval)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TerminalCyan
                        )
                    }
                    currentTicker?.let { t ->
                        Text(
                            text = String.format(Locale.US, "$%,.2f", t.lastPrice),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (t.isPositive) TerminalBullishGreen else TerminalBearishRed
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                currentTicker?.let { PriceChangeChip(changePercent = it.priceChangePercent) }
                Spacer(modifier = Modifier.width(6.dp))
                ConnectionStatusBadge(status = connectionStatus)
            }
        }

        // 2. Interval Selection & Indicator Toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Intervals
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                intervals.forEach { intv ->
                    val isSel = intv == selectedInterval
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) TerminalCyan else TerminalSurfaceElevated)
                            .clickable { viewModel.selectInterval(intv) }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                            .testTag("interval_$intv")
                    ) {
                        Text(
                            text = intv,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSel) Color.Black else TerminalTextSecondary
                        )
                    }
                }
            }

            // Indicator Toggles
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (showRsi) TerminalCyan.copy(alpha = 0.2f) else TerminalSurfaceElevated)
                        .border(1.dp, if (showRsi) TerminalCyan else TerminalSurfaceBorder, RoundedCornerShape(6.dp))
                        .clickable { viewModel.toggleRsi() }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("toggle_rsi_button")
                ) {
                    Text(
                        text = "RSI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (showRsi) TerminalCyan else TerminalTextMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (showMacd) TerminalCyan.copy(alpha = 0.2f) else TerminalSurfaceElevated)
                        .border(1.dp, if (showMacd) TerminalCyan else TerminalSurfaceBorder, RoundedCornerShape(6.dp))
                        .clickable { viewModel.toggleMacd() }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("toggle_macd_button")
                ) {
                    Text(
                        text = "MACD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (showMacd) TerminalCyan else TerminalTextMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(TerminalCyan.copy(alpha = 0.2f))
                        .border(1.dp, TerminalCyan, RoundedCornerShape(6.dp))
                        .clickable { showBacktestDialog = true }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("run_backtest_button")
                ) {
                    Text(
                        text = "BACKTEST",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TerminalCyan
                    )
                }
            }
        }

        // 3. OHLC Inspector Pill
        if (displayCandle != null) {
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
            val candleTime = timeFormat.format(Date(displayCandle.timestamp))
            val isBull = displayCandle.isBullish
            val candleColor = if (isBull) TerminalBullishGreen else TerminalBearishRed

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(TerminalSurfaceElevated)
                    .border(1.dp, TerminalSurfaceBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = candleTime,
                    fontSize = 10.sp,
                    color = TerminalTextMuted,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = String.format(Locale.US, "O: %.2f", displayCandle.open),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TerminalTextSecondary
                )
                Text(
                    text = String.format(Locale.US, "H: %.2f", displayCandle.high),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TerminalBullishGreen
                )
                Text(
                    text = String.format(Locale.US, "L: %.2f", displayCandle.low),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TerminalBearishRed
                )
                Text(
                    text = String.format(Locale.US, "C: %.2f", displayCandle.close),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = candleColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format(Locale.US, "Vol: %.1f", displayCandle.volume),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TerminalCyan
                )
            }
        }

        // 4. Primary Candlestick Chart Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(TerminalSurface)
                .border(1.dp, TerminalSurfaceBorder, RoundedCornerShape(10.dp))
                .padding(4.dp)
                .testTag("candlestick_chart_container")
        ) {
            CandleChart(
                candles = candles,
                indicators = indicators,
                onCrosshairSelected = { candle ->
                    viewModel.setHoveredCandle(candle)
                }
            )
        }

        // 5. RSI Sub-Pane
        if (showRsi) {
            RsiPane(
                rsiValues = rsiHistory,
                currentRsi = indicators.rsi14
            )
        }

        // 6. MACD Sub-Pane
        if (showMacd) {
            MacdPane(
                macdHistory = macdHistory,
                currentMacd = indicators.macd
            )
        }

        // 7. Active Signal Footer
        signal?.let { sig ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(TerminalSurface)
                    .border(1.dp, TerminalSurfaceBorder, RoundedCornerShape(8.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ANALYSIS RECOMMENDATION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TerminalTextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = sig.summary,
                        fontSize = 11.sp,
                        color = TerminalTextSecondary
                    )
                }
                SignalBadge(direction = sig.direction, compositeScore = sig.compositeScore)
            }
        }
    }

    if (showBacktestDialog) {
        BacktestDialog(
            symbol = selectedSymbol,
            candles = candles,
            currentTimeframe = selectedInterval,
            onDismiss = { showBacktestDialog = false }
        )
    }
}
