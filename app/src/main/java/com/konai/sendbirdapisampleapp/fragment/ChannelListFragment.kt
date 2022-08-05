package com.konai.sendbirdapisampleapp.fragment

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.konai.sendbirdapisampleapp.R
import com.konai.sendbirdapisampleapp.activity.ChannelActivity
import com.konai.sendbirdapisampleapp.adapter.ChannelListAdapter
import com.konai.sendbirdapisampleapp.databinding.FragmentChannelBinding
import com.konai.sendbirdapisampleapp.model.ChannelListModel
import com.konai.sendbirdapisampleapp.strongbox.KeyProvider
import com.konai.sendbirdapisampleapp.strongbox.KeyStoreUtil
import com.konai.sendbirdapisampleapp.util.Constants.ALL_MESSAGE_RECEIVE_HANDLER
import com.konai.sendbirdapisampleapp.util.Constants.CHANNEL_ACTIVITY_INTENT_ACTION
import com.konai.sendbirdapisampleapp.util.Constants.CHANNEL_META_DATA
import com.konai.sendbirdapisampleapp.util.Constants.FIRE_STORE_DOCUMENT_PUBLIC_KEY
import com.konai.sendbirdapisampleapp.util.Constants.FIRE_STORE_FIELD_AFFINE_X
import com.konai.sendbirdapisampleapp.util.Constants.FIRE_STORE_FIELD_AFFINE_Y
import com.konai.sendbirdapisampleapp.util.Constants.FIRE_STORE_FIELD_USER_ID
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
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + Job()

    override fun initView() {
        super.initView()

        initRecyclerView()
        initMessageHandler()
        initCreateChannelButtonState()
    }

    override fun onResume() {
        super.onResume()
        initRecyclerView()
        initMessageHandler()
    }

    override fun onStop() {
        super.onStop()
        SendbirdChat.removeChannelHandler(ALL_MESSAGE_RECEIVE_HANDLER)
    }

//[START init]
    private fun initRecyclerView() {
        launch {
            showProgress()
            initChannelList()
        }
        val adapter = ChannelListAdapter(requireContext()).apply {
            channelList = _channelList
        }

        binding.chatListRecyclerView.adapter = adapter
        binding.chatListRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun initMessageHandler() {
        SendbirdChat.addChannelHandler(
            ALL_MESSAGE_RECEIVE_HANDLER,
            object : GroupChannelHandler() {
                override fun onMessageReceived(channel: BaseChannel, message: BaseMessage) {
                    when (message) {
                        is UserMessage -> {
                            showToast("상대방 메시지 수신 : 채널 리스트 갱신")
                            launch {
                                showProgress()
                                initChannelList()
                                //dismissProgress()
                            }
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

    private suspend fun initChannelList() = with(Dispatchers.IO){
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
            binding.chatListRecyclerView.adapter?.notifyDataSetChanged()//TODO It will always be more efficient to use more specific change events if you can. Rely on `notifyDataSetChanged` as a last resort.
        }
    }
//[END init]

//[START Click event]
    private fun onCreateChannelButtonClicked() {
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
                channelURL = channel.url
                if (channelURL == null) return@createChannel
                requireContext().getSharedPreferences(PREFERENCE_NAME_HASH, Context.MODE_PRIVATE).let { sharedPreference ->
                    if (sharedPreference.contains(channelURL)) {
                        return@createChannel
                    }
                }

                Toast.makeText(requireContext(), "채널 생성", Toast.LENGTH_SHORT).show()
                launch {
                    showProgress()
                    createChannelMetadataAndSharedKey(channel, invitedUserId)
                    initChannelList()
                }
            }
        }
        binding.userIdInputEditText.text = null
    }

    private suspend fun createChannelMetadataAndSharedKey(channel: GroupChannel, invitedUserId: String) = withContext(Dispatchers.IO) {
        val privateKey: PrivateKey = KeyStoreUtil().getPrivateKeyFromKeyStore(USER_ID)!!
        KeyProvider().getRandomNumbers().let { randomNumber_bytearray -> //키 생성용
            //hash
            createSharedHash(privateKey, invitedUserId,  randomNumber_bytearray, channel.url)
            //random number
            Base64.encodeToString(randomNumber_bytearray, Base64.DEFAULT).let { randomNumber_str -> //서버 업로드용
                val metadata = mapOf(
                    CHANNEL_META_DATA to randomNumber_str
                )
                channel.createMetaData(metadata) { _, e ->
                    if (e != null) {
                        Log.e(TAG, "Creating channel metadata was failed : $e ")
                        return@createMetaData
                    }
                }
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

    private suspend fun showProgress() = withContext(coroutineContext) {
        with(binding) {
            progressBar.visibility = View.VISIBLE
            loginTextView.visibility = View.VISIBLE
        }
    }

    private suspend fun dismissProgress() = withContext(coroutineContext) {
        with(binding) {
            progressBar.visibility = View.INVISIBLE
            loginTextView.visibility = View.INVISIBLE
        }
    }
}