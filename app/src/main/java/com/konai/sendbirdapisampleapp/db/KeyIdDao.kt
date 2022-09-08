package com.konai.sendbirdapisampleapp.db

import androidx.room.*
import com.konai.sendbirdapisampleapp.db.DBProvider.DB_NAME

@Dao
interface KeyIdDao {
    @Query("SELECT keyId FROM $DB_NAME WHERE urlHash = :urlHash")
    fun getKeyId(urlHash: String): String

    @Query("SELECT * FROM $DB_NAME")
    fun getAll(): List<KeyIdEntity>

    @Delete
    fun delete(keyIdEntity: KeyIdEntity)

    @Update
    fun update(keyIdEntity: KeyIdEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(keyIdEntity: KeyIdEntity)
}