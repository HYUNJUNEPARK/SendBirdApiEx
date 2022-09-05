package com.konai.sendbirdapisampleapp.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [KeyId::class], version = 1)
abstract class KeyIdDatabase: RoomDatabase() {
    abstract fun keyIdDao(): KeyIdDao
}