package com.uas.papb.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "item")
data class Item(
    @PrimaryKey(autoGenerate = false) var id: String = "",
    @ColumnInfo(name = "name") var name: String? = null,
    @ColumnInfo(name = "author") var author: String? = null,
    @ColumnInfo(name = "tag") var tag: String? = null,
    @ColumnInfo(name = "storyline") var storyline: String? = null,
    @ColumnInfo(name = "image") var image: String? = null,
    @ColumnInfo(name = "rating") var rating: Double? = null,
)