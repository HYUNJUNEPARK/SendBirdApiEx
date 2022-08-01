package com.konai.sendbirdapisampleapp.fragment

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.konai.sendbirdapisampleapp.R
import com.konai.sendbirdapisampleapp.activity.ChannelActivity
import com.konai.sendbirdapisampleapp.adapter.ChannelListAdapter
import com.konai.sendbirdapisampleapp.databinding.FragmentChannelBinding
import com.konai.sendbirdapisampleapp.model.ChannelListModel
import com.konai.sendbirdapisampleapp.preference.KeySharedPreference
import com.konai.sendbirdapisampleapp.strongbox.KeyProvider
import com.konai.sendbirdapisampleapp.strongbox.KeyStoreUtil
import com.konai.sendbirdapisampleapp.util.Constants
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
import com.sendbird.android.channel.GroupChannel
import com.sendbird.android.channel.query.GroupChannelListQueryOrder
import com.sendbird.android.channel.query.MyMemberStateFilter
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
        binding.createChannelLayoutButton.setOnClickListener {
            onCreateChannelButtonClicked()
        }
    }

    override fun onResume() {
        super.onResume()
        initChannelList()
    }

    private fun initRecyclerView() {
        initChannelList()

        val adapter = ChannelListAdapter(requireContext())
        adapter.channelList = _channelList
        binding.chatListRecyclerView.adapter = adapter
        binding.chatListRecyclerView.layoutManager = LinearLayoutManager(requireContext())
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

            //TODO lastMessageTime Type string -> long
            for (idx in channels.indices) {
                _channelList.add(
                    ChannelListModel(
                        name = channels[idx].name,
                        url = channels[idx].url,
                        lastMessage = channels[idx].lastMessage?.message,
                        lastMessageTime = channels[idx].lastMessage?.createdAt
                    )
                )
            }

            //TODO It will always be more efficient to use more specific change events if you can. Rely on `notifyDataSetChanged` as a last resort.
            binding.chatListRecyclerView.adapter?.notifyDataSetChanged()
        }
    }

    //TODO dataBinding onClicked
    private fun onCreateChannelButtonClicked() {
        val invitedUserId = binding.userIdInputEditText.text.toString()
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
                showToast("$e")
            }

            if (channel != null) {
                Toast.makeText(requireContext(), "채널 생성", Toast.LENGTH_SHORT).show()
                createChannelMetadataAndSharedKey(channel, invitedUserId)

                initChannelList()

//TODO 액티비티로 이동하면 처음 sp 값이 초기화가 안됨 -> 해결할 방법 필요함
//                val intent = Intent(requireContext(), ChannelActivity::class.java)
//                intent.putExtra(INTENT_NAME_CHANNEL_URL, channel.url)
//                intent.action = CHANNEL_ACTIVITY_INTENT_ACTION
//                startActivity(intent)
            }
        }
        binding.userIdInputEditText.text = null
    }

    private fun createChannelMetadataAndSharedKey(channel: GroupChannel, invitedUserId: String) {
        //SharedHash
        val randomNumbers_byteArray = KeyProvider().getRandomNumbers() //키 생성용
        val randomNumbers_str = Base64.encodeToString(randomNumbers_byteArray, Base64.DEFAULT) //서버 업로드용
        val privateKey: PrivateKey = KeyStoreUtil().getPrivateKeyFromKeyStore(USER_ID)!!

        createSharedHash(privateKey, invitedUserId, randomNumbers_byteArray, channel.url)

        //Meta data
        val metadata = mapOf(
            CHANNEL_META_DATA to randomNumbers_str
        )
        channel.createMetaData(metadata) { map, e ->
            if (e != null) {
                Log.e(TAG, "Creating channel metadata was failed : $e ")
                return@createMetaData
            }
        }
    }

    private fun createSharedHash(privateKey: PrivateKey, invitedUserId: String, randomNumbers: ByteArray, preferenceKey: String) {
        var affineX: BigInteger?
        var affineY: BigInteger?

        db?.collection(FIRE_STORE_DOCUMENT_PUBLIC_KEY)
            ?.get()
            ?.addOnSuccessListener { result ->
                for (document in result) {
                    if (document.data[FIRE_STORE_FIELD_USER_ID] == invitedUserId) {
                        showToast("상대방 공개키 affineX/affineY 얻음")
                        affineX = BigInteger(document.data[FIRE_STORE_FIELD_AFFINE_X].toString())
                        affineY = BigInteger(document.data[FIRE_STORE_FIELD_AFFINE_Y].toString())
                        val publicKey: PublicKey = KeyStoreUtil().createPublicKeyByECPoint(affineX!!, affineY!!)
                        val sharedSecretHash: ByteArray = KeyProvider().createSharedSecretHash(
                            privateKey,
                            publicKey!!,
                            randomNumbers
                        )

                        //Shared preference
                        val sharedPreferences = requireContext().getSharedPreferences(PREFERENCE_NAME_HASH, MODE_PRIVATE)
                        val editor: SharedPreferences.Editor = sharedPreferences.edit()
                        val hash: String = Base64.encodeToString(sharedSecretHash, Base64.DEFAULT)
                        editor.putString(preferenceKey, hash)
                        editor.apply()
                        //Shared preference



                        Log.d(TAG, "getSharedKey: $sharedSecretHash")
                    }
                }
            }
            ?.addOnFailureListener { exception ->
                showToast("키 가져오기 실패")
                Log.e(TAG, "Error getting documents from firestore : $exception")
            }
    }
}