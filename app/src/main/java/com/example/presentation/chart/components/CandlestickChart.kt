package com.example.presentation.chart.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.domain.model.Candle
import com.example.domain.model.TechnicalIndicators

/**
 * Backward compatibility forwarder delegating to [CandleChart].
 */
@Composable
fun CandlestickChart(
    candles: List<Candle>,
    indicators: TechnicalIndicators,
    modifier: Modifier = Modifier,
    onCrosshairSelected: ((Candle?) -> Unit)? = null
) {
    CandleChart(
        candles = candles,
        indicators = indicators,
        modifier = modifier,
        onCrosshairSelected = onCrosshairSelected
    )
}
