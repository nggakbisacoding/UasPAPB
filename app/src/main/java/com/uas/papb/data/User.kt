package com.uas.papb.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class User(
    @PrimaryKey (autoGenerate = false) val id: String,
    @ColumnInfo(name = "name") val name: String?,
    @ColumnInfo(name = "profile_image") val profileImage: String?,
    @ColumnInfo(name = "email") val email: String?,
    @ColumnInfo(name = "password") val password: String?,
    @ColumnInfo(name = "roles") val role: String?
)