package com.konai.sendbirdapisampleapp.activity

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.konai.sendbirdapisampleapp.R
import com.konai.sendbirdapisampleapp.databinding.ActivityChannelBinding
import com.konai.sendbirdapisampleapp.util.Constants.CHANNEL_ACTIVITY_INTENT_ACTION
import com.konai.sendbirdapisampleapp.util.Constants.INTENT_NAME_CHANNEL_URL
import com.konai.sendbirdapisampleapp.util.Constants.TAG
import com.konai.sendbirdapisampleapp.util.Extension.toast
import com.sendbird.android.channel.GroupChannel
import com.sendbird.android.params.UserMessageCreateParams

class ChannelActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChannelBinding
    private lateinit var channelURL: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_channel)

        if (intent.action == CHANNEL_ACTIVITY_INTENT_ACTION) {
            channelURL = intent.getStringExtra(INTENT_NAME_CHANNEL_URL)!!
        }
        binding.channelActivity = this
    }

    fun sendButtonClicked() {
        val userInput: String = binding.messageEditText.text.toString()
        val params = UserMessageCreateParams(userInput)
        GroupChannel.getChannel(channelURL) { groupChannel, e ->
            if (e != null) {
                toast("Error : $e")
                Log.e(TAG, "getChannel Error: $e", )
            }
            groupChannel?.sendUserMessage(params) { message, e ->
                if (e != null) {
                    toast("Error : $e")
                    Log.e(TAG, "sendUserMessage Error: $e", )
                }
                else {
                    toast("메시지 전송")
                }
            }
        }
    }
}