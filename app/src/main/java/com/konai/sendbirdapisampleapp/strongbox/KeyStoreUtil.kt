package com.konai.sendbirdapisampleapp.strongbox

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import com.konai.sendbirdapisampleapp.strongbox.StrongBoxConstants.CURVE_TYPE
import com.konai.sendbirdapisampleapp.strongbox.StrongBoxConstants.KEYSTORE_TYPE
import com.konai.sendbirdapisampleapp.strongbox.StrongBoxConstants.KEY_GEN_ALGORITHM
import com.konai.sendbirdapisampleapp.util.Constants.TAG
import java.lang.Exception
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PublicKey
import java.security.spec.*

class KeyStoreUtil {
    private val keyStore = KeyStore.getInstance(KEYSTORE_TYPE).apply {
        load(null)
    }

    fun updateKeyPairToKeyStore(keyStoreAlias: String) {
        val keyPairGenerator = KeyPairGenerator.getInstance(
            KEY_GEN_ALGORITHM, //EC
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
        val keyStoreEntry: KeyStore.Entry? = keyStore.getEntry(keyStoreAlias, null)
        return keyStoreEntry != null
    }

    fun getPublicKeyFromKeyStore(keyStoreAlias: String): PublicKey? {
        return keyStore.getCertificate(keyStoreAlias).publicKey
    }

    //Elliptic Curve Domain Parameters : https://www.secg.org/sec2-v2.pdf Page9 of 33-34
    fun getECParameterSpec(): ECParameterSpec {
        val p = BigInteger("FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFF", 16)
        val a = BigInteger("FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFC", 16)
        val b = BigInteger("5AC635D8AA3A93E7B3EBBD55769886BC651D06B0CC53B0F63BCE3C3E27D2604B", 16)
        val g_x = BigInteger("6B17D1F2E12C4247F8BCE6E563A440F277037D812DEB33A0F4A13945D898C296", 16)
        val g_y = BigInteger("4FE342E2FE1A7F9B8EE7EB4A7C0F9E162BCE33576B315ECECBB6406837BF51F5", 16)
        val n = BigInteger("FFFFFFFF00000000FFFFFFFFFFFFFFFFBCE6FAADA7179E84F3B9CAC2FC632551", 16)
        val h = 1
        val ecField = ECFieldFp(p)
        val curve = EllipticCurve(ecField, a, b)
        val g = ECPoint(g_x, g_y)
        return ECParameterSpec(curve, g, n, h)
    }

    fun getKeyPairFromKeyStore(keyStoreAlias: String): KeyPairModel {
        val keyStoreEntry = keyStore.getEntry(keyStoreAlias, null)
        val privateKey = (keyStoreEntry as KeyStore.PrivateKeyEntry).privateKey
        //val test = (privateKey as ECPrivateKey).params ****ClassCastException
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