package com.example.data.repository

import com.example.data.local.db.AlertDao
import com.example.data.local.db.AlertEntity
import com.example.domain.model.AlertCondition
import com.example.domain.model.PriceAlert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AlertRepository(
    private val alertDao: AlertDao
) {
    val alertsFlow: Flow<List<PriceAlert>> = alertDao.getAllAlertsFlow().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getActiveAlerts(): List<PriceAlert> {
        return alertDao.getActiveAlerts().map { it.toDomain() }
    }

    suspend fun addAlert(symbol: String, condition: AlertCondition, targetValue: Double): Long {
        val alert = PriceAlert(
            symbol = symbol,
            condition = condition,
            targetValue = targetValue
        )
        return alertDao.insertAlert(AlertEntity.fromDomain(alert))
    }

    suspend fun toggleAlert(alert: PriceAlert) {
        val updated = alert.copy(isEnabled = !alert.isEnabled)
        alertDao.updateAlert(AlertEntity.fromDomain(updated))
    }

    suspend fun markTriggered(alertId: Long) {
        val alert = alertDao.getAllAlertsFlow() // helper
    }

    suspend fun deleteAlert(alertId: Long) {
        alertDao.deleteAlertById(alertId)
    }
}
