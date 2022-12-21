package com.konai.sendbirdapisampleapp.db.keyid

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [KeyIdEntity::class], version = 1)
abstract class KeyIdDatabase: RoomDatabase() {
    abstract fun keyIdDao(): KeyIdDao
}