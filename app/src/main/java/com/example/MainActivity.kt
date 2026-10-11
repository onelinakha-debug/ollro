package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.presentation.alerts.AlertsDialog
import com.example.presentation.alerts.AlertsViewModel
import com.example.presentation.chart.ChartScreen
import com.example.presentation.chart.ChartViewModel
import com.example.presentation.dashboard.DashboardScreen
import com.example.presentation.dashboard.DashboardViewModel
import com.example.presentation.journal.JournalScreen
import com.example.presentation.journal.JournalViewModel
import com.example.presentation.holdings.HoldingsDialog
import com.example.presentation.holdings.HoldingsViewModel
import com.example.presentation.navigation.OlroScreen
import com.example.presentation.news.NewsScreen
import com.example.presentation.news.NewsViewModel
import com.example.presentation.pocketoption.PocketOptionScreen
import com.example.presentation.pocketoption.PocketOptionViewModel
import com.example.presentation.settings.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TerminalBackground
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceBorder
import com.example.ui.theme.TerminalSurfaceElevated
import com.example.ui.theme.TerminalTextMuted
import com.example.ui.theme.TerminalTextPrimary
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as OlroApplication
        val container = app.container

        setContent {
            MyApplicationTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                var currentScreen by remember { mutableStateOf(OlroScreen.DASHBOARD) }
                var showAlertsDialog by remember { mutableStateOf(false) }
                var showHoldingsDialog by remember { mutableStateOf(false) }

                // Create ViewModels backed by OlroContainer
                val dashboardViewModel: DashboardViewModel = viewModel {
                    DashboardViewModel(container.marketDataManager)
                }

                val chartViewModel: ChartViewModel = viewModel {
                    ChartViewModel(container.marketDataManager)
                }

                val pocketOptionViewModel: PocketOptionViewModel = viewModel {
                    PocketOptionViewModel(container.pocketOptionRepository)
                }

                val alertsViewModel: AlertsViewModel = viewModel {
                    AlertsViewModel(container.alertRepository, container.alertManager)
                }

                val holdingsViewModel: HoldingsViewModel = viewModel {
                    HoldingsViewModel(container.holdingRepository, container.marketDataManager)
                }

                val newsViewModel: NewsViewModel = viewModel {
                    NewsViewModel(container.newsRepository)
                }

                val journalViewModel: JournalViewModel = viewModel {
                    JournalViewModel(container.journalRepository, container.geminiTradeExplainer)
                }

                // Listen for alert triggers
                LaunchedEffect(Unit) {
                    container.marketDataManager.alertTriggeredEvent.collectLatest { triggeredAlert ->
                        snackbarHostState.showSnackbar(
                            "ALERT TRIGGERED: ${triggeredAlert.symbol} ${triggeredAlert.condition.name} ${String.format(Locale.US, "%.2f", triggeredAlert.targetValue)}"
                        )
                    }
                }

                // Handle system back button
                if (currentScreen != OlroScreen.DASHBOARD) {
                    BackHandler {
                        currentScreen = OlroScreen.DASHBOARD
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = TerminalBackground,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        NavigationBar(
                            containerColor = TerminalSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, TerminalSurfaceBorder)
                        ) {
                            OlroScreen.values().forEach { screen ->
                                val isSelected = currentScreen == screen
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { currentScreen = screen },
                                    icon = {
                                        Icon(
                                            imageVector = screen.icon,
                                            contentDescription = screen.title
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = screen.title,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = TerminalCyan,
                                        selectedTextColor = TerminalCyan,
                                        indicatorColor = TerminalSurfaceElevated,
                                        unselectedIconColor = TerminalTextMuted,
                                        unselectedTextColor = TerminalTextMuted
                                    ),
                                    modifier = Modifier.testTag("nav_item_${screen.route}")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            OlroScreen.DASHBOARD -> DashboardScreen(
                                viewModel = dashboardViewModel,
                                onNavigateToChart = { symbol ->
                                    chartViewModel.selectSymbol(symbol)
                                    currentScreen = OlroScreen.CHART
                                },
                                onOpenAlerts = { showAlertsDialog = true },
                                onOpenHoldings = { showHoldingsDialog = true }
                            )

                            OlroScreen.CHART -> ChartScreen(
                                viewModel = chartViewModel,
                                onBack = { currentScreen = OlroScreen.DASHBOARD }
                            )

                            OlroScreen.JOURNAL -> JournalScreen(
                                viewModel = journalViewModel
                            )

                            OlroScreen.NEWS -> NewsScreen(
                                viewModel = newsViewModel
                            )

                            OlroScreen.POCKET_OPTION -> PocketOptionScreen(
                                viewModel = pocketOptionViewModel
                            )

                            OlroScreen.SETTINGS -> SettingsScreen()
                        }
                    }

                    if (showAlertsDialog) {
                        val sym = container.marketDataManager.selectedSymbol.value
                        AlertsDialog(
                            currentSymbol = sym,
                            viewModel = alertsViewModel,
                            onDismiss = { showAlertsDialog = false }
                        )
                    }

                    if (showHoldingsDialog) {
                        HoldingsDialog(
                            viewModel = holdingsViewModel,
                            onDismiss = { showHoldingsDialog = false }
                        )
                    }
                }
            }
        }
    }
}
