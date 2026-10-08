package com.example.presentation.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.domain.model.AlertCondition
import com.example.domain.model.PriceAlert
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsDialog(
    currentSymbol: String,
    viewModel: AlertsViewModel,
    onDismiss: () -> Unit
) {
    val alerts by viewModel.alerts.collectAsState()
    var showAddForm by remember { mutableStateOf(false) }

    var targetValueText by remember { mutableStateOf("98000.0") }
    var selectedCondition by remember { mutableStateOf(AlertCondition.PRICE_ABOVE) }
    var conditionDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TerminalSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Surveillance Alerts",
                    color = TerminalTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { showAddForm = !showAddForm },
                    modifier = Modifier.testTag("toggle_add_alert_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Alert",
                        tint = TerminalCyan
                    )
                }
            }
        },
        text = {
            val isPollingActive by viewModel.isPollingActive.collectAsState()

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Background WorkManager Surveillance Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TerminalSurfaceElevated)
                        .border(1.dp, TerminalCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "WORKMANAGER BACKGROUND",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TerminalCyan,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (isPollingActive) "Active (Polls every 15m & notifies)" else "Inactive",
                            fontSize = 11.sp,
                            color = if (isPollingActive) TerminalBullishGreen else TerminalTextMuted
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { viewModel.triggerBackgroundCheck() },
                            modifier = Modifier.testTag("poll_now_button")
                        ) {
                            Text("POLL NOW", fontSize = 10.sp, color = TerminalCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                }

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
                            text = "NEW SURVEILLANCE TRIGGER ($currentSymbol)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TerminalCyan,
                            fontFamily = FontFamily.Monospace
                        )

                        ExposedDropdownMenuBox(
                            expanded = conditionDropdownExpanded,
                            onExpandedChange = { conditionDropdownExpanded = !conditionDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedCondition.name.replace("_", " "),
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = conditionDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TerminalCyan,
                                    unfocusedBorderColor = TerminalSurfaceBorder
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = conditionDropdownExpanded,
                                onDismissRequest = { conditionDropdownExpanded = false }
                            ) {
                                AlertCondition.values().forEach { cond ->
                                    DropdownMenuItem(
                                        text = { Text(cond.name.replace("_", " ")) },
                                        onClick = {
                                            selectedCondition = cond
                                            conditionDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = targetValueText,
                            onValueChange = { targetValueText = it },
                            label = { Text("Trigger Threshold") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TerminalCyan,
                                unfocusedBorderColor = TerminalSurfaceBorder
                            )
                        )

                        Button(
                            onClick = {
                                val v = targetValueText.toDoubleOrNull() ?: 0.0
                                viewModel.addAlert(currentSymbol, selectedCondition, v)
                                showAddForm = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TerminalCyan),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_alert_button")
                        ) {
                            Text("Save Alert to Terminal", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (alerts.isEmpty()) {
                    Text(
                        text = "No active price or indicator alerts. Click '+' to add one.",
                        fontSize = 12.sp,
                        color = TerminalTextMuted
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(alerts) { alert ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(TerminalSurfaceElevated)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = alert.symbol,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = TerminalTextPrimary
                                    )
                                    Text(
                                        text = "${alert.condition.name.replace("_", " ")} ${String.format(Locale.US, "%,.2f", alert.targetValue)}",
                                        fontSize = 11.sp,
                                        color = TerminalCyan
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = alert.isEnabled,
                                        onCheckedChange = { viewModel.toggleAlert(alert) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = TerminalCyan)
                                    )
                                    IconButton(
                                        onClick = { viewModel.deleteAlert(alert.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = TerminalBearishRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = TerminalCyan, fontWeight = FontWeight.Bold)
            }
        }
    )
}
