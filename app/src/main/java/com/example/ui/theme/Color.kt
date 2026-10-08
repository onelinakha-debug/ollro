package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * OLRO TRADING ANALYSIS APP COLOR PALETTE
 * Balanced according to the Golden Ratio (61.8% Foundation / 38.2% Typography / 10% Focused Accents)
 *
 * 1. Deep Forest    (#0F3D3A) - 61.8% Base Canvas & Surface Structure
 * 2. Warm Sand      (#F2E6D6) - 38.2% High-Legibility Typography & Neutral Accents
 * 3. Golden Hour    (#F4B13B) - Primary Brand Focus, Interactive Affordances & Bullish Signals
 * 4. Terracotta Clay (#E0745E) - Bearish Direction, Sell Triggers & Risk Metrics
 */

// --- Direct Palette Constants ---
val OlroDeepForest = Color(0xFF0F3D3A)
val OlroGoldenHour = Color(0xFFF4B13B)
val OlroTerracottaClay = Color(0xFFE0745E)
val OlroWarmSand = Color(0xFFF2E6D6)

// --- 61.8% Foundation: Deep Forest Tier ---
val TerminalBackground = Color(0xFF0A2422)        // Ultra deep forest dark terminal canvas
val TerminalSurface = Color(0xFF0F3D3A)           // Deep Forest primary surface
val TerminalSurfaceElevated = Color(0xFF16524E)   // Elevated forest cards, chips, dialogs
val TerminalSurfaceBorder = Color(0xFF22756F)     // Refined forest container border
val TerminalGridLine = Color(0xFF134542)          // Subtle chart gridlines

// --- 38.2% Content: Warm Sand Tier ---
val TerminalTextPrimary = Color(0xFFF2E6D6)       // Warm Sand high-contrast primary text
val TerminalTextSecondary = Color(0xFFD6C8B4)     // Warm Sand secondary text & metadata
val TerminalTextMuted = Color(0xFFA89984)         // Warm Sand muted captions, timestamps
val TerminalCrosshair = Color(0xFFD6C8B4)         // Warm Sand crosshair lines

// --- 10% Strategic Focus: Golden Hour & Terracotta Clay ---
// Primary Brand & Interactive Highlights (Golden Hour)
val TerminalCyan = Color(0xFFF4B13B)              // Golden Hour primary interactive accent
val TerminalCyanDark = Color(0xFFD69424)          // Golden Hour shaded
val TerminalWarningAmber = Color(0xFFF4B13B)      // Golden Hour warning / attention
val TerminalWarningAmberBg = Color(0x24F4B13B)

// Directional Bullish Indicator (Luminous Botanical Emerald in harmony with Deep Forest & Golden Hour)
val TerminalBullishGreen = Color(0xFF38B293)
val TerminalBullishGreenDim = Color(0xFF299478)
val TerminalBullishGreenBg = Color(0x2438B293)

// Directional Bearish Indicator (Terracotta Clay #E0745E)
val TerminalBearishRed = Color(0xFFE0745E)        // Terracotta Clay bearish price drops & sells
val TerminalBearishRedDim = Color(0xFFC75A45)     // Terracotta Clay shaded
val TerminalBearishRedBg = Color(0x24E0745E)      // Terracotta Clay translucent badge

// Material 3 Fallbacks
val Purple80 = TerminalCyan
val PurpleGrey80 = TerminalSurfaceElevated
val Pink80 = TerminalBullishGreen

val Purple40 = TerminalCyanDark
val PurpleGrey40 = TerminalSurface
val Pink40 = TerminalBearishRed
