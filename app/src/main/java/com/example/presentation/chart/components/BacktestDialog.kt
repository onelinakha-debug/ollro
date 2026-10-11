package com.example.presentation.chart.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.domain.engine.BacktestEngine
import com.example.domain.engine.BinanceHistoricalReplayHarness
import com.example.domain.model.Candle
import com.example.ui.theme.TerminalBearishRed
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
fun BacktestDialog(
    symbol: String,
    candles: List<Candle>,
    currentTimeframe: String = "1m",
    onDismiss: () -> Unit
) {
    var strategyMode by remember { mutableIntStateOf(0) } // 0: Spot ATR TP/SL, 1: 5-Bar Prediction Harness, 2: Binary 1M
    var selectedTf by remember { mutableStateOf(currentTimeframe) }

    val report by remember(strategyMode, candles.size, selectedTf) {
        mutableStateOf(
            when (strategyMode) {
                0 -> BacktestEngine.runSpotStrategy(
                    symbol = symbol,
                    candles = candles,
                    timeframe = selectedTf,
                    initialCapital = 10_000.0,
                    tradeSizeUsd = 1_000.0,
                    takeProfitPercent = 2.0,
                    stopLossPercent = 1.0,
                    minSignalScore = 25
                )
                1 -> {
                    val summary = BinanceHistoricalReplayHarness.replayHistory(
                        symbol = symbol,
                        timeframe = selectedTf,
                        candles = candles,
                        initialCapital = 10_000.0,
                        tradeSizeUsd = 1_000.0,
                        useAtrStops = true,
                        takeProfitAtrMultiplier = 1.5,
                        stopLossAtrMultiplier = 1.0,
                        horizonBars = 5
                    )
                    // Adapt to BacktestReport
                    BacktestEngine.runSpotStrategy(
                        symbol = symbol,
                        candles = candles,
                        timeframe = selectedTf
                    ).copy(
                        expectancyUsd = summary.expectancyUsd,
                        expectancyPercent = summary.expectancyPercent,
                        winRatePercent = summary.winRatePercent,
                        calibratedAccuracyPercent = summary.calibratedAccuracyPercent,
                        maxDrawdownPercent = summary.maxDrawdownPercent
                    )
                }
                else -> BacktestEngine.runBinaryOptionsStrategy(symbol, candles)
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TerminalSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "BINANCE REPLAY HARNESS",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = TerminalCyan
                    )
                    Text(
                        text = "$symbol • $selectedTf • ${candles.size} Closed Bars",
                        fontSize = 11.sp,
                        color = TerminalTextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Strategy Mode Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(TerminalSurfaceElevated)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("SPOT ATR", "PREDICTION HARNESS", "BINARY 1M").forEachIndexed { index, title ->
                        val isSelected = strategyMode == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) TerminalCyan else Color.Transparent)
                                .clickable { strategyMode = index }
                                .padding(vertical = 6.dp)
                                .testTag("mode_$index"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else TerminalTextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Metric Cards Grid
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TerminalSurfaceElevated)
                        .border(1.dp, TerminalCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isProfit = report.netProfitUsd >= 0
                    val profitColor = if (isProfit) TerminalBullishGreen else TerminalBearishRed

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "NET RETURN",
                                fontSize = 9.sp,
                                color = TerminalTextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = String.format(Locale.US, "%s$%,.2f (%+.2f%%)", if (isProfit) "+" else "", report.netProfitUsd, report.netProfitPercent),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = profitColor,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Win Rate Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(profitColor.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = String.format(Locale.US, "WIN: %.1f%%", report.winRatePercent),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = profitColor,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Mathematical Expectancy & Max Drawdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "EXPECTANCY", fontSize = 9.sp, color = TerminalTextMuted)
                            Text(
                                text = String.format(Locale.US, "%+$,.2f (%+.2f%%)", report.expectancyUsd, report.expectancyPercent),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (report.expectancyUsd >= 0) TerminalBullishGreen else TerminalBearishRed,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "PROFIT FACTOR", fontSize = 9.sp, color = TerminalTextMuted)
                            Text(
                                text = String.format(Locale.US, "%.2f", report.profitFactor),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TerminalCyan,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "MAX DRAWDOWN", fontSize = 9.sp, color = TerminalTextMuted)
                            Text(
                                text = String.format(Locale.US, "%.2f%%", report.maxDrawdownPercent),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TerminalBearishRed,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Trades Count & Prediction Calibration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Trades: ${report.totalTrades} (${report.winningTrades}W / ${report.losingTrades}L)",
                            fontSize = 9.5.sp,
                            color = TerminalTextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Prediction Accuracy: ${String.format(Locale.US, "%.1f%%", report.calibratedAccuracyPercent)}",
                            fontSize = 9.5.sp,
                            color = TerminalCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Executed Trades Log Header
                Text(
                    text = "REPLAY EXECUTIONS (${report.trades.size})",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TerminalCyan,
                    fontFamily = FontFamily.Monospace
                )

                if (report.trades.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No trades triggered for current parameters.",
                            color = TerminalTextMuted,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        items(report.trades) { trade ->
                            val tradeColor = if (trade.isWin) TerminalBullishGreen else TerminalBearishRed
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(TerminalSurfaceElevated)
                                    .border(0.5.dp, TerminalSurfaceBorder, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (trade.isWin) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                        contentDescription = null,
                                        tint = tradeColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Column {
                                        Text(
                                            text = "#${trade.tradeId} ${trade.direction.name} (${trade.exitReason})",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TerminalTextPrimary,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = String.format(Locale.US, "In: $%,.1f -> Out: $%,.1f", trade.entryPrice, trade.exitPrice),
                                            fontSize = 8.sp,
                                            color = TerminalTextMuted,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                Text(
                                    text = String.format(Locale.US, "%s$%,.2f (%+.1f%%)", if (trade.isWin) "+" else "", trade.pnlUsd, trade.pnlPercent),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = tradeColor,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_backtest_button")
            ) {
                Text("CLOSE", color = TerminalCyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    )
}
