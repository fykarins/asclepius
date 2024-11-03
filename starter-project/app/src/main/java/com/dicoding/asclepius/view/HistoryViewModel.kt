package com.dicoding.asclepius.view

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.dicoding.asclepius.adapter.HistoryRepository
import com.dicoding.asclepius.adapter.Result
import com.dicoding.asclepius.mycamera.data.local.HistoryEntity

class HistoryViewModel(repository: HistoryRepository) : ViewModel() {

    val historyList: LiveData<Result<List<HistoryEntity>>> = repository.getAllHistory()
}