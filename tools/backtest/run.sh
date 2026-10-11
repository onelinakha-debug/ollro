#!/usr/bin/env bash
# Compiles the app's pure-Kotlin domain code (indicators, signal engine, backtester) with the CLI
# and runs it. Needs only a JDK 11+ and `kotlinc` (https://kotlinlang.org/docs/command-line.html).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SRC="$ROOT/app/src/main/java/com/example/domain"
OUT="${TMPDIR:-/tmp}/olro-backtest"
mkdir -p "$OUT"

kotlinc -nowarn -include-runtime -d "$OUT/backtest.jar" \
  "$SRC/model/Candle.kt" \
  "$SRC/model/MarketSignal.kt" \
  "$SRC/model/TechnicalIndicators.kt" \
  "$SRC/indicators/IndicatorCalculator.kt" \
  "$SRC/engine/SignalAggregator.kt" \
  "$SRC/backtest/BacktestEngine.kt" \
  "$ROOT/tools/backtest/BacktestCli.kt"

java -jar "$OUT/backtest.jar" "$@"
