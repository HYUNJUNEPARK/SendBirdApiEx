package com.konai.sendbirdapisampleapp.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.konai.sendbirdapisampleapp.db.DBProvider.DB_NAME

@Entity(tableName = DB_NAME)
data class KeyIdEntity(
    @PrimaryKey
    var urlHash: String,
    @ColumnInfo
    var keyId: String
)
