package com.konai.sendbirdapisampleapp.strongbox

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import com.konai.sendbirdapisampleapp.strongbox.Constants.CURVE_TYPE
import com.konai.sendbirdapisampleapp.strongbox.Constants.KEYSTORE_TYPE
import com.konai.sendbirdapisampleapp.strongbox.Constants.KEY_GEN_ALGORITHM
import com.konai.sendbirdapisampleapp.util.Constants.TAG
import java.lang.Exception
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PublicKey
import java.security.spec.ECGenParameterSpec

class KeyStoreUtil {
    private val keyStore = KeyStore.getInstance(KEYSTORE_TYPE).apply {
        load(null)
    }

    fun updateKeyPairToKeyStore(keyStoreAlias: String) {
        val keyPairGenerator = KeyPairGenerator.getInstance(
            KEY_GEN_ALGORITHM,
            KEYSTORE_TYPE
        )
        val parameterSpec = KeyGenParameterSpec.Builder(
            keyStoreAlias, //== KEYSTORE_MY_KEYPAIR_ALIAS
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).run {
            setUserAuthenticationRequired(false)
            ECGenParameterSpec(CURVE_TYPE) //secp256r1
            build()
        }
        keyPairGenerator.initialize(parameterSpec)
        keyPairGenerator.generateKeyPair()
    }

    fun isKeyPairInKeyStore(keyStoreAlias: String): Boolean {
        var keyStoreEntry: KeyStore.Entry? = keyStore.getEntry(keyStoreAlias, null)
        return keyStoreEntry != null
    }

    fun getPublicKeyFromKeyStore(keyStoreAlias: String): PublicKey? {
        return keyStore.getCertificate(keyStoreAlias).publicKey
    }

    fun getKeyPairFromKeyStore(keyStoreAlias: String): KeyPairModel {
        val keyStoreEntry = keyStore.getEntry(keyStoreAlias, null)
        val privateKey = (keyStoreEntry as KeyStore.PrivateKeyEntry).privateKey
        val publicKey = keyStore.getCertificate(keyStoreAlias).publicKey
        return KeyPairModel(privateKey, publicKey)
    }

    fun deleteKeyStoreKeyPair(keyStoreAlias: String) {
        try {
            keyStore.deleteEntry(keyStoreAlias)
            Log.i(TAG, "keystore key is deleted")
        }
        catch (e: Exception) {
            Log.e(TAG, "keystore key is deleted failed")
        }

    }
}