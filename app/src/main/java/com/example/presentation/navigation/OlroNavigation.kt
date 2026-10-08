package com.example.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.ui.graphics.vector.ImageVector

enum class OlroScreen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    DASHBOARD("dashboard", "Terminal", Icons.Default.Dashboard),
    CHART("chart", "Charts", Icons.Default.CandlestickChart),
    NEWS("news", "News", Icons.Default.Newspaper),
    POCKET_OPTION("pocket_option", "Binary OTC", Icons.Default.Speed),
    SETTINGS("settings", "Security", Icons.Default.Settings)
}
