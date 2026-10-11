package com.example.presentation.dashboard.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.IndicatorContribution
import com.example.domain.model.MarketSignal
import com.example.domain.model.SignalDirection
import com.example.presentation.common.SignalBadge
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
import java.util.Locale

@Composable
fun SignalSummaryCard(
    signal: MarketSignal?,
    modifier: Modifier = Modifier,
    onLogToJournal: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TerminalSurface)
            .border(1.dp, TerminalSurfaceBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
            .testTag("signal_summary_card"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "QUANTITATIVE FORECAST",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TerminalCyan,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(TerminalBullishGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "CLOSED BARS ONLY",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = TerminalBullishGreen,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Text(
                    text = "Probabilistic Multi-Timeframe Forecast Engine",
                    fontSize = 11.sp,
                    color = TerminalTextMuted
                )
            }
            if (signal != null) {
                SignalBadge(direction = signal.direction, compositeScore = signal.compositeScore)
            }
        }

        if (signal == null) {
            Text(
                text = "Calculating real-time telemetry...",
                fontSize = 12.sp,
                color = TerminalTextMuted
            )
            return
        }

        val forecast = signal.forecast

        // 1. Explicit Probabilistic Forecast Banner
        forecast?.let { fc ->
            val isBullish = fc.primaryDirection.isBullish
            val forecastColor = if (isBullish) TerminalBullishGreen else if (fc.primaryDirection.isBearish) TerminalBearishRed else TerminalCyan
            val prob = if (isBullish) fc.probabilityUpPercent else if (fc.primaryDirection.isBearish) fc.probabilityDownPercent else 50.0

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(TerminalSurfaceElevated)
                    .border(1.dp, forecastColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EXPLICIT HORIZON PREDICTION",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TerminalTextMuted,
                        fontFamily = FontFamily.Monospace
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(forecastColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${fc.confidenceTier} CONVICTION",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = forecastColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Text(
                    text = fc.formatForecastStatement(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = forecastColor
                )

                // Probability Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "UP ${String.format(Locale.US, "%.0f%%", fc.probabilityUpPercent)}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TerminalBullishGreen,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    LinearProgressIndicator(
                        progress = { (fc.probabilityUp).toFloat() },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = TerminalBullishGreen,
                        trackColor = TerminalBearishRed
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DOWN ${String.format(Locale.US, "%.0f%%", fc.probabilityDownPercent)}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TerminalBearishRed,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // ATR Stops & Targets Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "ATR TARGET (1.5x)", fontSize = 8.5.sp, color = TerminalTextMuted)
                        Text(
                            text = String.format(Locale.US, "$%,.2f", fc.targetPrice),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TerminalBullishGreen,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "ATR STOP (1.0x)", fontSize = 8.5.sp, color = TerminalTextMuted)
                        Text(
                            text = String.format(Locale.US, "$%,.2f", fc.stopLossPrice),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TerminalBearishRed,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "RISK/REWARD", fontSize = 8.5.sp, color = TerminalTextMuted)
                        Text(
                            text = "${fc.riskRewardRatio}:1 R:R",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TerminalCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Sub-details: MTF & Order Book Imbalance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Book Imbalance: ${String.format(Locale.US, "%+.1f%%", fc.orderBookImbalance * 100.0)}",
                        fontSize = 9.5.sp,
                        color = if (fc.orderBookImbalance > 0) TerminalBullishGreen else TerminalBearishRed,
                        fontFamily = FontFamily.Monospace
                    )

                    Text(
                        text = fc.mtfConfirmation.summary,
                        fontSize = 9.5.sp,
                        color = if (fc.mtfConfirmation.isAligned) TerminalCyan else TerminalWarningAmber,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Action: Log Signal to Journal
        if (onLogToJournal != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(TerminalSurfaceElevated)
                    .border(0.5.dp, TerminalCyan, RoundedCornerShape(6.dp))
                    .clickable(onClick = onLogToJournal)
                    .padding(vertical = 8.dp)
                    .testTag("log_signal_to_journal_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BookmarkAdd,
                        contentDescription = "Log to Journal",
                        tint = TerminalCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RECORD SIGNAL TO TRADE JOURNAL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TerminalCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Indicator Breakdown Table
        Text(
            text = "INDICATOR WEIGHTS (FITTED VIA BACKTEST)",
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = TerminalTextMuted,
            fontFamily = FontFamily.Monospace
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            signal.contributions.forEach { item ->
                IndicatorRow(contribution = item)
            }
        }
    }
}

@Composable
private fun IndicatorRow(contribution: IndicatorContribution) {
    val indicatorColor = when {
        contribution.signalScore > 0 -> TerminalBullishGreen
        contribution.signalScore < 0 -> TerminalBearishRed
        else -> TerminalTextMuted
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(TerminalSurfaceElevated)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = contribution.indicatorName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TerminalTextPrimary,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "(${String.format(Locale.US, "%.0f%%", contribution.weight * 100)})",
                    fontSize = 9.sp,
                    color = TerminalTextMuted
                )
            }
            Text(
                text = contribution.state,
                fontSize = 10.sp,
                color = indicatorColor
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = contribution.valueFormatted,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = TerminalTextSecondary
            )
            val scorePrefix = if (contribution.signalScore > 0) "+" else ""
            Text(
                text = "$scorePrefix${contribution.signalScore}",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = indicatorColor
            )
        }
    }
}
