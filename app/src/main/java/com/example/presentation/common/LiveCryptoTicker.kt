package com.example.presentation.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Ticker24h
import com.example.ui.theme.TerminalBearishRed
import com.example.ui.theme.TerminalBearishRedBg
import com.example.ui.theme.TerminalBullishGreen
import com.example.ui.theme.TerminalBullishGreenBg
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceBorder
import com.example.ui.theme.TerminalSurfaceElevated
import com.example.ui.theme.TerminalTextMuted
import com.example.ui.theme.TerminalTextPrimary
import com.example.ui.theme.TerminalTextSecondary
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Real-time cryptocurrency ticker component displaying live price updates,
 * animated tick flash highlights, green/red trend arrows, 24h change, and mini sparkline.
 */
@Composable
fun LiveCryptoTicker(
    ticker: Ticker24h,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    showSparkline: Boolean = true,
    showRangeBar: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    var previousPrice by remember { mutableDoubleStateOf(ticker.lastPrice) }
    var flashColor by remember { mutableStateOf<Color?>(null) }

    // Flash border/background color on real-time price tick updates
    LaunchedEffect(ticker.lastPrice) {
        if (ticker.lastPrice != previousPrice) {
            flashColor = if (ticker.lastPrice > previousPrice) {
                TerminalBullishGreen.copy(alpha = 0.35f)
            } else {
                TerminalBearishRed.copy(alpha = 0.35f)
            }
            previousPrice = ticker.lastPrice
            delay(400)
            flashColor = null
        }
    }

    val animatedBorderColor by animateColorAsState(
        targetValue = flashColor ?: if (isSelected) TerminalCyan else TerminalSurfaceBorder,
        animationSpec = tween(durationMillis = 350),
        label = "ticker_border_anim"
    )

    val isPositive = ticker.isPositive
    val trendColor = if (isPositive) TerminalBullishGreen else TerminalBearishRed
    val trendBg = if (isPositive) TerminalBullishGreenBg else TerminalBearishRedBg
    val cleanSymbol = ticker.symbol.replace("USDT", "")

    val cardModifier = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(if (isSelected) TerminalSurfaceElevated else TerminalSurface)
        .border(1.dp, animatedBorderColor, RoundedCornerShape(12.dp))
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        .padding(14.dp)
        .testTag("crypto_ticker_${ticker.symbol.lowercase(Locale.US)}")

    Column(
        modifier = cardModifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Header: Symbol name, badge, and Trend Indicator Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = cleanSymbol,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TerminalTextPrimary,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "/USDT",
                    fontSize = 12.sp,
                    color = TerminalTextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Green / Red Trend Indicator Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(trendBg)
                    .border(0.5.dp, trendColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("ticker_trend_${ticker.symbol.lowercase(Locale.US)}")
            ) {
                Icon(
                    imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = if (isPositive) "Uptrend" else "Downtrend",
                    tint = trendColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = String.format(Locale.US, "%s%.2f%%", if (isPositive) "+" else "", ticker.priceChangePercent),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = trendColor,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Middle Row: Big Live Price, Absolute Change, and Sparkline Preview
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = String.format(Locale.US, "$%,.2f", ticker.lastPrice),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = TerminalTextPrimary,
                    modifier = Modifier.testTag("ticker_price_${ticker.symbol.lowercase(Locale.US)}")
                )
                Text(
                    text = String.format(
                        Locale.US,
                        "%s$%,.2f 24h",
                        if (ticker.priceChange >= 0) "+" else "",
                        ticker.priceChange
                    ),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = trendColor,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (showSparkline) {
                // Mini 24H Price Path Sparkline
                MiniTickerSparkline(
                    isPositive = isPositive,
                    high = ticker.highPrice24h,
                    low = ticker.lowPrice24h,
                    current = ticker.lastPrice,
                    modifier = Modifier
                        .width(72.dp)
                        .height(32.dp)
                )
            }
        }

        // Bottom 24-Hour Range Visualizer
        if (showRangeBar) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = String.format(Locale.US, "24h L: $%,.0f", ticker.lowPrice24h),
                        fontSize = 9.5.sp,
                        color = TerminalTextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = String.format(Locale.US, "24h H: $%,.0f", ticker.highPrice24h),
                        fontSize = 9.5.sp,
                        color = TerminalTextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Range Progress Bar
                val rangeSpan = (ticker.highPrice24h - ticker.lowPrice24h).coerceAtLeast(0.001)
                val positionRatio = ((ticker.lastPrice - ticker.lowPrice24h) / rangeSpan).toFloat().coerceIn(0f, 1f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(TerminalSurfaceBorder)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(positionRatio)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(trendColor)
                    )
                }
            }
        }
    }
}

/**
 * Compact Horizontal Strip Ticker suitable for market carousels and top bars.
 */
@Composable
fun CompactCryptoTicker(
    ticker: Ticker24h,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val isPositive = ticker.isPositive
    val trendColor = if (isPositive) TerminalBullishGreen else TerminalBearishRed
    val cleanSymbol = ticker.symbol.replace("USDT", "")

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(TerminalSurface)
            .border(1.dp, TerminalSurfaceBorder, RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 10.dp, vertical = 7.dp)
            .testTag("compact_ticker_${ticker.symbol.lowercase(Locale.US)}"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = cleanSymbol,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TerminalTextPrimary,
            fontFamily = FontFamily.Monospace
        )

        Text(
            text = String.format(Locale.US, "$%,.2f", ticker.lastPrice),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TerminalTextPrimary,
            fontFamily = FontFamily.Monospace
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                contentDescription = null,
                tint = trendColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = String.format(Locale.US, "%s%.2f%%", if (isPositive) "+" else "", ticker.priceChangePercent),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = trendColor,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Lightweight Canvas mini-sparkline curve rendering the 24h price trend arc.
 */
@Composable
private fun MiniTickerSparkline(
    isPositive: Boolean,
    high: Double,
    low: Double,
    current: Double,
    modifier: Modifier = Modifier
) {
    val trendColor = if (isPositive) TerminalBullishGreen else TerminalBearishRed

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val range = (high - low).coerceAtLeast(0.001)
        val currentY = (h * (1.0 - (current - low) / range)).toFloat().coerceIn(4f, h - 4f)
        val startY = if (isPositive) h - 4f else 4f
        val midY = if (isPositive) h * 0.45f else h * 0.55f

        val path = Path().apply {
            moveTo(0f, startY)
            cubicTo(
                w * 0.35f, startY,
                w * 0.65f, midY,
                w, currentY
            )
        }

        drawPath(
            path = path,
            color = trendColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw endpoint pulse dot
        drawCircle(
            color = trendColor,
            radius = 3.dp.toPx(),
            center = Offset(w, currentY)
        )
    }
}
