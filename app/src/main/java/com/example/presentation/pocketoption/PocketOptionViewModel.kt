package com.example.presentation.pocketoption

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.pocketoption.OptionTick
import com.example.data.remote.pocketoption.PocketOptionSession
import com.example.data.repository.PocketOptionRepository
import com.example.domain.model.BinaryOptionAsset
import com.example.domain.model.MarketConnectionStatus
import com.example.domain.model.SignalDirection
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PocketOptionViewModel(
    private val repository: PocketOptionRepository
) : ViewModel() {

    val connectionStatus: StateFlow<MarketConnectionStatus> = repository.connectionStatus
    val sessionState: StateFlow<PocketOptionSession> = repository.sessionState
    val assets: StateFlow<List<BinaryOptionAsset>> = repository.assets

    private val _selectedAsset = MutableStateFlow<BinaryOptionAsset?>(null)
    val selectedAsset: StateFlow<BinaryOptionAsset?> = _selectedAsset.asStateFlow()

    private val _selectedExpirySeconds = MutableStateFlow(60)
    val selectedExpirySeconds: StateFlow<Int> = _selectedExpirySeconds.asStateFlow()

    private val _countdownRemaining = MutableStateFlow(60)
    val countdownRemaining: StateFlow<Int> = _countdownRemaining.asStateFlow()

    private val _recentTicks = MutableStateFlow<List<Double>>(emptyList())
    val recentTicks: StateFlow<List<Double>> = _recentTicks.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private var timerJob: Job? = null

    init {
        repository.connect()
        observeTicks()
        viewModelScope.launch {
            assets.collectLatest { list ->
                if (_selectedAsset.value == null && list.isNotEmpty()) {
                    _selectedAsset.value = list.first()
                } else if (_selectedAsset.value != null) {
                    val updated = list.find { it.id == _selectedAsset.value?.id }
                    if (updated != null) _selectedAsset.value = updated
                }
            }
        }
    }

    private fun observeTicks() {
        viewModelScope.launch {
            repository.tickStream.collectLatest { tick ->
                val current = _recentTicks.value.toMutableList()
                current.add(tick.price)
                if (current.size > 50) current.removeAt(0)
                _recentTicks.value = current
            }
        }
    }

    fun selectAsset(asset: BinaryOptionAsset) {
        _selectedAsset.value = asset
        repository.selectAsset(asset.id)
        _recentTicks.value = emptyList()
    }

    fun setExpiry(seconds: Int) {
        _selectedExpirySeconds.value = seconds
        _countdownRemaining.value = seconds
    }

    fun startExpiryCountdown() {
        timerJob?.cancel()
        _isTimerRunning.value = true
        _countdownRemaining.value = _selectedExpirySeconds.value

        timerJob = viewModelScope.launch {
            while (isActive && _countdownRemaining.value > 0) {
                delay(1000L)
                _countdownRemaining.value -= 1
            }
            _isTimerRunning.value = false
        }
    }

    fun saveSessionCredentials(ssid: String, uid: String, isDemo: Boolean) {
        repository.saveCredentials(ssid, uid, isDemo)
    }

    fun toggleDemoMode(isDemo: Boolean) {
        repository.setDemoMode(isDemo)
    }

    fun clearSession() {
        repository.clearSession()
    }
}
