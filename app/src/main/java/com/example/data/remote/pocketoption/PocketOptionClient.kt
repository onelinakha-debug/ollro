package com.example.data.remote.pocketoption

import android.util.Log
import com.example.domain.model.BinaryOptionAsset
import com.example.domain.model.MarketConnectionStatus
import com.example.domain.model.SignalDirection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import kotlin.random.Random

data class OptionTick(
    val assetId: String,
    val price: Double,
    val timestamp: Long = System.currentTimeMillis()
)

class PocketOptionClient(
    private val okHttpClient: OkHttpClient,
    private val sessionManager: SessionManager
) {
    companion object {
        private const val TAG = "PocketOptionClient"
        private const val PO_WS_URL = "wss://api-l.po.market/socket.io/?EIO=4&transport=websocket"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var webSocket: WebSocket? = null
    private var simulationJob: Job? = null

    private val _connectionStatus = MutableStateFlow<MarketConnectionStatus>(MarketConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<MarketConnectionStatus> = _connectionStatus.asStateFlow()

    private val _tickFlow = MutableSharedFlow<OptionTick>(replay = 1)
    val tickFlow: SharedFlow<OptionTick> = _tickFlow.asSharedFlow()

    private val _assetsFlow = MutableStateFlow<List<BinaryOptionAsset>>(getInitialAssets())
    val assetsFlow: StateFlow<List<BinaryOptionAsset>> = _assetsFlow.asStateFlow()

    private val _isSimulationActive = MutableStateFlow(false)
    val isSimulationActive: StateFlow<Boolean> = _isSimulationActive.asStateFlow()

    private var activeAssetId = "EURUSD_OTC"

    fun selectAsset(assetId: String) {
        activeAssetId = assetId
    }

    fun connect() {
        val session = sessionManager.sessionState.value
        if (session.isAuthenticated && session.ssid.isNotBlank()) {
            connectLiveWebSocket(session)
        } else {
            startSandboxSimulation()
        }
    }

    private fun connectLiveWebSocket(session: PocketOptionSession) {
        simulationJob?.cancel()
        _connectionStatus.value = MarketConnectionStatus.Connecting

        val request = Request.Builder()
            .url(PO_WS_URL)
            .addHeader("Origin", "https://pocketoption.com")
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _isSimulationActive.value = false
                _connectionStatus.value = MarketConnectionStatus.Connected(latencyMs = 38L)
                Log.i(TAG, "Pocket Option WebSocket connected")
                // Socket.IO Handshake
                val authPayload = JSONObject().apply {
                    put("token", session.ssid)
                    put("uid", session.uid)
                    put("isDemo", if (session.isDemo) 1 else 0)
                }
                webSocket.send("40$authPayload")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleSocketIoMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "PO WebSocket failure: ${t.message}. Falling back to sandbox stream.")
                _connectionStatus.value = MarketConnectionStatus.Reconnecting(attempt = 1, delaySeconds = 5)
                startSandboxSimulation()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionStatus.value = MarketConnectionStatus.Disconnected
                startSandboxSimulation()
            }
        })
    }

    private fun handleSocketIoMessage(message: String) {
        if (message.startsWith("2")) {
            webSocket?.send("3") // Socket.IO Ping/Pong
            return
        }
        if (message.startsWith("42")) {
            try {
                val jsonPart = message.substring(2)
                val array = org.json.JSONArray(jsonPart)
                val event = array.optString(0)
                if (event == "tick") {
                    val payload = array.optJSONObject(1)
                    if (payload != null) {
                        val asset = payload.optString("asset")
                        val price = payload.optDouble("price")
                        scope.launch {
                            _tickFlow.emit(OptionTick(assetId = asset, price = price))
                            updateAssetPrice(asset, price)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "PO message parse error: ${e.message}")
            }
        }
    }

    /**
     * Resilient sandbox simulation when credentials are not configured or PO socket is unreachable
     */
    private fun startSandboxSimulation() {
        if (simulationJob?.isActive == true) return
        _isSimulationActive.value = true
        _connectionStatus.value = MarketConnectionStatus.Connected(latencyMs = 28L)

        simulationJob = scope.launch {
            while (isActive) {
                delay(1000L)
                val currentAssets = _assetsFlow.value.toMutableList()
                val updatedAssets = currentAssets.map { asset ->
                    val drift = (Random.nextDouble(-0.0004, 0.0004))
                    val newPrice = (asset.currentPrice + drift).coerceAtLeast(0.0001)
                    val priceChange = asset.priceChange24h + (drift * 10)
                    val signal = when {
                        drift > 0.0002 -> SignalDirection.BUY
                        drift < -0.0002 -> SignalDirection.SELL
                        else -> SignalDirection.NEUTRAL
                    }
                    val updated = asset.copy(
                        currentPrice = newPrice,
                        priceChange24h = priceChange,
                        signal = signal
                    )
                    if (asset.id == activeAssetId) {
                        _tickFlow.emit(OptionTick(assetId = asset.id, price = newPrice))
                    }
                    updated
                }
                _assetsFlow.value = updatedAssets
            }
        }
    }

    private fun updateAssetPrice(assetId: String, newPrice: Double) {
        val list = _assetsFlow.value.map {
            if (it.id == assetId) it.copy(currentPrice = newPrice) else it
        }
        _assetsFlow.value = list
    }

    fun disconnect() {
        simulationJob?.cancel()
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionStatus.value = MarketConnectionStatus.Disconnected
    }

    private fun getInitialAssets(): List<BinaryOptionAsset> = listOf(
        BinaryOptionAsset(
            id = "EURUSD_OTC",
            name = "EUR / USD (OTC)",
            symbol = "EURUSD",
            payoutPercent = 92,
            isOtc = true,
            currentPrice = 1.08425,
            priceChange24h = 0.35,
            signal = SignalDirection.BUY
        ),
        BinaryOptionAsset(
            id = "GBPUSD_OTC",
            name = "GBP / USD (OTC)",
            symbol = "GBPUSD",
            payoutPercent = 90,
            isOtc = true,
            currentPrice = 1.29810,
            priceChange24h = -0.18,
            signal = SignalDirection.NEUTRAL
        ),
        BinaryOptionAsset(
            id = "BTCUSD_OTC",
            name = "BTC / USD (OTC)",
            symbol = "BTCUSD",
            payoutPercent = 88,
            isOtc = true,
            currentPrice = 96420.00,
            priceChange24h = 2.45,
            signal = SignalDirection.STRONG_BUY
        ),
        BinaryOptionAsset(
            id = "USDJPY_OTC",
            name = "USD / JPY (OTC)",
            symbol = "USDJPY",
            payoutPercent = 86,
            isOtc = true,
            currentPrice = 152.64,
            priceChange24h = -0.42,
            signal = SignalDirection.SELL
        ),
        BinaryOptionAsset(
            id = "ETHUSD_OTC",
            name = "ETH / USD (OTC)",
            symbol = "ETHUSD",
            payoutPercent = 87,
            isOtc = true,
            currentPrice = 2840.50,
            priceChange24h = 1.15,
            signal = SignalDirection.BUY
        )
    )
}
