package com.konai.sendbirdapisampleapp.fragment

import android.content.Context.MODE_PRIVATE
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.konai.sendbirdapisampleapp.R
import com.konai.sendbirdapisampleapp.adapter.ChannelListAdapter
import com.konai.sendbirdapisampleapp.databinding.FragmentChannelBinding
import com.konai.sendbirdapisampleapp.model.ChannelListModel
import com.konai.sendbirdapisampleapp.strongbox.KeyProvider
import com.konai.sendbirdapisampleapp.strongbox.KeyStoreUtil
import com.konai.sendbirdapisampleapp.util.Constants.CHANNEL_META_DATA
import com.konai.sendbirdapisampleapp.util.Constants.FIRE_STORE_DOCUMENT_PUBLIC_KEY
import com.konai.sendbirdapisampleapp.util.Constants.FIRE_STORE_FIELD_AFFINE_X
import com.konai.sendbirdapisampleapp.util.Constants.FIRE_STORE_FIELD_AFFINE_Y
import com.konai.sendbirdapisampleapp.util.Constants.FIRE_STORE_FIELD_USER_ID
import com.konai.sendbirdapisampleapp.util.Constants.PREFERENCE_NAME_HASH
import com.konai.sendbirdapisampleapp.util.Constants.ALL_MESSAGE_RECEIVE_HANDLER
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
import java.math.BigInteger
import java.security.PrivateKey
import java.security.PublicKey

class ChannelListFragment : BaseFragment<FragmentChannelBinding>(R.layout.fragment_channel) {
    private var _channelList: MutableList<ChannelListModel> = mutableListOf()

    override fun initView() {
        super.initView()

        initRecyclerView()
        initMessageHandler()
        initCreateChannelButtonState()
    }

    override fun onResume() {
        super.onResume()
        initMessageHandler()
    }

    override fun onStop() {
        super.onStop()
        SendbirdChat.removeChannelHandler(ALL_MESSAGE_RECEIVE_HANDLER)
    }

//[START init]
    private fun initRecyclerView() = with(binding) {
        initChannelList()
        val adapter = ChannelListAdapter(requireContext()).apply {
            channelList = _channelList
        }
        chatListRecyclerView.adapter = adapter
        chatListRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun initMessageHandler() {
        SendbirdChat.addChannelHandler(
            ALL_MESSAGE_RECEIVE_HANDLER,
            object : GroupChannelHandler() {
                override fun onMessageReceived(channel: BaseChannel, message: BaseMessage) {
                    when (message) {
                        is UserMessage -> {
                            showToast("상대방 메시지 수신 : 채널 리스트 갱신")
                            initChannelList()
                        }
                    }
                }
            }
        )
    }

    private fun initCreateChannelButtonState() = with(binding) {
        if (!KeyStoreUtil().isKeyInKeyStore(USER_ID)) {
            createChannelLayoutButton.isEnabled = false
        }
        else {
            createChannelLayoutButton.setOnClickListener {
                onCreateChannelButtonClicked()
            }
        }
    }

    private fun initChannelList() {
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
                channels[idx].unreadMessageCount
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
            binding.chatListRecyclerView.adapter?.notifyDataSetChanged() //TODO It will always be more efficient to use more specific change events if you can. Rely on `notifyDataSetChanged` as a last resort.
        }
    }
//[END init]

//[START Click event]
    private fun onCreateChannelButtonClicked() {
        val invitedUserId = binding.userIdInputEditText.text.toString().ifEmpty { return }

        //TODO USER CHECK
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

            if (channel != null && db != null) {
                if (channel.name == "나와의 채팅") {
                    showToast("등록되지 않은 사용자입니다.")
                    return@createChannel
                }

                if (channel.members.size == 1) {
                    showToast("등록되지 않은 사용자입니다.")
                    channel.delete { exception ->
                        if (exception != null) {
                            Log.e(TAG, "can't delete dummy channel: $exception")
                        }
                    }
                    return@createChannel
                }
                Toast.makeText(requireContext(), "채널 생성", Toast.LENGTH_SHORT).show()


                //progress visible
                //suspend 함수로 묶어버림
                createChannelMetadataAndSharedKey(channel, invitedUserId)
                initChannelList()
                //progress invisible

                //액티비티 이동



//TODO 액티비티로 이동하면 처음 sp 값이 초기화가 안됨 -> 해결할 방법 필요함
// hash 까지 모두 생성되고 나서 이동하는 걸로 수정
//                val intent = Intent(requireContext(), ChannelActivity::class.java)
//                intent.putExtra(INTENT_NAME_CHANNEL_URL, channel.url)
//                intent.action = CHANNEL_ACTIVITY_INTENT_ACTION
//                startActivity(intent)
            }
        }
        binding.userIdInputEditText.text = null
    }
//[END Click event]

    private fun createChannelMetadataAndSharedKey(channel: GroupChannel, invitedUserId: String) {
        //SharedHash : XY Point(Firestore)-> update hash (SP)
        val randomNumbers_byteArray = KeyProvider().getRandomNumbers() //키 생성용
        val randomNumbers_str = Base64.encodeToString(randomNumbers_byteArray, Base64.DEFAULT) //서버 업로드용
        val privateKey: PrivateKey = KeyStoreUtil().getPrivateKeyFromKeyStore(USER_ID)!!

        createSharedHash(privateKey, invitedUserId, randomNumbers_byteArray, channel.url)

        //Meta data : sendbird channel meta data
        val metadata = mapOf(
            CHANNEL_META_DATA to randomNumbers_str
        )
        channel.createMetaData(metadata) { _, e ->
            if (e != null) {
                Log.e(TAG, "Creating channel metadata was failed : $e ")
                return@createMetaData
            }
        }
    }

    private fun createSharedHash(privateKey: PrivateKey, invitedUserId: String, randomNumbers: ByteArray, preferenceKey: String) {
        var affineX: BigInteger?
        var affineY: BigInteger?

        db!!.collection(FIRE_STORE_DOCUMENT_PUBLIC_KEY)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    if (document.data[FIRE_STORE_FIELD_USER_ID] == invitedUserId) {
                        showToast("상대방 공개키 affineX/affineY 얻음")
                        affineX = BigInteger(document.data[FIRE_STORE_FIELD_AFFINE_X].toString())
                        affineY = BigInteger(document.data[FIRE_STORE_FIELD_AFFINE_Y].toString())
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
                    }
                }
            }
            .addOnFailureListener { exception ->
                showToast("키 가져오기 실패")
                Log.e(TAG, "Error getting documents from firebase DB : $exception")
            }
    }
}