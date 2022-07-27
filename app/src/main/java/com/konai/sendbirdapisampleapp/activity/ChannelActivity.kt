package com.konai.sendbirdapisampleapp.activity

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.konai.sendbirdapisampleapp.R
import com.konai.sendbirdapisampleapp.adapter.ChannelMessageAdapter
import com.konai.sendbirdapisampleapp.databinding.ActivityChannelBinding
import com.konai.sendbirdapisampleapp.model.MessageModel
import com.konai.sendbirdapisampleapp.util.Constants.CHANNEL_ACTIVITY_INTENT_ACTION
import com.konai.sendbirdapisampleapp.util.Constants.CONVERSATION_CHANNEL
import com.konai.sendbirdapisampleapp.util.Constants.INTENT_NAME_CHANNEL_URL
import com.konai.sendbirdapisampleapp.util.Constants.MY_PERSONAL_CHANNEL
import com.konai.sendbirdapisampleapp.util.Constants.TAG
import com.konai.sendbirdapisampleapp.util.Constants.USER_ID
import com.konai.sendbirdapisampleapp.util.Constants.USER_NICKNAME
import com.konai.sendbirdapisampleapp.util.Extension.toast
import com.konai.sendbirdapisampleapp.util.Util
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
    private val adapter = ChannelMessageAdapter() //TODO lateinit ?

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_channel)
        binding.channelActivity = this

        if (intent.action == CHANNEL_ACTIVITY_INTENT_ACTION) {
            channelURL = intent.getStringExtra(INTENT_NAME_CHANNEL_URL)!!
        }
        initChannelPartnerInfo()
        readAllMessages()
        initRecyclerView()
    }

    private fun messageReceived() {
        //TODO Receive messages through a channel event handler
        SendbirdChat.addChannelHandler(
            "UNIQUE_HANDLER_ID",
            object : GroupChannelHandler() {
                override fun onMessageReceived(channel: BaseChannel, message: BaseMessage) {
                    //Log.d(TAG, "onMessageReceived: $channel // $message")
                    when (message) {
                        is UserMessage -> {
                            //TODO 상대방이 메시지 보낼 때 마다 호출됨 -> 뷰 다시 그려주면 될 듯
                            toast(message.message)
                            Log.d(TAG, "--Message--: ${message.message}")
                        }
                    }
                }
            }
        )
    }

    private fun initRecyclerView() {
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
    }

    //TODO 최초 한번만 실행되면 될 듯 -> 실행 후 데이터는 ROOM 에다가 저장
    private fun readAllMessages() {
        GroupChannel.getChannel(channelURL) { groupChannel, e ->
            if (e != null) {
                toast("Get Channel Error : $e")
                Log.e(TAG, "Get Channel Error : $e")
                return@getChannel
            }

            val query = groupChannel!!.createPreviousMessageListQuery(
                PreviousMessageListQueryParams() //TODO CUSTOM Query Params if it is needed by using .apply {}
            )
            query.load { messages, e ->
                if (e != null) {
                    Log.e(TAG, "Load Previous message Error : $e")
                    toast("Load Message Error : $e")
                    return@load
                }

                if (messages!!.isEmpty()) return@load

                for (message in messages) {
                    _messageList.add(
                        MessageModel(
                            message = message.message,
                            sender = message.sender!!.userId,
                            messageId = message.messageId,
                            createdAt = message.createdAt
                        )
                    )
                }
                Log.d(TAG, "readAllMessages: $_messageList")
                adapter.submitList(_messageList)
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
            if (partnerId == null) {
                binding.userIdTextView.text = Util.displayUserInfo(USER_NICKNAME, USER_ID, CONVERSATION_CHANNEL)
                binding.myIdDetailLayoutTextView.text = Util.displayUserInfo(USER_NICKNAME, USER_ID, CONVERSATION_CHANNEL)
            }
            else {
                binding.userIdTextView.text = Util.displayUserInfo(partnerNickname, partnerId, CONVERSATION_CHANNEL)
                binding.myIdDetailLayoutTextView.text = Util.displayUserInfo(USER_NICKNAME, USER_ID, MY_PERSONAL_CHANNEL)
                binding.partnerIdDetailLayoutTextView.text = Util.displayUserInfo(partnerNickname, partnerId, CONVERSATION_CHANNEL)
            }
        }
    }


    fun onDeleteChannelButtonClicked() {
        AlertDialog.Builder(this)
            .setTitle("채널 삭제")
            .setMessage("채널을 삭제하시겠습니까? \n삭제한 채널과 대화내용은 다시 복구 할 수 없습니다.")
            .setPositiveButton("취소") { _, _ -> }
            .setNegativeButton("삭제") { _, _ ->
                deleteChannel()
                finish()
            }
            .create()
            .show()
    }

    private fun deleteChannel() {
        GroupChannel.getChannel(channelURL) { groupChannel, e ->
            if (e != null) {
                toast("Get Channel Error: $e")
                Log.e(TAG, "Get Channel Error: $e")
                return@getChannel
            }
            groupChannel?.delete { e->
                if (e != null) {
                    //TODO Delete Error: SendbirdException{code=400108, message=Not authorized. "To delete the channel, the user should be an operator.".
                    toast("Delete Error: $e")
                    Log.e(TAG, "Delete Error: $e" )
                    return@delete
                }
                toast("채널이 삭제되었습니다.")
            }
        }
    }

    fun onSendButtonClicked() {
        val userInput: String = binding.messageEditText.text.toString()
        val params = UserMessageCreateParams(userInput)
        GroupChannel.getChannel(channelURL) { groupChannel, e ->
            if (e != null) {
                toast("Error : $e")
                Log.e(TAG, "getChannel Error: $e")
                return@getChannel
            }
            groupChannel?.sendUserMessage(params) { _, e ->
                if (e != null) {
                    toast("Error : $e")
                    Log.e(TAG, "sendUserMessage Error: $e")
                    return@sendUserMessage
                }
                toast("메시지 전송 완료")
                binding.messageEditText.text = null
            }
        }
    }
}