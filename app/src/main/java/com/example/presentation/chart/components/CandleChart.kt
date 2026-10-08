package com.example.presentation.chart.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.indicators.IndicatorCalculator
import com.example.domain.model.Candle
import com.example.domain.model.TechnicalIndicators
import com.example.ui.theme.TerminalBearishRed
import com.example.ui.theme.TerminalBullishGreen
import com.example.ui.theme.TerminalCrosshair
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.TerminalGridLine
import com.example.ui.theme.TerminalSurfaceElevated
import com.example.ui.theme.TerminalTextMuted
import com.example.ui.theme.TerminalTextPrimary
import com.example.ui.theme.TerminalWarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * High-performance Canvas-based CandleChart supporting real-time market updates,
 * pinch-to-zoom, pan navigation, and crosshair touch inspection for OHLCV data.
 */
@Composable
fun CandleChart(
    candles: List<Candle>,
    indicators: TechnicalIndicators = TechnicalIndicators(),
    modifier: Modifier = Modifier,
    onCrosshairSelected: ((Candle?) -> Unit)? = null
) {
    if (candles.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Awaiting market candle feed...",
                color = TerminalTextMuted,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        return
    }

    val textMeasurer = rememberTextMeasurer()
    var visibleCount by remember { mutableIntStateOf(35) }
    var scrollOffset by remember { mutableIntStateOf(0) }
    var crosshairX by remember { mutableFloatStateOf(-1f) }
    var crosshairY by remember { mutableFloatStateOf(-1f) }
    var isUserInteracting by remember { mutableStateOf(false) }

    // Auto-scroll to latest candle on incoming real-time updates unless actively scrolling
    LaunchedEffect(candles.size) {
        if (!isUserInteracting && scrollOffset > 0) {
            scrollOffset = 0
        }
    }

    val safeVisibleCount = visibleCount.coerceIn(15, 100)
    val maxScroll = max(0, candles.size - safeVisibleCount)
    val currentScroll = scrollOffset.coerceIn(0, maxScroll)

    val startIndex = (candles.size - safeVisibleCount - currentScroll).coerceAtLeast(0)
    val windowCandles = candles.drop(startIndex).take(safeVisibleCount)

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("candle_chart_canvas")
            .pointerInput(candles.size) {
                detectTransformGestures { _, pan, zoom, _ ->
                    isUserInteracting = true
                    if (zoom != 1.0f) {
                        val newCount = (safeVisibleCount / zoom).toInt().coerceIn(15, 100)
                        visibleCount = newCount
                    }
                    if (pan.x != 0f) {
                        val delta = (pan.x / 18f).toInt()
                        scrollOffset = (scrollOffset + delta).coerceIn(0, max(0, candles.size - safeVisibleCount))
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        crosshairX = offset.x
                        crosshairY = offset.y
                    },
                    onDoubleTap = {
                        crosshairX = -1f
                        crosshairY = -1f
                        isUserInteracting = false
                        onCrosshairSelected?.invoke(null)
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isUserInteracting = true
                        crosshairX = offset.x
                        crosshairY = offset.y
                    },
                    onDrag = { change, _ ->
                        crosshairX = change.position.x
                        crosshairY = change.position.y
                    },
                    onDragEnd = {
                        isUserInteracting = false
                    },
                    onDragCancel = {
                        isUserInteracting = false
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (windowCandles.isEmpty()) return@Canvas

            val canvasWidth = size.width
            val canvasHeight = size.height

            val rightPadding = 62.dp.toPx()
            val bottomPadding = 24.dp.toPx()
            val topPadding = 16.dp.toPx()

            val chartWidth = canvasWidth - rightPadding
            val chartHeight = canvasHeight - bottomPadding - topPadding
            val volumeHeight = chartHeight * 0.18f
            val candleAreaHeight = chartHeight - volumeHeight

            // Calculate min/max price and volume
            val minPrice = windowCandles.minOf { it.low }
            val maxPrice = windowCandles.maxOf { it.high }
            val priceSpan = (maxPrice - minPrice).coerceAtLeast(0.0001)

            val maxVolume = windowCandles.maxOfOrNull { it.volume }?.coerceAtLeast(1.0) ?: 1.0

            // 1. Draw horizontal price grid lines and Y-axis labels
            val gridLineCount = 5
            for (i in 0..gridLineCount) {
                val ratio = i.toFloat() / gridLineCount
                val y = topPadding + (candleAreaHeight * (1f - ratio))
                val priceAtY = minPrice + (priceSpan * ratio)

                drawLine(
                    color = TerminalGridLine,
                    start = Offset(0f, y),
                    end = Offset(chartWidth, y),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )

                val priceText = String.format(Locale.US, "%,.2f", priceAtY)
                drawText(
                    textMeasurer = textMeasurer,
                    text = priceText,
                    topLeft = Offset(chartWidth + 6.dp.toPx(), y - 7.sp.toPx()),
                    style = TextStyle(
                        color = TerminalTextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }

            // 2. Draw candlesticks and volume bars
            val candleCount = windowCandles.size
            val slotWidth = chartWidth / candleCount
            val candleWidth = (slotWidth * 0.72f).coerceAtLeast(2f)

            var selectedHoveredCandle: Candle? = null

            for (i in windowCandles.indices) {
                val candle = windowCandles[i]
                val centerX = (i * slotWidth) + (slotWidth / 2f)

                val openY = topPadding + (candleAreaHeight * (1.0 - (candle.open - minPrice) / priceSpan)).toFloat()
                val closeY = topPadding + (candleAreaHeight * (1.0 - (candle.close - minPrice) / priceSpan)).toFloat()
                val highY = topPadding + (candleAreaHeight * (1.0 - (candle.high - minPrice) / priceSpan)).toFloat()
                val lowY = topPadding + (candleAreaHeight * (1.0 - (candle.low - minPrice) / priceSpan)).toFloat()

                val isBullish = candle.isBullish
                val color = if (isBullish) TerminalBullishGreen else TerminalBearishRed

                // Wick line
                drawLine(
                    color = color,
                    start = Offset(centerX, highY),
                    end = Offset(centerX, lowY),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Candle body
                val bodyTop = min(openY, closeY)
                val bodyBottom = max(openY, closeY)
                val bodyHeight = max(1.5f, bodyBottom - bodyTop)

                drawRect(
                    color = color,
                    topLeft = Offset(centerX - (candleWidth / 2f), bodyTop),
                    size = Size(candleWidth, bodyHeight)
                )

                // Volume Histogram
                val volRatio = (candle.volume / maxVolume).toFloat().coerceIn(0f, 1f)
                val volBarHeight = volumeHeight * volRatio
                val volTop = canvasHeight - bottomPadding - volBarHeight

                drawRect(
                    color = color.copy(alpha = 0.35f),
                    topLeft = Offset(centerX - (candleWidth / 2f), volTop),
                    size = Size(candleWidth, volBarHeight)
                )

                // Check crosshair proximity
                if (crosshairX >= 0f && abs(crosshairX - centerX) <= slotWidth / 2f) {
                    selectedHoveredCandle = candle
                }
            }

            // 3. Draw EMA indicator lines if available
            val fullCloses = candles.map { it.close }
            if (fullCloses.size >= 21) {
                val ema9 = IndicatorCalculator.calculateEMA(fullCloses, 9)
                val ema21 = IndicatorCalculator.calculateEMA(fullCloses, 21)

                val ema9Path = Path()
                val ema21Path = Path()

                var ema9Started = false
                var ema21Started = false

                for (i in windowCandles.indices) {
                    val globalIdx = startIndex + i
                    val x = (i * slotWidth) + (slotWidth / 2f)

                    if (globalIdx < ema9.size) {
                        val e9 = ema9[globalIdx]
                        val y9 = topPadding + (candleAreaHeight * (1.0 - (e9 - minPrice) / priceSpan)).toFloat()
                        if (!ema9Started) {
                            ema9Path.moveTo(x, y9)
                            ema9Started = true
                        } else {
                            ema9Path.lineTo(x, y9)
                        }
                    }

                    if (globalIdx < ema21.size) {
                        val e21 = ema21[globalIdx]
                        val y21 = topPadding + (candleAreaHeight * (1.0 - (e21 - minPrice) / priceSpan)).toFloat()
                        if (!ema21Started) {
                            ema21Path.moveTo(x, y21)
                            ema21Started = true
                        } else {
                            ema21Path.lineTo(x, y21)
                        }
                    }
                }

                if (ema9Started) {
                    drawPath(
                        path = ema9Path,
                        color = TerminalCyan.copy(alpha = 0.85f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                if (ema21Started) {
                    drawPath(
                        path = ema21Path,
                        color = TerminalWarningAmber.copy(alpha = 0.85f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }

            // 4. Time labels on X-axis (bottom)
            val timeStep = max(1, candleCount / 4)
            val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
            for (i in 0 until candleCount step timeStep) {
                val c = windowCandles[i]
                val x = (i * slotWidth) + (slotWidth / 2f)
                val timeStr = timeFormat.format(Date(c.timestamp))

                drawText(
                    textMeasurer = textMeasurer,
                    text = timeStr,
                    topLeft = Offset(x - 14.dp.toPx(), canvasHeight - bottomPadding + 4.dp.toPx()),
                    style = TextStyle(
                        color = TerminalTextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }

            // 5. Interactive Crosshair and Inspection Tooltip
            if (crosshairX in 0f..chartWidth && crosshairY in topPadding..(canvasHeight - bottomPadding)) {
                // Vertical crosshair line
                drawLine(
                    color = TerminalCrosshair,
                    start = Offset(crosshairX, topPadding),
                    end = Offset(crosshairX, canvasHeight - bottomPadding),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )

                // Horizontal crosshair line
                drawLine(
                    color = TerminalCrosshair,
                    start = Offset(0f, crosshairY),
                    end = Offset(chartWidth, crosshairY),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )

                // Price tag at crosshair Y on the right
                val crosshairPrice = minPrice + (priceSpan * (1f - (crosshairY - topPadding) / candleAreaHeight))
                val priceTag = String.format(Locale.US, "$%,.2f", crosshairPrice)
                val tagWidth = 56.dp.toPx()
                val tagHeight = 16.dp.toPx()

                drawRect(
                    color = TerminalCyan,
                    topLeft = Offset(chartWidth + 2.dp.toPx(), crosshairY - (tagHeight / 2f)),
                    size = Size(tagWidth, tagHeight)
                )

                drawText(
                    textMeasurer = textMeasurer,
                    text = priceTag,
                    topLeft = Offset(chartWidth + 4.dp.toPx(), crosshairY - 5.5.sp.toPx()),
                    style = TextStyle(
                        color = Color.Black,
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )

                // Notify callback
                onCrosshairSelected?.invoke(selectedHoveredCandle)
            }
        }
    }
}

private fun abs(value: Float): Float = kotlin.math.abs(value)
