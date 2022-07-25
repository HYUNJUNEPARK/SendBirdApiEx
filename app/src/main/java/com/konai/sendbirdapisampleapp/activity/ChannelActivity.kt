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
import com.konai.sendbirdapisampleapp.util.Constants.USER_ID
import com.konai.sendbirdapisampleapp.util.Constants.USER_NICKNAME
import com.konai.sendbirdapisampleapp.util.Extension.toast
import com.sendbird.android.channel.GroupChannel
import com.sendbird.android.params.UserMessageCreateParams
import com.sendbird.android.user.Member

class ChannelActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChannelBinding
    private lateinit var channelURL: String
    private lateinit var userId: String
    private lateinit var userNickname: String
    private var partnerId: String? = null
    private var partnerNickname: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_channel)
        binding.channelActivity = this

        if (intent.action == CHANNEL_ACTIVITY_INTENT_ACTION) {
            channelURL = intent.getStringExtra(INTENT_NAME_CHANNEL_URL)!!
        }
        initChannelPartnerInfo()
    }

    //채널에 참여하고 있는 멤버 리스트를 가져와 view 초기화
    private fun initChannelPartnerInfo() {
        GroupChannel.getChannel(channelURL) { groupChannel, e ->
            if (e != null) {
                Log.e(TAG, "Get Channel Activity Error: $e")
                toast("Get Channel Activity Error: $e")
                return@getChannel
            }
            val members: List<Member> = groupChannel!!.members
            for (member in members) {
                if (member.userId != USER_ID) {
                    partnerId = member.userId
                    partnerNickname = member.nickname
                }
            }
            if (partnerId == null) binding.userIdTextView.text = "${USER_NICKNAME}($USER_ID)"
            else binding.userIdTextView.text = "${partnerNickname}(${partnerId})"
        }
    }

    fun onSendButtonClicked() {
        val userInput: String = binding.messageEditText.text.toString()
        val params = UserMessageCreateParams(userInput)
        GroupChannel.getChannel(channelURL) { groupChannel, e ->
            if (e != null) {
                toast("Error : $e")
                Log.e(TAG, "getChannel Error: $e")
            }
            groupChannel?.sendUserMessage(params) { message, e ->
                if (e != null) {
                    toast("Error : $e")
                    Log.e(TAG, "sendUserMessage Error: $e")
                } else {
                    toast("메시지 전송 완료")
                    binding.messageEditText.text = null
                }
            }
        }
    }
}