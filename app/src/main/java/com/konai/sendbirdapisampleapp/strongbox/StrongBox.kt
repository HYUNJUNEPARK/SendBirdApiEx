package com.konai.sendbirdapisampleapp.strongbox

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.*
import java.security.spec.*
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Android API 31 이상 사용 가능
 *
 * 패키지 구성
 * StrongBox : EC 키쌍(ECKeyPair) 생성, AES 암복호화할 때 필요한 클래스
 * ECKeyUtil : 공개키(Public Key)를 가공할 때 필요한 클래스
 * EncryptedSharedPreferencesManager : 암호화 된 공유키(Shared Secret Key) 를 EncryptedSharedPreferences 에서 관리할 때 필요한 클래스
 *
 * 메서드 목록
 * resetStrongBox()
 * generateECKeyPair()
 * getECPublicKey(): PublicKey?
 * deleteECKeyPair(): Boolean
 * generateRandom(size: Int): String?
 * generateSharedSecretKey(publicKey: PublicKey, nonce: String): String?
 * deleteSharedSecretKey(keyId: String): Boolean
 * encrypt(message: String, keyId: String): String?
 * decrypt(message: String, keyId: String): String?
 */
class StrongBox {
    companion object {
        private var instance: StrongBox? = null
        private lateinit var encryptedSharedPreferencesManager: EncryptedSharedPreferencesManager
        private lateinit var context: Context

        /**
         * StrongBox SDK 를 사용하기 위한 새로운 라이브러리 인스턴스 생성.
         *
         * 라이브러리 초기화 작업 수행
         *
         * @param context application context
         * @return Interface 싱글톤 객체
         * @throws IllegalArgumentException
         * 제공된 application context 로 부터 확인한 application 이 허용 목록에 없는 경우
         */
        @Throws(IllegalArgumentException::class)
        fun getInstance(context: Context): StrongBox {
            return instance ?: synchronized(this) {
                instance ?: StrongBox().also { strongBox ->
                    encryptedSharedPreferencesManager =
                        EncryptedSharedPreferencesManager.getInstance(context)!!
                    this.context = context
                    instance = strongBox
                }
            }
        }

        //안드로이드 키스토어(AndroidKeyStore)에 저장되어있는 EC 키쌍의 식별자
        const val ecKeyPairAlias = "androidKeyStoreKey"

        //안드로이드 키스토어(AndroidKeyStore) : 해당 키스토어에 사용자의 EC 키쌍이 저장되어 있음
        val androidKeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }

        //CBC(Cipher Block Chaining)Mode 에서 첫번째 암호문 대신 사용되는 IV(Initial Vector)로 0으로 초기화되어 있음
        private val iv: ByteArray = ByteArray(16)
    }



    //StrongBox 에 저장된 모든 보안 데이터를 삭제하고 초기화
    @Throws(Exception::class)
    fun resetStrongBox() {
        deleteECKeyPair()
        encryptedSharedPreferencesManager.removeAll()
    }

    /**
     * EC 키 쌍을 생성
     *
     * 생성된 키 쌍은 StrongBox 의 안전한 저장소에 저장
     *
     * warning
     * Field requires API level 31 (current min is 23): `android.security.keystore.KeyProperties#PURPOSE_AGREE_KEY`
     *
     * @throws Exception
     * UserException. Key Pair 생성에 실패한 경우. 재생성 조건을 충족하지 못한 경우
     */
    @Throws(Exception::class)
    fun generateECKeyPair() {
        //Android API 31 이상
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC,
                "AndroidKeyStore"
            )
            val parameterSpec = KeyGenParameterSpec.Builder(
                ecKeyPairAlias,
                KeyProperties.PURPOSE_ENCRYPT or
                        KeyProperties.PURPOSE_DECRYPT or
                        KeyProperties.PURPOSE_AGREE_KEY
            ).run {
                setUserAuthenticationRequired(false)
                ECGenParameterSpec("secp256r1") //curve type
                build()
            }
            keyPairGenerator.initialize(parameterSpec)
            keyPairGenerator.generateKeyPair()
        }
        //Android API 30 이하
        else {
            //SDK 사용 불가
        }
    }

    //오버로드 for 테스트 앱
    fun generateECKeyPair(keyStoreAlias: String) {
        //Android API 31 이상
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC,
                "AndroidKeyStore"
            )
            val parameterSpec = KeyGenParameterSpec.Builder(
                keyStoreAlias,
                KeyProperties.PURPOSE_ENCRYPT or
                        KeyProperties.PURPOSE_DECRYPT or
                        KeyProperties.PURPOSE_AGREE_KEY
            ).run {
                setUserAuthenticationRequired(false)
                ECGenParameterSpec("secp256r1") //curve type
                build()
            }
            keyPairGenerator.initialize(parameterSpec)
            keyPairGenerator.generateKeyPair()
        }
        //Android API 30 이하
        else {
            //SDK 사용 불가
        }
    }


    /**
     * 생성된 EC 키 쌍 중 공개키를 반환
     *
     * @return PublicKey
     * @throws Exception
     * User Exception 정의. 키스토어에 공개키가 없는 경우
     */
    @Throws(Exception::class)
    fun getECPublicKey(): PublicKey {
        return androidKeyStore.getCertificate(ecKeyPairAlias).publicKey
    }

    //오버로드 for 테스트 앱
    fun getECPublicKey(keyStoreAlias: String): PublicKey {
        return androidKeyStore.getCertificate(keyStoreAlias).publicKey
    }

    /**
     * 사용자의 EC 키쌍을 삭제
     *
     * @return 안전하게 삭제되었다면 true 를, 그렇지 않다면, false 를 반환
     * @throws Exception
     * User Exception 정의. false 반환
     */
    fun deleteECKeyPair(): Boolean {
        return try {
            androidKeyStore.deleteEntry(ecKeyPairAlias)
            true
        }
        catch (e: Exception) {
            false
        }
    }

    /**
     * Nonce 로 사용될 랜덤 데이터를 생성 후 반환
     *
     * 생성된 랜덤 데이터 사용처
     * 1. 해시 만들 때 사용
     * 2. keyId로 사용
     *
     * @param size 랜덤 데이터의 길이
     * @return 랜덤 데이터를 String 타입으로 바꾼 뒤 반환
     * @throws Exception
     * User Exception 정의. 랜덤 데이터 생성 실패 시
     */
    @Throws(Exception::class)
    fun generateRandom(size: Int): String {
        return ByteArray(size).apply {
            SecureRandom().nextBytes(this)
        }.let { randomBytes ->
            Base64.encodeToString(randomBytes, Base64.DEFAULT)
        }
    }

    /**
     * 전달받은 상대방의 정보(상대방의 공개키)와 Nonce 를 이용하여 메시지 암호화를 위한 비밀키(SharedSecretKey) 생성
     * 생성된 비밀키는 암호화되어 EncryptedSharedPreferences 에 저장됨
     *
     * 추가 구현 요소
     * 1. 채널을 초대"한" 사람이 해당 메서드를 사용할 때
     * 1-1) keyId(랜덤 데이터)를 채널(채팅방)의 메타데이터로 업로드하는 기능 구현 필요
     * 1-2) keyId와 채널 URL 주소를 매핑해주는 로컬 DB(ex. Room) 구현 필요 (Key(URL): Value(KeyId))
     * -------------------------------------------------------
     * 2. 채널 초대"받은" 사람이 해당 메서드를 사용할 때
     * 2-1) 채널 메타데이터를 가져와 해당 메서드의 nonce 파라미터로 사용
     * 2-2) keyId와 채널 URL 주소를 매핑해주는 로컬 DB 구현 필요
     *
     * @param publicKey 대화 상대의 공개키
     * @param nonce 램덤 데이터를 의미하며, 사용자가 직접 생성하거(1.의 경우)나 채널 메타데이터를 가져와(2.의 경우) 파라미터로 사용
     * @return keyId 를 반환 -> keyId와 채널 URL 주소를 매핑해 로컬 DB에 저장
     * @throws Exception
     * User Exception 정의. 비밀키 생성 실패 시
     */
    @Throws(Exception::class)
    fun generateSharedSecretKey(publicKey: PublicKey, nonce: String): String {
        val keyId: String = nonce
        val random: ByteArray = Base64.decode(nonce, Base64.DEFAULT)

        val privateKey: PrivateKey
        androidKeyStore.getEntry(ecKeyPairAlias, null).let { keyStoreEntry ->
            privateKey = (keyStoreEntry as KeyStore.PrivateKeyEntry).privateKey
        }

        var sharedSecretKey: String
        KeyAgreement.getInstance("ECDH").apply {
            init(privateKey)
            doPhase(publicKey, true)
        }.generateSecret().let { _sharedSecret ->
            val messageDigest = MessageDigest.getInstance(KeyProperties.DIGEST_SHA256).apply {
                update(_sharedSecret)
            }
            val hash = messageDigest.digest(random)
            SecretKeySpec(
                hash,
                KeyProperties.KEY_ALGORITHM_AES
            ).let { secretKeySpec ->
                sharedSecretKey = Base64.encodeToString(secretKeySpec.encoded, Base64.DEFAULT)
            }
            //TODO 메모리 초기화
            //hash
            //private
        }
        encryptedSharedPreferencesManager.putString(keyId, sharedSecretKey)

        //TODO 메모리 초기화
        //sharedSecretKey

        return keyId
    }

    //오버로드 for 테스트 앱
    //userId 는 privateKey를 가져오기 위해 필요함
    @Throws(Exception::class)
    fun generateSharedSecretKey(
        userId:String,
        publicKey: PublicKey,
        nonce: String
    ): String {
        val keyId: String = nonce
        val random: ByteArray = Base64.decode(nonce, Base64.DEFAULT)

        //TODO

        val privateKey: PrivateKey
        androidKeyStore.getEntry(userId, null).let { keyStoreEntry ->
            privateKey = (keyStoreEntry as KeyStore.PrivateKeyEntry).privateKey
        }

        var sharedSecretKey: String
        KeyAgreement.getInstance("ECDH").apply {
            init(privateKey)
            doPhase(publicKey, true)
        }.generateSecret().let { _sharedSecret ->
            val messageDigest = MessageDigest.getInstance(KeyProperties.DIGEST_SHA256).apply {
                update(_sharedSecret)
            }
            val hash = messageDigest.digest(random)
            SecretKeySpec(
                hash,
                KeyProperties.KEY_ALGORITHM_AES
            ).let { secretKeySpec ->
                sharedSecretKey = Base64.encodeToString(secretKeySpec.encoded, Base64.DEFAULT)
            }
            //TODO 메모리 초기화
            //hash
            //private
        }
        encryptedSharedPreferencesManager.putString(keyId, sharedSecretKey)

        //TODO 메모리 초기화
        //sharedSecretKey

        return keyId
    }






//    fun generateSharedSecretKey(pointX: String, pointY: String, nonce: String): String? {
//        var publicKey = assemble(pointX, pointY)
//        if (publicKey != null) {
//            return generateSharedSecretKey(publicKey, nonce)
//        }
//
//        return null
//    }
//
//    fun generateSharedSecretKey(publicKey: String, nonce: String): String? {
//        if (publicKey.startsWith("04", false)) {
//            val pointX = publicKey.substring(1 .. 33)
//            val pointY = publicKey.substring(33)
//            return generateSharedSecretKey(pointX, pointY, nonce)
//        }
//
//        return null
//    }

    /**
     * keyId에 해당하는 비밀 키를 삭제
     *
     * @param keyId 삭제할 비밀키의 식별자
     * @return 비밀키가 안전하게 삭제되었다면 true. 그렇지 않다면, false 를 반환
     * @exception
     * 비밀 키 삭제 실패 시 false 반환
     */
    fun deleteSharedSecretKey(keyId: String): Boolean {
        encryptedSharedPreferencesManager.apply {
            try {
                remove(keyId)
            }
            catch (e: Exception) {
                return false
            }
            //해당 키가 지워졌는지 확인하고 지워졌다면 true, 아니라면 false 반환
            //해당 작업이 불필요하다면 try 블럭에서 바로 true 반환할 것
            getString(keyId, "").let { result ->
                return result == ""
            }
        }
    }

    /**
     * 메시지를 암호화
     *
     * @param message 암호화 전 원본 메시지
     * @param keyId 비밀키를 가져오기 위해 필요한 식별자
     * @return 암호화된 메시지를 반환
     * @exception
     * User Exception 정의. 비밀키를 정상적으로 가져오지 못했을 경우. keyId 가 유효하지 않는 경우
     */
    @Throws(Exception::class)
    fun encrypt(message: String, keyId: String): String {
        var encodedSharedSecretKey: String? =
            if (encryptedSharedPreferencesManager.getString(keyId, "") == "") null
            else encryptedSharedPreferencesManager.getString(keyId, "")

        val encryptedMessage: String
        Base64.decode(encodedSharedSecretKey, Base64.DEFAULT).let { decodedKey ->
            SecretKeySpec(
                decodedKey,
                0,
                decodedKey.size,
                KeyProperties.KEY_ALGORITHM_AES
            ).let { secretKeySpec ->
                val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
                cipher.init(
                    Cipher.ENCRYPT_MODE,
                    secretKeySpec,
                    IvParameterSpec(iv)
                )
                cipher.doFinal(message.toByteArray()).let { _encryptedMessage ->
                    encryptedMessage = Base64.encodeToString(_encryptedMessage, Base64.DEFAULT)
                }
            }
        }
        //TODO 메모리 초기화
        //encodedSharedSecretKey = generateRandom(32)

        return encryptedMessage
    }

    /**
     * 메시지를 복호화
     *
     * @param message 암호화된 메시지
     * @param keyId 비밀키를 가져오기 위해 필요한 식별자
     * @return 복호화된 원본 메시지를 반환
     * @exception
     * User Exception 정의. 비밀키를 정상적으로 가져오지 못했을 경우.
     */
    @Throws(Exception::class)
    fun decrypt(message: String, keyId: String): String {
        var encodedSharedSecretKey: String? =
            if (encryptedSharedPreferencesManager.getString(keyId, "") == "") null
            else encryptedSharedPreferencesManager.getString(keyId, "")

        var decryptedMessage: ByteArray
        Base64.decode(encodedSharedSecretKey, Base64.DEFAULT).let { decodedKey ->
            SecretKeySpec(
                decodedKey,
                0,
                decodedKey.size,
                KeyProperties.KEY_ALGORITHM_AES
            ).let { secretKeySpec ->
                val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
                cipher.init(
                    Cipher.DECRYPT_MODE,
                    secretKeySpec,
                    IvParameterSpec(iv)
                )
                Base64.decode(message, Base64.DEFAULT).let { decryption ->
                    decryptedMessage = cipher.doFinal(decryption)
                }
            }
        }
        //TODO 메모리 초기화
        //encodedSharedSecretKey = generateRandom(32)

        return String(decryptedMessage)
    }
}