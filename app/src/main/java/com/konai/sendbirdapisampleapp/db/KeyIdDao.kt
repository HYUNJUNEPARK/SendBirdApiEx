package com.konai.sendbirdapisampleapp.db

import androidx.room.Dao
import androidx.room.Query
import com.konai.sendbirdapisampleapp.db.DBProvider.DB_NAME

@Dao
interface KeyIdDao {
    @Query("SELECT keyId FROM $DB_NAME WHERE urlHash = :urlHash")
    fun getKeyId(urlHash: String): String

    @Query("SELECT * FROM $DB_NAME")
    fun getAll(): List<KeyId>
}