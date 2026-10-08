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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TerminalBearishRed
import com.example.ui.theme.TerminalBullishGreen
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.TerminalGridLine
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceBorder
import com.example.ui.theme.TerminalTextMuted
import java.util.Locale

@Composable
fun RsiPane(
    rsiValues: List<Double>,
    currentRsi: Double?,
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
                text = "RSI (14)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TerminalTextMuted,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(8.dp))
            val rsiColor = when {
                currentRsi == null -> TerminalTextMuted
                currentRsi >= 70.0 -> TerminalBearishRed
                currentRsi <= 30.0 -> TerminalBullishGreen
                else -> TerminalCyan
            }
            Text(
                text = currentRsi?.let { String.format(Locale.US, "%.1f", it) } ?: "--",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = rsiColor,
                fontFamily = FontFamily.Monospace
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Draw 70 overbought and 30 oversold reference lines
                val y70 = h * 0.30f
                val y50 = h * 0.50f
                val y30 = h * 0.70f

                drawLine(
                    color = TerminalBearishRed.copy(alpha = 0.5f),
                    start = Offset(0f, y70),
                    end = Offset(w, y70),
                    strokeWidth = 1f
                )
                drawLine(
                    color = TerminalGridLine,
                    start = Offset(0f, y50),
                    end = Offset(w, y50),
                    strokeWidth = 1f
                )
                drawLine(
                    color = TerminalBullishGreen.copy(alpha = 0.5f),
                    start = Offset(0f, y30),
                    end = Offset(w, y30),
                    strokeWidth = 1f
                )

                if (rsiValues.size < 2) return@Canvas

                val stepX = w / (rsiValues.size - 1)
                val path = Path()

                for (i in rsiValues.indices) {
                    val rsi = rsiValues[i].coerceIn(0.0, 100.0)
                    val x = i * stepX
                    val y = (h * (1.0 - (rsi / 100.0))).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(
                    path = path,
                    color = TerminalCyan,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}
