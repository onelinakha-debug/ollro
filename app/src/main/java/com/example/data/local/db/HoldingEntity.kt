package com.example.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.Holding

@Entity(
    tableName = "holdings",
    indices = [
        Index(value = ["symbol"], unique = true)
    ]
)
data class HoldingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val symbol: String,
    val amount: Double,
    val averageBuyPrice: Double,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Holding = Holding(
        id = id,
        symbol = symbol,
        amount = amount,
        averageBuyPrice = averageBuyPrice,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(holding: Holding): HoldingEntity = HoldingEntity(
            id = holding.id,
            symbol = holding.symbol,
            amount = holding.amount,
            averageBuyPrice = holding.averageBuyPrice,
            createdAt = holding.createdAt
        )
    }
}
