package com.example.presentation.chart.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.MacdOutput
import com.example.ui.theme.TerminalBearishRed
import com.example.ui.theme.TerminalBullishGreen
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.TerminalGridLine
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceBorder
import com.example.ui.theme.TerminalTextMuted
import com.example.ui.theme.TerminalWarningAmber
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

@Composable
fun MacdPane(
    macdHistory: List<MacdOutput>,
    currentMacd: MacdOutput?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(TerminalSurface)
            .border(1.dp, TerminalSurfaceBorder, RoundedCornerShape(8.dp))
            .padding(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MACD (12,26,9)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TerminalTextMuted,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (currentMacd != null) {
                Text(
                    text = String.format(Locale.US, "M: %.2f", currentMacd.macdLine),
                    fontSize = 10.sp,
                    color = TerminalCyan,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = String.format(Locale.US, "S: %.2f", currentMacd.signalLine),
                    fontSize = 10.sp,
                    color = TerminalWarningAmber,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(6.dp))
                val histColor = if (currentMacd.histogram >= 0) TerminalBullishGreen else TerminalBearishRed
                Text(
                    text = String.format(Locale.US, "H: %.2f", currentMacd.histogram),
                    fontSize = 10.sp,
                    color = histColor,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val midY = h / 2f

                // Zero line
                drawLine(
                    color = TerminalGridLine,
                    start = Offset(0f, midY),
                    end = Offset(w, midY),
                    strokeWidth = 1f
                )

                if (macdHistory.isEmpty()) return@Canvas

                val maxAbs = macdHistory.maxOfOrNull {
                    max(abs(it.macdLine), max(abs(it.signalLine), abs(it.histogram)))
                }?.coerceAtLeast(0.01) ?: 1.0

                val barWidth = (w / macdHistory.size).coerceAtLeast(2f)

                // Draw histogram bars
                for (i in macdHistory.indices) {
                    val item = macdHistory[i]
                    val x = i * barWidth
                    val barHeight = ((abs(item.histogram) / maxAbs) * (h / 2f)).toFloat()
                    val barColor = if (item.histogram >= 0) TerminalBullishGreen.copy(alpha = 0.6f) else TerminalBearishRed.copy(alpha = 0.6f)

                    if (item.histogram >= 0) {
                        drawRect(
                            color = barColor,
                            topLeft = Offset(x, midY - barHeight),
                            size = Size(barWidth * 0.8f, barHeight)
                        )
                    } else {
                        drawRect(
                            color = barColor,
                            topLeft = Offset(x, midY),
                            size = Size(barWidth * 0.8f, barHeight)
                        )
                    }
                }

                // Draw MACD Line & Signal Line
                if (macdHistory.size >= 2) {
                    val macdPath = Path()
                    val signalPath = Path()

                    for (i in macdHistory.indices) {
                        val item = macdHistory[i]
                        val x = i * barWidth + (barWidth / 2f)
                        val my = (midY - (item.macdLine / maxAbs * (h / 2f))).toFloat()
                        val sy = (midY - (item.signalLine / maxAbs * (h / 2f))).toFloat()

                        if (i == 0) {
                            macdPath.moveTo(x, my)
                            signalPath.moveTo(x, sy)
                        } else {
                            macdPath.lineTo(x, my)
                            signalPath.lineTo(x, sy)
                        }
                    }

                    drawPath(path = macdPath, color = TerminalCyan, style = Stroke(width = 1.8f))
                    drawPath(path = signalPath, color = TerminalWarningAmber, style = Stroke(width = 1.8f))
                }
            }
        }
    }
}
