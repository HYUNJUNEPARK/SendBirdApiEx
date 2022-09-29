package com.konai.sendbirdapisampleapp.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.DialogInterface
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.konai.sendbirdapisampleapp.Constants.MY_MESSAGE
import com.konai.sendbirdapisampleapp.Constants.PARTNER_MESSAGE
import com.konai.sendbirdapisampleapp.Constants.TAG
import com.konai.sendbirdapisampleapp.Constants.USER_ID
import com.konai.sendbirdapisampleapp.Extension.convertLongToTime
import com.konai.sendbirdapisampleapp.databinding.ItemMyMessageBinding
import com.konai.sendbirdapisampleapp.databinding.ItemPartnerMessageBinding
import com.konai.sendbirdapisampleapp.db.DBProvider
import com.konai.sendbirdapisampleapp.db.keyid.KeyIdDatabase
import com.konai.sendbirdapisampleapp.models.MessageModel
import com.konai.sendbirdapisampleapp.strongbox.StrongBox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MessageAdapter(val context: Context, val channelURL: String) : ListAdapter<MessageModel, RecyclerView.ViewHolder>(diffUtil) {
    lateinit var localDB: KeyIdDatabase
    lateinit var strongBox: StrongBox

    //대화 상대의 홀더
    inner class PartnerMessageViewHolder(private val binding: ItemPartnerMessageBinding): RecyclerView.ViewHolder(binding.root) {
        lateinit var messageModel: MessageModel

        init {
            //롱클릭 이벤트
            binding.root.setOnLongClickListener {
                showAlertDialog(messageModel)
                true
            }

            //숏클릭 이벤트
            binding.root.setOnClickListener {
                decryptMessage(messageModel)
            }
        }

        fun bind(message: MessageModel){
            this.messageModel = message
            binding.dateTextView.text = message.createdAt?.convertLongToTime()
            binding.messageTextView.text = message.message
        }
    }

    //로그인 사용자의 홀더
    inner class MyMessageViewHolder(private val binding: ItemMyMessageBinding): RecyclerView.ViewHolder(binding.root) {
        lateinit var messageModel: MessageModel

        init {
            //롱클릭 이벤트
            binding.root.setOnLongClickListener {
                showAlertDialog(messageModel)
                true
            }

            //숏클릭 이벤트
            binding.root.setOnClickListener {
                decryptMessage(messageModel)
            }
        }

        fun bind(message: MessageModel){
            this.messageModel = message
            binding.dateTextView.text = message.createdAt?.convertLongToTime()
            binding.messageTextView.text = message.message
        }
    }

    //메시지 보낸이에 따라서 뷰올더를 다르게 사용
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
        localDB = DBProvider.getInstance(context)!!
        strongBox = StrongBox.getInstance(context)

        val partnerMessageBinding =
            ItemPartnerMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val myMessageBinding =
            ItemMyMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)

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

    private fun showAlertDialog(messageModel: MessageModel) {
        AlertDialog.Builder(context)
            .setItems(
                arrayOf(
                    "복호화",
                    "복사",
                    "삭제",
                    "답장"
                ),
                object : DialogInterface.OnClickListener {
                    override fun onClick(p0: DialogInterface?, position: Int) {
                        when(position) {
                            0 -> {
                                decryptMessage(messageModel)
                            }
                            1 -> {
                                copyMessage(messageModel)
                            }
                            2 -> {
                                deleteMessage()
                            }
                            3 -> {
                                replyMessage()
                            }
                        }
                    }
                }
            )
            .create()
            .show()
    }

    private fun decryptMessage(messageModel: MessageModel) {
        Toast.makeText(context, "복호화", Toast.LENGTH_SHORT).show()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                localDB.keyIdDao().getKeyId(channelURL).let { keyId ->
                    strongBox.decrypt(
                        message = messageModel.message!!,
                        keyId = keyId
                    ).let { decryptedMessage ->
                        val decryptedMessageModel = MessageModel(
                            message = decryptedMessage,
                            sender = messageModel.sender,
                            messageId = messageModel.messageId,
                            createdAt = messageModel.createdAt,
                        )

                        //TODO 더 생각해볼것 ....
                        val list: MutableList<MessageModel> = currentList.toMutableList()
                        list[currentList.indexOf(messageModel)] = decryptedMessageModel

                        withContext(Dispatchers.Main) {
                            submitList(list)
                            notifyItemChanged(currentList.indexOf(messageModel))
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "$e", )
            }
        }
    }

    private fun copyMessage(messageModel: MessageModel) {
        try {
            val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText(
                "decrypted message",
                messageModel.message
            )
            clipboard.setPrimaryClip(clipData)
            Toast.makeText(context, "텍스트 복사", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "텍스트 복사 실패", Toast.LENGTH_SHORT).show()
            return
        }

    }

    private fun deleteMessage() {
        Toast.makeText(context, "삭제", Toast.LENGTH_SHORT).show()
    }

    private fun replyMessage() {
        Toast.makeText(context, "답장", Toast.LENGTH_SHORT).show()
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