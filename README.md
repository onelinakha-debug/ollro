# Olro — Professional Cryptocurrency & Market Surveillance Terminal

Olro is a production-grade, real-time market analysis and surveillance terminal engineered for active traders. Built natively for Android using 100% Kotlin, Jetpack Compose, MVVM Clean Architecture, and reactive Coroutine Flows.

---

## 🏛️ System Architecture

Olro strictly adheres to Clean Architecture with unidirectional data flow (MVI/MVVM):

```
┌──────────────────────────────────────────────────────────────┐
│                    Presentation Layer                        │
│   DashboardScreen  │  ChartScreen  │  PocketOptionScreen    │
│            Jetpack Compose Material 3 UI                     │
└──────────────────────────────▲───────────────────────────────┘
                               │ StateFlow / Actions
┌──────────────────────────────┴───────────────────────────────┐
│                       Domain Layer                           │
│   • IndicatorCalculator (RSI, MACD, BB, EMA, ATR, RVOL)      │
│   • SignalAggregator (Multi-indicator weighted consensus)    │
│   • MarketDataManager (WebSocket owner & stream arbiter)     │
└──────────────────────────────▲───────────────────────────────┘
                               │ Repositories
┌──────────────────────────────┴───────────────────────────────┐
│                        Data Layer                            │
│   • BinanceRepository (REST + wss://stream.binance.com)     │
│   • PocketOptionRepository (Socket.IO + Sandbox Stream)      │
│   • Room Database (Historical OHLCV Candle & Alert cache)    │
│   • KeyStoreManager (AES-256-GCM hardware-backed encryption) │
└──────────────────────────────────────────────────────────────┘
```

---

## ⚡ Real-Time Data Ingestion & Reliability

### 1. Binance Integration
- **REST Endpoints**: `api.binance.com` for historical klines and 24h ticker baseline.
- **WebSocket Streams**: Multiplexed stream (`<symbol>@kline_1m/<symbol>@ticker/<symbol>@depth20@100ms`).
- **Heartbeat & Resiliency**:
  - RFC 6455 25-second WebSocket ping frame.
  - Exponential backoff with randomized jitter on disconnect.
  - Proactive 23-hour connection rotation to gracefully bypass Binance's 24-hour socket termination.
  - Stale data detection watchdog: flags data as stale if incoming stream drops for > 10 seconds.
  - Room database offline fallback.

### 2. Pocket Option Integration
- **Protocol**: Unofficial session-based Socket.IO WebSocket stream.
- **Data Segregation**: Completely isolated repository and domain layer to prevent binary option OTC ticks from contaminating crypto spot/perp depth order books.
- **Security Warning**: Pocket Option does not offer an official public developer API. Credentials (`SSID` and `UID`) are strictly encrypted at rest via the Android KeyStore using AES-256-GCM.
- **Sandbox Fallback**: If no session token is configured, an active OTC tick simulation engine runs automatically so you can evaluate binary options payout timing and signals out of the box.

---

## 🔐 Credentials & Secrets Setup

### 1. Binance API Keys
To connect your personal Binance account:
1. Log in to [Binance](https://www.binance.com/) and navigate to **API Management**.
2. Click **Create API**, select system generated key.
3. Keep **Read Only** permission enabled (Never enable Withdrawal permissions).
4. In AI Studio, configure secrets via the **Secrets panel** or inject keys via `.env` / `BuildConfig`.

### 2. Pocket Option SSID Extraction (Security Disclaimer)
> ⚠️ **CRITICAL SECURITY NOTICE**: Never share your Pocket Option session cookies or tokens with third parties. Extract tokens only on trusted devices.

To obtain your session `SSID`:
1. Open Chrome/Firefox on desktop and log into [Pocket Option](https://pocketoption.com/).
2. Press `F12` to open Developer Tools and select the **Application** (or **Storage**) tab.
3. Under **Cookies** -> `https://pocketoption.com`, locate the cookie named `PHPSESSID` or `SSID`.
4. Copy the cookie value.
5. In Olro, open the **Pocket Option** tab, tap the **Key** icon, and paste the token. It is immediately encrypted using AES-256-GCM.

---

## 📈 Technical Indicators & Signal Aggregator

Olro computes indicators purely in background Coroutines:
- **RSI (14)**: Wilder's smoothing with overbought (70) and oversold (30) levels.
- **MACD (12, 26, 9)**: Fast EMA (12), Slow EMA (26), Signal Line (9), and momentum histogram.
- **Bollinger Bands (20, 2.0)**: Upper, Middle, Lower bands, Bandwidth, and %B oscillator.
- **EMA Alignment**: EMA 9, 21, 50, 200 trend alignment.
- **Supertrend (10, 3.0)**: Trailing stop support/resistance.
- **ATR (14)**: Average True Range volatility measure.
- **RVOL (24H)**: Relative volume ratio compared against average volume.

**Signal Aggregator Weights:**
- RSI Momentum: 20%
- MACD Expansion: 25%
- EMA Trend Alignment: 20%
- Bollinger Bands %B: 15%
- Supertrend Trailing: 10%
- Relative Volume (RVOL): 10%

Output states: `STRONG BUY`, `BUY`, `NEUTRAL`, `SELL`, `STRONG SELL`.

---

## ✅ Validating the signals (backtest)

The signal engine is a rule-based scorer, not a proven predictor. Before trusting it, replay history through the
**same** indicator and signal code the app runs:

```bash
./tools/backtest/run.sh --symbols BTCUSDT,ETHUSDT --intervals 5m,15m --horizons 3,5,10 --candles 5000
./tools/backtest/run.sh --csv data.csv --symbols BTCUSDT      # offline: timestamp,open,high,low,close,volume
```

Needs a JDK and `kotlinc`. For each signal it enters at the next candle's open, exits after N candles, and reports
win rate with a 95% confidence interval, average return net of fees (`--cost`), profit factor, max drawdown, the
first-half vs second-half split, the market's base rate of "up" moves, and the binary-option break-even win rate
for the given `--payout`. Read **"NO statistically clear edge"** literally.

Signal rules enforced in the app: signals use **closed candles only**, and are **disabled when the data is simulated**
(Binance blocked / no history). The Pocket Option sandbox feed never produces BUY/SELL.

## 🧪 Testing

Run JVM unit and indicator tests:
```bash
gradle :app:testDebugUnitTest
```
Unit test coverage includes:
- `IndicatorCalculatorTest.kt`: Mathematical correctness of RSI, MACD, EMA, Bollinger Bands, and Signal weights against known trends.
- `WebSocketConnectionTest.kt`: Exponential backoff algorithm bounds and status transitions.
- `IndicatorFixesTest.kt`: EMA seeding, MACD crossover-only-on-flip, Supertrend trend/flip behaviour.
- `BacktestEngineTest.kt`: no look-ahead, entry/exit timing, non-overlap, costs, win-rate/drawdown maths.
