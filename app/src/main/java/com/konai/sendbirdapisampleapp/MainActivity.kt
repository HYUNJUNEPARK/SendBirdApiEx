package com.konai.sendbirdapisampleapp

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.konai.sendbirdapisampleapp.databinding.ActivityMainBinding
import com.sendbird.android.SendbirdChat
import com.sendbird.android.exception.SendbirdException
import com.sendbird.android.handler.InitResultHandler
import com.sendbird.android.params.InitParams

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "applicationLog"
        const val APP_ID = "D4FCF442-A653-49B3-9D87-6134CD87CA81"
    }
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        initializeChatSdk()
    }

    fun Context.toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    fun className(): String {
        return localClassName
    }

    private fun initializeChatSdk() {
        SendbirdChat.init(
            InitParams(APP_ID, applicationContext, useCaching = true),
            object : InitResultHandler {
                override fun onMigrationStarted() {
                    Log.i(TAG, "${className()} : Called when there's an update in Sendbird server.")
                }

                override fun onInitFailed(e: SendbirdException) {
                    Log.e(TAG, "${className()} : Called when initialize failed. $e \n SDK will still operate properly as if useLocalCaching is set to false.")
                }

                override fun onInitSucceed() {
                    toast("Called when initialization is completed.")
                    Log.i(TAG, "${className()} : Called when initialization is completed.")
                }
            }
        )
    }
}