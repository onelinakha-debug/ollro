package com.example.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.AlertCondition
import com.example.domain.model.PriceAlert

@Entity(tableName = "price_alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val symbol: String,
    val condition: String,
    val targetValue: Double,
    val isTriggered: Boolean,
    val isEnabled: Boolean,
    val createdAt: Long
) {
    fun toDomain(): PriceAlert = PriceAlert(
        id = id,
        symbol = symbol,
        condition = try { AlertCondition.valueOf(condition) } catch (e: Exception) { AlertCondition.PRICE_ABOVE },
        targetValue = targetValue,
        isTriggered = isTriggered,
        isEnabled = isEnabled,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(alert: PriceAlert): AlertEntity = AlertEntity(
            id = alert.id,
            symbol = alert.symbol,
            condition = alert.condition.name,
            targetValue = alert.targetValue,
            isTriggered = alert.isTriggered,
            isEnabled = alert.isEnabled,
            createdAt = alert.createdAt
        )
    }
}
