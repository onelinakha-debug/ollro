package com.example.presentation.holdings.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.HoldingWithPnL
import com.example.ui.theme.TerminalBearishRed
import com.example.ui.theme.TerminalBullishGreen
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.TerminalSurfaceBorder
import com.example.ui.theme.TerminalTextMuted
import com.example.ui.theme.TerminalTextPrimary
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

data class TreeMapNode(
    val item: HoldingWithPnL,
    val weightPercent: Double,
    val rect: Rect
)

/**
 * Interactive Squarified / Binary Partitioning TreeMap Composable
 * visualizing asset allocation weightings and live mark-to-market performance.
 */
@Composable
fun PortfolioTreeMap(
    holdingsWithPnL: List<HoldingWithPnL>,
    modifier: Modifier = Modifier,
    onHoldingClick: ((HoldingWithPnL) -> Unit)? = null
) {
    if (holdingsWithPnL.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No holdings recorded for distribution visualization.",
                color = TerminalTextMuted,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        return
    }

    val textMeasurer = rememberTextMeasurer()
    var selectedNode by remember { mutableStateOf<TreeMapNode?>(null) }
    var layoutNodes by remember { mutableStateOf<List<TreeMapNode>>(emptyList()) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .testTag("portfolio_treemap_canvas")
            .pointerInput(layoutNodes) {
                detectTapGestures { tapOffset ->
                    val clicked = layoutNodes.firstOrNull { it.rect.contains(tapOffset) }
                    selectedNode = clicked
                    clicked?.let { onHoldingClick?.invoke(it.item) }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            if (width <= 0 || height <= 0) return@Canvas

            val totalValue = holdingsWithPnL.sumOf { it.currentValue.coerceAtLeast(0.001) }
            val sortedHoldings = holdingsWithPnL.sortedByDescending { it.currentValue }

            // Compute proportional TreeMap nodes
            val nodes = computeTreeMapNodes(
                holdings = sortedHoldings,
                totalValue = totalValue,
                bounds = Rect(0f, 0f, width, height)
            )
            layoutNodes = nodes

            val cornerRadiusPx = 8.dp.toPx()
            val innerPaddingPx = 3.dp.toPx()

            for (node in nodes) {
                val r = node.rect
                val nodeWidth = r.width - (innerPaddingPx * 2)
                val nodeHeight = r.height - (innerPaddingPx * 2)

                if (nodeWidth <= 4f || nodeHeight <= 4f) continue

                val tileRect = Rect(
                    left = r.left + innerPaddingPx,
                    top = r.top + innerPaddingPx,
                    right = r.right - innerPaddingPx,
                    bottom = r.bottom - innerPaddingPx
                )

                val item = node.item
                val isProfit = item.isProfit
                val pnlMag = abs(item.pnlPercent).coerceIn(0.0, 30.0) / 30.0

                val baseColor = if (isProfit) {
                    // Deep Forest base with luminous botanical emerald saturation
                    Color(
                        red = 0.06f,
                        green = (0.24f + (pnlMag * 0.26f)).toFloat().coerceIn(0.24f, 0.52f),
                        blue = 0.22f,
                        alpha = 0.95f
                    )
                } else {
                    // Terracotta Clay tone with warmth
                    Color(
                        red = (0.35f + (pnlMag * 0.35f)).toFloat().coerceIn(0.35f, 0.70f),
                        green = 0.16f,
                        blue = 0.14f,
                        alpha = 0.95f
                    )
                }

                val borderColor = when {
                    selectedNode == node -> TerminalCyan
                    isProfit -> TerminalBullishGreen.copy(alpha = 0.6f)
                    else -> TerminalBearishRed.copy(alpha = 0.6f)
                }

                val strokeWidth = if (selectedNode == node) 2.5.dp.toPx() else 1.dp.toPx()

                // Draw rounded rectangle tile
                val roundRect = RoundRect(
                    rect = tileRect,
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                )
                val path = Path().apply { addRoundRect(roundRect) }

                drawPath(path = path, color = baseColor)
                drawPath(path = path, color = borderColor, style = Stroke(width = strokeWidth))

                // Render Labels if tile is large enough
                val canFitDetails = nodeWidth >= 60.dp.toPx() && nodeHeight >= 44.dp.toPx()
                val canFitCompact = nodeWidth >= 36.dp.toPx() && nodeHeight >= 28.dp.toPx()

                if (canFitCompact) {
                    val cleanSymbol = item.holding.symbol.replace("USDT", "")
                    val weightText = String.format(Locale.US, "%.1f%%", node.weightPercent)

                    // Symbol text
                    drawText(
                        textMeasurer = textMeasurer,
                        text = cleanSymbol,
                        topLeft = Offset(tileRect.left + 8.dp.toPx(), tileRect.top + 6.dp.toPx()),
                        style = TextStyle(
                            color = TerminalTextPrimary,
                            fontSize = if (canFitDetails) 13.sp else 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )

                    // Weight text
                    drawText(
                        textMeasurer = textMeasurer,
                        text = weightText,
                        topLeft = Offset(
                            tileRect.left + 8.dp.toPx(),
                            tileRect.top + (if (canFitDetails) 22.dp.toPx() else 18.dp.toPx())
                        ),
                        style = TextStyle(
                            color = TerminalCyan,
                            fontSize = if (canFitDetails) 11.sp else 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    )

                    // Value and PnL text if room permits
                    if (canFitDetails && nodeHeight >= 64.dp.toPx()) {
                        val valText = String.format(Locale.US, "$%,.0f", item.currentValue)
                        val pnlText = String.format(Locale.US, "%s%.2f%%", if (isProfit) "+" else "", item.pnlPercent)

                        drawText(
                            textMeasurer = textMeasurer,
                            text = valText,
                            topLeft = Offset(tileRect.left + 8.dp.toPx(), tileRect.top + 36.dp.toPx()),
                            style = TextStyle(
                                color = TerminalTextMuted,
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )

                        drawText(
                            textMeasurer = textMeasurer,
                            text = pnlText,
                            topLeft = Offset(tileRect.left + 8.dp.toPx(), tileRect.top + 48.dp.toPx()),
                            style = TextStyle(
                                color = if (isProfit) TerminalBullishGreen else TerminalBearishRed,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Recursive binary space partitioning producing squarified TreeMap rectangles.
 */
private fun computeTreeMapNodes(
    holdings: List<HoldingWithPnL>,
    totalValue: Double,
    bounds: Rect
): List<TreeMapNode> {
    if (holdings.isEmpty()) return emptyList()

    if (holdings.size == 1) {
        val h = holdings.first()
        val weight = if (totalValue > 0) (h.currentValue / totalValue) * 100.0 else 100.0
        return listOf(TreeMapNode(item = h, weightPercent = weight, rect = bounds))
    }

    val subsetTotal = holdings.sumOf { it.currentValue.coerceAtLeast(0.001) }
    var halfWeight = subsetTotal / 2.0
    var accumulated = 0.0
    var splitIndex = 1

    for (i in holdings.indices) {
        accumulated += holdings[i].currentValue.coerceAtLeast(0.001)
        if (accumulated >= halfWeight) {
            splitIndex = (i + 1).coerceIn(1, holdings.size - 1)
            break
        }
    }

    val firstHalf = holdings.take(splitIndex)
    val secondHalf = holdings.drop(splitIndex)

    val firstHalfValue = firstHalf.sumOf { it.currentValue.coerceAtLeast(0.001) }
    val ratio = if (subsetTotal > 0) (firstHalfValue / subsetTotal).toFloat().coerceIn(0.05f, 0.95f) else 0.5f

    val isHorizontalSplit = bounds.width >= bounds.height

    val firstBounds: Rect
    val secondBounds: Rect

    if (isHorizontalSplit) {
        val splitX = bounds.left + (bounds.width * ratio)
        firstBounds = Rect(bounds.left, bounds.top, splitX, bounds.bottom)
        secondBounds = Rect(splitX, bounds.top, bounds.right, bounds.bottom)
    } else {
        val splitY = bounds.top + (bounds.height * ratio)
        firstBounds = Rect(bounds.left, bounds.top, bounds.right, splitY)
        secondBounds = Rect(bounds.left, splitY, bounds.right, bounds.bottom)
    }

    return computeTreeMapNodes(firstHalf, totalValue, firstBounds) +
            computeTreeMapNodes(secondHalf, totalValue, secondBounds)
}
