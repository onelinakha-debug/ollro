package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.db.JournalEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {

    @Query("SELECT * FROM trade_journal ORDER BY entryTimestamp DESC")
    fun getAllEntriesFlow(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM trade_journal WHERE status = 'OPEN' ORDER BY entryTimestamp DESC")
    fun getOpenEntriesFlow(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM trade_journal WHERE status = 'CLOSED' ORDER BY exitTimestamp DESC")
    fun getClosedEntriesFlow(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM trade_journal WHERE id = :id LIMIT 1")
    suspend fun getEntryById(id: Long): JournalEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: JournalEntryEntity): Long

    @Update
    suspend fun updateEntry(entry: JournalEntryEntity)

    @Delete
    suspend fun deleteEntry(entry: JournalEntryEntity)

    @Query("DELETE FROM trade_journal WHERE id = :id")
    suspend fun deleteEntryById(id: Long)

    @Query("DELETE FROM trade_journal")
    suspend fun clearAll()
}
