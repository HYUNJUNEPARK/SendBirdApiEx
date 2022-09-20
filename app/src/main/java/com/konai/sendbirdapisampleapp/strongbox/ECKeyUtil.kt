package com.konai.sendbirdapisampleapp.strongbox

import android.security.keystore.KeyProperties
import com.konai.sendbirdapisampleapp.strongbox.StrongBox.Companion.ecKeyPairAlias
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyStore
import java.security.PublicKey
import java.security.interfaces.ECPublicKey
import java.security.spec.*
import java.util.*
import kotlin.collections.HashMap

object ECKeyUtil {
    /**
     * 공개키로 affineX, affineY 를생성
     *
     * @param publicKey 서버에 업로드할 공개키
     * @return affineX, affineY를 HashMap<String, String>으로 반환
     * @throws Exception
     */
    @Throws(Exception::class)
    fun extractAffineXY(userId: String, publicKey: PublicKey): HashMap<String, String> {

        //TODO keyValue(ByteArray) -> Base64 String -> upload to server
        val encodedData = (publicKey as ECPublicKey).encoded
        val keyValue = Arrays.copyOfRange(encodedData, encodedData.size - 65, encodedData.size )

        //publickey 로 다시 바꾸는 과정에서 잘라야하는 데이터가 생길 수 있음
        (publicKey as ECPublicKey).let { ecPublicKey ->
            return hashMapOf(
                "userId" to userId,
                "affineX" to ecPublicKey.w.affineX.toString(),
                "affineY" to ecPublicKey.w.affineY.toString()
            )
        }
    }

    /**
     * affineX, affineY 로 공개키 생성
     *
     * @param affineX 공개키의 x 좌표
     * @param affineY 공개키의 y 좌표
     * @return 완성한 공개키 반환
     */
    @Throws(Exception::class)
    fun coordinatePublicKey(affineX: String, affineY: String): PublicKey {
        val affineX = BigInteger(affineX)
        val affineY = BigInteger(affineY)
        val ecPoint = ECPoint(affineX, affineY)
        val keySpec = ECPublicKeySpec(
            ecPoint,
            ecParameterSpec
        )
        val keyFactory = KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_EC)
        return keyFactory.generatePublic(keySpec)
    }

    //Reference : Elliptic Curve Domain Parameters (https://www.secg.org/sec2-v2.pdf Page9 of 33-34)
    private val ecParameterSpec= with(this) {
        val p = BigInteger("FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFF", 16)
        val a = BigInteger("FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFC", 16)
        val b = BigInteger("5AC635D8AA3A93E7B3EBBD55769886BC651D06B0CC53B0F63BCE3C3E27D2604B", 16)
        val gX = BigInteger("6B17D1F2E12C4247F8BCE6E563A440F277037D812DEB33A0F4A13945D898C296", 16)
        val gY = BigInteger("4FE342E2FE1A7F9B8EE7EB4A7C0F9E162BCE33576B315ECECBB6406837BF51F5", 16)
        val n = BigInteger("FFFFFFFF00000000FFFFFFFFFFFFFFFFBCE6FAADA7179E84F3B9CAC2FC632551", 16)
        val h = 1
        val ecField = ECFieldFp(p)
        val curve = EllipticCurve(ecField, a, b)
        val g = ECPoint(gX, gY)
        ECParameterSpec(curve, g, n, h)
    }

    //새로 추가한 함수
    fun isECKeyPair(): Boolean {
        val keyStoreEntry: KeyStore.Entry? = StrongBox.androidKeyStore.getEntry(ecKeyPairAlias, null)
        return keyStoreEntry != null
    }

    //테스트 앱용
    fun isECKeyPair(userId: String): Boolean {
        val keyStoreEntry: KeyStore.Entry? = StrongBox.androidKeyStore.getEntry(userId, null)
        return keyStoreEntry != null
    }
}