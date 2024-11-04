package com.dicoding.asclepius.adapter

import androidx.recyclerview.widget.DiffUtil
import com.dicoding.asclepius.data.local.PredictionHistory

class PredictionDiffCallback(
    private val oldList: List<PredictionHistory>,
    private val newList: List<PredictionHistory>
) : DiffUtil.Callback() {

    override fun getOldListSize() = oldList.size
    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
