package com.konai.sendbirdapisampleapp.strongbox

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.konai.sendbirdapisampleapp.strongbox.Constants.CURVE_TYPE
import com.konai.sendbirdapisampleapp.strongbox.Constants.KEYSTORE_MY_KEYPAIR_ALIAS
import com.konai.sendbirdapisampleapp.strongbox.Constants.KEYSTORE_TYPE
import com.konai.sendbirdapisampleapp.strongbox.Constants.KEY_GEN_ALGORITHM
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.spec.ECGenParameterSpec

class KeyStoreUtil {
    private val keyStore = KeyStore.getInstance(KEYSTORE_TYPE).apply {
        load(null)
    }

    fun updateKeyPairToKeyStore() {
        val keyPairGenerator = KeyPairGenerator.getInstance(
            KEY_GEN_ALGORITHM,
            KEYSTORE_TYPE
        )
        val parameterSpec = KeyGenParameterSpec.Builder(
            KEYSTORE_MY_KEYPAIR_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).run {
            setUserAuthenticationRequired(false)
            ECGenParameterSpec(CURVE_TYPE) //secp256r1
            build()
        }
        keyPairGenerator.initialize(parameterSpec)
        keyPairGenerator.generateKeyPair()
    }

    fun getKeyPairFromKeyStore(): KeyPairModel {
        val keyStoreEntry = keyStore.getEntry(KEYSTORE_MY_KEYPAIR_ALIAS, null)
        val privateKey = (keyStoreEntry as KeyStore.PrivateKeyEntry).privateKey
        val publicKey = keyStore.getCertificate(KEYSTORE_MY_KEYPAIR_ALIAS).publicKey
        return KeyPairModel(privateKey, publicKey)
    }

    fun deleteKeyStoreKeyPair() {
        keyStore.deleteEntry(KEYSTORE_MY_KEYPAIR_ALIAS)
    }
}