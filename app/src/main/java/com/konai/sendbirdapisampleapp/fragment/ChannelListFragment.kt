package com.konai.sendbirdapisampleapp.fragment

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.util.Base64
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.konai.sendbirdapisampleapp.R
import com.konai.sendbirdapisampleapp.activity.ChannelActivity
import com.konai.sendbirdapisampleapp.adapter.ChannelListAdapter
import com.konai.sendbirdapisampleapp.databinding.FragmentChannelBinding
import com.konai.sendbirdapisampleapp.models.ChannelListModel
import com.konai.sendbirdapisampleapp.strongbox.ECKeyUtil
import com.konai.sendbirdapisampleapp.strongbox.StrongBox
import com.konai.sendbirdapisampleapp.tmpstrongbox.KeyProvider
import com.konai.sendbirdapisampleapp.tmpstrongbox.KeyStoreUtil
import com.konai.sendbirdapisampleapp.util.Constants.ALL_MESSAGE_RECEIVE_HANDLER
import com.konai.sendbirdapisampleapp.util.Constants.CHANNEL_ACTIVITY_INTENT_ACTION
import com.konai.sendbirdapisampleapp.util.Constants.CHANNEL_META_DATA
import com.konai.sendbirdapisampleapp.util.Constants.FIRESTORE_DOCUMENT_PUBLIC_KEY
import com.konai.sendbirdapisampleapp.util.Constants.FIRESTORE_FIELD_AFFINE_X
import com.konai.sendbirdapisampleapp.util.Constants.FIRESTORE_FIELD_AFFINE_Y
import com.konai.sendbirdapisampleapp.util.Constants.FIRESTORE_FIELD_USER_ID
import com.konai.sendbirdapisampleapp.util.Constants.INTENT_NAME_CHANNEL_URL
import com.konai.sendbirdapisampleapp.util.Constants.PREFERENCE_NAME_HASH
import com.konai.sendbirdapisampleapp.util.Constants.TAG
import com.konai.sendbirdapisampleapp.util.Constants.USER_ID
import com.konai.sendbirdapisampleapp.util.Extension.showToast
import com.sendbird.android.SendbirdChat
import com.sendbird.android.channel.BaseChannel
import com.sendbird.android.channel.GroupChannel
import com.sendbird.android.channel.query.GroupChannelListQueryOrder
import com.sendbird.android.channel.query.MyMemberStateFilter
import com.sendbird.android.handler.GroupChannelHandler
import com.sendbird.android.message.BaseMessage
import com.sendbird.android.message.UserMessage
import com.sendbird.android.params.GroupChannelCreateParams
import com.sendbird.android.params.GroupChannelListQueryParams
import kotlinx.coroutines.*
import java.math.BigInteger
import java.security.PrivateKey
import java.security.PublicKey
import kotlin.coroutines.CoroutineContext

class ChannelListFragment : BaseFragment<FragmentChannelBinding>(R.layout.fragment_channel), CoroutineScope {
    private var _channelList: MutableList<ChannelListModel> = mutableListOf()
    private var channelURL: String? = null
    private lateinit var strongBox: StrongBox
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + Job()

    override fun initView() {
        super.initView()

        try {
            strongBox = StrongBox.getInstance(requireContext())
            initRecyclerView()
            addMessageHandler()
            createChannelButtonState()
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()

        try {
            initRecyclerView()
            addMessageHandler()
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStop() {
        super.onStop()
        SendbirdChat.removeChannelHandler(ALL_MESSAGE_RECEIVE_HANDLER)
    }

    private fun initRecyclerView() {
        launch {
            showProgressBar()
            initChannelList()
            dismissProgressBar()
        }
        val adapter = ChannelListAdapter(requireContext()).apply {
            channelList = _channelList
        }

        binding.chatListRecyclerView.adapter = adapter
        binding.chatListRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }


    //메시지가 수신될 때마다 채널 리스트를 갱신
    private fun addMessageHandler() {
        SendbirdChat.addChannelHandler(
            ALL_MESSAGE_RECEIVE_HANDLER,
            object : GroupChannelHandler() {
                override fun onMessageReceived(channel: BaseChannel, message: BaseMessage) {
                    when (message) {
                        is UserMessage -> {
                            showToast("상대방 메시지 수신 : 채널 리스트 갱신")
                            launch {
                                showProgressBar()
                                initChannelList()
                                dismissProgressBar()
                            }
                        }
                    }
                }
            }
        )
    }

    //사용자 자신의 디바이스인 경우에만 채널 생성 버튼이 활성화됨
    private fun createChannelButtonState() {
        //키스토어에 사용자의 ECKeyPair 가 있을 때 (사용자 디바이스)
        if(!ECKeyUtil.isECKeyPair(USER_ID)) {
            binding.createChannelLayoutButton.isEnabled = false
        }
        //키스토어에 사용자의 ECKeyPair 가 없을 때 (타인 디바이스를 사용하는 경우)
        else {
            binding.createChannelLayoutButton.setOnClickListener {
                createChannel()
            }
        }
    }

    //사용자가 참여하고 있는 채널의 데이터를 센드버드 서버로부터 받아와 채널 리스트를 생성
    private suspend fun initChannelList() = withContext(Dispatchers.IO){
        val query = GroupChannel.createMyGroupChannelListQuery(
            GroupChannelListQueryParams().apply {
                includeEmpty = true
                myMemberStateFilter = MyMemberStateFilter.JOINED
                order = GroupChannelListQueryOrder.LATEST_LAST_MESSAGE
            }
        )
        query.next { channels, e ->
            if (e != null) {
                showToast("$e")
                return@next
            }
            if (channels!!.isEmpty()) return@next
            _channelList.clear() //to make the list empty
            for (idx in channels.indices) {
                _channelList.add(
                    ChannelListModel(
                        name = channels[idx].name,
                        url = channels[idx].url,
                        lastMessage = channels[idx].lastMessage?.message,
                        lastMessageTime = channels[idx].lastMessage?.createdAt,
                        memberSize = channels[idx].memberCount
                    )
                )
            }
            launch(Dispatchers.Main) {
                binding.chatListRecyclerView.adapter?.notifyDataSetChanged()
                //TODO It will always be more efficient to use more specific change events if you can. Rely on `notifyDataSetChanged` as a last resort.
            }
        }
    }

    //유효하지 않은 사용자를 걸러내고 채널을 생성
    private fun createChannel() {
        val invitedUserId = binding.userIdInputEditText.text.toString().ifEmpty { return }
        val users: List<String> = listOf(USER_ID, invitedUserId)
        val params = GroupChannelCreateParams().apply {
            userIds = users
            operatorUserIds = users
            isDistinct = true
            name = "$USER_ID, $invitedUserId"
            isSuper = false
        }
        GroupChannel.createChannel(params) { channel, e ->
            if (e != null) {
                showToast("channel create error : $e")
                return@createChannel
            }
            if (channel == null) return@createChannel
            // 유효하지 않은 사용자를 입력해 멤버가 1인 채널이 생성되었지만
            // 그 채널이 기존에 만들어두었던 "나와의 채팅"의 채널을 재활용한 경우
            if (channel.name == "나와의 채팅") {
                showToast("등록되지 않은 사용자입니다.")
                return@createChannel
            }
            // 유효하지 않은 사용자를 입력해 멤버가 1인 채널이 생성된 경우 -> 생성된 채널 삭제
            if (channel.members.size == 1) {
                showToast("등록되지 않은 사용자입니다.")
                channel.delete { e ->
                    e?.printStackTrace()
                }
                return@createChannel
            }
            channelURL = channel.url

            //TODO 멤버는 2명이지만 채널에 해당하는 Key 가 있다면 -> 추가 작업 없이 종료

//            requireContext().getSharedPreferences(PREFERENCE_NAME_HASH, Context.MODE_PRIVATE)
//                .let { sharedPreference ->
//                    if (sharedPreference.contains(channelURL)) {
//                        return@createChannel
//                    }
//                }
            //Toast.makeText(requireContext(), "채널 생성", Toast.LENGTH_SHORT).show()

            launch {
                showProgressBar()
                generateSharedSecreteKey(channel, invitedUserId)
                initChannelList()
                dismissProgressBar()
            }
        }
        binding.userIdInputEditText.text = null
    }

    //SharedSecretKey 생성, 채널 메타데이터로 secureRandom 업로드
    private suspend fun generateSharedSecreteKey(
        channel: GroupChannel, friendId: String
    )= withContext(Dispatchers.IO) {

        //TODO 시큐어 랜덤을 만든다
        val secureRandom = strongBox.generateRandom(32)

        //TODO 상대방의 ID 를 갖고 파이어베이스에서 X,Y 를 가져와 퍼블릭키를 만든다.
        db!!.collection(FIRESTORE_DOCUMENT_PUBLIC_KEY)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    if (document.data[FIRESTORE_FIELD_USER_ID] == friendId) {
                        val affineX = document.data[FIRESTORE_FIELD_AFFINE_X].toString()
                        val affineY = document.data[FIRESTORE_FIELD_AFFINE_Y].toString()
                        showToast("상대방 공개키 affineX/affineY 얻음")

                        //TODO SSK 를 만든다.
                        //TODO 이거 쓰면 안되고 테스트용으로 하나 오버로딩해야함!
                        strongBox.generateSharedSecretKey(
                            publicKey = ECKeyUtil.coordinatePublicKey(affineX, affineY),
                            nonce = secureRandom
                        )


//                        val publicKey: PublicKey = KeyStoreUtil().createPublicKeyByECPoint(affineX!!, affineY!!)
//
//
//                        val sharedSecretHash: ByteArray = KeyProvider().createSharedSecretHash(
//                            privateKey,
//                            publicKey,
//                            randomNumbers
//                        )
//                        val hash: String = Base64.encodeToString(sharedSecretHash, Base64.DEFAULT)
//                        requireContext().getSharedPreferences(PREFERENCE_NAME_HASH, MODE_PRIVATE).edit().apply {
//                            putString(preferenceKey, hash)
//                            apply()
//                        }
//                        Log.d(TAG, "getSharedKey: $sharedSecretHash")
//                        showToast("해시 저장 완료")
//
//                        val intent = Intent(requireContext(), ChannelActivity::class.java).apply {
//                            putExtra(INTENT_NAME_CHANNEL_URL, channelURL)
//                            action = CHANNEL_ACTIVITY_INTENT_ACTION
//                        }
//                        startActivity(intent)
                    }
                }
            }
            .addOnFailureListener { exception ->
                showToast("키 가져오기 실패")
                Log.e(TAG, "Error getting documents from firebase DB : $exception")
            }






        //TODO SSK 를 만들고 반환 받은 KeyId 는 Room 의 Value 가 된다. (Key: 채널 URL//Value: KeyId)

        val privateKey: PrivateKey = KeyStoreUtil().getPrivateKeyFromKeyStore(USER_ID)!!
        KeyProvider().getRandomNumbers().let { randomNumber_bytearray -> //키 생성용
            //hash
            createSharedHash(privateKey, friendId,  randomNumber_bytearray, channel.url)
            //random number
            Base64.encodeToString(randomNumber_bytearray, Base64.DEFAULT).let { randomNumber_str -> //서버 업로드용
                val metadata = mapOf(
                    CHANNEL_META_DATA to randomNumber_str
                )
                channel.createMetaData(metadata) { _, e ->
                    if (e != null) {
                        e.printStackTrace()
                        return@createMetaData
                    }
                }
            }
        }
    }











    //TODO 위에랑 합쳐버릴 것
    private fun createSharedHash(privateKey: PrivateKey, invitedUserId: String, randomNumbers: ByteArray, preferenceKey: String) {
        var affineX: BigInteger?
        var affineY: BigInteger?

        db!!.collection(FIRESTORE_DOCUMENT_PUBLIC_KEY)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    if (document.data[FIRESTORE_FIELD_USER_ID] == invitedUserId) {
                        showToast("상대방 공개키 affineX/affineY 얻음")
                        affineX = BigInteger(document.data[FIRESTORE_FIELD_AFFINE_X].toString())
                        affineY = BigInteger(document.data[FIRESTORE_FIELD_AFFINE_Y].toString())
                        val publicKey: PublicKey = KeyStoreUtil().createPublicKeyByECPoint(affineX!!, affineY!!)
                        val sharedSecretHash: ByteArray = KeyProvider().createSharedSecretHash(
                            privateKey,
                            publicKey,
                            randomNumbers
                        )
                        val hash: String = Base64.encodeToString(sharedSecretHash, Base64.DEFAULT)
                        requireContext().getSharedPreferences(PREFERENCE_NAME_HASH, MODE_PRIVATE).edit().apply {
                            putString(preferenceKey, hash)
                            apply()
                        }
                        Log.d(TAG, "getSharedKey: $sharedSecretHash")
                        showToast("해시 저장 완료")

                        val intent = Intent(requireContext(), ChannelActivity::class.java).apply {
                            putExtra(INTENT_NAME_CHANNEL_URL, channelURL)
                            action = CHANNEL_ACTIVITY_INTENT_ACTION
                        }
                        startActivity(intent)
                    }
                }
            }
            .addOnFailureListener { exception ->
                showToast("키 가져오기 실패")
                Log.e(TAG, "Error getting documents from firebase DB : $exception")
            }
    }

    private suspend fun showProgressBar() = withContext(coroutineContext) {
        binding.progressBar.visibility = View.VISIBLE
        binding.loginTextView.visibility = View.VISIBLE
    }

    private suspend fun dismissProgressBar() = withContext(coroutineContext) {
        binding.progressBar.visibility = View.INVISIBLE
        binding.loginTextView.visibility = View.INVISIBLE
    }
}