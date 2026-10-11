import com.example.domain.backtest.BacktestConfig
import com.example.domain.backtest.BacktestEngine
import com.example.domain.model.Candle
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlin.system.exitProcess

/**
 * Command-line backtester. Runs the app's real indicator + signal code over historical candles.
 *
 *   ./tools/backtest/run.sh --symbols BTCUSDT,ETHUSDT --intervals 5m,15m --horizons 3,5,10 --candles 5000
 *   ./tools/backtest/run.sh --csv data.csv --symbols BTCUSDT           (offline; columns: timestamp,open,high,low,close,volume)
 *
 * Options: --cost <round-trip % cost, default 0.2>  --payout <binary payout %, default 90>  --overlap (allow overlapping trades)
 */

private val BASES = listOf("https://api.binance.com", "https://data-api.binance.vision")

private fun usage(): Nothing {
    System.err.println("Usage: run.sh [--symbols A,B] [--intervals 5m,15m] [--horizons 3,5,10] [--candles N] [--cost PCT] [--payout PCT] [--overlap] [--csv FILE]")
    exitProcess(2)
}

fun main(args: Array<String>) {
    val opts = HashMap<String, String>()
    var overlap = false
    var i = 0
    while (i < args.size) {
        when (val a = args[i]) {
            "--overlap" -> overlap = true
            "--symbols", "--intervals", "--horizons", "--candles", "--cost", "--payout", "--csv" -> {
                if (i + 1 >= args.size) usage()
                opts[a] = args[++i]
            }
            else -> usage()
        }
        i++
    }

    val symbols = (opts["--symbols"] ?: "BTCUSDT,ETHUSDT").split(",").map { it.trim().uppercase() }
    val intervals = (opts["--intervals"] ?: "5m").split(",").map { it.trim() }
    val horizons = (opts["--horizons"] ?: "3,5,10").split(",").map { it.trim().toInt() }
    val wanted = (opts["--candles"] ?: "3000").toInt()
    val cost = (opts["--cost"] ?: "0.2").toDouble()
    val payout = (opts["--payout"] ?: "90").toDouble()

    for (symbol in symbols) {
        for (interval in intervals) {
            val candles = if (opts["--csv"] != null) loadCsv(opts["--csv"]!!) else fetchKlines(symbol, interval, wanted)
            if (candles.isEmpty()) {
                System.err.println("No candles for $symbol $interval (network blocked or bad symbol?)")
                continue
            }
            println("\n##### $symbol $interval")
            for (h in horizons) {
                val config = BacktestConfig(horizonCandles = h, roundTripCostPct = cost, binaryPayoutPercent = payout, nonOverlapping = !overlap)
                println(BacktestEngine.run("$symbol/$interval", candles, config).toText())
            }
        }
    }
}

private fun loadCsv(path: String): List<Candle> =
    File(path).readLines().filter { it.isNotBlank() && it.first().isDigit() }.map { line ->
        val p = line.split(",")
        Candle(p[0].toLong(), p[1].toDouble(), p[2].toDouble(), p[3].toDouble(), p[4].toDouble(), p[5].toDouble())
    }

/** Pages backwards through Binance's public klines endpoint (max 1000 per request). */
private fun fetchKlines(symbol: String, interval: String, total: Int): List<Candle> {
    val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build()
    val rowRegex = Regex("""\[(\d+),"([\d.eE+-]+)","([\d.eE+-]+)","([\d.eE+-]+)","([\d.eE+-]+)","([\d.eE+-]+)",(\d+)""")
    val out = ArrayList<Candle>()
    var endTime: Long? = null

    while (out.size < total) {
        val limit = minOf(1000, total - out.size)
        var body: String? = null
        for (base in BASES) {
            val url = "$base/api/v3/klines?symbol=$symbol&interval=$interval&limit=$limit" + (endTime?.let { "&endTime=$it" } ?: "")
            try {
                val res = client.send(HttpRequest.newBuilder(URI(url)).timeout(Duration.ofSeconds(30)).GET().build(), HttpResponse.BodyHandlers.ofString())
                if (res.statusCode() == 200) { body = res.body(); break }
                System.err.println("$base -> HTTP ${res.statusCode()}")
            } catch (e: Exception) {
                System.err.println("$base -> ${e.javaClass.simpleName}: ${e.message}")
            }
        }
        if (body == null) break

        val page = rowRegex.findAll(body).map { m ->
            val g = m.groupValues
            Candle(
                timestamp = g[1].toLong(), open = g[2].toDouble(), high = g[3].toDouble(), low = g[4].toDouble(),
                close = g[5].toDouble(), volume = g[6].toDouble(), closeTime = g[7].toLong(), isClosed = true
            )
        }.toList()
        if (page.isEmpty()) break
        out.addAll(0, page)
        endTime = page.first().timestamp - 1
        if (page.size < limit) break
    }
    // The newest kline may still be forming; drop it so only closed candles are tested.
    val now = System.currentTimeMillis()
    return out.distinctBy { it.timestamp }.sortedBy { it.timestamp }.filter { it.closeTime < now }
}
