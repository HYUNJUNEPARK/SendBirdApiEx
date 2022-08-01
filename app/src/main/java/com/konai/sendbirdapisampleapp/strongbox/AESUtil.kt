package com.konai.sendbirdapisampleapp.strongbox

import android.util.Base64
import com.konai.sendbirdapisampleapp.strongbox.StrongBoxConstants.CIPHER_CBC_ALGORITHM
import com.konai.sendbirdapisampleapp.strongbox.StrongBoxConstants.CIPHER_ECB_ALGORITHM
import com.konai.sendbirdapisampleapp.strongbox.StrongBoxConstants.KEY_ALGORITHM
import com.konai.sendbirdapisampleapp.strongbox.StrongBoxConstants.iv
import java.security.Key
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class AESUtil {
    //ECB Mode
    fun encryptionECBMode(userInputData: String, hash: ByteArray): String {
        val userInputData: ByteArray = userInputData.toByteArray()
        val key: Key = convertHashToKey(hash)
        val cipher = Cipher.getInstance(CIPHER_ECB_ALGORITHM) //AES/ECB/PKCS5Padding
        cipher.init(
            Cipher.ENCRYPT_MODE,
            key
        )
        val _result: ByteArray = cipher.doFinal(userInputData)
        val result: String = Base64.encodeToString(_result, Base64.DEFAULT)
        return result
    }

    fun decryptionECBMode(encryptedData: String, hash: ByteArray): String {
        val key: Key = convertHashToKey(hash)
        val cipher = Cipher.getInstance(CIPHER_ECB_ALGORITHM) //AES/ECB/PKCS5Padding
        cipher.init(
            Cipher.DECRYPT_MODE,
            key
        )
        val decryptedData: ByteArray = Base64.decode(encryptedData, Base64.DEFAULT)
        val result: ByteArray = cipher.doFinal(decryptedData)
        return String(result)
    }

    //CBC Mode
    fun encryptionCBCMode(userInputData: String, hash: ByteArray): String {
        val key: Key = convertHashToKey(hash)
        val userInputData: ByteArray = userInputData.toByteArray()
        val cipher = Cipher.getInstance(CIPHER_CBC_ALGORITHM) //AES/CBC/PKCS7Padding
        cipher.init(
            Cipher.ENCRYPT_MODE,
            key,
            IvParameterSpec(iv)
        )

        val _result: ByteArray = cipher.doFinal(userInputData)
        val result: String = Base64.encodeToString(_result, Base64.DEFAULT)
        return result
    }

    fun decryptionCBCMode(encryptedData: String, hash: ByteArray): String {
        val key: Key = convertHashToKey(hash)
        val cipher = Cipher.getInstance(CIPHER_CBC_ALGORITHM) //AES/CBC/PKCS7Padding
        cipher.init(
            Cipher.DECRYPT_MODE,
            key,
            IvParameterSpec(iv)
        )
        val decryptedData: ByteArray = Base64.decode(encryptedData, Base64.DEFAULT)
        val result: ByteArray = cipher.doFinal(decryptedData)
        return String(result)
    }

    //common
    fun convertHashToKey(sharedSecretHash : ByteArray): Key {
        return SecretKeySpec(sharedSecretHash, KEY_ALGORITHM)
    }
}