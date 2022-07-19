package com.konai.sendbirdapisampleapp.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.konai.sendbirdapisampleapp.Util.toast
import com.konai.sendbirdapisampleapp.activity.MainActivity.Companion.TAG
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
//                val number = _myData.number
//                val title = _myData.title
//                val timestamp = _myData.timestamp
//                Toast.makeText(binding.root.context,"ITEM DETAIL : $number / $title / $timestamp ", Toast.LENGTH_LONG).show()



            }
        }
        fun setContents (myData: ChannelListModel) {
            _myData = myData
            binding.channelNameTextView.text = "${myData.name}"
            binding.lastMessageTextView.text = "${myData.lastMessage}"

//            binding.textNo.text = "${myData.number}"
//            binding.textTitle.text = myData.title
//            var sdf = SimpleDateFormat("yyyy/MM/dd")
//            var formattedDate = sdf.format(myData.timestamp)
//            binding.textDate.text = formattedDate
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