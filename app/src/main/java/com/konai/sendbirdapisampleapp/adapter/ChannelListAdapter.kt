package com.konai.sendbirdapisampleapp.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.konai.sendbirdapisampleapp.activity.ChannelActivity
import com.konai.sendbirdapisampleapp.databinding.ItemChatChannelListBinding
import com.konai.sendbirdapisampleapp.model.ChannelListModel
import com.konai.sendbirdapisampleapp.util.Constants.CHANNEL_ACTIVITY_INTENT_ACTION
import com.konai.sendbirdapisampleapp.util.Constants.INTENT_NAME_CHANNEL_URL
import com.konai.sendbirdapisampleapp.util.Constants.INTENT_NAME_USER_ID
import com.konai.sendbirdapisampleapp.util.Constants.INTENT_NAME_USER_NICK
import com.konai.sendbirdapisampleapp.util.Constants.USER_ID
import com.konai.sendbirdapisampleapp.util.Constants.USER_NICKNAME
import com.konai.sendbirdapisampleapp.util.Extension.convertLongToTime

class ChannelListAdapter(val context: Context) : RecyclerView.Adapter<ChannelListAdapter.MyHolder>() {
    var channelList = mutableListOf<ChannelListModel>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        val binding = ItemChatChannelListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyHolder(binding)
    }

    inner class MyHolder(private val binding: ItemChatChannelListBinding) : RecyclerView.ViewHolder(binding.root) {
        lateinit var _myData: ChannelListModel

        init {
            binding.root.setOnClickListener {
                val intent = Intent(context, ChannelActivity::class.java)
                intent.putExtra(INTENT_NAME_CHANNEL_URL, "${_myData.url}")
                intent.action = CHANNEL_ACTIVITY_INTENT_ACTION
                context.startActivity(intent)
            }
        }
        fun setContents (myData: ChannelListModel) {
            _myData = myData
            binding.channelNameTextView.text = "${myData.name}"
            binding.lastMessageTextView.text = "${myData.lastMessage}"
            binding.timeTextView.text = "${myData.lastMessageTime?.convertLongToTime()}"
        }
    }

    override fun onBindViewHolder(myHolder: MyHolder, position: Int) {
        val data = channelList[position]
        myHolder.setContents(data)
    }

    override fun getItemCount(): Int = channelList.size
}