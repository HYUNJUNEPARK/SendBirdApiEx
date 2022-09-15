package com.konai.sendbirdapisampleapp.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.konai.sendbirdapisampleapp.activity.ChannelActivity
import com.konai.sendbirdapisampleapp.activity.MyChannelActivity
import com.konai.sendbirdapisampleapp.databinding.ItemChatChannelListBinding
import com.konai.sendbirdapisampleapp.models.ChannelModel
import com.konai.sendbirdapisampleapp.Constants.CHANNEL_ACTIVITY_INTENT_ACTION
import com.konai.sendbirdapisampleapp.Constants.INTENT_NAME_CHANNEL_URL
import com.konai.sendbirdapisampleapp.Extension.convertLongToTime

class ChannelListAdapter(val context: Context) : RecyclerView.Adapter<ChannelListAdapter.MyHolder>() {
    var channelList = mutableListOf<ChannelModel>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        val binding = ItemChatChannelListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyHolder(binding)
    }

    inner class MyHolder(private val binding: ItemChatChannelListBinding) : RecyclerView.ViewHolder(binding.root) {
        lateinit var channel: ChannelModel

        init {
            binding.root.setOnClickListener {
                //Go to MyChannel
                if (channel.memberSize == 1) {
                    val intent = Intent(context, MyChannelActivity::class.java)
                    intent.putExtra(INTENT_NAME_CHANNEL_URL, "${channel.url}")
                    intent.action = CHANNEL_ACTIVITY_INTENT_ACTION
                    context.startActivity(intent)
                    return@setOnClickListener
                }
                //Go to Group Channel
                val intent = Intent(context, ChannelActivity::class.java)
                intent.putExtra(INTENT_NAME_CHANNEL_URL, "${channel.url}")
                intent.action = CHANNEL_ACTIVITY_INTENT_ACTION
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