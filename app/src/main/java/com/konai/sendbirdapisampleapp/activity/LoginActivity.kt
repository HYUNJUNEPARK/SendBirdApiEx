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

        db = Firebase.firestore
        initializeSendBirdSdk()
        initUiKitLaunchButtonState()
    }

    override fun onDestroy() {
        super.onDestroy()
        db = null
    }

//[START init]
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

    //1)editText 에 userId 와 2)디바이스에 uiKit 앱이 설치되어있어야 UiKitAppLaunchButton 활성화
    private fun initUiKitLaunchButtonState() {
        binding.userIdEditText.addTextChangedListener(
            object: TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
                override fun afterTextChanged(p0: Editable?) {
                    val userId = binding.userIdEditText.text
                    val isUiKitAppOnMyDevice: () -> Boolean = {
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
                            showToast("Can't try to search Ui kit app error : $e")
                            Log.e(TAG, "Can't try to search Ui kit app error : $e")
                            isExist = false
                        }
                        isExist
                    }
                    binding.UiKitAppLaunchButton.isEnabled = userId.isNotEmpty() && isUiKitAppOnMyDevice()
                }
            }
        )
    }
//[END init]

//[START Click Event]
    fun onLogInButtonClicked() {
        val userId = binding.userIdEditText.text.toString().ifEmpty { return }
        if (db == null) {
            showToast("firebase DB is not initialized")
            Log.e(TAG, "firebase DB is not initialized")
            return
        }




        //TODO progressbar




        SendbirdChat.connect(userId) { user, e ->
            USER_NICKNAME = binding.nickNameEditText.text.toString().ifEmpty { userId }
            USER_ID = user?.userId.toString()

            if (e != null) {
                showToast("로그인 에러 : $e")
                Log.e(TAG, ": $e")
                return@connect
            }

            //update user nickname
            val params = UserUpdateParams().apply {
                nickname = USER_NICKNAME
            }
            SendbirdChat.updateCurrentUserInfo(params) { exception ->
                if (exception != null)  {
                    Log.e(TAG, ": updateCurrentUserInfo Error : $exception")
                    showToast("유저 닉네임 업데이트 에러 : $exception")
                    return@updateCurrentUserInfo
                }
            }
            updatePublicKeyOnServer(USER_ID)
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
//[END Click Event]

//[START Firestore: Public Key]
    private fun updatePublicKeyOnServer(userId: String) {
        db!!.collection(FIRE_STORE_DOCUMENT_PUBLIC_KEY)
            .get()
            .addOnSuccessListener { result ->
                //Firestore 에 어떤 데이터도 없을 때(따로 처리 안해주면 앱 크래시)
                if (result.isEmpty) {
                    Log.i(TAG, "Empty Firebase DB")
                    updatePublicKeyToKeyStoreAndServer(userId)
                    return@addOnSuccessListener
                }

                //Firestore 에 데이터가 하나라도 있을 때
                for (document in result) {
                    //1)로그인 하려는 사용자의 퍼블릭키가 업로드 되어 있을 때 -> 로그인
                    if (document.data[FIRE_STORE_FIELD_USER_ID] == userId) {
                        Log.i(TAG, "서버에 퍼블릭키 있음")
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        return@addOnSuccessListener
                    }
                }
                //2)로그인 하려는 사용자의 퍼블릭키가 없을 때 -> 키스토어/서버 키를 업로드 -> 로그인
                Log.i(TAG, "서버에 퍼블릭키 없음")
                updatePublicKeyToKeyStoreAndServer(userId)
            }
            .addOnFailureListener { exception ->
                showToast("키 가져오기 실패")
                Log.e(TAG, "Error getting documents from firebase DB : $exception")
            }
    }

    private fun updatePublicKeyToKeyStoreAndServer(userId: String) {
        //키스토어에 키 있는 경우 -> 키스토어에서 퍼블릭 키를 가져와 서버 업데이트 -> 로그인
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
        //키스토어 키 없는 경우 -> 키쌍 생성 후 키스토어에 업데이트 -> 키스토어에서 퍼블릭 키를 가져와 서버 업데이트 -> 로그인
        else {
            Log.i(TAG, "키스토어에 키 없음")
            KeyStoreUtil().createKeyPairToKeyStore(userId)
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
        db!!.collection(FIRE_STORE_DOCUMENT_PUBLIC_KEY)
            .add(user)
            .addOnSuccessListener {
                showToast("키 업로드 성공")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error adding document", e)
                showToast("키 업로드 실패")
            }
    }
//[END Firestore: Public Key]
}