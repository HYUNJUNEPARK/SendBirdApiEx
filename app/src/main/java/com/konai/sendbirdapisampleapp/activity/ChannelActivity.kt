package com.konai.sendbirdapisampleapp.activity

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Base64
import android.util.Log
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
import com.konai.sendbirdapisampleapp.model.MessageModel
import com.konai.sendbirdapisampleapp.strongbox.AESUtil
import com.konai.sendbirdapisampleapp.strongbox.KeyProvider
import com.konai.sendbirdapisampleapp.strongbox.KeyStoreUtil
import com.konai.sendbirdapisampleapp.util.Constants
import com.konai.sendbirdapisampleapp.util.Constants.CHANNEL_ACTIVITY_INTENT_ACTION
import com.konai.sendbirdapisampleapp.util.Constants.CHANNEL_META_DATA
import com.konai.sendbirdapisampleapp.util.Constants.CONVERSATION_CHANNEL
import com.konai.sendbirdapisampleapp.util.Constants.INTENT_NAME_CHANNEL_URL
import com.konai.sendbirdapisampleapp.util.Constants.MY_PERSONAL_CHANNEL
import com.konai.sendbirdapisampleapp.util.Constants.PREFERENCE_NAME_HASH
import com.konai.sendbirdapisampleapp.util.Constants.RECEIVE_MESSAGE_HANDLER
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
import com.sendbird.android.message.UserMessage
import com.sendbird.android.params.PreviousMessageListQueryParams
import com.sendbird.android.params.UserMessageCreateParams
import com.sendbird.android.user.Member
import java.math.BigInteger
import java.security.Key
import java.security.PrivateKey
import java.security.PublicKey

class ChannelActivity : AppCompatActivity() {
    private var partnerId: String? = null
    private var messageList: MutableList<MessageModel> = mutableListOf()
    private var partnerNickname: String? = null
    private var sharedSecretKey: Key? = null
    private lateinit var binding: ActivityChannelBinding
    private lateinit var adapter: ChannelMessageAdapter
    private lateinit var channelURL: String
    private lateinit var hash: ByteArray
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_channel)
        binding.channelActivity = this







        
        



        if(intent.action != CHANNEL_ACTIVITY_INTENT_ACTION) return



        //TODO 복호화 기능 만들기
        binding.decryptionButton.setOnClickListener {
            showToast("복호화 시작")





            for(message in messageList) {
                Log.d(TAG, "message: ${message.message}")
            }
        }
        ////TODO
        
        
        
        
        
        

        channelURL = intent.getStringExtra(INTENT_NAME_CHANNEL_URL)!!
        db = Firebase.firestore

        initChannelMembersInfo()
        initMessageRecyclerView()
        initDecryptionButtonState()
        readAllMessages()
        messageReceived()

        val sharedPreferences = getSharedPreferences(PREFERENCE_NAME_HASH, Context.MODE_PRIVATE)
        if(sharedPreferences.contains(channelURL)) {
            val _hash: String? = sharedPreferences.getString(channelURL, "empty hash")

            if (_hash == "empty hash") {
                showToast("Can't get proper hash from shared preferences")
                Log.e(TAG, "Can't get proper hash from shared preferences : ChannelActivity")
                return
            }
            else {
                hash = Base64.decode(_hash, Base64.DEFAULT)
                sharedSecretKey = AESUtil().convertHashToKey(hash)
                binding.secretKeyStateImageView.setImageResource(R.drawable.ic_baseline_check_circle_24)
            }
        }

        else {
            showToast("SP 에 값 없음")

            //내 퍼블릭 키
            val key = listOf(CHANNEL_META_DATA)
            GroupChannel.getChannel(channelURL) { groupChannel, e ->
                //var _metadata: String? = null
                if (e != null) {
                    showToast("Can't get channel : $e")
                    Log.e(TAG, "Can't get channel / ChannelActivity :$e" )
                    return@getChannel
                }
                groupChannel!!.getMetaData(key) { map, e ->
                    if (e != null) {
                        showToast("Can't get channel meta data error : $e")
                        Log.e(TAG, "Can't get channel meta data error / ChannelActivity :$e" )
                        return@getMetaData
                    }
                    //서버로 부터 온 메타데이터 앞에 [ 가 붙어 있음 [QCPPm6RxW6tJtLJrVcqVq5wMVEhvXsvfLli2bG9P3g4=
                    val _metadata = map!!.values.toString().substring(1 until map!!.values.toString().length)
                    val metadata: ByteArray = Base64.decode(_metadata, Base64.DEFAULT)

                    val privateKey = KeyStoreUtil().getPrivateKeyFromKeyStore(USER_ID)
                    Log.d(TAG, "meta data: $_metadata // $privateKey")

                    createSharedHash(privateKey!!, partnerId!!, metadata, channelURL)

                }
            }
        }
    }


    //TODO 중복되는 함수
    private fun createSharedHash(privateKey: PrivateKey, invitedUserId: String, randomNumbers: ByteArray, preferenceKey: String) {
        var affineX: BigInteger?
        var affineY: BigInteger?

        db?.collection(Constants.FIRE_STORE_DOCUMENT_PUBLIC_KEY)
            ?.get()
            ?.addOnSuccessListener { result ->
                for (document in result) {
                    if (document.data[Constants.FIRE_STORE_FIELD_USER_ID] == invitedUserId) {
                        showToast("상대방 공개키 affineX/affineY 얻음")
                        affineX = BigInteger(document.data[Constants.FIRE_STORE_FIELD_AFFINE_X].toString())
                        affineY = BigInteger(document.data[Constants.FIRE_STORE_FIELD_AFFINE_Y].toString())
                        val publicKey: PublicKey = KeyStoreUtil().createPublicKeyByECPoint(affineX!!, affineY!!)
                        val sharedSecretHash: ByteArray = KeyProvider().createSharedSecretHash(
                            privateKey,
                            publicKey!!,
                            randomNumbers
                        )
                        //Shared preference
                        val sharedPreferences = getSharedPreferences(PREFERENCE_NAME_HASH, MODE_PRIVATE)
                        val editor: SharedPreferences.Editor = sharedPreferences.edit()
                        val hash: String = Base64.encodeToString(sharedSecretHash, Base64.DEFAULT)
                        editor.putString(preferenceKey, hash)
                        editor.apply()
                        //Shared preference
                        showToast("public key 저장 완료")
                    }
                }
            }
            ?.addOnFailureListener { exception ->
                showToast("키 가져오기 실패")
                Log.e(TAG, "Error getting documents from firestore : $exception")
            }
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

    private fun initDecryptionButtonState() {
        val sharedPreferences = getSharedPreferences(PREFERENCE_NAME_HASH, Context.MODE_PRIVATE)
        if(sharedPreferences.contains(channelURL)) {
            binding.decryptionButton.isEnabled = true
        }
    }
//[END Init]

//[START Read message]
    //TODO 최초 한번만 실행 -> 실행 후 데이터는 ROOM 에다가 저장
    private fun readAllMessages() {
        GroupChannel.getChannel(channelURL) { channel, e ->
            if (e != null) {
                showToast("Get Channel Error : $e")
                Log.e(TAG, "Get Channel Error : $e")
                return@getChannel
            }

            val query = channel!!.createPreviousMessageListQuery(
                PreviousMessageListQueryParams() //Custom QueryParams if it's needed. use .apply {}
            )
            query.load { messages, e ->
                if (e != null) {
                    Log.e(TAG, "Load Previous message Error : $e")
                    showToast("Load Message Error : $e")
                    return@load
                }
                if (messages!!.isEmpty()) return@load
                messageList.clear() //메시지 리스트를 한번 초기화하고 새로운 메시지를 추가
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
            }
        }
    }

    private fun messageReceived() {
        SendbirdChat.addChannelHandler(
            RECEIVE_MESSAGE_HANDLER,
            object : GroupChannelHandler() {
                override fun onMessageReceived(channel: BaseChannel, message: BaseMessage) {
                    when (message) {
                        is UserMessage -> {
                            //TODO 생각해볼 것 : 상대방이 메시지 보낼 때 마다 호출 -> channelListFragment 에서 message.message 토스트가 뜨는것이 확인됨
                            //TODO 여기서 신호가 오면 채팅 리스트를 바꿔줌
                            showToast("상대방 메시지 수신")

                            messageList.add(
                                MessageModel(
                                    message = message.message,
                                    sender = message.sender!!.userId,
                                    messageId = message.messageId,
                                    createdAt = message.createdAt
                                )
                            )
                            adapter.submitList(messageList)
                            adapter.notifyDataSetChanged()

                            binding.recyclerView.run { //리사이클러뷰 위치 조정
                                postDelayed({
                                    scrollToPosition(adapter!!.itemCount - 1)
                                }, 300)
                            }
                        }
                    }
                }
            }
        )
    }
//[END Read message]

//[START Clicked event]
    fun onSendButtonClicked() {
        val userMessage: String = binding.messageEditText.text.toString()
        val encryptedMessage = AESUtil().encryptionCBCMode(userMessage, hash)
        val params = UserMessageCreateParams(encryptedMessage)

        GroupChannel.getChannel(channelURL) { groupChannel, e ->
            if (e != null) {
                showToast("Error : $e")
                Log.e(TAG, "getChannel Error: $e")
                return@getChannel
            }
            groupChannel?.sendUserMessage(params) { message, e ->
                if (e != null) {
                    showToast("Error : $e")
                    Log.e(TAG, "sendUserMessage Error: $e")
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
                //TODO It will always be more efficient to use more specific change events if you can. Rely on `notifyDataSetChanged` as a last resort.
                adapter.notifyDataSetChanged()

                binding.recyclerView.run {
                    postDelayed({
                        scrollToPosition(adapter!!.itemCount - 1)
                    }, 300)
                }
                binding.messageEditText.text = null
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
                    channel?.delete { e->
                        if (e != null) {
                            // cf. error code 400108 : Not authorized. To delete the channel, the user should be an operator.
                            showToast("Delete Error: $e")
                            Log.e(TAG, "Delete Error: $e" )
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
//[END Clicked event]

//    private fun deleteChannel() {
//        GroupChannel.getChannel(channelURL) { channel, e ->
//            if (e != null) {
//                showToast("Get Channel Error: $e")
//                Log.e(TAG, "Get Channel Error: $e")
//                return@getChannel
//            }
//            channel?.delete { e->
//                if (e != null) {
//                    /* error code 400108
//                    Not authorized. To delete the channel, the user should be an operator.*/
//                    showToast("Delete Error: $e")
//                    Log.e(TAG, "Delete Error: $e" )
//                    return@delete
//                }
//                showToast("채널이 삭제되었습니다.")
//            }
//        }
//    }
}