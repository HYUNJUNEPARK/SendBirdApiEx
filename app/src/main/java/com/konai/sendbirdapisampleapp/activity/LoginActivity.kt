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
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.konai.sendbirdapisampleapp.R
import com.konai.sendbirdapisampleapp.databinding.ActivityLoginBinding
import com.konai.sendbirdapisampleapp.strongbox.KeyStoreUtil
import com.konai.sendbirdapisampleapp.util.Constants.APP_ID
import com.konai.sendbirdapisampleapp.util.Constants.FIRE_STORE_DOCUMENT_PUBLIC_KEY
import com.konai.sendbirdapisampleapp.util.Constants.FIRE_STORE_FIELD_AFFINE_X
import com.konai.sendbirdapisampleapp.util.Constants.FIRE_STORE_FIELD_AFFINE_Y
import com.konai.sendbirdapisampleapp.util.Constants.FIRE_STORE_FIELD_USER_ID
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
import java.security.PublicKey
import java.security.interfaces.ECPublicKey

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private var db: FirebaseFirestore? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)

        binding.loginActivity = this
        initializeSendBirdSdk()
        db = Firebase.firestore
        updateUiKitLaunchButtonState()
    }

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
            }

            updatePublicKeyOnServer(USER_ID)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        db = null
    }

    private fun updatePublicKeyOnServer(userId: String) {
        db?.collection(FIRE_STORE_DOCUMENT_PUBLIC_KEY)
            ?.get()
            ?.addOnSuccessListener { result ->
                if (result.isEmpty) {
                    Log.i(TAG, "Empty Server")
                    syncKeyStoreWithServer(userId, result)
                    return@addOnSuccessListener
                }
                for (document in result) {
                    if (document.data[FIRE_STORE_FIELD_USER_ID] == userId) {
                        Log.i(TAG, "서버에 키 있음")
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        return@addOnSuccessListener
                    }
                }
                Log.i(TAG, "서버에 키 없음")
                syncKeyStoreWithServer(userId, result)
            }
            ?.addOnFailureListener { exception ->
                showToast("키 가져오기 실패")
                Log.e(TAG, "Error getting documents from firestore : $exception")
            }
    }

    private fun syncKeyStoreWithServer(userId: String, result: QuerySnapshot) {
        if (KeyStoreUtil().isKeyPairInKeyStore(userId)) {
            Log.i(TAG, "키스토어에 키 있음")
            KeyStoreUtil().getPublicKeyFromKeyStore(userId)?.let { publicKey ->
                updatePublicKeyAffineXYToServer(userId, publicKey)
                Log.i(TAG, "서버에 키 업로드 완료")
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                return
            }
        }
        else {
            Log.i(TAG, "키스토어에 키 없음")
            //키 스토어에 키생성
            KeyStoreUtil().createKeyPairToKeyStore(userId)

            //키스토어에서 퍼블릭 키 가져와 서버에 키 업로드
            KeyStoreUtil().getPublicKeyFromKeyStore(userId)?.let { publicKey ->
                updatePublicKeyAffineXYToServer(userId, publicKey)
                Log.i(TAG, "서버에 키 업로드 완료")
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                return
            }
        }
    }

    private fun updatePublicKeyAffineXYToServer(userId: String, publicKey: PublicKey) {
        val ecPublicKey = publicKey as ECPublicKey
        val user = hashMapOf(
            FIRE_STORE_FIELD_USER_ID to userId,
            FIRE_STORE_FIELD_AFFINE_X to ecPublicKey.w.affineX.toString(),
            FIRE_STORE_FIELD_AFFINE_Y to ecPublicKey.w.affineY.toString()
        )
        db?.collection(FIRE_STORE_DOCUMENT_PUBLIC_KEY)
            ?.add(user)
            ?.addOnSuccessListener {
                showToast("키 업로드 성공")
            }
            ?.addOnFailureListener { e ->
                Log.e(TAG, "Error adding document", e)
                showToast("키 업로드 실패")
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