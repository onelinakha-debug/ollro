package com.example.presentation.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.OrderBook
import com.example.ui.theme.TerminalBearishRed
import com.example.ui.theme.TerminalBearishRedBg
import com.example.ui.theme.TerminalBullishGreen
import com.example.ui.theme.TerminalBullishGreenBg
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceBorder
import com.example.ui.theme.TerminalTextMuted
import com.example.ui.theme.TerminalTextPrimary
import com.example.ui.theme.TerminalTextSecondary
import java.util.Locale

@Composable
fun OrderBookPreview(
    orderBook: OrderBook?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TerminalSurface)
            .border(1.dp, TerminalSurfaceBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
            .testTag("order_book_preview")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "L2 ORDER BOOK DEPTH",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TerminalCyan,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Live Binance 100ms Stream",
                    fontSize = 11.sp,
                    color = TerminalTextMuted
                )
            }
            if (orderBook != null) {
                Text(
                    text = String.format(Locale.US, "Spread: $%,.2f (%.3f%%)", orderBook.spread, orderBook.spreadPercent),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = TerminalTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (orderBook == null || orderBook.bids.isEmpty() || orderBook.asks.isEmpty()) {
            Text(
                text = "Connecting to depth stream...",
                fontSize = 12.sp,
                color = TerminalTextMuted
            )
            return
        }

        val maxBidQty = orderBook.bids.take(5).maxOfOrNull { it.quantity }?.coerceAtLeast(0.01) ?: 1.0
        val maxAskQty = orderBook.asks.take(5).maxOfOrNull { it.quantity }?.coerceAtLeast(0.01) ?: 1.0

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Bids Column
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("BID (USD)", fontSize = 10.sp, color = TerminalTextMuted, fontWeight = FontWeight.Bold)
                    Text("QTY", fontSize = 10.sp, color = TerminalTextMuted, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                orderBook.bids.take(5).forEach { bid ->
                    val fraction = (bid.quantity / maxBidQty).toFloat().coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction)
                                .align(Alignment.CenterEnd)
                                .background(TerminalBullishGreenBg)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                                .align(Alignment.Center),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = String.format(Locale.US, "%,.2f", bid.price),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TerminalBullishGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = String.format(Locale.US, "%.3f", bid.quantity),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TerminalTextPrimary
                            )
                        }
                    }
                }
            }

            // Asks Column
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("ASK (USD)", fontSize = 10.sp, color = TerminalTextMuted, fontWeight = FontWeight.Bold)
                    Text("QTY", fontSize = 10.sp, color = TerminalTextMuted, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                orderBook.asks.take(5).forEach { ask ->
                    val fraction = (ask.quantity / maxAskQty).toFloat().coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction)
                                .align(Alignment.CenterStart)
                                .background(TerminalBearishRedBg)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                                .align(Alignment.Center),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = String.format(Locale.US, "%,.2f", ask.price),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TerminalBearishRed,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = String.format(Locale.US, "%.3f", ask.quantity),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TerminalTextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
