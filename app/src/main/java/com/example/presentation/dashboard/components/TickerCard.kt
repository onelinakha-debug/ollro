package com.example.presentation.dashboard.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.domain.model.Ticker24h
import com.example.presentation.common.LiveCryptoTicker

/**
 * Backward compatibility wrapper delegating to [LiveCryptoTicker].
 */
@Composable
fun TickerCard(
    ticker: Ticker24h,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    LiveCryptoTicker(
        ticker = ticker,
        modifier = modifier,
        isSelected = isSelected,
        onClick = onClick
    )
}
