package com.konai.sendbirdapisampleapp.adapter

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.konai.sendbirdapisampleapp.Constants.INTENT_ACTION_GROUP_CHANNEL
import com.konai.sendbirdapisampleapp.Constants.INTENT_ACTION_MY_CHANNEL
import com.konai.sendbirdapisampleapp.Constants.INTENT_NAME_CHANNEL_URL
import com.konai.sendbirdapisampleapp.Extension.convertLongToTime
import com.konai.sendbirdapisampleapp.activity.ChannelActivity
import com.konai.sendbirdapisampleapp.databinding.ItemChatChannelListBinding
import com.konai.sendbirdapisampleapp.models.ChannelModel
import com.sendbird.android.channel.GroupChannel

class ChannelAdapter(val context: Context) : RecyclerView.Adapter<ChannelAdapter.MyHolder>() {
    var channelList = mutableListOf<ChannelModel>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        val binding = ItemChatChannelListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyHolder(binding)
    }

    inner class MyHolder(private val binding: ItemChatChannelListBinding) : RecyclerView.ViewHolder(binding.root) {
        lateinit var channel: ChannelModel

        init {
            //숏클릭 이벤트
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

            //롱클릭 이벤트
            binding.root.setOnLongClickListener {
                AlertDialog.Builder(context)
                    .setTitle("${channel.name}")
                    .setItems(
                        arrayOf(
                            "나가기"
                        ),
                        object : DialogInterface.OnClickListener {
                            override fun onClick(p0: DialogInterface?, position: Int) {
                                when(position) {
                                    0 -> {
                                        deleteChannel(channel.url!!, channel)
                                        channelList.remove(channel)
                                    }
                                }
                            }
                        }
                    )
                    .create()
                    .show()
                true
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

    //TODO ChannelActivity deleteChannel() 과 중복되는 함수
    //TODO ChannelModel 에 lastMessage/lastMessageTime 이 있음
    // -> 삭제 중에 변경이 일어나면 정삭적으로 동작하지 않을 가능성이 있음
    //채널 삭제 버튼 클릭 이벤트
    fun deleteChannel(channelURL: String, targetChannel: ChannelModel) {
        AlertDialog.Builder(context)
            .setTitle("채널 삭제")
            .setMessage("채널을 삭제하시겠습니까? \n삭제한 채널과 대화내용은 다시 복구 할 수 없습니다.")
            .setPositiveButton("취소") { _, _ -> }
            .setNegativeButton("삭제") { _, _ ->
                //delete channel
                GroupChannel.getChannel(channelURL) { channel, e1 ->
                    if (e1 != null) {
                        e1.printStackTrace()
                        return@getChannel
                    }
                    channel?.delete { e2->
                        if (e2 != null) {
                            e2.printStackTrace()
                            return@delete
                        }
                        //TODO 지워지는 건 제대로 지워졌는데 UI랑 동기화가 안됨- idx 0 이 지워짐
                        val idx = channelList.indexOf(targetChannel)
                        channelList.remove(targetChannel)
                        notifyItemRemoved(idx)
                        Toast.makeText(context, "채널이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .create()
            .show()
    }

}