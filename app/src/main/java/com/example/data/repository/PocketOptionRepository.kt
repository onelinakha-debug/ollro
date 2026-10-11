package com.example.data.repository

import com.example.data.remote.pocketoption.OptionTick
import com.example.data.remote.pocketoption.PocketOptionClient
import com.example.data.remote.pocketoption.PocketOptionSession
import com.example.data.remote.pocketoption.SessionManager
import com.example.domain.model.BinaryOptionAsset
import com.example.domain.model.MarketConnectionStatus
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class PocketOptionRepository(
    private val client: PocketOptionClient,
    private val sessionManager: SessionManager
) {
    val connectionStatus: StateFlow<MarketConnectionStatus> = client.connectionStatus
    val isSimulationActive: StateFlow<Boolean> = client.isSimulationActive
    val sessionState: StateFlow<PocketOptionSession> = sessionManager.sessionState
    val assets: StateFlow<List<BinaryOptionAsset>> = client.assetsFlow
    val tickStream: SharedFlow<OptionTick> = client.tickFlow

    fun connect() {
        client.connect()
    }

    fun disconnect() {
        client.disconnect()
    }

    fun selectAsset(assetId: String) {
        client.selectAsset(assetId)
    }

    fun saveCredentials(ssid: String, uid: String, isDemo: Boolean) {
        sessionManager.saveCredentials(ssid, uid, isDemo)
        client.connect()
    }

    fun setDemoMode(isDemo: Boolean) {
        sessionManager.setDemoMode(isDemo)
        client.connect()
    }

    fun clearSession() {
        sessionManager.clearSession()
        client.connect()
    }
}
