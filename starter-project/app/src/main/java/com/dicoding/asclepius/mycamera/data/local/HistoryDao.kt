package com.dicoding.asclepius.mycamera.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(historyEntity: HistoryEntity)

    @Query("SELECT * FROM history_table")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Delete
    suspend fun deleteHistory(historyEntity: HistoryEntity)
}
