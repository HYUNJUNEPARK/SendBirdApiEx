package com.konai.sendbirdapisampleapp.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.konai.sendbirdapisampleapp.Constants.INTENT_ACTION_MY_CHANNEL
import com.konai.sendbirdapisampleapp.R
import com.konai.sendbirdapisampleapp.adapter.MessageAdapter
import com.konai.sendbirdapisampleapp.databinding.ActivityMyChannelBinding
import com.konai.sendbirdapisampleapp.models.MessageModel
import com.konai.sendbirdapisampleapp.Constants.INTENT_NAME_CHANNEL_URL
import com.sendbird.android.channel.GroupChannel
import com.sendbird.android.params.PreviousMessageListQueryParams
import com.sendbird.android.params.UserMessageCreateParams

class MyChannelActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMyChannelBinding
    private var messageList: MutableList<MessageModel> = mutableListOf()

    private lateinit var adapter: MessageAdapter
    private lateinit var channelURL: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_my_channel)
        binding.myChannelActivity = this

        if (intent.action != INTENT_ACTION_MY_CHANNEL) {
            return
        }
        channelURL = intent.getStringExtra(INTENT_NAME_CHANNEL_URL)!!

        initAdapter()
        readAllMessages()
    }

    //END
    private fun initAdapter() {
        adapter = MessageAdapter()
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
    }

    //END
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

    //END
    fun onSendButtonClicked() {
        val userMessage: String = binding.messageEditText.text.toString()
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

    //END
    private fun adjustRecyclerViewPosition() {
        binding.recyclerView.run { //리사이클러뷰 위치 조정
            postDelayed({
                scrollToPosition(adapter!!.itemCount - 1)
            }, 300)
        }
    }
}