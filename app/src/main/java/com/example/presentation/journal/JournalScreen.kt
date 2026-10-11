package com.example.presentation.journal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.SignalDirection
import com.example.domain.model.TradeJournalEntry
import com.example.ui.theme.TerminalBackground
import com.example.ui.theme.TerminalBearishRed
import com.example.ui.theme.TerminalBullishGreen
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceBorder
import com.example.ui.theme.TerminalSurfaceElevated
import com.example.ui.theme.TerminalTextMuted
import com.example.ui.theme.TerminalTextPrimary
import com.example.ui.theme.TerminalTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun JournalScreen(
    viewModel: JournalViewModel,
    modifier: Modifier = Modifier
) {
    val entries by viewModel.filteredEntries.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val activeFilter by viewModel.selectedFilter.collectAsState()
    val explainingId by viewModel.explainingTradeId.collectAsState()
    val statusMsg by viewModel.statusMessage.collectAsState()

    var showLogDialog by remember { mutableStateOf(false) }
    var tradeToClose by remember { mutableStateOf<TradeJournalEntry?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBackground)
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TRADE JOURNAL",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = TerminalTextPrimary
                )
                Text(
                    text = "Audited P&L • Expectancy • AI Trade Audits",
                    fontSize = 11.sp,
                    color = TerminalTextMuted
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(TerminalCyan)
                    .clickable { showLogDialog = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("log_trade_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Log Trade",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "LOG TRADE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Performance Metrics Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(TerminalSurface)
                .border(1.dp, TerminalSurfaceBorder, RoundedCornerShape(12.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val isProfit = summary.totalRealizedPnlUsd >= 0
            val pnlColor = if (isProfit) TerminalBullishGreen else TerminalBearishRed

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "REALIZED P&L",
                        fontSize = 9.sp,
                        color = TerminalTextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = String.format(Locale.US, "%s$%,.2f", if (isProfit) "+" else "", summary.totalRealizedPnlUsd),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = pnlColor
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Win Rate Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(TerminalBullishGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "WIN: %.1f%%", summary.winRatePercent),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TerminalBullishGreen,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Open Trades Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(TerminalCyan.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "${summary.openTradesCount} OPEN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TerminalCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Sub-metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "EXPECTANCY", fontSize = 9.sp, color = TerminalTextMuted)
                    Text(
                        text = String.format(Locale.US, "%+$,.2f / trade", summary.expectancyUsd),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TerminalTextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "PROFIT FACTOR", fontSize = 9.sp, color = TerminalTextMuted)
                    Text(
                        text = String.format(Locale.US, "%.2f", summary.profitFactor),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TerminalCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "CLOSED TRADES", fontSize = 9.sp, color = TerminalTextMuted)
                    Text(
                        text = "${summary.closedTradesCount} (${summary.winCount}W / ${summary.lossCount}L)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TerminalTextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(TerminalSurfaceElevated)
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            JournalFilter.values().forEach { filter ->
                val isSelected = activeFilter == filter
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) TerminalCyan else Color.Transparent)
                        .clickable { viewModel.setFilter(filter) }
                        .padding(vertical = 6.dp)
                        .testTag("filter_${filter.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filter.name,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.Black else TerminalTextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Entries List
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = TerminalTextMuted,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No journal entries for current filter.",
                        color = TerminalTextMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Log a trade manually or tap 'Log from Signal' on dashboard.",
                        color = TerminalTextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries, key = { it.id }) { trade ->
                    JournalTradeCard(
                        trade = trade,
                        isExplaining = explainingId == trade.id,
                        onCloseTrade = { tradeToClose = trade },
                        onExplainTrade = { viewModel.explainTradeWithGemini(trade) },
                        onDelete = { viewModel.deleteTrade(trade.id) }
                    )
                }
            }
        }
    }

    // Log Trade Dialog
    if (showLogDialog) {
        LogTradeDialog(
            onDismiss = { showLogDialog = false },
            onConfirm = { sym, tf, dir, entry, size, tp, sl, notes ->
                viewModel.logTrade(sym, tf, dir, entry, size, tp, sl, notes)
                showLogDialog = false
            }
        )
    }

    // Close Trade Dialog
    tradeToClose?.let { trade ->
        CloseTradeDialog(
            trade = trade,
            onDismiss = { tradeToClose = null },
            onConfirm = { exitPrice ->
                viewModel.closeTrade(trade.id, exitPrice)
                tradeToClose = null
            }
        )
    }
}

@Composable
fun JournalTradeCard(
    trade: TradeJournalEntry,
    isExplaining: Boolean,
    onCloseTrade: () -> Unit,
    onExplainTrade: () -> Unit,
    onDelete: () -> Unit
) {
    val isLong = trade.direction.isBullish
    val dirColor = if (isLong) TerminalBullishGreen else TerminalBearishRed
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.US) }
    var expandedAnalysis by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(TerminalSurface)
            .border(1.dp, TerminalSurfaceBorder, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: Header (Symbol, Direction, Status)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(dirColor.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = trade.direction.name,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = dirColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${trade.symbol} • ${trade.timeframe}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TerminalTextPrimary,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (trade.isOpen) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(TerminalCyan.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ACTIVE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TerminalCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    val pnl = trade.pnlUsd ?: 0.0
                    val pnlColor = if (pnl >= 0) TerminalBullishGreen else TerminalBearishRed
                    Text(
                        text = String.format(Locale.US, "%s$%,.2f (%+.1f%%)", if (pnl >= 0) "+" else "", pnl, trade.pnlPercent ?: 0.0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = pnlColor,
                        fontFamily = FontFamily.Monospace
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = TerminalTextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Row 2: Price Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "ENTRY", fontSize = 8.5.sp, color = TerminalTextMuted)
                Text(
                    text = String.format(Locale.US, "$%,.2f", trade.entryPrice),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TerminalTextPrimary,
                    fontFamily = FontFamily.Monospace
                )
            }

            Column {
                Text(text = "EXIT", fontSize = 8.5.sp, color = TerminalTextMuted)
                Text(
                    text = trade.exitPrice?.let { String.format(Locale.US, "$%,.2f", it) } ?: "--",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TerminalTextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }

            Column {
                Text(text = "TARGET (TP)", fontSize = 8.5.sp, color = TerminalTextMuted)
                Text(
                    text = trade.targetPrice?.let { String.format(Locale.US, "$%,.2f", it) } ?: "--",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TerminalBullishGreen,
                    fontFamily = FontFamily.Monospace
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(text = "STOP (SL)", fontSize = 8.5.sp, color = TerminalTextMuted)
                Text(
                    text = trade.stopLossPrice?.let { String.format(Locale.US, "$%,.2f", it) } ?: "--",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TerminalBearishRed,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Row 3: Action Buttons & AI Explainer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dateFormat.format(Date(trade.entryTimestamp)),
                fontSize = 9.sp,
                color = TerminalTextMuted,
                fontFamily = FontFamily.Monospace
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (trade.isOpen) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(TerminalSurfaceElevated)
                            .border(0.5.dp, TerminalCyan, RoundedCornerShape(4.dp))
                            .clickable(onClick = onCloseTrade)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "CLOSE POSITION",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TerminalCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // AI Explanation Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(TerminalSurfaceElevated)
                        .border(0.5.dp, TerminalCyan.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .clickable {
                            if (trade.aiAnalysis == null) {
                                onExplainTrade()
                            }
                            expandedAnalysis = !expandedAnalysis
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isExplaining) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(10.dp),
                                strokeWidth = 1.5.dp,
                                color = TerminalCyan
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = TerminalCyan,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = if (trade.aiAnalysis != null) (if (expandedAnalysis) "HIDE AUDIT" else "VIEW AUDIT") else "AI EXPLAIN",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TerminalCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Expandable Gemini AI Analysis Panel
        AnimatedVisibility(
            visible = expandedAnalysis && trade.aiAnalysis != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            trade.aiAnalysis?.let { analysis ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(TerminalSurfaceElevated)
                        .border(0.5.dp, TerminalCyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = TerminalCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "GEMINI QUANTITATIVE AUDIT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TerminalCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = analysis,
                        fontSize = 10.sp,
                        color = TerminalTextSecondary,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun LogTradeDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        symbol: String,
        timeframe: String,
        direction: SignalDirection,
        entryPrice: Double,
        sizeUsd: Double,
        targetPrice: Double?,
        stopLossPrice: Double?,
        notes: String
    ) -> Unit
) {
    var symbol by remember { mutableStateOf("BTCUSDT") }
    var timeframe by remember { mutableStateOf("1m") }
    var isLong by remember { mutableStateOf(true) }
    var entryPriceText by remember { mutableStateOf("96500.0") }
    var sizeUsdText by remember { mutableStateOf("500.0") }
    var targetText by remember { mutableStateOf("") }
    var stopLossText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TerminalSurface,
        title = {
            Text(
                text = "LOG TRADE EXECUTION",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = TerminalCyan
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Direction Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(TerminalSurfaceElevated)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isLong) TerminalBullishGreen else Color.Transparent)
                            .clickable { isLong = true }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "BUY / LONG",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLong) Color.Black else TerminalTextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (!isLong) TerminalBearishRed else Color.Transparent)
                            .clickable { isLong = false }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SELL / SHORT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!isLong) Color.White else TerminalTextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it },
                    label = { Text("Symbol (e.g. BTCUSDT)", fontSize = 10.sp) },
                    singleLine = true,
                    colors = terminalTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = entryPriceText,
                        onValueChange = { entryPriceText = it },
                        label = { Text("Entry Price ($)", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = terminalTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = sizeUsdText,
                        onValueChange = { sizeUsdText = it },
                        label = { Text("Size ($)", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = terminalTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it },
                        label = { Text("Target TP ($)", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = terminalTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = stopLossText,
                        onValueChange = { stopLossText = it },
                        label = { Text("Stop SL ($)", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = terminalTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Strategy", fontSize = 10.sp) },
                    singleLine = true,
                    colors = terminalTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val entry = entryPriceText.toDoubleOrNull() ?: 0.0
                    val size = sizeUsdText.toDoubleOrNull() ?: 500.0
                    val tp = targetText.toDoubleOrNull()
                    val sl = stopLossText.toDoubleOrNull()
                    val dir = if (isLong) SignalDirection.BUY else SignalDirection.SELL
                    onConfirm(symbol, timeframe, dir, entry, size, tp, sl, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = TerminalCyan)
            ) {
                Text("SAVE ENTRY", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TerminalTextMuted)
            }
        }
    )
}

@Composable
fun CloseTradeDialog(
    trade: TradeJournalEntry,
    onDismiss: () -> Unit,
    onConfirm: (exitPrice: Double) -> Unit
) {
    var exitPriceText by remember { mutableStateOf(trade.targetPrice?.toString() ?: trade.entryPrice.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TerminalSurface,
        title = {
            Text(
                text = "CLOSE POSITION: ${trade.symbol}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = TerminalCyan
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Enter actual realized exit price to compute P&L:",
                    fontSize = 11.sp,
                    color = TerminalTextSecondary
                )
                OutlinedTextField(
                    value = exitPriceText,
                    onValueChange = { exitPriceText = it },
                    label = { Text("Exit Price ($)", fontSize = 10.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = terminalTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = exitPriceText.toDoubleOrNull() ?: trade.entryPrice
                    onConfirm(price)
                },
                colors = ButtonDefaults.buttonColors(containerColor = TerminalCyan)
            ) {
                Text("CONFIRM EXIT", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TerminalTextMuted)
            }
        }
    )
}

@Composable
private fun terminalTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = TerminalCyan,
    unfocusedBorderColor = TerminalSurfaceBorder,
    focusedLabelColor = TerminalCyan,
    unfocusedLabelColor = TerminalTextMuted,
    focusedTextColor = TerminalTextPrimary,
    unfocusedTextColor = TerminalTextPrimary,
    cursorColor = TerminalCyan
)
