package com.uas.papb.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class User(
    @PrimaryKey (autoGenerate = false) val id: String = "",
    @ColumnInfo(name = "name") val name: String? = null,
    @ColumnInfo(name = "profileImage") var profileImage: String? = null,
    @ColumnInfo(name = "email") val email: String? = null,
    @ColumnInfo(name = "password") val password: String? = null,
    @ColumnInfo(name = "roles") var role: String? = null
)