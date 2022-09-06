package com.konai.sendbirdapisampleapp.strongbox

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Android M (API 23) 이상 사용 가능
 *
 * 빌드 방법
 * implementation 'androidx.security:security-crypto-ktx:1.1.0-alpha03'
 * or
 * File > Project Structure > Dependencies > app > Declared Dependencies [+]
 * > Library Dependency> 'androidx.security' 검색 > security-crypto 최신 버전을 설치
 *
 * XML 파일 위치
 * data > data > 패키지명 > shared_prefs > encrypted_pref.xml
 *
 * EncryptedSharedPreferencesManager 인스턴스 초기화
 *  private lateinit var encryptedSpm: EncryptedSharedPreferencesManager
 *      ...
 *  override fun onCreate(savedInstanceState: Bundle?) {
 *      ...
 *      encryptedSpm = EncryptedSharedPreferencesManager.getInstance(this)!!
 *  }
 *
 * MasterKey 정보
 * 키스토어 : AndroidKeyStore
 * keyAlias : "_androidx_security_master_key_"
 */

class EncryptedSharedPreferencesManager {
    companion object {
        const val PREFERENCE_NAME = "encrypted_pref"
        private var instance: EncryptedSharedPreferencesManager? = null
        private lateinit var context: Context
        private lateinit var prefs: SharedPreferences
        private lateinit var prefsEditor: SharedPreferences.Editor

        fun getInstance(_context: Context):EncryptedSharedPreferencesManager? {
            if (instance == null) {
                context = _context
                instance = EncryptedSharedPreferencesManager()
            }
            return instance
        }
    }

    init {
        prefs = EncryptedSharedPreferences.create(
            context,
            PREFERENCE_NAME,
            generateMasterKey(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, //The scheme to use for encrypting keys.
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM //The scheme to use for encrypting values.
        )
        prefsEditor = prefs.edit()
    }

    private fun generateMasterKey(): MasterKey {
        return MasterKey
            .Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    fun getString(key: String, defValue: String?): String {
        return prefs.getString(key, defValue)!!
    }

    fun putString(key: String, value: String?) {
        prefsEditor.apply {
            putString(key, value)
            apply()
        }
    }

    fun getInt(key: String, defValue: Int): Int {
        return prefs.getInt(key, defValue)
    }

    fun putInt(key: String, value: Int?) {
        prefsEditor.apply {
            putInt(key, value!!)
            apply()
        }
    }

    fun getBoolean(key: String, defValue: Boolean): Boolean {
        return prefs.getBoolean(key, defValue)
    }

    fun putBoolean(key: String, value: Boolean) {
        prefsEditor.apply {
            putBoolean(key, value)
            apply()
        }
    }

    fun remove(key: String) {
        prefsEditor.apply {
            remove(key)
            apply()
        }
    }

    fun removeAll() {
        prefsEditor.apply {
            clear()
            apply()
        }
    }

    fun getKeyIdList(): List<String>? {
        val keys:Map<String, *> = prefs.all
        val keyList:MutableList<String> = mutableListOf()
        for ((key, value) in keys.entries) {
            keyList.add(key)
        }
        return keyList
    }
}