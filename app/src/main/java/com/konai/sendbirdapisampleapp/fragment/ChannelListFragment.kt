package com.konai.sendbirdapisampleapp.fragment

import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.konai.sendbirdapisampleapp.R
import com.konai.sendbirdapisampleapp.activity.ChannelActivity
import com.konai.sendbirdapisampleapp.adapter.ChannelListAdapter
import com.konai.sendbirdapisampleapp.databinding.FragmentChannelBinding
import com.konai.sendbirdapisampleapp.db.DBProvider
import com.konai.sendbirdapisampleapp.db.KeyIdDatabase
import com.konai.sendbirdapisampleapp.db.KeyIdEntity
import com.konai.sendbirdapisampleapp.models.ChannelListModel
import com.konai.sendbirdapisampleapp.strongbox.ECKeyUtil
import com.konai.sendbirdapisampleapp.strongbox.StrongBox
import com.konai.sendbirdapisampleapp.util.Constants.ALL_MESSAGE_RECEIVE_HANDLER
import com.konai.sendbirdapisampleapp.util.Constants.CHANNEL_ACTIVITY_INTENT_ACTION
import com.konai.sendbirdapisampleapp.util.Constants.CHANNEL_META_DATA
import com.konai.sendbirdapisampleapp.util.Constants.FIRESTORE_DOCUMENT_PUBLIC_KEY
import com.konai.sendbirdapisampleapp.util.Constants.FIRESTORE_FIELD_AFFINE_X
import com.konai.sendbirdapisampleapp.util.Constants.FIRESTORE_FIELD_AFFINE_Y
import com.konai.sendbirdapisampleapp.util.Constants.FIRESTORE_FIELD_USER_ID
import com.konai.sendbirdapisampleapp.util.Constants.INTENT_NAME_CHANNEL_URL
import com.konai.sendbirdapisampleapp.util.Constants.TAG
import com.konai.sendbirdapisampleapp.util.Constants.USER_ID
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
import kotlin.coroutines.CoroutineContext

class ChannelListFragment : BaseFragment<FragmentChannelBinding>(R.layout.fragment_channel), CoroutineScope {
    private var _channelList: MutableList<ChannelListModel> = mutableListOf()
    private lateinit var strongBox: StrongBox
    private lateinit var localDB: KeyIdDatabase
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + Job()

    override fun initView() {
        super.initView()

        try {
            binding.progressBarLayout.visibility = View.VISIBLE
            strongBox = StrongBox.getInstance(requireContext())
            localDB = DBProvider.getInstance(requireContext())!!
            initAdapter()
            launch {
                addMessageHandler()
            }
            showCreateChannelButtonState()
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()

        try {
            initAdapter()
            launch {
                addMessageHandler()
            }
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStop() {
        super.onStop()
        SendbirdChat.removeChannelHandler(ALL_MESSAGE_RECEIVE_HANDLER)
    }


    private fun initAdapter() {
        val adapter = ChannelListAdapter(requireContext()).apply {
            channelList = _channelList
        }
        binding.chatListRecyclerView.adapter = adapter
        binding.chatListRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        launch {
            fetchChannelList()
        }
    }

    //메시지가 수신될 때마다 채널 리스트를 갱신
    private suspend fun addMessageHandler() = withContext(Dispatchers.IO) {
        SendbirdChat.addChannelHandler(
            ALL_MESSAGE_RECEIVE_HANDLER,
            object : GroupChannelHandler() {
                override fun onMessageReceived(channel: BaseChannel, message: BaseMessage) {
                    when (message) {
                        is UserMessage -> {
                            Toast.makeText(requireContext(), "상대방 메시지 수신 : 채널 리스트 갱신", Toast.LENGTH_SHORT).show()
                                try {

                                    fetchLatestChannel(message.channelUrl, message.message, message.createdAt)
//                                    launch {
//                                        //TODO 순서만 바꿔주면 되는데 notifyDataSetChanged()는 무거움 다른 함수 필요
//                                        //fetchChannelList()
//                                    }
                                }
                                catch (e: Exception) {
                                    e.printStackTrace()
                                }
                        }
                    }
                }
            }
        )
    }


    private fun fetchLatestChannel(url:String, latestMessage:String, sendAt:Long) {
        //TODO sendAt, lastMessage 도 넣어서 UI를 갱신해 줘야함
        var idx = 0
        Log.d(TAG, "url: $url")

        for (i in _channelList) {
            Log.d(TAG, "channelList: $i")
        }

        for (channel in _channelList) {
            if(channel.url == url) {
                Log.d(TAG, "fetchLatestChannel idx: $idx")
                _channelList[idx].lastMessage = latestMessage //
                _channelList[idx].lastMessageTime = sendAt //TODO 이거 타입변환 해야할듯


                binding.chatListRecyclerView.adapter!!.notifyItemMoved(idx, 0)

                //순서바꾸기 ?
                val tt = _channelList[0]
                _channelList[0] = _channelList[idx]
                _channelList[idx] = tt
                //TODO _channelList 를 갱신해야함
                return
            }
            idx ++
        }
        Log.d(TAG, "newChannel: $idx")
        //새로운 채널에서 온 메시지 -> 가장 상당에 배치시킴
        //TODO out of idx exception
        //TODO _channelList 를 갱신해야함
        binding.chatListRecyclerView.adapter!!.notifyItemInserted(0)
    }




    //사용자 자신의 디바이스인 경우에만 채널 생성 버튼이 활성화됨
    private fun showCreateChannelButtonState() {
        //1. 키스토어에 사용자의 ECKeyPair 가 없을 때 (타인 디바이스를 사용하는 경우)
        if(ECKeyUtil.isECKeyPair(USER_ID).not()) {
            binding.createChannelLayoutButton.isEnabled = false
        }
        //2. 키스토어에 사용자의 ECKeyPair 가 있을 때 (사용자 디바이스)
        else {
            binding.createChannelLayoutButton.setOnClickListener {
                try {
                    createChannel()
                }
                catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    //사용자가 참여하고 있는 채널 데이터를 센드버드 서버로부터 받아와 채널 리스트를 생성
    private suspend fun fetchChannelList() = withContext(Dispatchers.IO) {
        val query = GroupChannel.createMyGroupChannelListQuery(
            GroupChannelListQueryParams().apply {
                includeEmpty = false //비어있는 채팅방은 허용 안함
                myMemberStateFilter = MyMemberStateFilter.JOINED //사용자가 참여하고 있는 채널
                order = GroupChannelListQueryOrder.LATEST_LAST_MESSAGE //가장 최근 채널 순으로 정렬
            }
        )
        query.next { channels, e ->
            if (e != null) {
                e.printStackTrace()
                return@next
            }

            if (channels!!.isEmpty()) {
                return@next
            }

            _channelList.clear() //To make the list empty

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
            binding.chatListRecyclerView.adapter?.notifyDataSetChanged()
            binding.progressBarLayout.visibility = View.GONE
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
                e.printStackTrace()
                return@createChannel
            }

            if (channel == null) return@createChannel

            // 유효하지 않은 사용자를 입력해 멤버가 1인 채널이 생성되었지만
            // 그 채널이 기존에 만들어두었던 "나와의 채팅"의 채널을 재활용한 경우
            if (channel.name == "나와의 채팅") {
                Toast.makeText(requireContext(), "등록되지 않은 사용자입니다.", Toast.LENGTH_SHORT).show()
                return@createChannel
            }

            // 유효하지 않은 사용자를 입력해 멤버가 1인 채널이 생성된 경우 -> 생성된 채널 삭제
            if (channel.members.size == 1) {
                Toast.makeText(requireContext(), "등록되지 않은 사용자입니다.", Toast.LENGTH_SHORT).show()
                channel.delete { e ->
                    e?.printStackTrace()
                }
                return@createChannel
            }

            launch {
                generateSharedSecreteKey(channel, invitedUserId).let { keyId ->
                    withContext(Dispatchers.IO) {
                        localDB.keyIdDao().insert(
                            KeyIdEntity(
                                urlHash = channel.url,
                                keyId = keyId
                            )
                        )
                        fetchChannelList()
                        //TODO TEST Check
                        //정상적으로 채널과 SharedSecretKey 생성을 마쳤다면 ChannelActivity 로 이동
                        startActivity(
                            Intent(requireContext(), ChannelActivity::class.java).apply {
                                putExtra(INTENT_NAME_CHANNEL_URL, channel.url)
                                action = CHANNEL_ACTIVITY_INTENT_ACTION
                            }
                        )

                    }
                }
            }

            //정상적으로 채널과 SharedSecretKey 생성을 마쳤다면 ChannelActivity 로 이동
//            startActivity(
//                Intent(requireContext(), ChannelActivity::class.java).apply {
//                    putExtra(INTENT_NAME_CHANNEL_URL, channel.url)
//                    action = CHANNEL_ACTIVITY_INTENT_ACTION
//                }
//            )
        }
        binding.userIdInputEditText.text = null
    }

    //SharedSecretKey 생성, 채널 메타데이터로 secureRandom 업로드
    private suspend fun generateSharedSecreteKey(channel: GroupChannel, friendId: String): String {
        //SharedSecretKey 만들 때와 KeyId, 채널 메타데이터로 사용됨
        val secureRandom = strongBox.generateRandom(32)

        withContext(Dispatchers.IO) {
            //firebaseDB
            remoteDB!!.collection(FIRESTORE_DOCUMENT_PUBLIC_KEY)
                .get()
                .addOnSuccessListener { result ->
                    for (document in result) {
                        if (document.data[FIRESTORE_FIELD_USER_ID] == friendId) {
                            //SharedSecretKey 생성
                            strongBox.generateSharedSecretKey(
                                USER_ID, //PrivateKey 가 저장되어 있는 keyAlias
                                publicKey = ECKeyUtil.coordinatePublicKey(
                                    affineX = document.data[FIRESTORE_FIELD_AFFINE_X].toString(),
                                    affineY = document.data[FIRESTORE_FIELD_AFFINE_Y].toString()
                                ),
                                nonce = secureRandom
                            ).let { keyId ->
                                //채널 메타데이터로 secureRandom 을 센드버드 서버에 업로드
                                val metadata = mapOf(
                                    CHANNEL_META_DATA to keyId
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
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                }
        }
        return secureRandom
    }
}