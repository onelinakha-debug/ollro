package com.example.domain.backtest

import com.example.domain.engine.SignalAggregator
import com.example.domain.indicators.IndicatorCalculator
import com.example.domain.model.Candle
import com.example.domain.model.SignalDirection
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Settings for a backtest run.
 *
 * Timing convention (no look-ahead): the signal is computed from candles up to and including the
 * CLOSE of bar `i`. The trade is entered at the OPEN of bar `i + 1` and exited at the CLOSE of
 * bar `i + horizonCandles`, so it is held for exactly [horizonCandles] candles.
 */
data class BacktestConfig(
    /** How many candles each trade is held. For a binary option this is the expiry in candles. */
    val horizonCandles: Int = 5,
    /** Candles of history the signal engine sees. Mirrors the live app (MarketDataManager.HISTORY_CANDLE_LIMIT). */
    val lookbackCandles: Int = 500,
    /** First bar index at which a signal may be evaluated; the indicators need this much warm-up. */
    val warmupCandles: Int = 60,
    /** Directions that open a trade. NEUTRAL is never traded. */
    val tradeDirections: Set<SignalDirection> = setOf(
        SignalDirection.STRONG_BUY, SignalDirection.BUY, SignalDirection.SELL, SignalDirection.STRONG_SELL
    ),
    /**
     * When true, a new trade is only opened after the previous one has exited. Consecutive signals
     * on neighbouring candles describe almost the same price move, so counting each one as a separate
     * trade inflates the sample size and makes the results look far more certain than they are.
     */
    val nonOverlapping: Boolean = true,
    /** Round-trip trading cost (fees + slippage) in percent of notional, subtracted from every return. */
    val roundTripCostPct: Double = 0.0,
    /** Binary option payout, e.g. 90.0 for 90%. Used for the binary-option expected value. */
    val binaryPayoutPercent: Double = 90.0
) {
    init {
        require(horizonCandles >= 1) { "horizonCandles must be >= 1" }
        require(warmupCandles >= 2) { "warmupCandles must be >= 2" }
        require(lookbackCandles >= warmupCandles) { "lookbackCandles must be >= warmupCandles" }
        require(binaryPayoutPercent > 0.0) { "binaryPayoutPercent must be > 0" }
    }
}

/** One simulated trade produced by a signal. */
data class BacktestTrade(
    val signalIndex: Int,
    val signalTimestamp: Long,
    val direction: SignalDirection,
    val compositeScore: Int,
    val entryPrice: Double,
    val exitPrice: Double,
    /** Return in percent in the direction of the trade, before costs. */
    val grossReturnPct: Double,
    /** [grossReturnPct] minus the configured round-trip cost. */
    val netReturnPct: Double
) {
    val isWin: Boolean get() = grossReturnPct > 0.0
    val isLoss: Boolean get() = grossReturnPct < 0.0
}

/** Aggregate statistics for a set of trades. */
data class TradeStats(
    val trades: Int,
    val wins: Int,
    val losses: Int,
    /** Trades that exited at exactly the entry price (rare; no win, no loss). */
    val flats: Int,
    /** wins / (wins + losses), in 0..1. Null if there are no decided trades. */
    val winRate: Double?,
    /** 95% Wilson confidence interval for [winRate]. */
    val winRateCi95: ClosedFloatingPointRange<Double>?,
    /** Mean net return per trade in percent (expectancy). */
    val avgNetReturnPct: Double?,
    val totalNetReturnPct: Double,
    /** Gross profit / gross loss on net returns. Null if undefined (no trades); infinity if no losing trades. */
    val profitFactor: Double?,
    /** Largest peak-to-trough fall of the cumulative net return, in percentage points. */
    val maxDrawdownPct: Double
) {
    companion object {
        val EMPTY = TradeStats(0, 0, 0, 0, null, null, null, 0.0, null, 0.0)

        fun of(trades: List<BacktestTrade>): TradeStats {
            if (trades.isEmpty()) return EMPTY
            val wins = trades.count { it.isWin }
            val losses = trades.count { it.isLoss }
            val decided = wins + losses
            val winRate = if (decided > 0) wins.toDouble() / decided else null

            var gains = 0.0
            var lossesSum = 0.0
            var equity = 0.0
            var peak = 0.0
            var maxDd = 0.0
            for (t in trades) {
                if (t.netReturnPct > 0) gains += t.netReturnPct else lossesSum += -t.netReturnPct
                equity += t.netReturnPct
                peak = max(peak, equity)
                maxDd = max(maxDd, peak - equity)
            }
            val total = trades.sumOf { it.netReturnPct }
            return TradeStats(
                trades = trades.size,
                wins = wins,
                losses = losses,
                flats = trades.size - decided,
                winRate = winRate,
                winRateCi95 = winRate?.let { wilsonInterval(wins, decided) },
                avgNetReturnPct = total / trades.size,
                totalNetReturnPct = total,
                profitFactor = when {
                    lossesSum > 0.0 -> gains / lossesSum
                    gains > 0.0 -> Double.POSITIVE_INFINITY
                    else -> null
                },
                maxDrawdownPct = maxDd
            )
        }
    }
}

/** What a binary option on these signals would have paid. */
data class BinaryOptionStats(
    val payoutPercent: Double,
    /** Win rate needed just to break even at this payout: 1 / (1 + payout). */
    val breakEvenWinRate: Double,
    /** Average profit per 1.00 staked (e.g. 0.05 = +5%). Null if there are no decided trades. */
    val expectedValuePerStake: Double?,
    /** True only if the whole 95% confidence interval of the win rate is above break-even. */
    val edgeIsStatisticallyClear: Boolean
)

data class BacktestReport(
    val symbol: String,
    val config: BacktestConfig,
    val candlesTested: Int,
    /** Bars at which a signal was evaluated (whether or not a trade was opened). */
    val barsEvaluated: Int,
    val trades: List<BacktestTrade>,
    val overall: TradeStats,
    val byDirection: Map<SignalDirection, TradeStats>,
    /** First vs second half of the trades in time: a cheap check that results are not one lucky stretch. */
    val firstHalf: TradeStats,
    val secondHalf: TradeStats,
    /**
     * Share of ALL evaluated bars where price was simply higher after the horizon. A BUY-heavy signal
     * set can look good just because the market rose; compare the win rate to this number.
     */
    val baselineUpRate: Double?,
    val binary: BinaryOptionStats
) {
    fun toText(): String {
        val sb = StringBuilder()
        sb.appendLine("=== Backtest: $symbol | horizon ${config.horizonCandles} candles | cost ${fmt(config.roundTripCostPct)}% round trip ===")
        sb.appendLine("Candles: $candlesTested | bars evaluated: $barsEvaluated | trades: ${overall.trades}" +
            if (config.nonOverlapping) " (non-overlapping)" else " (overlapping: results overstated)")
        sb.appendLine("Baseline: price higher after ${config.horizonCandles} candles in ${pct(baselineUpRate)} of bars")
        sb.appendLine(row("", "trades", "win rate", "95% CI", "avg ret %", "PF", "max DD %"))
        sb.appendLine(statsRow("ALL", overall))
        for (d in listOf(SignalDirection.STRONG_BUY, SignalDirection.BUY, SignalDirection.SELL, SignalDirection.STRONG_SELL)) {
            byDirection[d]?.let { sb.appendLine(statsRow(d.name, it)) }
        }
        sb.appendLine(statsRow("1st half", firstHalf))
        sb.appendLine(statsRow("2nd half", secondHalf))
        sb.appendLine(
            "Binary @ ${fmt(binary.payoutPercent)}% payout: break-even win rate ${pct(binary.breakEvenWinRate)}, " +
                "EV per stake ${binary.expectedValuePerStake?.let { fmt(it * 100) + "%" } ?: "n/a"}, " +
                if (binary.edgeIsStatisticallyClear) "edge statistically clear" else "NO statistically clear edge"
        )
        return sb.toString()
    }

    private fun statsRow(label: String, s: TradeStats) = row(
        label,
        s.trades.toString(),
        pct(s.winRate),
        s.winRateCi95?.let { "${pct(it.start)}-${pct(it.endInclusive)}" } ?: "n/a",
        s.avgNetReturnPct?.let { fmt(it, 3) } ?: "n/a",
        s.profitFactor?.let { if (it.isInfinite()) "inf" else fmt(it) } ?: "n/a",
        fmt(s.maxDrawdownPct)
    )

    private fun row(a: String, b: String, c: String, d: String, e: String, f: String, g: String) =
        String.format(Locale.US, "%-12s %7s %9s %15s %10s %7s %9s", a, b, c, d, e, f, g)

    private fun pct(v: Double?) = v?.let { String.format(Locale.US, "%.1f%%", it * 100) } ?: "n/a"
    private fun fmt(v: Double, digits: Int = 2) = String.format(Locale.US, "%.${digits}f", v)
}

/**
 * Replays history through the SAME [IndicatorCalculator] and [SignalAggregator] the live app uses
 * and records what happened after every signal. This is the only honest way to know whether the
 * signals carry any information.
 */
object BacktestEngine {

    fun run(symbol: String, candles: List<Candle>, config: BacktestConfig = BacktestConfig()): BacktestReport {
        // Only closed candles are ever analysed, exactly as in the live app.
        val data = candles.filter { it.isClosed }.sortedBy { it.timestamp }

        val trades = ArrayList<BacktestTrade>()
        var barsEvaluated = 0
        var upMoves = 0
        var nextAllowedIndex = config.warmupCandles - 1

        val firstSignalIndex = config.warmupCandles - 1
        val lastSignalIndex = data.size - 1 - config.horizonCandles // need entry bar and exit bar to exist

        var i = firstSignalIndex
        while (i <= lastSignalIndex) {
            barsEvaluated++

            val entry = data[i + 1].open
            val exit = data[i + config.horizonCandles].close
            if (exit > entry) upMoves++

            if (i >= nextAllowedIndex) {
                val from = max(0, i + 1 - config.lookbackCandles)
                val window = data.subList(from, i + 1)
                val indicators = IndicatorCalculator.calculateAll(window)
                val signal = SignalAggregator.aggregate(symbol, window, indicators)

                if (signal.direction in config.tradeDirections && entry > 0.0) {
                    val raw = (exit - entry) / entry * 100.0
                    val gross = if (signal.direction.isBullish) raw else -raw
                    trades.add(
                        BacktestTrade(
                            signalIndex = i,
                            signalTimestamp = data[i].timestamp,
                            direction = signal.direction,
                            compositeScore = signal.compositeScore,
                            entryPrice = entry,
                            exitPrice = exit,
                            grossReturnPct = gross,
                            netReturnPct = gross - config.roundTripCostPct
                        )
                    )
                    if (config.nonOverlapping) nextAllowedIndex = i + config.horizonCandles
                }
            }
            i++
        }

        val overall = TradeStats.of(trades)
        val half = trades.size / 2
        return BacktestReport(
            symbol = symbol,
            config = config,
            candlesTested = data.size,
            barsEvaluated = barsEvaluated,
            trades = trades,
            overall = overall,
            byDirection = trades.groupBy { it.direction }.mapValues { TradeStats.of(it.value) },
            firstHalf = TradeStats.of(trades.subList(0, half)),
            secondHalf = TradeStats.of(trades.subList(half, trades.size)),
            baselineUpRate = if (barsEvaluated > 0) upMoves.toDouble() / barsEvaluated else null,
            binary = binaryStats(overall, config.binaryPayoutPercent)
        )
    }

    /** Break-even win rate for a binary option paying [payoutPercent] on a win and losing the stake on a loss. */
    fun breakEvenWinRate(payoutPercent: Double): Double = 1.0 / (1.0 + payoutPercent / 100.0)

    private fun binaryStats(stats: TradeStats, payoutPercent: Double): BinaryOptionStats {
        val breakEven = breakEvenWinRate(payoutPercent)
        val decided = stats.wins + stats.losses
        val ev = if (decided > 0) {
            (stats.wins * (payoutPercent / 100.0) - stats.losses) / decided
        } else null
        val clear = stats.winRateCi95?.let { it.start > breakEven } ?: false
        return BinaryOptionStats(payoutPercent, breakEven, ev, clear)
    }
}

/** 95% Wilson score interval for a binomial proportion. Behaves sensibly for small samples. */
fun wilsonInterval(successes: Int, total: Int, z: Double = 1.96): ClosedFloatingPointRange<Double> {
    require(total > 0) { "total must be > 0" }
    val n = total.toDouble()
    val p = successes / n
    val z2 = z * z
    val denom = 1.0 + z2 / n
    val centre = (p + z2 / (2 * n)) / denom
    val margin = z * sqrt(p * (1 - p) / n + z2 / (4 * n * n)) / denom
    return (centre - abs(margin)).coerceAtLeast(0.0)..(centre + abs(margin)).coerceAtMost(1.0)
}
