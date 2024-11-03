package com.dicoding.asclepius.view

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dicoding.asclepius.adapter.HistoryRepository
import com.dicoding.asclepius.di.Injection

class ViewModelFactory private constructor(
    private val historyRepository: HistoryRepository
) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            return HistoryViewModel(historyRepository) as T
        } else if (modelClass.isAssignableFrom(ResultViewModel::class.java)) {
            return  ResultViewModel(historyRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class" + modelClass.name)
    }

    companion object {
        @Volatile
        private var instance: ViewModelFactory? = null

        fun getInstance(context: Context): ViewModelFactory =
            instance ?: synchronized(this) {
                instance ?: ViewModelFactory(
                    Injection.provideRepository(context)
                ).also { instance = it }
            }
    }
}
