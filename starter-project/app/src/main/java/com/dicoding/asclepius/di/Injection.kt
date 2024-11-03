package com.dicoding.asclepius.di

import android.content.Context
import com.dicoding.asclepius.adapter.HistoryRepository
import com.dicoding.asclepius.mycamera.data.local.HistoryRoomDatabase

object Injection {
    fun provideRepository(context: Context): HistoryRepository {
        val database = HistoryRoomDatabase.getDatabase(context)
        val dao = database.historyDao()
        return HistoryRepository.getInstance(dao)
    }
}