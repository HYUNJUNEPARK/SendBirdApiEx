package com.konai.sendbirdapisampleapp.strongbox

import java.security.PrivateKey
import java.security.PublicKey

data class KeyPairModel (
    val privateKey: PrivateKey,
    val publicKey: PublicKey
)