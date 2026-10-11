package com.example.presentation.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
    unavailableReason: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TerminalSurface)
            .border(1.dp, TerminalSurfaceBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
            .testTag("signal_summary_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SURVEILLANCE SIGNAL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TerminalCyan,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Aggregated Multi-Indicator Consensus",
                    fontSize = 11.sp,
                    color = TerminalTextMuted
                )
            }
            if (signal != null) {
                SignalBadge(direction = signal.direction, compositeScore = signal.compositeScore)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (signal == null) {
            Text(
                text = unavailableReason ?: "Calculating real-time telemetry...",
                fontSize = 12.sp,
                color = if (unavailableReason != null) TerminalWarningAmber else TerminalTextMuted
            )
            return
        }

        // Composite Score Meter
        val normalizedScore = ((signal.compositeScore + 100) / 200f).coerceIn(0f, 1f)
        val meterColor = when (signal.direction) {
            SignalDirection.STRONG_BUY, SignalDirection.BUY -> TerminalBullishGreen
            SignalDirection.NEUTRAL -> TerminalWarningAmber
            SignalDirection.SELL, SignalDirection.STRONG_SELL -> TerminalBearishRed
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(TerminalSurfaceElevated)
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Conviction Score: ${signal.compositeScore}/100",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = meterColor
                )
                Text(
                    text = "Confidence: ${signal.confidencePercent}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = TerminalTextSecondary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { normalizedScore },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = meterColor,
                trackColor = TerminalSurfaceBorder
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = signal.summary,
                fontSize = 12.sp,
                color = TerminalTextSecondary,
                lineHeight = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "INDICATOR CONTRIBUTIONS & WEIGHTS",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = TerminalTextMuted,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Breakdown table
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = contribution.indicatorName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TerminalTextPrimary,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "(${String.format(Locale.US, "%.0f%%", contribution.weight * 100)})",
                    fontSize = 10.sp,
                    color = TerminalTextMuted
                )
            }
            Text(
                text = contribution.state,
                fontSize = 11.sp,
                color = indicatorColor
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = contribution.valueFormatted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = TerminalTextSecondary
            )
            val scorePrefix = if (contribution.signalScore > 0) "+" else ""
            Text(
                text = "$scorePrefix${contribution.signalScore}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = indicatorColor
            )
        }
    }
}
