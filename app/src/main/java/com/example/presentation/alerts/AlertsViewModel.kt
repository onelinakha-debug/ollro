package com.example.presentation.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AlertRepository
import com.example.domain.manager.AlertManager
import com.example.domain.model.AlertCondition
import com.example.domain.model.PriceAlert
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlertsViewModel(
    private val alertRepository: AlertRepository,
    private val alertManager: AlertManager? = null
) : ViewModel() {

    val alerts: StateFlow<List<PriceAlert>> = alertRepository.alertsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val isPollingActive: StateFlow<Boolean> = alertManager?.isPollingActiveFlow()
        ?.stateIn(viewModelScope, SharingStarted.Lazily, true)
        ?: MutableStateFlow(true)

    fun addAlert(symbol: String, condition: AlertCondition, targetValue: Double) {
        viewModelScope.launch {
            alertRepository.addAlert(symbol, condition, targetValue)
            alertManager?.triggerImmediateCheck()
        }
    }

    fun toggleAlert(alert: PriceAlert) {
        viewModelScope.launch {
            alertRepository.toggleAlert(alert)
        }
    }

    fun deleteAlert(alertId: Long) {
        viewModelScope.launch {
            alertRepository.deleteAlert(alertId)
        }
    }

    fun triggerBackgroundCheck() {
        alertManager?.triggerImmediateCheck()
    }

    fun toggleBackgroundPolling(enabled: Boolean) {
        if (enabled) {
            alertManager?.startBackgroundPolling(15)
        } else {
            alertManager?.stopBackgroundPolling()
        }
    }
}
