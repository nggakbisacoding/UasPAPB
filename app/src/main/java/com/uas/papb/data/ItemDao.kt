package com.uas.papb.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ItemDao {
    @Query("SELECT * FROM item")
    fun getAll(): List<Item>

    @Query("SELECT * FROM item WHERE id LIKE :itemId")
    fun selectById(itemId: String): Item?

    @Delete
    fun delete(item: Item)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert (item: Item)

    @Update
    fun update (item: Item)

    @get:Query("SELECT * FROM item ORDER BY id ASC")
    val allNotes: LiveData<List<Item>>
}