package com.dicoding.asclepius.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PredictionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrediction(prediction: PredictionHistory)

    @Query("SELECT * FROM prediction_history")
    suspend fun getAllPredictions(): List<PredictionHistory>

    @Delete
    suspend fun deletePrediction(prediction: PredictionHistory)
}

