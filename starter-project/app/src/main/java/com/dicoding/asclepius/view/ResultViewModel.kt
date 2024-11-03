
package com.dicoding.asclepius.view

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dicoding.asclepius.adapter.HistoryRepository
import com.dicoding.asclepius.mycamera.data.local.HistoryEntity
import kotlinx.coroutines.launch

class ResultViewModel(private val repository: HistoryRepository) : ViewModel() {

    private val _history = MutableLiveData<Boolean>()
    val history: LiveData<Boolean> = _history

    fun saveHistory(historyEntity: HistoryEntity) {
        viewModelScope.launch {
            repository.insertHistory(historyEntity)
            _history.value = true
        }
    }
}
