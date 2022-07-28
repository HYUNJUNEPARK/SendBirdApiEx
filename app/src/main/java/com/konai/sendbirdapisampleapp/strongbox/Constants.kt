package com.konai.sendbirdapisampleapp.strongbox

object Constants {
    var iv: ByteArray? = ByteArray(16)

    const val CURVE_TYPE = "secp256r1"
    const val KEY_ALGORITHM = "AES"
    const val KEY_GEN_ALGORITHM = "EC"
    const val CIPHER_ECB_ALGORITHM = "AES/ECB/PKCS5Padding"
    const val CIPHER_CBC_ALGORITHM = "AES/CBC/PKCS7Padding"
    const val KEY_AGREEMENT_ALGORITHM = "ECDH"
    const val MESSAGE_DIGEST_ALGORITHM = "SHA-256"

    const val KEYSTORE_MY_KEYPAIR_ALIAS = "mKey"
    const val KEYSTORE_SECRET_KEY_ALIAS = "ssk"
    const val KEYSTORE_TYPE = "AndroidKeyStore"
}