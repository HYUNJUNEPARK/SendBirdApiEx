package com.konai.sendbirdapisampleapp.activity

import android.content.Context
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
import com.konai.sendbirdapisampleapp.db.DBProvider
import com.konai.sendbirdapisampleapp.db.KeyIdDatabase
import com.konai.sendbirdapisampleapp.db.KeyIdEntity
import com.konai.sendbirdapisampleapp.models.MessageModel
import com.konai.sendbirdapisampleapp.strongbox.ECKeyUtil
import com.konai.sendbirdapisampleapp.strongbox.EncryptedSharedPreferencesManager
import com.konai.sendbirdapisampleapp.strongbox.StrongBox
import com.konai.sendbirdapisampleapp.tmpstrongbox.AESUtil
import com.konai.sendbirdapisampleapp.util.Constants.CHANNEL_ACTIVITY_INTENT_ACTION
import com.konai.sendbirdapisampleapp.util.Constants.CHANNEL_META_DATA
import com.konai.sendbirdapisampleapp.util.Constants.FIRESTORE_DOCUMENT_PUBLIC_KEY
import com.konai.sendbirdapisampleapp.util.Constants.FIRESTORE_FIELD_AFFINE_X
import com.konai.sendbirdapisampleapp.util.Constants.FIRESTORE_FIELD_AFFINE_Y
import com.konai.sendbirdapisampleapp.util.Constants.FIRESTORE_FIELD_USER_ID
import com.konai.sendbirdapisampleapp.util.Constants.INTENT_NAME_CHANNEL_URL
import com.konai.sendbirdapisampleapp.util.Constants.LOGIN_ACCOUNT_MESSAGE_RECEIVE_HANDLER
import com.konai.sendbirdapisampleapp.util.Constants.PREFERENCE_NAME_HASH
import com.konai.sendbirdapisampleapp.util.Constants.TAG
import com.konai.sendbirdapisampleapp.util.Constants.USER_ID
import com.konai.sendbirdapisampleapp.util.Constants.USER_NICKNAME
import com.konai.sendbirdapisampleapp.util.Extension.showToast
import com.sendbird.android.SendbirdChat
import com.sendbird.android.channel.BaseChannel
import com.sendbird.android.channel.GroupChannel
import com.sendbird.android.handler.GroupChannelHandler
import com.sendbird.android.message.BaseMessage
import com.sendbird.android.params.PreviousMessageListQueryParams
import com.sendbird.android.user.Member
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class ChannelActivity : AppCompatActivity(), CoroutineScope {
    private var db: FirebaseFirestore? = null
    private var friendId: String? = null
    private var friendNickname: String? = null
    private var encryptionMessageList: MutableList<MessageModel> = mutableListOf()
    private var decryptionMessageList: MutableList<MessageModel> = mutableListOf()
    private lateinit var binding: ActivityChannelBinding
    private lateinit var adapter: ChannelMessageAdapter
    private lateinit var channelURL: String
    private lateinit var hash: ByteArray

    lateinit var strongBox: StrongBox
    lateinit var localDB: KeyIdDatabase
    lateinit var encryptedSharedPreferencesManager: EncryptedSharedPreferencesManager

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + Job()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            //정상적이지 않은 방법으로 ChannelActivity 에 접근한 경우
            if(intent.action != CHANNEL_ACTIVITY_INTENT_ACTION) return

            binding = DataBindingUtil.setContentView(this, R.layout.activity_channel)
            binding.channelActivity = this
            db = Firebase.firestore
            localDB = DBProvider.getInstance(this)!!
            strongBox = StrongBox.getInstance(this)
            encryptedSharedPreferencesManager = EncryptedSharedPreferencesManager.getInstance(this)!!
            channelURL = intent.getStringExtra(INTENT_NAME_CHANNEL_URL)!!

            //내 디바이스에 로그인한 경우
            if (isMyDevice()) {
                initAdapter()
                displayChannelMembersId()
                launch {
                    showProgress()
                    confirmSharedSecretKey()
                    //readAllMessages()
                    dismissProgress()
                }
            }
            //다른 사람 디바이스에 로그인한 경우
            else {
                AlertDialog.Builder(this)
                    .setTitle("경고")
                    .setCancelable(false)
                    .setMessage("계정에 등록된 기기가 아닙니다. \n채널 생성/메시지 송신/메시지 복호화가 불가능합니다. ")
                    .setPositiveButton("확인") { _, _ ->
                        finish()
                    }
                    .create()
                    .show()
            }
        }
        catch (e: Exception) {
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
        db = null
    }

    private fun initAdapter() {
        adapter = ChannelMessageAdapter()
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
    }

    //채널에 참여 중인 사용자의 정보 UI 세팅
    private fun displayChannelMembersId() {
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
                binding.userIdTextView.text = "$USER_NICKNAME (ID : $USER_ID) [나]" //Activity : 내 정보 ex) userNickname(ID : userId)
                binding.myIdDetailLayoutTextView.text = "$USER_NICKNAME (ID : $USER_ID) [나]" //MotionLayout : 내 정보
            }
            //2. 그룹 채팅인 경우
            else {
                binding.userIdTextView.text = "$friendNickname (ID : $friendId)" //Activity : 친구 정보
                binding.partnerIdDetailLayoutTextView.text = "$friendNickname (ID : $friendId)" //MotionLayout : 친구 정보
                binding.myIdDetailLayoutTextView.text = "$USER_NICKNAME (ID : $USER_ID) [나]" //MotionLayout : 내 정보
            }
        }
    }

    //사용자가 자신의 디바이스에 로그인한지 확인
    private fun isMyDevice(): Boolean {
        //try 블럭이 정상적으로 실행되었다면 내 디바이스에 로그인한 경우
        try {
            strongBox.getECPublicKey(USER_ID)
            return true
        }
        //다른 사람 디바이스에 로그인한 경우 예외 발생 -> 메시지 암호화/복호화 불가
        catch (e: Exception) {
            return false
        }
    }

    //사용자가 대화 채널에 입장할 때 채널에 해당하는 SharedSecretKey 확인
    private suspend fun confirmSharedSecretKey() = withContext(Dispatchers.IO) {
        //val keyId: String? = localDB.keyIdDao().getKeyId(channelURL)

        localDB.keyIdDao().getKeyId(channelURL).let { keyId ->
            /*1. localDB 에 url 키에 해당하는 KeyId 가 없음
            사용자가 채널에 초대되고 채널에 처음으로 접근한 상황

            warning : Condition 'keyId == null' is always 'false'
            디바이스 하나에서 테스트하면 당연히 위와 같은 결과가 나올 수 밖에 없음. 따라서 신경 안써도 되는 경고 */
            if (keyId == null) {
                db!!.collection(FIRESTORE_DOCUMENT_PUBLIC_KEY)
                    .get()
                    .addOnSuccessListener { result ->
                        for (document in result) {
                            if (document.data[FIRESTORE_FIELD_USER_ID] == friendId) {
                                //1.1 FirebaseDB 에서 상대방 PublicKey 를 가져옴
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
                                    channel!!.getMetaData(data) { metaDataMap, e2 ->
                                        if (e2 != null) {
                                            e2.printStackTrace()
                                            return@getMetaData
                                        }
                                        val metadata = metaDataMap!!.values.toString()
                                            .substring(1 until metaDataMap.values.toString().length)
                                        Log.d(TAG, "metadata: $metadata")

                                        //1.3 sharedSecretKey 생성
                                        strongBox.generateSharedSecretKey(
                                            USER_ID,
                                            publicKey,
                                            metadata
                                        ).let { sharedSecretKey ->

                                            //2.4 ESP 에 sharedSecretKey 등록
                                            /*
                                            EncryptedSharedPreferences
                                            =======================================
                                            KeyId(Secure Random) | SharedSecretKey |
                                            =======================================
                                            */
                                            encryptedSharedPreferencesManager.putString(
                                                metadata,
                                                sharedSecretKey
                                            )
                                        }

                                        /*
                                        Local DB
                                        ====================================
                                        Channel URL | KeyId(Secure Random) |
                                        ====================================
                                        */
                                        CoroutineScope(Dispatchers.IO).launch {
                                            localDB.keyIdDao().insert(
                                                KeyIdEntity(
                                                    channelURL,
                                                    metadata
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        e.printStackTrace()
                        showToast("키 가져오기 실패")
                    }
            }
            /*2. localDB 에 url 키에 해당하는 KeyId 가 있음
            사용자가 채널을 만든 상황 or
            채널에 초대받은 사용자가 해당 채널에 처음 접근한게 아닌 상황*/
            else {
                CoroutineScope(Dispatchers.Main).launch {
                    AlertDialog.Builder(this@ChannelActivity)
                        .setCancelable(false)
                        .setMessage("SharedSecretKey 가 확인되었습니다.")
                        .setPositiveButton("확인") { _, _ -> }
                        .create()
                }
            }
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
                            encryptionMessageList.add(
                                MessageModel(
                                    message = message.message,
                                    sender = message.sender!!.userId,
                                    messageId = message.messageId,
                                    createdAt = message.createdAt
                                )
                            )
                            adapter.submitList(encryptionMessageList)
                            adapter.notifyDataSetChanged()
                            //TODO It will always be more efficient to use more specific change events if you can.
                        }
                    }
                }
            }
        )
    }

//[START Read message]
    private fun readAllMessages() {
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
    fun sendMessage() {
        //Local DB 에서 keyId 꺼내기
        CoroutineScope(Dispatchers.IO).launch {
            //1.URL 에 해당하는 KeyId 를 가져옴
            localDB.keyIdDao().getKeyId(channelURL).let { keyId ->
                //메시지 암호화
                strongBox.encrypt(
                    message = binding.messageEditText.text.toString(),
                    keyId = keyId
                ).let { encryptedMessage ->
                    Log.d(TAG, "sendMessage: $encryptedMessage")
                }
            }
        }
        
    
    
    
//        val userMessage: String = binding.messageEditText.text.toString()
//        val encryptedMessage = AESUtil().encryptionCBCMode(userMessage, hash)
//        val params = UserMessageCreateParams(encryptedMessage)
//        binding.messageEditText.text = null

//        GroupChannel.getChannel(channelURL) { groupChannel, e1 ->
//            if (e1 != null) {
//                e1.printStackTrace()
//                return@getChannel
//            }
//            groupChannel?.sendUserMessage(params) { message, e2 ->
//                if (e2 != null) {
//                    e2.printStackTrace()
//                    return@sendUserMessage
//                }
//                encryptionMessageList.add(
//                    MessageModel(
//                        message = message?.message,
//                        sender = message?.sender?.userId,
//                        messageId = message?.messageId,
//                        createdAt = message?.createdAt
//                    )
//                )
//                adapter.submitList(encryptionMessageList)
//                adapter.notifyDataSetChanged()
//                //TODO It will always be more efficient to use more specific change events if you can.
//                adjustRecyclerViewPosition()
//            }
//
//        }
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
    //리사이클러뷰 위치 조정
    private fun adjustRecyclerViewPosition() {
        binding.recyclerView.run {
            postDelayed({
                scrollToPosition(adapter!!.itemCount - 1)
            }, 300)
        }
    }

    private suspend fun showProgress() = withContext(coroutineContext) {
        binding.progressBar.visibility = View.VISIBLE
    }

    private suspend fun dismissProgress() = withContext(coroutineContext) {
        binding.progressBar.visibility = View.GONE
    }
//[END Util]
}