package com.konai.sendbirdapisampleapp.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.konai.sendbirdapisampleapp.R
import com.konai.sendbirdapisampleapp.databinding.ActivityLoginBinding
import com.konai.sendbirdapisampleapp.strongbox.ECKeyUtil
import com.konai.sendbirdapisampleapp.strongbox.StrongBox
import com.konai.sendbirdapisampleapp.Constants.FIRESTORE_DOCUMENT_PUBLIC_KEY
import com.konai.sendbirdapisampleapp.Constants.FIRESTORE_FIELD_USER_ID
import com.konai.sendbirdapisampleapp.Constants.SENDBIRD_API_KEY
import com.konai.sendbirdapisampleapp.Constants.USER_ID
import com.konai.sendbirdapisampleapp.Constants.USER_NICKNAME
import com.sendbird.android.SendbirdChat
import com.sendbird.android.exception.SendbirdException
import com.sendbird.android.handler.InitResultHandler
import com.sendbird.android.params.InitParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.PublicKey
import kotlin.coroutines.CoroutineContext

class LoginActivity : AppCompatActivity(), CoroutineScope {
    private lateinit var strongBox: StrongBox
    private lateinit var binding: ActivityLoginBinding
    private var remoteDB: FirebaseFirestore? = null
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = DataBindingUtil.setContentView(this, R.layout.activity_login)
            binding.loginActivity = this
            strongBox = StrongBox.getInstance(this)
            remoteDB = Firebase.firestore

            launch {
                initSendBirdSdk()
            }
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        remoteDB = null
    }

    //SendBirdSDK 초기화
    private suspend fun initSendBirdSdk() = withContext(Dispatchers.IO) {
        SendbirdChat.init(
            InitParams(
                SENDBIRD_API_KEY,
                this@LoginActivity,
                useCaching = true
            ),
            object : InitResultHandler {
                override fun onMigrationStarted() { }
                override fun onInitFailed(e: SendbirdException) {
                    e.printStackTrace()
                }
                override fun onInitSucceed() {
                    binding.loginButton.isEnabled = true
                }
            }
        )
    }

    //로그인 버튼 클릭 -> 사용자 센드버드 서버 로그인
    fun signIn() {
        binding.progressBarLayout.visibility = View.VISIBLE
        val userId = binding.userIdEditText.text.toString().ifEmpty { return }
        try {
            
        }
        catch (e: SendbirdException) {
            if (e.code == 800190) {
                Toast.makeText(this, "로그인 시간 초과. \n다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
        
        SendbirdChat.connect(userId) { user, e ->
            if (e != null) {
                binding.progressBarLayout.visibility = View.GONE
                e.printStackTrace()
                return@connect
            }

            USER_NICKNAME = binding.nickNameEditText.text.toString().ifEmpty { userId }
            USER_ID = user?.userId.toString()

            launch {
                signInAlgorithm(USER_ID)
            }
        }
    }

    /**
     * 최초 등록자인지, 서버 PublicKey 등록 상태, KeyStore ECKeyPair 등록 상태에 따라 계정 등록 및 키 등록 알고리즘 실행
     * @param userId 사용자 id. ECKeyPair 식별자
     */
    private suspend fun signInAlgorithm(userId: String) = withContext(Dispatchers.IO) {
        remoteDB!!.collection(FIRESTORE_DOCUMENT_PUBLIC_KEY)
            .get()
            .addOnSuccessListener { result ->
                //1.Firestore 에 데이터가 없을 때 (따로 처리 안해주면 앱 크래시)
                if (result.isEmpty) {
                    generateECKeyPair(userId)
                    return@addOnSuccessListener
                }
                //2.Firestore 에 데이터가 있을 때
                for (document in result) {
                    //2.1. 로그인하려는 사용자의 publicKey 가 서버에 등록되어 있을 때
                    if (document.data[FIRESTORE_FIELD_USER_ID] == userId) {
                        binding.progressBarLayout.visibility = View.GONE
                        startActivity(
                            Intent(this@LoginActivity, MainActivity::class.java)
                        )
                        return@addOnSuccessListener
                    }
                }
                //2.2. 로그인하려는 사용자의 publicKey 가 서버에 없을 때, 키스토어/서버 키를 업로드
                generateECKeyPair(userId)
            }
            .addOnFailureListener { e ->
                binding.progressBarLayout.visibility = View.GONE
                e.printStackTrace()
            }
    }

    /**
     * 키스토어에 사용자 ECKeyPair 등록 여부 따라 ECKeyPair 생성
     * @param userId 사용자 id. ECKeyPair 의 식별자로 사용
     */
    private fun generateECKeyPair(userId: String) {
        try {
            //1. 키스토어에 키 있는 경우
            if (ECKeyUtil.isECKeyPair(userId)) {
                //1.1. 키스토어에서 publicKey 를 가져와 서버에 등록
                enrollPublicKey(
                    userId,
                    publicKey = strongBox.getECPublicKey(userId)
                )
                binding.progressBarLayout.visibility = View.GONE
                //1.1.2. 로그인(액티비티 이동)
                startActivity(
                    Intent(this, MainActivity::class.java)
                )
                return
            }
            //2. 키스토어 키 없는 경우
            else {
                //2.1. ECKeyPair 생성
                strongBox.generateECKeyPair(userId)

                //2.1.1. 키스토어에서 publicKey 를 가져와 서버에 등록
                enrollPublicKey(
                    userId,
                    publicKey = strongBox.getECPublicKey(userId)
                )
                binding.progressBarLayout.visibility = View.GONE
                //2.1.1.1. 로그인(액티비티 이동)
                startActivity(
                    Intent(this, MainActivity::class.java)
                )
                return
            }
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 사용자의 publicKey 로 부터 affineX, affineY 를 생성해 Firebase DB에 업로드
     * @param userId publicKey 를 등록하려는 사용자의 id
     * @param publicKey 사용자의 PublicKey
     */
    private fun enrollPublicKey(userId: String, publicKey: PublicKey) {
        try {
            ECKeyUtil.extractAffineXY(userId, publicKey).let { hashMap ->
                remoteDB!!.collection(FIRESTORE_DOCUMENT_PUBLIC_KEY)
                    .add(hashMap)
                    .addOnSuccessListener {
                        Toast.makeText(this, "퍼블릭키 업로드", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        binding.progressBarLayout.visibility = View.GONE
                        e.printStackTrace()
                        finish()
                    }
            }
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
    }
}