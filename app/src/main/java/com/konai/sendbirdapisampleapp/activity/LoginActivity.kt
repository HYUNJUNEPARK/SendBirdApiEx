package com.konai.sendbirdapisampleapp.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.konai.sendbirdapisampleapp.Constants
import com.konai.sendbirdapisampleapp.Constants.USER_ID
import com.konai.sendbirdapisampleapp.Constants.USER_NICKNAME
import com.konai.sendbirdapisampleapp.R
import com.konai.sendbirdapisampleapp.Util.toast
import com.konai.sendbirdapisampleapp.databinding.ActivityLoginBinding
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
        initializeChatSdk()
    }

    private fun initializeChatSdk() {
        SendbirdChat.init(
            InitParams(Constants.APP_ID, this, useCaching = true),
            object : InitResultHandler {
                override fun onMigrationStarted() {
                    Log.i(Constants.TAG, "initializeChatSdk: Ca lled when there's an update in Sendbird server.")
                }

                override fun onInitFailed(e: SendbirdException) {
                    Log.e(Constants.TAG,"initializeChatSdk : Called when initialize failed. $e \n SDK will still operate properly as if useLocalCaching is set to false.")
                }

                override fun onInitSucceed() {
                    binding.loginButton.isEnabled = true
                    toast("Called when initialization is completed.")
                    Log.i(Constants.TAG, "initializeChatSdk : Called when initialization is completed.")
                }
            }
        )
    }

    //TODO ProgressBar, ASYNC
    fun logInButtonClicked() {
        val _userId = binding.userIdEditText.text.toString()
        val userId = if(_userId != "") binding.userIdEditText.text.toString() else return

        SendbirdChat.connect(userId) { user, e ->
            val userInputNickName = binding.nickNameEditText.text.toString()
            USER_NICKNAME = if(userInputNickName == "") "-" else userInputNickName
            USER_ID = user?.userId.toString()

            if (e != null) {
                toast("로그인 에러 : $e")
                Log.e(Constants.TAG, ": $e")
                return@connect
            }

            val params = UserUpdateParams().apply {
                nickname = USER_NICKNAME
            }

            SendbirdChat.updateCurrentUserInfo(params) { e ->
                Log.e(Constants.TAG, ": updateCurrentUserInfo Error : $e")
            }

            val intent = Intent(this, MainActivity2::class.java)
            startActivity(intent)
            finish()
        }
    }
}