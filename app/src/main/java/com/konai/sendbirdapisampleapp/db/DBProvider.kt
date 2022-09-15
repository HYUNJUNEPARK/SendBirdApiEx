package com.konai.sendbirdapisampleapp.db

import android.content.Context
import androidx.room.Room
import com.konai.sendbirdapisampleapp.db.keyid.KeyIdDatabase

/*
LOCAL DB(ROOM)
====================================
Channel URL | KeyId(Secure Random) |
====================================

cf. EncryptedSharedPreferences
=======================================
KeyId(Secure Random) | SharedSecretKey |
=======================================

cf. keyStore
====================
userId | ECKeyPair |
====================
*/
object DBProvider {
    private var instance: KeyIdDatabase? = null
    const val DB_NAME = "key_db"

    @Synchronized
    fun getInstance(context: Context): KeyIdDatabase? {
        if (instance == null) {
            synchronized(KeyIdDatabase::class){
                instance = Room.databaseBuilder(
                    context.applicationContext,
                    KeyIdDatabase::class.java,
                    DB_NAME
                ).build()
            }
        }
        return instance
    }
}