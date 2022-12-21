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

        //사용자 디바이스가 아닌 경우
        if (!isMyDevice()) {
            AlertDialog.Builder(this)
                .setTitle("경고")
                .setCancelable(false)
                .setMessage("계정에 등록된 기기가 아닙니다. \n채널 생성/메시지 송신/메시지 복호화가 불가능합니다.")
                .setPositiveButton("확인") { _, _ ->
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
                    //1. keyId 이미 있음. 채널 생성자 or 이미 채널에 한번 접근한적이 있는 채널에 초대 받은 사용자
                    if (keyId != null) {
                        CoroutineScope(Dispatchers.Main).launch {
                            binding.decryptionButton.isEnabled = true
                            binding.secretKeyStateImageView.setImageResource(R.drawable.ic_baseline_check_circle_24)
                            readAllMessages()
                        }
                    }
                    //2. keyId가 없음.
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

    //채널에 참여 중인 사용자의 정보 UI 세팅
    private fun displayMembersId() {
        GroupChannel.getChannel(channelURL) { groupChannel, e ->
            if (e != null) {
                e.printStackTrace()
                return@getChannel
            }
            val memberList: List<Member> = groupChannel!!.members
            for (member in memberList) {
                //대화 중인 친구 정보
                if (member.userId != USER_ID) {
                    friendId = member.userId
                    friendNickname = if(member.nickname == "") "-" else member.nickname
                }
            }
            //1. 나와의 채팅은 경우
            if (friendId == null) {
                binding.secretKeyStateImageView.visibility = View.GONE
                binding.userIdTextView.text =
                    "$USER_NICKNAME (ID : $USER_ID) [나]" //Activity : 내 정보 ex) userNickname(ID : userId)
                binding.myIdDetailLayoutTextView.text =
                    "$USER_NICKNAME (ID : $USER_ID) [나]" //MotionLayout : 내 정보
            }
            //2. 그룹 채팅인 경우
            else {
                binding.userIdTextView.text =
                    "$friendNickname (ID : $friendId)" //Activity : 친구 정보
                binding.partnerIdDetailLayoutTextView.text =
                    "$friendNickname (ID : $friendId)" //MotionLayout : 친구 정보
                binding.myIdDetailLayoutTextView.text =
                    "$USER_NICKNAME (ID : $USER_ID) [나]" //MotionLayout : 내 정보
            }
        }
    }

    //사용자가 자신의 디바이스에 로그인한지 확인
    private fun isMyDevice(): Boolean {
        //try 블럭이 정상적으로 실행되었다면 내 디바이스에 로그인한 경우
        return try {
            strongBox.getECPublicKey(USER_ID)
            true
        }
        //다른 사람 디바이스에 로그인한 경우 예외 발생 -> 메시지 암호화/복호화 불가
        catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    //사용자가 대화 채널에 입장할 때 채널에 해당하는 SharedSecretKey 확인
    private suspend fun generateSharedSecretKey() = withContext(Dispatchers.IO) {
        /* 1. localDB 에 url 키에 해당하는 KeyId 가 없음 -> 사용자가 채널에 초대되고 채널에 처음으로 접근한 상황
           warning : Condition 'keyId == null' is always 'false'
           디바이스 하나에서 테스트하면 당연히 위와 같은 결과가 나올 수 밖에 없음. 따라서 신경 안써도 되는 경고 */
        remoteDB!!.collection(FIRESTORE_DOCUMENT_PUBLIC_KEY)
            .get()
            .addOnSuccessListener { result ->
                //1.1 FirebaseDB 에서 상대방 PublicKey 를 가져옴
                for (document in result) {
                    if (document.data[FIRESTORE_FIELD_USER_ID] == friendId) {
                        val publicKey = ECKeyUtil.coordinatePublicKey(
                            affineX = document.data[FIRESTORE_FIELD_AFFINE_X].toString(),
                            affineY = document.data[FIRESTORE_FIELD_AFFINE_Y].toString()
                        )
                        //1.2 센드버드 서버에서 채널 메타데이터를(keyId(secureRandom)) 가져 옴
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

                                //1.3 sharedSecretKey 생성
                                strongBox.generateSharedSecretKey(
                                    USER_ID,
                                    publicKey,
                                    metadata
                                ).let { sharedSecretKey ->
                                    //2.4 ESP 에 sharedSecretKey 등록
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
                                //TODO CoroutineScope 없애도 될 듯 ?
                                CoroutineScope(Dispatchers.IO).launch {
                                    localDB.keyIdDao().insert(
                                        KeyIdEntity(
                                            channelURL,
                                            metadata
                                        )
                                    )
                                }
                                CoroutineScope(Dispatchers.Main).launch {
                                    Toast.makeText(this@ChannelActivity, "키 생성 성공", Toast.LENGTH_SHORT).show()
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

    //해당 채널 메시지가 오면 호출되는 핸들러
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
            //나와의 채팅
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
                    Toast.makeText(this@ChannelActivity, "메시지를 읽어오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            //그룹 채팅
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

                            //채널에 이전 메시지가 없을 떄
                            if (messages!!.isEmpty()) {
                                return@load
                            }

                            //채널에 이전 메시지가 있을 때
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
                    Toast.makeText(this@ChannelActivity, "메시지를 읽어오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                //INTENT_ACTION_MY_CHANNEL, INTENT_ACTION_GROUP_CHANNEL 가 아닌 예외
            }
        }
    }
    //메시지 전송 버튼 클릭 이벤트
    fun sendMessage() {
        if (binding.messageEditText.text.isEmpty()) {
            return
        }
        when (intent.action) {
            //나와의 채팅
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
                    Toast.makeText(this, "메시지 전송에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }

            }
            //그룹 채팅
            INTENT_ACTION_GROUP_CHANNEL -> {
                try {
                    //Local DB 에서 keyId 꺼내기
                    CoroutineScope(Dispatchers.IO).launch {
                        //1.URL 에 해당하는 KeyId 를 가져옴
                        localDB.keyIdDao().getKeyId(channelURL).let { keyId ->
                            //2. 메시지 암호화
                            strongBox.encrypt(
                                message = binding.messageEditText.text.toString(),
                                keyId = keyId
                            ).let { encryptedMessage ->
                                //3. 센드버드 서버에 암호화된 메시지 전송
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
                                        //4. 전송 완료된 메시지 UI에 띄우기
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
                    Toast.makeText(this, "메시지 전송에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            else -> return
        }
    }

    //메시지 복호화 버튼 클릭 이벤트
    fun decryptMessage() {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    //1. localDB 에서 key(channelURL) 로 value(keyId) 꺼내옴
                    localDB.keyIdDao().getKeyId(channelURL).let { keyId ->
                        decryptedMessageList.clear()
                        for (message in encryptedMessageList) {
                            decryptedMessageList.add(
                                //2. keyId 와 암호화된 메시지를 파라미터로 전달해 메시지 복호화
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
                        Toast.makeText(this@ChannelActivity, "예기치 못한 예러로 메시지를 복호화할 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
            }
            //3. 복호화된 메시지를 UI에 띄움
            adapter.submitList(decryptedMessageList)
            adapter.notifyDataSetChanged()
            adjustRecyclerViewPosition()
    }

    //채널 삭제 버튼 클릭 이벤트
    fun deleteChannel() {
        AlertDialog.Builder(this)
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
                            // cf. error code 400108 : Not authorized. To delete the channel, the user should be an operator.
                            e2.printStackTrace()
                            return@delete
                        }
                        Toast.makeText(this, "채널이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                finish() //close channel activity
            }
            .create()
            .show()
    }

    //리사이클러뷰 위치 조정
    private fun adjustRecyclerViewPosition() {
        binding.recyclerView.run {
            postDelayed(
                { scrollToPosition(adapter!!.itemCount - 1) },
                200
            )
        }
    }
}