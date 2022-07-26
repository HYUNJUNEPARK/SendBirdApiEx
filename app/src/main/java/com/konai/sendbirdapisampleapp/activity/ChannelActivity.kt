package com.konai.sendbirdapisampleapp.activity

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.konai.sendbirdapisampleapp.R
import com.konai.sendbirdapisampleapp.databinding.ActivityChannelBinding
import com.konai.sendbirdapisampleapp.model.MessageModel
import com.konai.sendbirdapisampleapp.util.Constants.CHANNEL_ACTIVITY_INTENT_ACTION
import com.konai.sendbirdapisampleapp.util.Constants.INTENT_NAME_CHANNEL_URL
import com.konai.sendbirdapisampleapp.util.Constants.TAG
import com.konai.sendbirdapisampleapp.util.Constants.USER_ID
import com.konai.sendbirdapisampleapp.util.Constants.USER_NICKNAME
import com.konai.sendbirdapisampleapp.util.Extension.toast
import com.sendbird.android.SendbirdChat
import com.sendbird.android.channel.BaseChannel
import com.sendbird.android.channel.GroupChannel
import com.sendbird.android.handler.GroupChannelHandler
import com.sendbird.android.message.BaseMessage
import com.sendbird.android.message.UserMessage
import com.sendbird.android.params.PreviousMessageListQueryParams
import com.sendbird.android.params.UserMessageCreateParams
import com.sendbird.android.user.Member

class ChannelActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChannelBinding
    private lateinit var channelURL: String
    private var _messageList: MutableList<MessageModel> = mutableListOf()
    private var partnerNickname: String? = null
    private var partnerId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_channel)
        binding.channelActivity = this

        if (intent.action == CHANNEL_ACTIVITY_INTENT_ACTION) {
            channelURL = intent.getStringExtra(INTENT_NAME_CHANNEL_URL)!!
        }
        initChannelPartnerInfo()
        readAllMessages()

        //TODO Receive messages through a channel event handler
        SendbirdChat.addChannelHandler(
            "UNIQUE_HANDLER_ID",
            object : GroupChannelHandler() {
                override fun onMessageReceived(channel: BaseChannel, message: BaseMessage) {
                    //Log.d(TAG, "onMessageReceived: $channel // $message")
                    when (message) {
                        is UserMessage -> {
                            //TODO 상대방이 메시지 보낼 때 마다 호출됨 -> 뷰 다시 그려주면 될 듯
                            toast("${message.message}")
                            Log.d(TAG, "--Message--: ${message.message}")
                        }
                    }
                }
            }
        )
    }

    //TODO 최초 한번만 실행되면 될 듯 -> 실행 후 데이터는 ROOM 에다가 저장
    private fun readAllMessages() {
        GroupChannel.getChannel(channelURL) { groupChannel, e ->
            val query = groupChannel!!.createPreviousMessageListQuery(
                PreviousMessageListQueryParams().apply {
                    //TODO CUSTOM Query Params
                }
            )
            query.load() { messages, e ->
                if (e != null) {
                    Log.e(TAG, "Load Previous message Error : $e", )
                    toast("Load Message Error : $e")
                    return@load
                }


                if (messages!!.isEmpty()) return@load

                //TODO message.createdAt.convertLongToTime() : 보낸 시간
                for (message in messages!!) {
                    _messageList.add(
                        MessageModel(
                            message = message.message,
                            sender = message.sender!!.userId,
                            messageId = message.messageId,
                            createdAt = message.createdAt
                        )
                    )
                }
                //TODO notifyDtaSetChanged
                Log.d(TAG, "readAllMessages: $_messageList")
            }
        }
    }

    //채널에 참여하고 있는 멤버 리스트를 가져와 대화상대를 뷰에 표시
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