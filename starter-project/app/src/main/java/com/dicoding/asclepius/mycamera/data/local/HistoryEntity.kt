package com.dicoding.asclepius.mycamera.data.local

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize


@Entity(tableName = "history_table")
@Parcelize
data class HistoryEntity (
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id")
    var id: String = "",

    @ColumnInfo(name = "prediction")
    var prediction: String = "",

    @ColumnInfo(name = "image")
    var image: String = ""
): Parcelable