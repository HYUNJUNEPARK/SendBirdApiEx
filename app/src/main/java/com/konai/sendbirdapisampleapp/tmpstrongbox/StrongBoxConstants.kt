package com.konai.sendbirdapisampleapp.tmpstrongbox

object StrongBoxConstants {
    val iv: ByteArray = ByteArray(16)

    const val CURVE_TYPE = "secp256r1"
    const val KEY_ALGORITHM_AES = "AES"
    const val KEY_GEN_ALGORITHM_EC = "EC"
    const val CIPHER_AES_ECB_PADDING = "AES/ECB/PKCS5Padding"
    const val CIPHER_AES_CBC_PADDING = "AES/CBC/PKCS7Padding"
    const val KEY_AGREEMENT_ALGORITHM_ECDH = "ECDH"
    const val MESSAGE_DIGEST_ALGORITHM_SHA_256 = "SHA-256"
    const val KEYSTORE_TYPE = "AndroidKeyStore"
}