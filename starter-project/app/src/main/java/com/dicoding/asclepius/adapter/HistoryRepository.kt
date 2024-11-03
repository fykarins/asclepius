
package com.dicoding.asclepius.adapter

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.dicoding.asclepius.mycamera.data.local.HistoryDao
import com.dicoding.asclepius.mycamera.data.local.HistoryEntity
import kotlinx.coroutines.flow.map

class HistoryRepository private constructor(
    private val historyDao: HistoryDao
){
    suspend fun insertHistory(historyEntity: HistoryEntity) {
        val history = HistoryEntity(
            id = historyEntity.id,
            prediction = historyEntity.prediction,
            image = historyEntity.image
        )
        historyDao.insertHistory(history)
    }

    fun getAllHistory(): LiveData<Result<List<HistoryEntity>>> {
        return historyDao.getAllHistory()
            .map { history ->
                Result.Success(history)
            }
            .asLiveData()
    }

    companion object {
        @Volatile
        private var instance: HistoryRepository? = null

        fun getInstance(historyDao: HistoryDao): HistoryRepository =
            instance ?: synchronized(this) {
                instance ?: HistoryRepository(historyDao)
            }.also { instance = it }
    }
}
