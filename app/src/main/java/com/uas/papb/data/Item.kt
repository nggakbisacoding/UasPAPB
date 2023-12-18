package com.uas.papb.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "item")
data class Item(
    @PrimaryKey(autoGenerate = false) val id: String,
    @ColumnInfo(name = "name") val name: String?,
    @ColumnInfo(name = "author") val author: String?,
    @ColumnInfo(name = "tag") val tag: String?,
    @ColumnInfo(name = "storyline") val desc: String?,
    @ColumnInfo(name = "image") val image: String?,
    @ColumnInfo(name = "rating") val rating: Double?,
)