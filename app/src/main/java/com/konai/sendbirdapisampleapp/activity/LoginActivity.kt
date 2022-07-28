package com.konai.sendbirdapisampleapp.activity

import android.content.Intent
import android.content.pm.PackageInfo
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.konai.sendbirdapisampleapp.R
import com.konai.sendbirdapisampleapp.databinding.ActivityLoginBinding
import com.konai.sendbirdapisampleapp.strongbox.KeyPairModel
import com.konai.sendbirdapisampleapp.strongbox.KeyStoreUtil
import com.konai.sendbirdapisampleapp.util.Constants.APP_ID
import com.konai.sendbirdapisampleapp.util.Constants.INTENT_NAME_USER_ID
import com.konai.sendbirdapisampleapp.util.Constants.INTENT_NAME_USER_NICK
import com.konai.sendbirdapisampleapp.util.Constants.MY_APP_INTENT_ACTION
import com.konai.sendbirdapisampleapp.util.Constants.SENDBIRD_UI_KIT_APP
import com.konai.sendbirdapisampleapp.util.Constants.TAG
import com.konai.sendbirdapisampleapp.util.Constants.USER_ID
import com.konai.sendbirdapisampleapp.util.Constants.USER_NICKNAME
import com.konai.sendbirdapisampleapp.util.Extension.showToast
import com.sendbird.android.SendbirdChat
import com.sendbird.android.exception.SendbirdException
import com.sendbird.android.handler.InitResultHandler
import com.sendbird.android.params.InitParams
import com.sendbird.android.params.UserUpdateParams
import java.math.BigInteger
import java.security.KeyFactory
import java.security.PublicKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private var keyPair: KeyPairModel? = null
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)

        binding.loginActivity = this
        initializeSendBirdSdk()
        db = Firebase.firestore
        updateUiKitLaunchButtonState()

        //TEST
        updatePublicKey()
        getPublicKey()
        deleteKey()
    }


    /////[Start Firebase]
    fun updatePublicKey() {
        val userId = "userA"
        binding.publicKeyButton.setOnClickListener {
            //키스토어에 해당 키가 있는지 확인하고 키 있으면 키생성 안함
            //키 없으면 키 생성하고 서버에 올림
            if (KeyStoreUtil().isKeyPairInKeyStore(userId)) {
                showToast("키스토어에 키 있음")
                return@setOnClickListener
            }
            showToast("키스토어에 키 없음")

            //TODO coroutine suspend 처리
            KeyStoreUtil().updateKeyPairToKeyStore(userId)
            KeyStoreUtil().getPublicKeyFromKeyStore(userId)?.let { publicKey ->
                updatePublicKeyXYToServer(publicKey)
            }
        }
    }

    private fun updatePublicKeyXYToServer(publicKey: PublicKey) {
        val userId = "userA"
        val publicKey = publicKey as ECPublicKey
        val user = hashMapOf(
            "userId" to userId,
            "affineX" to publicKey.w.affineX.toString(),
            "affineY" to publicKey.w.affineY.toString()
        )
        db.collection("publicKey")
            .add(user)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding document", e)
            }
    }


    fun getPublicKey() {
        binding.loadPublicKeyButton.setOnClickListener {
            //TODO 키스토어에 키가 있는지 확인
            val userId = "userA"
            var affineX: BigInteger? = null
            var affineY: BigInteger? = null
            var publicKey: PublicKey? = null

            db.collection("publicKey")
                .get()
                .addOnSuccessListener { result ->
                    showToast("키 가져오기 성공")
                    for (document in result) {
                        if (document.data["userId"] == "userA") {
                            affineX = BigInteger(document.data["affineX"].toString())
                            affineY = BigInteger(document.data["affineY"].toString())

                            //TODO make publickey
                            //https://stackoverflow.com/questions/30116758/generating-publickey-from-x-and-y-values-of-elliptic-curve-point

                            val ecPoint = ECPoint(affineX, affineY)
                            val keySpec = ECPublicKeySpec(ecPoint, null) //TODO Check params

                            val keyFactory = KeyFactory.getInstance("EC")
                            val publicKey: PublicKey = keyFactory.generatePublic(keySpec)
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    showToast("키 가져오기 실패")
                    Log.w(TAG, "Error getting documents.", exception)
                }
        }
    }

    fun deleteKey() {
        binding.deleteKeyButton.setOnClickListener {
            val userId = "userA"
            KeyStoreUtil().deleteKeyStoreKeyPair(userId)
        }
    }
    ////[End Firebase]







    private fun initializeSendBirdSdk() {
        SendbirdChat.init(
            InitParams(APP_ID, this, useCaching = true),
            object : InitResultHandler {
                override fun onMigrationStarted() {
                    Log.i(TAG, "initializeChatSdk: Called when there's an update in Sendbird server.")
                }
                override fun onInitFailed(e: SendbirdException) {
                    Log.e(TAG,"initializeChatSdk : Called when initialize failed. $e \n SDK will still operate properly as if useLocalCaching is set to false.")
                }
                override fun onInitSucceed() {
                    binding.loginButton.isEnabled = true
                    showToast("Called when initialization is completed.")
                    Log.i(TAG, "initializeChatSdk : Called when initialization is completed.")
                }
            }
        )
    }

    fun onLogInButtonClicked() {
        val _userId = binding.userIdEditText.text.toString()
        val userId = if(_userId.isNotEmpty()) binding.userIdEditText.text.toString() else return

        SendbirdChat.connect(userId) { user, e ->
            val userInputNickName = binding.nickNameEditText.text.toString()
            USER_NICKNAME = userInputNickName.ifEmpty { userId }
            USER_ID = user?.userId.toString()

            if (e != null) {
                showToast("로그인 에러 : $e")
                Log.e(TAG, ": $e")
                return@connect
            }
            val params = UserUpdateParams().apply {
                nickname = USER_NICKNAME
            }
            SendbirdChat.updateCurrentUserInfo(params) { e ->
                if (e != null)  {
                    Log.e(TAG, ": updateCurrentUserInfo Error : $e")
                    showToast("유저 닉네임 업데이트 에러 : $e")
                    return@updateCurrentUserInfo
                }
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
        }
    }

    fun onUiKitLaunchButtonClicked() {
        val userId = binding.userIdEditText.text.toString()
        val userNick = binding.nickNameEditText.text.toString().ifEmpty { userId }
        val intent = packageManager.getLaunchIntentForPackage(SENDBIRD_UI_KIT_APP)?.run {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            action = MY_APP_INTENT_ACTION
            putExtra(INTENT_NAME_USER_ID, userId)
            putExtra(INTENT_NAME_USER_NICK, userNick)
        }
        startActivity(intent)
    }


    private fun updateUiKitLaunchButtonState() {
        val userId = binding.userIdEditText
        userId.addTextChangedListener(
            object: TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
                override fun afterTextChanged(p0: Editable?) {
                    val userId = binding.userIdEditText.text
                    //editText 에 userId 와 디바이스에 uiKit 앱이 설치되어있어야 UiKitAppLaunchButton 활성화
                    binding.UiKitAppLaunchButton.isEnabled = userId.isNotEmpty() && isUiKitAppOnMyDevice()
                }
            }
        )
    }

    private fun isUiKitAppOnMyDevice(): Boolean {
        var isExist = false
        val packageManager = packageManager
        val packages: List<PackageInfo> = packageManager.getInstalledPackages(0)
        try {
            for (info: PackageInfo in packages) {
                if (info.packageName == SENDBIRD_UI_KIT_APP) {
                    isExist = true
                    break
                }
            }
        }
        catch (e: Exception) {
            isExist = false
        }
        return isExist
    }
}