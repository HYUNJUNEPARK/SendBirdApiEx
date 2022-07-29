package com.konai.sendbirdapisampleapp.strongbox

import com.konai.sendbirdapisampleapp.strongbox.StrongBoxConstants.CURVE_TYPE
import com.konai.sendbirdapisampleapp.strongbox.StrongBoxConstants.KEY_AGREEMENT_ALGORITHM
import com.konai.sendbirdapisampleapp.strongbox.StrongBoxConstants.KEY_GEN_ALGORITHM
import com.konai.sendbirdapisampleapp.strongbox.StrongBoxConstants.MESSAGE_DIGEST_ALGORITHM
import java.security.*
import java.security.spec.ECGenParameterSpec
import javax.crypto.KeyAgreement

class KeyProvider {
    fun createKeyPair(): KeyPairModel {
        val keyPairGenerator = KeyPairGenerator.getInstance(KEY_GEN_ALGORITHM) //EC
        keyPairGenerator.initialize(ECGenParameterSpec(CURVE_TYPE)) //secp256r1
        val keyPair = keyPairGenerator.generateKeyPair()
        return KeyPairModel(keyPair.private, keyPair.public)
    }


    //TODO Error keyAgreement.init(myPrivateKey)
    //InvalidKeyException: Keystore operation failed
    fun createSharedSecretHash(myPrivateKey: PrivateKey, partnerPublicKey: PublicKey, randomNumber: ByteArray): ByteArray {
        val keyAgreement = KeyAgreement.getInstance(KEY_AGREEMENT_ALGORITHM) //ECDH
        keyAgreement.init(myPrivateKey)
        keyAgreement.doPhase(partnerPublicKey, true)
        val sharedSecret: ByteArray = keyAgreement.generateSecret()
        return hashSHA256(sharedSecret, randomNumber)
    }

    private fun hashSHA256(key: ByteArray, randomNumber: ByteArray): ByteArray {
        val hash: ByteArray
        try {
            val messageDigest = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM) //SHA-256
            messageDigest.update(key)
            hash = messageDigest.digest(randomNumber)
        }
        catch (e: CloneNotSupportedException) {
            throw DigestException("$e")
        }
        return hash
    }

    fun getRandomNumbers(): ByteArray {
        val randomByteArray = ByteArray(32)
        SecureRandom().nextBytes(randomByteArray)
        return randomByteArray
    }
}