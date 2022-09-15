package com.konai.sendbirdapisampleapp.activity

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.konai.sendbirdapisampleapp.R
import com.konai.sendbirdapisampleapp.adapter.MessageAdapter
import com.konai.sendbirdapisampleapp.databinding.ActivityMyChannelBinding
import com.konai.sendbirdapisampleapp.models.MessageModel
import com.konai.sendbirdapisampleapp.Constants
import com.konai.sendbirdapisampleapp.Constants.CHANNEL_ACTIVITY_INTENT_ACTION
import com.konai.sendbirdapisampleapp.Constants.INTENT_NAME_CHANNEL_URL
import com.konai.sendbirdapisampleapp.Constants.TAG
import com.sendbird.android.channel.GroupChannel
import com.sendbird.android.params.PreviousMessageListQueryParams
import com.sendbird.android.params.UserMessageCreateParams

class MyChannelActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMyChannelBinding
    private lateinit var adapter: MessageAdapter
    private lateinit var channelURL: String
    private var messageList: MutableList<MessageModel> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_my_channel)
        binding.myChannelActivity = this

        if (intent.action != CHANNEL_ACTIVITY_INTENT_ACTION) return
        channelURL = intent.getStringExtra(INTENT_NAME_CHANNEL_URL)!!

        initMessageRecyclerView()
        readAllMessages()

        val sharedPreferences = getSharedPreferences(Constants.PREFERENCE_NAME_HASH, Context.MODE_PRIVATE)
        Log.d(TAG, "onCreate: ${sharedPreferences.all.size}")
    }

//[START Init]
    private fun initMessageRecyclerView() {
        adapter = MessageAdapter()
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
    }
//[END Init]

//[START Read message]
    private fun readAllMessages() {
        GroupChannel.getChannel(channelURL) { channel, e ->
            if (e != null) {
                e.printStackTrace()
                return@getChannel
            }

            val query = channel!!.createPreviousMessageListQuery(
                PreviousMessageListQueryParams() //Custom QueryParams if it's needed. use .apply {}
            )
            query.load { messages, e ->
                if (e != null) {
                    e.printStackTrace()
                    return@load
                }
                if (messages!!.isEmpty()) return@load
                messageList.clear()
                for (message in messages) {
                    messageList.add(
                        MessageModel(
                            message = message.message,
                            sender = message.sender!!.userId,
                            messageId = message.messageId,
                            createdAt = message.createdAt
                        )
                    )
                }
                adapter.submitList(messageList)
                adapter.notifyDataSetChanged()
                //TODO It will always be more efficient to use more specific change events if you can.
            }
        }
    }
//[END Read message]

//[START Click Event]
    fun onSendButtonClicked() {
        val userMessage: String = binding.messageEditText.text.toString()
        //val encryptedMessage = AESUtil().encryptionCBCMode(userMessage, hash)
        val params = UserMessageCreateParams(userMessage)
        binding.messageEditText.text = null

        GroupChannel.getChannel(channelURL) { groupChannel, e ->
            if (e != null) {
                e.printStackTrace()
                return@getChannel
            }
            groupChannel?.sendUserMessage(params) { message, e ->
                if (e != null) {
                    e.printStackTrace()
                    return@sendUserMessage
                }
                messageList.add(
                    MessageModel(
                        message = message?.message,
                        sender = message?.sender?.userId,
                        messageId = message?.messageId,
                        createdAt = message?.createdAt
                    )
                )
                adapter.submitList(messageList)
                adapter.notifyDataSetChanged()
                //TODO It will always be more efficient to use more specific change events if you can.
                adjustRecyclerViewPosition()
            }
        }
    }
//[END Click Event]

//[START Util]
    private fun adjustRecyclerViewPosition() {
        binding.recyclerView.run { //리사이클러뷰 위치 조정
            postDelayed({
                scrollToPosition(adapter!!.itemCount - 1)
            }, 300)
        }
    }
//[END Util]
}