package com.konai.sendbirdapisampleapp.activity

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.konai.sendbirdapisampleapp.R
import com.konai.sendbirdapisampleapp.adapter.ChannelMessageAdapter
import com.konai.sendbirdapisampleapp.databinding.ActivityChannelBinding
import com.konai.sendbirdapisampleapp.models.MessageModel
import com.konai.sendbirdapisampleapp.tmpstrongbox.AESUtil
import com.konai.sendbirdapisampleapp.tmpstrongbox.KeyProvider
import com.konai.sendbirdapisampleapp.tmpstrongbox.KeyStoreUtil
import com.konai.sendbirdapisampleapp.util.Constants.CHANNEL_ACTIVITY_INTENT_ACTION
import com.konai.sendbirdapisampleapp.util.Constants.CHANNEL_META_DATA
import com.konai.sendbirdapisampleapp.util.Constants.CONVERSATION_CHANNEL
import com.konai.sendbirdapisampleapp.util.Constants.FIRESTORE_DOCUMENT_PUBLIC_KEY
import com.konai.sendbirdapisampleapp.util.Constants.FIRESTORE_FIELD_AFFINE_X
import com.konai.sendbirdapisampleapp.util.Constants.FIRESTORE_FIELD_AFFINE_Y
import com.konai.sendbirdapisampleapp.util.Constants.FIRESTORE_FIELD_USER_ID
import com.konai.sendbirdapisampleapp.util.Constants.INTENT_NAME_CHANNEL_URL
import com.konai.sendbirdapisampleapp.util.Constants.LOGIN_ACCOUNT_MESSAGE_RECEIVE_HANDLER
import com.konai.sendbirdapisampleapp.util.Constants.MY_PERSONAL_CHANNEL
import com.konai.sendbirdapisampleapp.util.Constants.PREFERENCE_NAME_HASH
import com.konai.sendbirdapisampleapp.util.Constants.TAG
import com.konai.sendbirdapisampleapp.util.Constants.USER_ID
import com.konai.sendbirdapisampleapp.util.Constants.USER_NICKNAME
import com.konai.sendbirdapisampleapp.util.Extension.showToast
import com.konai.sendbirdapisampleapp.util.Util
import com.sendbird.android.SendbirdChat
import com.sendbird.android.channel.BaseChannel
import com.sendbird.android.channel.GroupChannel
import com.sendbird.android.handler.GroupChannelHandler
import com.sendbird.android.message.BaseMessage
import com.sendbird.android.params.PreviousMessageListQueryParams
import com.sendbird.android.params.UserMessageCreateParams
import com.sendbird.android.user.Member
import kotlinx.coroutines.*
import java.math.BigInteger
import java.security.Key
import java.security.PrivateKey
import java.security.PublicKey
import kotlin.coroutines.CoroutineContext

class ChannelActivity : AppCompatActivity(), CoroutineScope {
    private var db: FirebaseFirestore? = null
    private var partnerId: String? = null
    private var partnerNickname: String? = null
    private var sharedSecretKey: Key? = null
    private var encryptionMessageList: MutableList<MessageModel> = mutableListOf()
    private var decryptionMessageList: MutableList<MessageModel> = mutableListOf()
    private lateinit var binding: ActivityChannelBinding
    private lateinit var adapter: ChannelMessageAdapter
    private lateinit var channelURL: String
    private lateinit var hash: ByteArray
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + Job()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_channel)
        binding.channelActivity = this
        if(intent.action != CHANNEL_ACTIVITY_INTENT_ACTION) return

        db = Firebase.firestore
        channelURL = intent.getStringExtra(INTENT_NAME_CHANNEL_URL)!!

        initChannelMembersInfo()
        initMessageRecyclerView()
        initSharedSecretKey()

        if (!KeyStoreUtil().isKeyInKeyStore(USER_ID)) {
            binding.decryptionButton.isEnabled = false
            binding.sendButton.isEnabled = false
            binding.sendButton.setImageResource(R.drawable.ic_baseline_cancel_schedule_send_24)
        }
        launch {
            showProgress()
            readAllMessages()
            dismissProgress()
        }
    }

    override fun onResume() {
        super.onResume()
        initMessageHandler()
    }

    override fun onPause() {
        super.onPause()
        SendbirdChat.removeChannelHandler(LOGIN_ACCOUNT_MESSAGE_RECEIVE_HANDLER)
    }

    override fun onDestroy() {
        super.onDestroy()
        db = null
    }

//[START Init]
    private fun initChannelMembersInfo() {
        GroupChannel.getChannel(channelURL) { groupChannel, e ->
            if (e != null) {
                Log.e(TAG, "Get Channel Activity Error: $e")
                showToast("Get Channel Activity Error: $e")
                return@getChannel
            }
            val members: List<Member> = groupChannel!!.members
            for (member in members) {
                if (member.userId != USER_ID) {
                    partnerId = member.userId
                    partnerNickname = member.nickname
                }
            }
            if (partnerId == null) {
                binding.userIdTextView.text = Util.displayUserInfo(USER_NICKNAME, USER_ID, CONVERSATION_CHANNEL)
                binding.myIdDetailLayoutTextView.text = Util.displayUserInfo(USER_NICKNAME, USER_ID, CONVERSATION_CHANNEL)
            }
            else {
                binding.userIdTextView.text = Util.displayUserInfo(partnerNickname, partnerId, CONVERSATION_CHANNEL)
                binding.myIdDetailLayoutTextView.text = Util.displayUserInfo(USER_NICKNAME, USER_ID, MY_PERSONAL_CHANNEL)
                binding.partnerIdDetailLayoutTextView.text = Util.displayUserInfo(partnerNickname, partnerId, CONVERSATION_CHANNEL)
            }
        }
    }

    private fun initMessageRecyclerView() {
        adapter = ChannelMessageAdapter()
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun initSharedSecretKey() {
        //내 키가 기기에 없다면(다른 기기로 로그인했다면) 공유키를 생성할 필요가 없음
        if (!KeyStoreUtil().isKeyInKeyStore(USER_ID)) return

        //channel 에 해당하는 Key 가 shared preference 에 있는 경우
        //-> Value 인 Hash 를 갖고 Shared Key 를 생성
        val sharedPreferences = getSharedPreferences(PREFERENCE_NAME_HASH, Context.MODE_PRIVATE)
        if(sharedPreferences.contains(channelURL)) {
            val data: String? = sharedPreferences.getString(channelURL, "empty hash")
            if (data != "empty hash") {
                hash = Base64.decode(data, Base64.DEFAULT)
                sharedSecretKey = AESUtil().convertHashToKey(hash)
                binding.secretKeyStateImageView.setImageResource(R.drawable.ic_baseline_check_circle_24)
                binding.decryptionButton.isEnabled = true //activate decryption button
            }
        }
        //channel 에 해당하는 Key 가 shared preference 에 없는 경우
        //ex. 채널을 막 생성했을 때 채널에 초대받은 사람 관점(초대한 사람은 기기에 키가 저장되어 있음) or 사용자가 다른 기기로 로그인(이 경우 keystore 에 keypair 없음)
        //-> 내 프라이빗 키(키스토어) + 랜덤데이터(채널 메타데이터) + 상대방 퍼블릭키(서버 XY 좌표) -> Shared Key 생성
        else {
            if (db == null) {
                showToast("Firebase DB initialize error")
                Log.e(TAG, "firebase database initialize error")
                return
            }
            Log.i(TAG, "shared preference 에 공유키 키 없음")
            val data = listOf(CHANNEL_META_DATA)
            GroupChannel.getChannel(channelURL) { channel, e ->
                //var _metadata: String? = null
                if (e != null) {
                    showToast("Can't get channel : $e")
                    Log.e(TAG, "Can't get channel / ChannelActivity :$e" )
                    return@getChannel
                }
                channel!!.getMetaData(data) { metaDataMap, exception ->
                    if (exception != null) {
                        showToast("Can't get channel meta data error : $exception")
                        Log.e(TAG, "Can't get channel meta data error / ChannelActivity :$exception" )
                        return@getMetaData
                    }
                    Log.i(TAG, "get channel metadata")

                    val metadata = metaDataMap!!.values.toString().substring(1 until metaDataMap.values.toString().length).let { data ->
                        Base64.decode(data, Base64.DEFAULT)
                    }
                    val privateKey: PrivateKey = KeyStoreUtil().getPrivateKeyFromKeyStore(USER_ID)!!
                    createSharedHash(
                        privateKey = privateKey,
                        invitedUserId = partnerId!!,
                        randomNumbers = metadata,
                        preferenceKey = channelURL
                    )
                }
            }
        }
    }

    private fun initMessageHandler() {
        SendbirdChat.addChannelHandler(
            LOGIN_ACCOUNT_MESSAGE_RECEIVE_HANDLER,
            object : GroupChannelHandler() {
                override fun onMessageReceived(channel: BaseChannel, message: BaseMessage) {
                    when (channel.url) {
                        channelURL -> {
                            encryptionMessageList.add(
                                MessageModel(
                                    message = message.message,
                                    sender = message.sender!!.userId,
                                    messageId = message.messageId,
                                    createdAt = message.createdAt
                                )
                            )
                            adapter.submitList(encryptionMessageList)
                            adapter.notifyDataSetChanged() //TODO It will always be more efficient to use more specific change events if you can.
                        }
                    }
                }
            }
        )
    }
//[END Init]

//[START Firestore: Public Key]
    private fun createSharedHash(privateKey: PrivateKey, invitedUserId: String, randomNumbers: ByteArray, preferenceKey: String) {
        var affineX: BigInteger?
        var affineY: BigInteger?

        db!!.collection(FIRESTORE_DOCUMENT_PUBLIC_KEY)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    if (document.data[FIRESTORE_FIELD_USER_ID] == invitedUserId) {
                        showToast("상대방 공개키 affineX/affineY 얻음")
                        Log.i(TAG, "상대방 공개키 affineX/affineY 얻음")
                        affineX = BigInteger(document.data[FIRESTORE_FIELD_AFFINE_X].toString())
                        affineY = BigInteger(document.data[FIRESTORE_FIELD_AFFINE_Y].toString())
                        val publicKey: PublicKey = KeyStoreUtil().createPublicKeyByECPoint(affineX!!, affineY!!)
                        val sharedSecretHash: ByteArray = KeyProvider().createSharedSecretHash(
                            privateKey,
                            publicKey,
                            randomNumbers
                        )
                        //Shared preference
                        val sharedPreferences = getSharedPreferences(PREFERENCE_NAME_HASH, MODE_PRIVATE)
                        val editor: SharedPreferences.Editor = sharedPreferences.edit()
                        val hash: String = Base64.encodeToString(sharedSecretHash, Base64.DEFAULT)
                        editor.putString(preferenceKey, hash)
                        editor.apply()
                        //Shared preference

                        showToast("channel hash 저장")
                        Log.i(TAG, "channel hash 저장")
                        binding.decryptionButton.isEnabled = true //activate decryption button
                    }
                }
            }
            .addOnFailureListener { exception ->
                showToast("키 가져오기 실패")
                Log.e(TAG, "Error getting documents from firebase DB : $exception")
            }
    }
//[END Firestore: Public Key]

//[START Read message]
    private suspend fun readAllMessages() {
        GroupChannel.getChannel(channelURL) { channel, e ->
            if (e != null) {
                showToast("Get Channel Error : $e")
                Log.e(TAG, "Get Channel Error : $e")
                return@getChannel
            }

            val query = channel!!.createPreviousMessageListQuery(
                PreviousMessageListQueryParams() //Custom QueryParams if it's needed. use .apply {}
            )
            query.load { messages, exception ->
                if (exception != null) {
                    Log.e(TAG, "Load Previous message Error : $exception")
                    showToast("Load Message Error : $exception")
                    return@load
                }
                if (messages!!.isEmpty()) return@load
                encryptionMessageList.clear()
                for (message in messages) {
                    encryptionMessageList.add(
                        MessageModel(
                            message = message.message,
                            sender = message.sender!!.userId,
                            messageId = message.messageId,
                            createdAt = message.createdAt
                        )
                    )
                }
                adapter.submitList(encryptionMessageList)
                adapter.notifyDataSetChanged()
                //TODO It will always be more efficient to use more specific change events if you can.
            }
        }
    }
//[END Read message]

//[START Click Event]
    fun onSendButtonClicked() {
        val userMessage: String = binding.messageEditText.text.toString()
        val encryptedMessage = AESUtil().encryptionCBCMode(userMessage, hash)
        val params = UserMessageCreateParams(encryptedMessage)
        binding.messageEditText.text = null

        GroupChannel.getChannel(channelURL) { groupChannel, channelException ->
            if (channelException != null) {
                showToast("Error : $channelException")
                Log.e(TAG, "getChannel Error: $channelException")
                return@getChannel
            }
            groupChannel?.sendUserMessage(params) { message, sendMessageException ->
                if (sendMessageException != null) {
                    showToast("Error : $sendMessageException")
                    Log.e(TAG, "sendUserMessage Error: $sendMessageException")
                    return@sendUserMessage
                }
                encryptionMessageList.add(
                    MessageModel(
                        message = message?.message,
                        sender = message?.sender?.userId,
                        messageId = message?.messageId,
                        createdAt = message?.createdAt
                    )
                )
                adapter.submitList(encryptionMessageList)
                adapter.notifyDataSetChanged() //TODO It will always be more efficient to use more specific change events if you can.
                adjustRecyclerViewPosition()
            }
        }
    }

    fun onDecryptionButtonClicked() {
        showToast("복호화")
        val sharedPreferences = getSharedPreferences(PREFERENCE_NAME_HASH, Context.MODE_PRIVATE)
        sharedPreferences.getString(channelURL, "empty hash").let { data ->
            if (data == "empty hash") {
                showToast("Can't get proper hash from shared preferences")
                Log.e(TAG, "Can't get proper hash from shared preferences : ChannelActivity")
                return
            }
            else {
                val hash = Base64.decode(data, Base64.DEFAULT)
                decryptionMessageList.clear()
                for (message in encryptionMessageList) {
                    decryptionMessageList.add(
                        MessageModel(
                            message = AESUtil().decryptionCBCMode(message.message!!, hash),
                            sender = message.sender,
                            messageId = message.messageId,
                            createdAt = message.createdAt
                        )
                    )
                }
                adapter.submitList(decryptionMessageList)
                adapter.notifyDataSetChanged() //TODO It will always be more efficient to use more specific change events if you can.
                adjustRecyclerViewPosition()
            }
        }
    }

    fun onDeleteChannelButtonClicked() {
        AlertDialog.Builder(this)
            .setTitle("채널 삭제")
            .setMessage("채널을 삭제하시겠습니까? \n삭제한 채널과 대화내용은 다시 복구 할 수 없습니다.")
            .setPositiveButton("취소") { _, _ -> }
            .setNegativeButton("삭제") { _, _ ->
                //delete channel
                GroupChannel.getChannel(channelURL) { channel, e ->
                    if (e != null) {
                        showToast("Get Channel Error: $e")
                        Log.e(TAG, "Get Channel Error: $e")
                        return@getChannel
                    }
                    channel?.delete { exception->
                        if (exception != null) {
                            // cf. error code 400108 : Not authorized. To delete the channel, the user should be an operator.
                            showToast("Delete Error: $exception")
                            Log.e(TAG, "Delete Error: $exception" )
                            return@delete
                        }
                        showToast("채널이 삭제되었습니다.")
                    }
                }
                //close channel activity
                finish()
            }
            .create()
            .show()
    }
//[END Click Event]

//[START Util]
    private fun adjustRecyclerViewPosition() {
        binding.recyclerView.run { //리사이클러뷰 위치 조정
            postDelayed({
                scrollToPosition(adapter!!.itemCount - 1)
            }, 300)
        }
    }

    private suspend fun showProgress() = withContext(coroutineContext) {
        with(binding) {
            progressBar.visibility = View.VISIBLE
        }
    }

    private suspend fun dismissProgress() = withContext(coroutineContext) {
        with(binding) {
            progressBar.visibility = View.GONE
        }
    }
//[END Util]
}