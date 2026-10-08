package com.example.presentation.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.example.domain.model.Ticker24h
import com.example.presentation.common.PriceChangeChip
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
fun TickerCard(
    ticker: Ticker24h,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    val borderColor = if (isSelected) TerminalCyan else TerminalSurfaceBorder
    val bgColor = if (isSelected) TerminalSurfaceElevated else TerminalSurface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
            .testTag("ticker_card_${ticker.symbol.lowercase()}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = ticker.symbol,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TerminalTextPrimary,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Binance Spot/Perp",
                    fontSize = 11.sp,
                    color = TerminalTextMuted
                )
            }
            PriceChangeChip(changePercent = ticker.priceChangePercent)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = "MARK PRICE",
                    fontSize = 9.sp,
                    color = TerminalTextMuted,
                    fontWeight = FontWeight.SemiBold
                )
                val priceColor = if (ticker.isPositive) TerminalBullishGreen else TerminalBearishRed
                Text(
                    text = String.format(Locale.US, "$%,.2f", ticker.lastPrice),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = priceColor
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "24H RANGE",
                    fontSize = 9.sp,
                    color = TerminalTextMuted,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = String.format(Locale.US, "H: $%,.0f  L: $%,.0f", ticker.highPrice24h, ticker.lowPrice24h),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TerminalTextSecondary
                )
            }
        }
    }
}
