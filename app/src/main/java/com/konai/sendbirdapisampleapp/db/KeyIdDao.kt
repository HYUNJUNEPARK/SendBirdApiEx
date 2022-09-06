package com.konai.sendbirdapisampleapp.db

import androidx.room.*
import com.konai.sendbirdapisampleapp.db.DBProvider.DB_NAME

@Dao
interface KeyIdDao {
    @Query("SELECT keyId FROM $DB_NAME WHERE urlHash = :urlHash")
    fun getKeyId(urlHash: String): String

    @Query("SELECT * FROM $DB_NAME")
    fun getAll(): List<KeyId>

    @Delete
    fun delete(keyId: KeyId)

    @Update
    fun update(keyId: KeyId)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(keyId: KeyId)
}