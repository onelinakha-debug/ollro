package com.example.presentation.holdings

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.domain.model.HoldingWithPnL
import com.example.presentation.holdings.components.PortfolioTreeMap
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
fun HoldingsDialog(
    viewModel: HoldingsViewModel,
    onDismiss: () -> Unit
) {
    val summary by viewModel.portfolioSummary.collectAsState()
    var showAddForm by remember { mutableStateOf(false) }

    var symbolText by remember { mutableStateOf("BTCUSDT") }
    var amountText by remember { mutableStateOf("0.5") }
    var buyPriceText by remember { mutableStateOf("92000.0") }
    var selectedTab by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TerminalSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PORTFOLIO HOLDINGS",
                        color = TerminalTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Real-Time Mark-to-Market PnL",
                        color = TerminalCyan,
                        fontSize = 11.sp
                    )
                }

                IconButton(
                    onClick = { showAddForm = !showAddForm },
                    modifier = Modifier.testTag("add_holding_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Holding",
                        tint = TerminalCyan
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Portfolio Summary Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TerminalSurfaceElevated)
                        .border(1.dp, TerminalCyan.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TOTAL PORTFOLIO VALUE",
                                fontSize = 10.sp,
                                color = TerminalTextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = String.format(Locale.US, "$%,.2f", summary.totalCurrentValue),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TerminalTextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Net PnL Pill
                        val isProfit = summary.isProfit
                        val pnlColor = if (isProfit) TerminalBullishGreen else TerminalBearishRed
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(pnlColor.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isProfit) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = pnlColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format(Locale.US, "%s$%,.2f (%.2f%%)", if (isProfit) "+" else "", summary.totalPnL, summary.totalPnLPercent),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = pnlColor,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = String.format(Locale.US, "Invested: $%,.2f", summary.totalInvested),
                        fontSize = 11.sp,
                        color = TerminalTextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Add Form
                if (showAddForm) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(TerminalSurfaceElevated)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "RECORD ASSET HOLDING",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TerminalCyan,
                            fontFamily = FontFamily.Monospace
                        )

                        OutlinedTextField(
                            value = symbolText,
                            onValueChange = { symbolText = it.uppercase(Locale.US) },
                            label = { Text("Asset Symbol (e.g. BTCUSDT)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("holding_symbol_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TerminalCyan,
                                unfocusedBorderColor = TerminalSurfaceBorder,
                                focusedTextColor = TerminalTextPrimary,
                                unfocusedTextColor = TerminalTextPrimary
                            ),
                            singleLine = true
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = amountText,
                                onValueChange = { amountText = it },
                                label = { Text("Amount") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("holding_amount_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TerminalCyan,
                                    unfocusedBorderColor = TerminalSurfaceBorder,
                                    focusedTextColor = TerminalTextPrimary,
                                    unfocusedTextColor = TerminalTextPrimary
                                ),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = buyPriceText,
                                onValueChange = { buyPriceText = it },
                                label = { Text("Avg Buy Price ($)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("holding_price_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TerminalCyan,
                                    unfocusedBorderColor = TerminalSurfaceBorder,
                                    focusedTextColor = TerminalTextPrimary,
                                    unfocusedTextColor = TerminalTextPrimary
                                ),
                                singleLine = true
                            )
                        }

                        Button(
                            onClick = {
                                val amt = amountText.toDoubleOrNull() ?: 0.0
                                val price = buyPriceText.toDoubleOrNull() ?: 0.0
                                if (symbolText.isNotBlank() && amt > 0 && price > 0) {
                                    viewModel.saveHolding(symbolText, amt, price)
                                    showAddForm = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_holding_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = TerminalCyan)
                        ) {
                            Text("SAVE HOLDING", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }

                // Holdings Distribution / List Section
                if (summary.holdingsWithPnL.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No holdings tracked yet. Tap + to add assets.",
                            color = TerminalTextMuted,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    // Tab Selector: TreeMap vs List
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
                                .background(if (selectedTab == 0) TerminalCyan else Color.Transparent)
                                .clickable { selectedTab = 0 }
                                .padding(vertical = 6.dp)
                                .testTag("treemap_tab"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "WEIGHTING TREEMAP",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 0) Color.Black else TerminalTextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (selectedTab == 1) TerminalCyan else Color.Transparent)
                                .clickable { selectedTab = 1 }
                                .padding(vertical = 6.dp)
                                .testTag("asset_list_tab"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ASSET LIST (${summary.holdingsWithPnL.size})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 1) Color.Black else TerminalTextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    if (selectedTab == 0) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            PortfolioTreeMap(
                                holdingsWithPnL = summary.holdingsWithPnL
                            )
                            Text(
                                text = "Tile Area = Portfolio Weighting %  •  Color = Net PnL %",
                                fontSize = 9.sp,
                                color = TerminalTextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(summary.holdingsWithPnL, key = { it.holding.id }) { item ->
                                HoldingCard(
                                    item = item,
                                    onDelete = { viewModel.deleteHolding(item.holding.id) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_holdings_button")
            ) {
                Text("CLOSE", color = TerminalCyan, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun HoldingCard(
    item: HoldingWithPnL,
    onDelete: () -> Unit
) {
    val isProfit = item.isProfit
    val pnlColor = if (isProfit) TerminalBullishGreen else TerminalBearishRed

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(TerminalSurfaceElevated)
            .border(1.dp, TerminalSurfaceBorder, RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.holding.symbol,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TerminalTextPrimary,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${item.holding.amount}",
                    fontSize = 11.sp,
                    color = TerminalCyan,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = String.format(Locale.US, "Value: $%,.2f (Avg: $%,.2f)", item.currentValue, item.holding.averageBuyPrice),
                fontSize = 10.sp,
                color = TerminalTextMuted,
                fontFamily = FontFamily.Monospace
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format(Locale.US, "%s$%,.2f", if (isProfit) "+" else "", item.pnl),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = pnlColor,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = String.format(Locale.US, "%s%.2f%%", if (isProfit) "+" else "", item.pnlPercent),
                    fontSize = 10.sp,
                    color = pnlColor,
                    fontFamily = FontFamily.Monospace
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = TerminalTextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
