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
import com.konai.sendbirdapisampleapp.util.Constants.RECEIVE_MESSAGE_HANDLER
import com.konai.sendbirdapisampleapp.util.Constants.TAG
import com.konai.sendbirdapisampleapp.util.Constants.USER_ID
import com.konai.sendbirdapisampleapp.util.Constants.USER_NICKNAME
import com.konai.sendbirdapisampleapp.util.Extension.showToast
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
    private lateinit var adapter: ChannelMessageAdapter
    private lateinit var channelURL: String
    private var partnerNickname: String? = null
    private var partnerId: String? = null
    private var _messageList: MutableList<MessageModel> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_channel)
        binding.channelActivity = this

        if (intent.action == CHANNEL_ACTIVITY_INTENT_ACTION) {
            channelURL = intent.getStringExtra(INTENT_NAME_CHANNEL_URL)!!
        }
        initChannelMembersInfo()
        initRecyclerView()
        readAllMessages()
        messageReceived()
    }

    private fun initChannelMembersInfo() {
        GroupChannel.getChannel(channelURL) { groupChannel, e ->
            if (e != null) {
                Log.e(TAG, "Get Channel Activity Error: $e")
                showToast("Get Channel Activity Error: $e")
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

    private fun initRecyclerView() {
        adapter = ChannelMessageAdapter()
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
    }

    //TODO 최초 한번만 실행되면 될 듯 -> 실행 후 데이터는 ROOM 에다가 저장
    private fun readAllMessages() {
        GroupChannel.getChannel(channelURL) { groupChannel, e ->
            if (e != null) {
                showToast("Get Channel Error : $e")
                Log.e(TAG, "Get Channel Error : $e")
                return@getChannel
            }

            val query = groupChannel!!.createPreviousMessageListQuery(
                PreviousMessageListQueryParams() //Custom QueryParams if it's needed. use .apply {}
            )
            query.load { messages, e ->
                if (e != null) {
                    Log.e(TAG, "Load Previous message Error : $e")
                    showToast("Load Message Error : $e")
                    return@load
                }

                if (messages!!.isEmpty()) return@load

                _messageList.clear()
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
                adapter.submitList(_messageList)
            }
        }
    }

    private fun messageReceived() {
        SendbirdChat.addChannelHandler(
            RECEIVE_MESSAGE_HANDLER,
            object : GroupChannelHandler() {
                override fun onMessageReceived(channel: BaseChannel, message: BaseMessage) {
                    when (message) {
                        is UserMessage -> {
                            //TODO 상대방이 메시지 보낼 때 마다 호출 -> 뷰 다시 그려주면 될 듯
                            //TODO 생각해볼 것 : channelListFragment 에서 message.message 토스트가 뜨는것이 확인됨
                            //TODO -> 여기서 신호가 오면 채팅방을 바꿔주는 것도 괜찮을 듯
                            showToast("상대방 메시지 수신")

                            _messageList.add(
                                MessageModel(
                                    message = message.message,
                                    sender = message.sender!!.userId,
                                    messageId = message.messageId,
                                    createdAt = message.createdAt
                                )
                            )
                            adapter.submitList(_messageList)
                            adapter.notifyDataSetChanged()

                            //리사이클러뷰 위치 조정
                            binding.recyclerView.run {
                                postDelayed({
                                    scrollToPosition(adapter!!.itemCount - 1)
                                }, 300)
                            }
                        }
                    }
                }
            }
        )
    }

    fun onSendButtonClicked() {
        val userInput: String = binding.messageEditText.text.toString()
        val params = UserMessageCreateParams(userInput)
        GroupChannel.getChannel(channelURL) { groupChannel, e ->
            if (e != null) {
                showToast("Error : $e")
                Log.e(TAG, "getChannel Error: $e")
                return@getChannel
            }
            groupChannel?.sendUserMessage(params) { message, e ->
                if (e != null) {
                    showToast("Error : $e")
                    Log.e(TAG, "sendUserMessage Error: $e")
                    return@sendUserMessage
                }
                _messageList.add(
                    MessageModel(
                        message = message?.message,
                        sender = message?.sender?.userId,
                        messageId = message?.messageId,
                        createdAt = message?.createdAt
                    )
                )
                adapter.submitList(_messageList)
                //TODO It will always be more efficient to use more specific change events if you can. Rely on `notifyDataSetChanged` as a last resort.
                adapter.notifyDataSetChanged()

                binding.recyclerView.run {
                    postDelayed({
                        scrollToPosition(adapter!!.itemCount - 1)
                    }, 300)
                }
                binding.messageEditText.text = null
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
                showToast("Get Channel Error: $e")
                Log.e(TAG, "Get Channel Error: $e")
                return@getChannel
            }
            groupChannel?.delete { e->
                if (e != null) {
                    /* error code 400108
                    Not authorized. To delete the channel, the user should be an operator.*/
                    showToast("Delete Error: $e")
                    Log.e(TAG, "Delete Error: $e" )
                    return@delete
                }
                showToast("채널이 삭제되었습니다.")
            }
        }
    }
}