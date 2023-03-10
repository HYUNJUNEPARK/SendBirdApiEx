package com.konai.sendbirdapisampleapp.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.konai.sendbirdapisampleapp.Constants.CHANNEL_META_DATA
import com.konai.sendbirdapisampleapp.Constants.FIRESTORE_DOCUMENT_PUBLIC_KEY
import com.konai.sendbirdapisampleapp.Constants.FIRESTORE_FIELD_AFFINE_X
import com.konai.sendbirdapisampleapp.Constants.FIRESTORE_FIELD_AFFINE_Y
import com.konai.sendbirdapisampleapp.Constants.FIRESTORE_FIELD_USER_ID
import com.konai.sendbirdapisampleapp.Constants.INTENT_ACTION_GROUP_CHANNEL
import com.konai.sendbirdapisampleapp.Constants.INTENT_ACTION_MY_CHANNEL
import com.konai.sendbirdapisampleapp.Constants.INTENT_NAME_CHANNEL_URL
import com.konai.sendbirdapisampleapp.Constants.LOGIN_ACCOUNT_MESSAGE_RECEIVE_HANDLER
import com.konai.sendbirdapisampleapp.Constants.USER_ID
import com.konai.sendbirdapisampleapp.Constants.USER_NICKNAME
import com.konai.sendbirdapisampleapp.R
import com.konai.sendbirdapisampleapp.adapter.MessageAdapter
import com.konai.sendbirdapisampleapp.databinding.ActivityChannelBinding
import com.konai.sendbirdapisampleapp.db.DBProvider
import com.konai.sendbirdapisampleapp.db.keyid.KeyIdDatabase
import com.konai.sendbirdapisampleapp.db.keyid.KeyIdEntity
import com.konai.sendbirdapisampleapp.models.MessageModel
import com.konai.sendbirdapisampleapp.strongbox.ECKeyUtil
import com.konai.sendbirdapisampleapp.strongbox.EncryptedSharedPreferencesManager
import com.konai.sendbirdapisampleapp.strongbox.StrongBox
import com.sendbird.android.SendbirdChat
import com.sendbird.android.channel.BaseChannel
import com.sendbird.android.channel.GroupChannel
import com.sendbird.android.handler.GroupChannelHandler
import com.sendbird.android.message.BaseMessage
import com.sendbird.android.params.PreviousMessageListQueryParams
import com.sendbird.android.params.UserMessageCreateParams
import com.sendbird.android.user.Member
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class ChannelActivity : AppCompatActivity(), CoroutineScope {
    private var remoteDB: FirebaseFirestore? = null
    private var friendId: String? = null
    private var friendNickname: String? = null
    private var messageList: MutableList<MessageModel> = mutableListOf()
    private var encryptedMessageList: MutableList<MessageModel> = mutableListOf()
    private var decryptedMessageList: MutableList<MessageModel> = mutableListOf()
    private lateinit var binding: ActivityChannelBinding
    private lateinit var adapter: MessageAdapter
    private lateinit var channelURL: String
    private lateinit var strongBox: StrongBox
    private lateinit var localDB: KeyIdDatabase
    private lateinit var espm: EncryptedSharedPreferencesManager
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            strongBox = StrongBox.getInstance(this)
            channelURL = intent.getStringExtra(INTENT_NAME_CHANNEL_URL)!!
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        //????????? ??????????????? ?????? ??????
        if (!isMyDevice()) {
            AlertDialog.Builder(this)
                .setTitle("??????")
                .setCancelable(false)
                .setMessage("????????? ????????? ????????? ????????????. \n?????? ??????/????????? ??????/????????? ???????????? ??????????????????.")
                .setPositiveButton("??????") { _, _ ->
                    finish()
                }
                .create()
                .show()
            return
        }

        when (intent.action) {
            INTENT_ACTION_MY_CHANNEL -> {
                activateMyChannel()
            }
            INTENT_ACTION_GROUP_CHANNEL -> {
                activateGroupChannel()
            }
            else -> {
                return
            }
        }
    }

    private fun activateMyChannel() {
        try {
            binding = DataBindingUtil.setContentView(this, R.layout.activity_channel)
            binding.channelActivity = this

            initAdapter()
            displayMembersId()

            CoroutineScope(Dispatchers.Main).launch {
                readAllMessages()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun activateGroupChannel() {
        try {
            binding = DataBindingUtil.setContentView(this, R.layout.activity_channel)
            binding.channelActivity = this
            remoteDB = Firebase.firestore
            localDB = DBProvider.getInstance(this)!!
            espm = EncryptedSharedPreferencesManager.getInstance(this)!!

            initAdapter()
            displayMembersId()

            CoroutineScope(Dispatchers.IO).launch {
                localDB.keyIdDao().getKeyId(channelURL).let { keyId ->
                    //1. keyId ?????? ??????. ?????? ????????? or ?????? ????????? ?????? ??????????????? ?????? ????????? ?????? ?????? ?????????
                    if (keyId != null) {
                        CoroutineScope(Dispatchers.Main).launch {
                            binding.decryptionButton.isEnabled = true
                            binding.secretKeyStateImageView.setImageResource(R.drawable.ic_baseline_check_circle_24)
                            readAllMessages()
                        }
                    }
                    //2. keyId??? ??????.
                    else {
                        CoroutineScope(Dispatchers.Main).launch {
                            generateSharedSecretKey()
                            readAllMessages()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        addMessageHandler()
    }

    override fun onPause() {
        super.onPause()
        SendbirdChat.removeChannelHandler(LOGIN_ACCOUNT_MESSAGE_RECEIVE_HANDLER)
    }

    override fun onDestroy() {
        super.onDestroy()
        remoteDB = null
    }

    private fun initAdapter() {
        adapter = MessageAdapter(
            this,
            channelURL
        )
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
    }

    //????????? ?????? ?????? ???????????? ?????? UI ??????
    private fun displayMembersId() {
        GroupChannel.getChannel(channelURL) { groupChannel, e ->
            if (e != null) {
                e.printStackTrace()
                return@getChannel
            }
            val memberList: List<Member> = groupChannel!!.members
            for (member in memberList) {
                //?????? ?????? ?????? ??????
                if (member.userId != USER_ID) {
                    friendId = member.userId
                    friendNickname = if(member.nickname == "") "-" else member.nickname
                }
            }
            //1. ????????? ????????? ??????
            if (friendId == null) {
                binding.secretKeyStateImageView.visibility = View.GONE
                binding.userIdTextView.text =
                    "$USER_NICKNAME (ID : $USER_ID) [???]" //Activity : ??? ?????? ex) userNickname(ID : userId)
                binding.myIdDetailLayoutTextView.text =
                    "$USER_NICKNAME (ID : $USER_ID) [???]" //MotionLayout : ??? ??????
            }
            //2. ?????? ????????? ??????
            else {
                binding.userIdTextView.text =
                    "$friendNickname (ID : $friendId)" //Activity : ?????? ??????
                binding.partnerIdDetailLayoutTextView.text =
                    "$friendNickname (ID : $friendId)" //MotionLayout : ?????? ??????
                binding.myIdDetailLayoutTextView.text =
                    "$USER_NICKNAME (ID : $USER_ID) [???]" //MotionLayout : ??? ??????
            }
        }
    }

    //???????????? ????????? ??????????????? ??????????????? ??????
    private fun isMyDevice(): Boolean {
        //try ????????? ??????????????? ?????????????????? ??? ??????????????? ???????????? ??????
        return try {
            strongBox.getECPublicKey(USER_ID)
            true
        }
        //?????? ?????? ??????????????? ???????????? ?????? ?????? ?????? -> ????????? ?????????/????????? ??????
        catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    //???????????? ?????? ????????? ????????? ??? ????????? ???????????? SharedSecretKey ??????
    private suspend fun generateSharedSecretKey() = withContext(Dispatchers.IO) {
        /* 1. localDB ??? url ?????? ???????????? KeyId ??? ?????? -> ???????????? ????????? ???????????? ????????? ???????????? ????????? ??????
           warning : Condition 'keyId == null' is always 'false'
           ???????????? ???????????? ??????????????? ????????? ?????? ?????? ????????? ?????? ??? ?????? ??????. ????????? ?????? ????????? ?????? ?????? */
        remoteDB!!.collection(FIRESTORE_DOCUMENT_PUBLIC_KEY)
            .get()
            .addOnSuccessListener { result ->
                //1.1 FirebaseDB ?????? ????????? PublicKey ??? ?????????
                for (document in result) {
                    if (document.data[FIRESTORE_FIELD_USER_ID] == friendId) {
                        val publicKey = ECKeyUtil.coordinatePublicKey(
                            affineX = document.data[FIRESTORE_FIELD_AFFINE_X].toString(),
                            affineY = document.data[FIRESTORE_FIELD_AFFINE_Y].toString()
                        )
                        //1.2 ???????????? ???????????? ?????? ??????????????????(keyId(secureRandom)) ?????? ???
                        val data = listOf(CHANNEL_META_DATA)
                        GroupChannel.getChannel(channelURL) { channel, e1 ->
                            if (e1 != null) {
                                e1.printStackTrace()
                                return@getChannel
                            }
                            channel!!.getMetaData(data) { map, e2 ->
                                if (e2 != null) {
                                    e2.printStackTrace()
                                    return@getMetaData
                                }

                                val metadata = map!!.get("metadata") ?: ""
                                if (metadata.isEmpty()) {
                                    return@getMetaData
                                }

                                //1.3 sharedSecretKey ??????
                                strongBox.generateSharedSecretKey(
                                    USER_ID,
                                    publicKey,
                                    metadata
                                ).let { sharedSecretKey ->
                                    //2.4 ESP ??? sharedSecretKey ??????
                                    /*EncryptedSharedPreferences
                                    =======================================
                                    KeyId(Secure Random) | SharedSecretKey |
                                    =======================================*/
                                    espm.putString(
                                        channelURL,
                                        sharedSecretKey
                                    )
                                }
                                /*LocalDB
                                ====================================
                                Channel URL | KeyId(Secure Random) |
                                ====================================*/
                                //TODO CoroutineScope ????????? ??? ??? ?
                                CoroutineScope(Dispatchers.IO).launch {
                                    localDB.keyIdDao().insert(
                                        KeyIdEntity(
                                            channelURL,
                                            metadata
                                        )
                                    )
                                }
                                CoroutineScope(Dispatchers.Main).launch {
                                    Toast.makeText(this@ChannelActivity, "??? ?????? ??????", Toast.LENGTH_SHORT).show()
                                    binding.decryptionButton.isEnabled = true
                                    binding.secretKeyStateImageView.setImageResource(R.drawable.ic_baseline_check_circle_24)
                                }
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                return@addOnFailureListener
            }
    }

    //?????? ?????? ???????????? ?????? ???????????? ?????????
    private fun addMessageHandler() {
        SendbirdChat.addChannelHandler(
            LOGIN_ACCOUNT_MESSAGE_RECEIVE_HANDLER,
            object : GroupChannelHandler() {
                override fun onMessageReceived(channel: BaseChannel, message: BaseMessage) {
                    when (channel.url) {
                        channelURL -> {
                            encryptedMessageList.add(
                                MessageModel(
                                    message = message.message,
                                    sender = message.sender!!.userId,
                                    messageId = message.messageId,
                                    createdAt = message.createdAt
                                )
                            )
                            adapter.submitList(encryptedMessageList)
                            adapter.notifyItemInserted(encryptedMessageList.size-1)
                            adjustRecyclerViewPosition()
                        }
                    }
                }
            }
        )
    }

    private suspend fun readAllMessages() = withContext(Dispatchers.IO) {
        when (intent.action) {
            //????????? ??????
            INTENT_ACTION_MY_CHANNEL -> {
                try {
                    GroupChannel.getChannel(channelURL) { channel, e1 ->
                        if (e1 != null) {
                            e1.printStackTrace()
                            return@getChannel
                        }

                        val query = channel!!.createPreviousMessageListQuery(
                            PreviousMessageListQueryParams() //Custom QueryParams if it's needed. use .apply {}
                        )
                        query.load { messages, e2 ->
                            if (e2 != null) {
                                e2.printStackTrace()
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
                            adjustRecyclerViewPosition()
                        }
                    }
                }
                catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@ChannelActivity, "???????????? ??????????????? ??????????????????.", Toast.LENGTH_SHORT).show()
                }
            }
            //?????? ??????
            INTENT_ACTION_GROUP_CHANNEL -> {
                try {
                    GroupChannel.getChannel(channelURL) { channel, e1 ->
                        if (e1 != null) {
                            e1.printStackTrace()
                            return@getChannel
                        }

                        val query = channel!!.createPreviousMessageListQuery(
                            PreviousMessageListQueryParams() //Custom QueryParams if it's needed. use .apply {}
                        )
                        query.load { messages, e2 ->
                            if (e2 != null) {
                                e2.printStackTrace()
                                return@load
                            }

                            //????????? ?????? ???????????? ?????? ???
                            if (messages!!.isEmpty()) {
                                return@load
                            }

                            //????????? ?????? ???????????? ?????? ???
                            encryptedMessageList.clear()
                            for (message in messages) {
                                encryptedMessageList.add(
                                    MessageModel(
                                        message = message.message,
                                        sender = message.sender!!.userId,
                                        messageId = message.messageId,
                                        createdAt = message.createdAt
                                    )
                                )
                            }
                            adapter.submitList(encryptedMessageList)
                            adapter.notifyDataSetChanged()
                            adjustRecyclerViewPosition()
                        }
                    }
                }
                catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@ChannelActivity, "???????????? ??????????????? ??????????????????.", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                //INTENT_ACTION_MY_CHANNEL, INTENT_ACTION_GROUP_CHANNEL ??? ?????? ??????
            }
        }
    }
    //????????? ?????? ?????? ?????? ?????????
    fun sendMessage() {
        if (binding.messageEditText.text.isEmpty()) {
            return
        }
        when (intent.action) {
            //????????? ??????
            INTENT_ACTION_MY_CHANNEL -> {
                try {
                    val userMessage: String = binding.messageEditText.text.toString()
                    val params = UserMessageCreateParams(userMessage)
                    binding.messageEditText.text = null

                    GroupChannel.getChannel(channelURL) { groupChannel, e1 ->
                        if (e1 != null) {
                            e1.printStackTrace()
                            return@getChannel
                        }
                        groupChannel?.sendUserMessage(params) { message, e2 ->
                            if (e2 != null) {
                                e2.printStackTrace()
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
                            adapter.notifyItemInserted(messageList.size-1)
                            adjustRecyclerViewPosition()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "????????? ????????? ??????????????????.", Toast.LENGTH_SHORT).show()
                }

            }
            //?????? ??????
            INTENT_ACTION_GROUP_CHANNEL -> {
                try {
                    //Local DB ?????? keyId ?????????
                    CoroutineScope(Dispatchers.IO).launch {
                        //1.URL ??? ???????????? KeyId ??? ?????????
                        localDB.keyIdDao().getKeyId(channelURL).let { keyId ->
                            //2. ????????? ?????????
                            strongBox.encrypt(
                                message = binding.messageEditText.text.toString(),
                                keyId = keyId
                            ).let { encryptedMessage ->
                                //3. ???????????? ????????? ???????????? ????????? ??????
                                val params = UserMessageCreateParams(encryptedMessage)
                                GroupChannel.getChannel(channelURL) { groupChannel, e1 ->
                                    if (e1 != null) {
                                        e1.printStackTrace()
                                        return@getChannel
                                    }
                                    groupChannel?.sendUserMessage(params) { message, e2 ->
                                        if (e2 != null) {
                                            e2.printStackTrace()
                                            return@sendUserMessage
                                        }
                                        //4. ?????? ????????? ????????? UI??? ?????????
                                        encryptedMessageList.add(
                                            MessageModel(
                                                message = message?.message,
                                                sender = message?.sender?.userId,
                                                messageId = message?.messageId,
                                                createdAt = message?.createdAt
                                            )
                                        )
                                        binding.messageEditText.text = null
                                        adapter.submitList(encryptedMessageList)
                                        adapter.notifyItemInserted(encryptedMessageList.size-1)
                                        adjustRecyclerViewPosition()
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "????????? ????????? ??????????????????.", Toast.LENGTH_SHORT).show()
                }
            }
            else -> return
        }
    }

    //????????? ????????? ?????? ?????? ?????????
    fun decryptMessage() {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    //1. localDB ?????? key(channelURL) ??? value(keyId) ?????????
                    localDB.keyIdDao().getKeyId(channelURL).let { keyId ->
                        decryptedMessageList.clear()
                        for (message in encryptedMessageList) {
                            decryptedMessageList.add(
                                //2. keyId ??? ???????????? ???????????? ??????????????? ????????? ????????? ?????????
                                MessageModel(
                                    message = strongBox.decrypt(
                                        message = message.message!!,
                                        keyId = keyId
                                    ),
                                    sender = message.sender,
                                    messageId = message.messageId,
                                    createdAt = message.createdAt
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(this@ChannelActivity, "????????? ?????? ????????? ???????????? ???????????? ??? ????????????.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
            }
            //3. ???????????? ???????????? UI??? ??????
            adapter.submitList(decryptedMessageList)
            adapter.notifyDataSetChanged()
            adjustRecyclerViewPosition()
    }

    //?????? ?????? ?????? ?????? ?????????
    fun deleteChannel() {
        AlertDialog.Builder(this)
            .setTitle("?????? ??????")
            .setMessage("????????? ????????????????????????? \n????????? ????????? ??????????????? ?????? ?????? ??? ??? ????????????.")
            .setPositiveButton("??????") { _, _ -> }
            .setNegativeButton("??????") { _, _ ->
                //delete channel
                GroupChannel.getChannel(channelURL) { channel, e1 ->
                    if (e1 != null) {
                        e1.printStackTrace()
                        return@getChannel
                    }
                    channel?.delete { e2->
                        if (e2 != null) {
                            // cf. error code 400108 : Not authorized. To delete the channel, the user should be an operator.
                            e2.printStackTrace()
                            return@delete
                        }
                        Toast.makeText(this, "????????? ?????????????????????.", Toast.LENGTH_SHORT).show()
                    }
                }
                finish() //close channel activity
            }
            .create()
            .show()
    }

    //?????????????????? ?????? ??????
    private fun adjustRecyclerViewPosition() {
        binding.recyclerView.run {
            postDelayed(
                { scrollToPosition(adapter!!.itemCount - 1) },
                200
            )
        }
    }
}