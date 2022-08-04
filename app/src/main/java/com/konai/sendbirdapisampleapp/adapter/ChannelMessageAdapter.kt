package com.konai.sendbirdapisampleapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.konai.sendbirdapisampleapp.databinding.ItemMyMessageBinding
import com.konai.sendbirdapisampleapp.databinding.ItemPartnerMessageBinding
import com.konai.sendbirdapisampleapp.model.MessageModel
import com.konai.sendbirdapisampleapp.util.Constants.MY_MESSAGE
import com.konai.sendbirdapisampleapp.util.Constants.PARTNER_MESSAGE
import com.konai.sendbirdapisampleapp.util.Constants.USER_ID
import com.konai.sendbirdapisampleapp.util.Extension.convertLongToTime

class ChannelMessageAdapter: ListAdapter<MessageModel, RecyclerView.ViewHolder>(diffUtil) {
    inner class PartnerMessageViewHolder(private val binding: ItemPartnerMessageBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(message: MessageModel){
            binding.dateTextView.text = message.createdAt?.convertLongToTime()
            binding.messageTextView.text = message.message
        }
    }

    inner class MyMessageViewHolder(private val binding: ItemMyMessageBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(message: MessageModel){
            binding.dateTextView.text = message.createdAt?.convertLongToTime()
            binding.messageTextView.text = message.message
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = currentList[position]
        return if(message.sender == USER_ID) {
            MY_MESSAGE
        }
        else {
            PARTNER_MESSAGE
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val partnerMessageBinding = ItemPartnerMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val myMessageBinding = ItemMyMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return when(viewType) {
            MY_MESSAGE -> MyMessageViewHolder(myMessageBinding)
            else -> PartnerMessageViewHolder(partnerMessageBinding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(currentList[position].sender) {
            USER_ID -> {
                (holder as MyMessageViewHolder).bind(currentList[position])
            }
            else -> {
                (holder as PartnerMessageViewHolder).bind(currentList[position])
            }
        }
    }

    companion object {
        val diffUtil = object : DiffUtil.ItemCallback<MessageModel>() {
            override fun areItemsTheSame(oldItem: MessageModel, newItem: MessageModel): Boolean {
                return oldItem.messageId == newItem.messageId
            }
            override fun areContentsTheSame(oldItem: MessageModel, newItem:MessageModel): Boolean {
                return oldItem == newItem
            }
        }
    }
}