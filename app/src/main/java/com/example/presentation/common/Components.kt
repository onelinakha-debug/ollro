package com.example.presentation.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.domain.model.MarketConnectionStatus
import com.example.domain.model.SignalDirection
import com.example.ui.theme.TerminalBearishRed
import com.example.ui.theme.TerminalBearishRedBg
import com.example.ui.theme.TerminalBullishGreen
import com.example.ui.theme.TerminalBullishGreenBg
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.TerminalSurfaceBorder
import com.example.ui.theme.TerminalSurfaceElevated
import com.example.ui.theme.TerminalTextMuted
import com.example.ui.theme.TerminalTextPrimary
import com.example.ui.theme.TerminalTextSecondary
import com.example.ui.theme.TerminalWarningAmber
import com.example.ui.theme.TerminalWarningAmberBg
import java.util.Locale

private data class StatusConfig(
    val dotColor: Color,
    val label: String,
    val bg: Color
)

@Composable
fun ConnectionStatusBadge(
    status: MarketConnectionStatus,
    modifier: Modifier = Modifier
) {
    val config = when (status) {
        is MarketConnectionStatus.Connected -> {
            StatusConfig(TerminalBullishGreen, "${status.latencyMs}ms LIVE", TerminalBullishGreenBg)
        }
        is MarketConnectionStatus.FallbackLive -> {
            StatusConfig(TerminalWarningAmber, "SIMULATED (451 GEO)", TerminalWarningAmberBg)
        }
        is MarketConnectionStatus.Connecting -> {
            StatusConfig(TerminalCyan, "CONNECTING", Color(0x1F00E5FF))
        }
        is MarketConnectionStatus.Reconnecting -> {
            StatusConfig(TerminalWarningAmber, "RETRY ${status.delaySeconds}s", TerminalWarningAmberBg)
        }
        is MarketConnectionStatus.Stale -> {
            StatusConfig(TerminalBearishRed, "STALE (${status.elapsedSeconds}s)", TerminalBearishRedBg)
        }
        is MarketConnectionStatus.Disconnected -> {
            StatusConfig(TerminalTextMuted, "OFFLINE", Color(0x1F64748B))
        }
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(config.bg)
            .border(1.dp, config.dotColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(config.dotColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = config.label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = config.dotColor
        )
    }
}

@Composable
fun PriceChangeChip(
    changePercent: Double,
    modifier: Modifier = Modifier
) {
    val isPositive = changePercent >= 0.0
    val color = if (isPositive) TerminalBullishGreen else TerminalBearishRed
    val bg = if (isPositive) TerminalBullishGreenBg else TerminalBearishRedBg
    val prefix = if (isPositive) "+" else ""

    Text(
        text = String.format(Locale.US, "%s%.2f%%", prefix, changePercent),
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.Monospace,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
fun SignalBadge(
    direction: SignalDirection,
    compositeScore: Int,
    modifier: Modifier = Modifier
) {
    val (label, color, bg) = when (direction) {
        SignalDirection.STRONG_BUY -> Triple("STRONG BUY", TerminalBullishGreen, TerminalBullishGreenBg)
        SignalDirection.BUY -> Triple("BUY", TerminalBullishGreen, TerminalBullishGreenBg)
        SignalDirection.NEUTRAL -> Triple("NEUTRAL", TerminalWarningAmber, TerminalWarningAmberBg)
        SignalDirection.SELL -> Triple("SELL", TerminalBearishRed, TerminalBearishRedBg)
        SignalDirection.STRONG_SELL -> Triple("STRONG SELL", TerminalBearishRed, TerminalBearishRedBg)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "${if (compositeScore > 0) "+" else ""}$compositeScore",
            color = color.copy(alpha = 0.8f),
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun MetricPill(
    label: String,
    value: String,
    valueColor: Color = TerminalTextPrimary,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(TerminalSurfaceElevated)
            .border(1.dp, TerminalSurfaceBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = TerminalTextMuted,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            color = valueColor
        )
    }
}
