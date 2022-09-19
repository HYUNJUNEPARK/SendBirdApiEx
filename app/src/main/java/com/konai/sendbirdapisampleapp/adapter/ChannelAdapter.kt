package com.konai.sendbirdapisampleapp.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.konai.sendbirdapisampleapp.Constants.INTENT_ACTION_GROUP_CHANNEL
import com.konai.sendbirdapisampleapp.Constants.INTENT_ACTION_MY_CHANNEL
import com.konai.sendbirdapisampleapp.Constants.INTENT_NAME_CHANNEL_URL
import com.konai.sendbirdapisampleapp.Extension.convertLongToTime
import com.konai.sendbirdapisampleapp.activity.ChannelActivity
import com.konai.sendbirdapisampleapp.databinding.ItemChatChannelListBinding
import com.konai.sendbirdapisampleapp.models.ChannelModel

class ChannelAdapter(val context: Context) : RecyclerView.Adapter<ChannelAdapter.MyHolder>() {
    var channelList = mutableListOf<ChannelModel>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        val binding = ItemChatChannelListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyHolder(binding)
    }

    inner class MyHolder(private val binding: ItemChatChannelListBinding) : RecyclerView.ViewHolder(binding.root) {
        lateinit var channel: ChannelModel

        init {
            binding.root.setOnClickListener {
                val intent = Intent(context, ChannelActivity::class.java)
                intent.putExtra(INTENT_NAME_CHANNEL_URL, "${channel.url}")
                if (channel.memberSize == 1) {
                    intent.action = INTENT_ACTION_MY_CHANNEL
                }
                else {
                    intent.action = INTENT_ACTION_GROUP_CHANNEL
                }
                context.startActivity(intent)
            }
        }
        fun setContents (channel: ChannelModel) {
            this.channel = channel
            binding.channelNameTextView.text = "${channel.name}"
            binding.lastMessageTextView.text = "${channel.lastMessage}"
            binding.timeTextView.text = "${channel.lastMessageTime?.convertLongToTime()}"
        }
    }

    override fun onBindViewHolder(myHolder: MyHolder, position: Int) {
        myHolder.setContents(channelList[position])
    }

    override fun getItemCount(): Int = channelList.size
}