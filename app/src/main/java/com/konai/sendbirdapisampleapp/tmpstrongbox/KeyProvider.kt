package com.konai.sendbirdapisampleapp.tmpstrongbox

import com.konai.sendbirdapisampleapp.tmpstrongbox.StrongBoxConstants.CURVE_TYPE
import com.konai.sendbirdapisampleapp.tmpstrongbox.StrongBoxConstants.KEY_AGREEMENT_ALGORITHM_ECDH
import com.konai.sendbirdapisampleapp.tmpstrongbox.StrongBoxConstants.KEY_GEN_ALGORITHM_EC
import com.konai.sendbirdapisampleapp.tmpstrongbox.StrongBoxConstants.MESSAGE_DIGEST_ALGORITHM_SHA_256
import java.security.*
import java.security.spec.ECGenParameterSpec
import javax.crypto.KeyAgreement

class KeyProvider {
    fun createKeyPair(): KeyPairModel {
        val keyPairGenerator = KeyPairGenerator.getInstance(KEY_GEN_ALGORITHM_EC).apply {
            initialize(ECGenParameterSpec(CURVE_TYPE)) //secp256r1
        }
        val keyPair = keyPairGenerator.generateKeyPair()
        return KeyPairModel(keyPair.private, keyPair.public)
    }


    //TODO Error keyAgreement.init(myPrivateKey) -> USE Android 12, API 31 Device if not InvalidKeyException: Keystore operation failed
    fun createSharedSecretHash(myPrivateKey: PrivateKey, partnerPublicKey: PublicKey, randomNumber: ByteArray): ByteArray {
        val sharedSecret: ByteArray
        try {
            val keyAgreement = KeyAgreement.getInstance(KEY_AGREEMENT_ALGORITHM_ECDH).apply {
                init(myPrivateKey)
                doPhase(partnerPublicKey, true)
            }
            sharedSecret = keyAgreement.generateSecret()

        }
        catch (e : Exception) {
            throw KeyException("$e")
        }
        return hashSHA256(sharedSecret, randomNumber)
    }

    private fun hashSHA256(key: ByteArray, randomNumber: ByteArray): ByteArray {
        val hash: ByteArray
        try {
            val messageDigest = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM_SHA_256)
            messageDigest.update(key)
            hash = messageDigest.digest(randomNumber)
        }
        catch (e: CloneNotSupportedException) {
            throw DigestException("$e")
        }
        return hash
    }

    fun getRandomNumbers(): ByteArray {
        val randomByteArray = ByteArray(32).apply {
            SecureRandom().nextBytes(this)
        }
        return randomByteArray
    }
}