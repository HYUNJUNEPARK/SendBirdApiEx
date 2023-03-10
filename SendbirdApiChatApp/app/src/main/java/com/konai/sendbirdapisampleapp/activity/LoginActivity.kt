package com.konai.sendbirdapisampleapp.activity

import android.content.Intent
import android.content.pm.PackageInfo
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.konai.sendbirdapisampleapp.R
import com.konai.sendbirdapisampleapp.databinding.ActivityLoginBinding
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

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)

        binding.loginActivity = this
        initializeSendBirdSdk()
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
                showToast("????????? ?????? : $e")
                Log.e(TAG, ": $e")
                return@connect
            }
            val params = UserUpdateParams().apply {
                nickname = USER_NICKNAME
            }
            SendbirdChat.updateCurrentUserInfo(params) { e ->
                if (e != null)  {
                    Log.e(TAG, ": updateCurrentUserInfo Error : $e")
                    showToast("?????? ????????? ???????????? ?????? : $e")
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
                    //editText ??? userId ??? ??????????????? uiKit ?????? ????????????????????? UiKitAppLaunchButton ?????????
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