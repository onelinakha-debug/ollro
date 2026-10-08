package com.example.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.local.dao.CandleDao
import com.example.data.local.dao.HoldingDao
import com.example.data.local.dao.NewsArticleDao

@Database(
    entities = [CandleEntity::class, AlertEntity::class, HoldingEntity::class, NewsArticleEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(IndicatorsTypeConverter::class)
abstract class OlroDatabase : RoomDatabase() {
    abstract fun candleDao(): CandleDao
    abstract fun alertDao(): AlertDao
    abstract fun holdingDao(): HoldingDao
    abstract fun newsArticleDao(): NewsArticleDao

    companion object {
        @Volatile
        private var INSTANCE: OlroDatabase? = null

        fun getInstance(context: Context): OlroDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OlroDatabase::class.java,
                    "olro_market_cache.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
