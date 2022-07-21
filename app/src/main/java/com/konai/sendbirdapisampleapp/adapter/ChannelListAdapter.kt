package com.konai.sendbirdapisampleapp.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.konai.sendbirdapisampleapp.util.Constants.TAG
import com.konai.sendbirdapisampleapp.databinding.ItemChatChannelListBinding
import com.konai.sendbirdapisampleapp.model.ChannelListModel

class ChannelListAdapter : RecyclerView.Adapter<ChannelListAdapter.MyHolder>() {
    var channelList = mutableListOf<ChannelListModel>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        val binding = ItemChatChannelListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyHolder(binding)
    }

    inner class MyHolder(private val binding: ItemChatChannelListBinding) : RecyclerView.ViewHolder(binding.root) {
        //TODO 클릭된 아이템의 세부 데이터가 필요할 때 사용
        lateinit var _myData: ChannelListModel

        init {
            binding.root.setOnClickListener {
                Log.d(TAG, "URL : ${_myData.url}")
            }
        }
        fun setContents (myData: ChannelListModel) {
            _myData = myData
            binding.channelNameTextView.text = "${myData.name}"
            binding.lastMessageTextView.text = "${myData.lastMessage}"
        }
    }

    override fun onBindViewHolder(myHolder: MyHolder, position: Int) {
        val data = channelList[position]
        Log.d(TAG, "onBindViewHolder: $data")
        myHolder.setContents(data)
    }

    override fun getItemCount(): Int {
        return channelList.size
    }
}