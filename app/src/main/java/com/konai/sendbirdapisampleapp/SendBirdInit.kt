package com.konai.sendbirdapisampleapp

import android.content.Context
import android.util.Log
import com.konai.sendbirdapisampleapp.Util.toast
import com.sendbird.android.SendbirdChat
import com.sendbird.android.exception.SendbirdException
import com.sendbird.android.handler.InitResultHandler
import com.sendbird.android.params.InitParams

class SendBirdInit {
    //TODO ASYNC By Coroutine
    fun initializeChatSdk(context: Context) {
        SendbirdChat.init(
            InitParams(MainActivity.APP_ID, context, useCaching = true),
            object : InitResultHandler {
                override fun onMigrationStarted() {
                    Log.i(MainActivity.TAG, "initializeChatSdk: Called when there's an update in Sendbird server.")
                }

                override fun onInitFailed(e: SendbirdException) {
                    Log.e(MainActivity.TAG,"initializeChatSdk : Called when initialize failed. $e \n SDK will still operate properly as if useLocalCaching is set to false.")
                }

                override fun onInitSucceed() {
                    context.toast("Called when initialization is completed.")
                    Log.i(MainActivity.TAG, "initializeChatSdk : Called when initialization is completed.")
                }
            }
        )
    }
}