package com.konai.sendbirdapisampleapp.preference

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import com.konai.sendbirdapisampleapp.util.Constants.PREFERENCE_NAME_HASH
import com.konai.sendbirdapisampleapp.util.Constants.TAG

class KeySharedPreference(val context: Context) {
    fun updateHash(channelUrl: String, _hash: ByteArray) {
        val sharedPreferences = context.getSharedPreferences(PREFERENCE_NAME_HASH, Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        val hash: String = Base64.encodeToString(_hash, Base64.DEFAULT)
        editor.putString(channelUrl, hash)
        editor.apply()
    }

    fun getHash(channelUrl: String): ByteArray? {
        val sharedPreferences = context.getSharedPreferences(PREFERENCE_NAME_HASH, Context.MODE_PRIVATE)
        val _hash = sharedPreferences.getString(channelUrl, "empty hash")
        val hash = Base64.decode(_hash, Base64.DEFAULT)
        return hash
    }
}