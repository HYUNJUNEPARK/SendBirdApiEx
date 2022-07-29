package com.konai.sendbirdapisampleapp.fragment

import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.konai.sendbirdapisampleapp.R
import com.konai.sendbirdapisampleapp.adapter.ChannelListAdapter
import com.konai.sendbirdapisampleapp.databinding.FragmentChannelBinding
import com.konai.sendbirdapisampleapp.model.ChannelListModel
import com.konai.sendbirdapisampleapp.strongbox.KeyProvider
import com.konai.sendbirdapisampleapp.strongbox.KeyStoreUtil
import com.konai.sendbirdapisampleapp.strongbox.StrongBoxConstants
import com.konai.sendbirdapisampleapp.strongbox.StrongBoxConstants.KEY_GEN_ALGORITHM
import com.konai.sendbirdapisampleapp.util.Constants
import com.konai.sendbirdapisampleapp.util.Constants.FIRE_STORE_DOCUMENT_PUBLIC_KEY
import com.konai.sendbirdapisampleapp.util.Constants.FIRE_STORE_FIELD_AFFINE_X
import com.konai.sendbirdapisampleapp.util.Constants.FIRE_STORE_FIELD_AFFINE_Y
import com.konai.sendbirdapisampleapp.util.Constants.TAG
import com.konai.sendbirdapisampleapp.util.Constants.USER_ID
import com.konai.sendbirdapisampleapp.util.Extension.showToast
import com.sendbird.android.channel.GroupChannel
import com.sendbird.android.channel.query.GroupChannelListQueryOrder
import com.sendbird.android.channel.query.MyMemberStateFilter
import com.sendbird.android.params.GroupChannelCreateParams
import com.sendbird.android.params.GroupChannelListQueryParams
import java.math.BigInteger
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec

class ChannelListFragment : BaseFragment<FragmentChannelBinding>(R.layout.fragment_channel) {
    private var _channelList: MutableList<ChannelListModel> = mutableListOf()

    override fun initView() {
        super.initView()

        initRecyclerView()
        //TODO clean code
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



            }
        }
        binding.userIdInputEditText.text = null
    }

    private fun createChannelMetadataAndSharedKey(channel: GroupChannel, invitedUserId: String) {
        val randomNumbers_byteArray = KeyProvider().getRandomNumbers() //키 생성용
        val randomNumbers_str = Base64.encodeToString(randomNumbers_byteArray, Base64.DEFAULT) //서버 업로드용

        //TODO ERROR
        val privateKey: PrivateKey = KeyStoreUtil().getPrivateKeyFromKeyStore(USER_ID)!!

        getSharedKey(privateKey, invitedUserId, randomNumbers_byteArray)


        //metaData
        val metadata = mapOf(
            "metadata" to randomNumbers_str
        )
        channel.createMetaData(metadata) { map, e ->
            if (e != null) {
                Log.e(TAG, "creating channel metadata was failed : $e ")
                return@createMetaData
            }
        }
    }

    private fun getSharedKey(privateKey: PrivateKey, invitedUserId: String, randomNumbers: ByteArray) {
        val db = Firebase.firestore

        //TODO 키스토어에 키가 있는지 확인

        var affineX: BigInteger?
        var affineY: BigInteger?

        db.collection(FIRE_STORE_DOCUMENT_PUBLIC_KEY)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    if (document.data[Constants.FIRE_STORE_FIELD_USER_ID] == invitedUserId) {
                        showToast("상대방 공개키 얻음")
                        //상대방 공개키 조합
                        affineX = BigInteger(document.data[FIRE_STORE_FIELD_AFFINE_X].toString())
                        affineY = BigInteger(document.data[FIRE_STORE_FIELD_AFFINE_Y].toString())
                        val ecPoint = ECPoint(affineX, affineY)
                        val params = KeyStoreUtil().getECParameterSpec()
                        val keySpec = ECPublicKeySpec(ecPoint, params)
                        val keyFactory = KeyFactory.getInstance(KEY_GEN_ALGORITHM) //EC
                        val publicKey: PublicKey = keyFactory.generatePublic(keySpec)

                        //TODO Error keyAgreement.init(myPrivateKey)
                        val sharedSecretHash = KeyProvider().createSharedSecretHash(
                            privateKey,
                            publicKey!!,
                            randomNumbers
                        )

                        //TODO 스트링 타입으로 바꾼다음에 sharedPreference 에 저장
                        Log.d(TAG, "getSharedKey: $sharedSecretHash")

                    }
                }
            }
            .addOnFailureListener { exception ->
                showToast("키 가져오기 실패")
                Log.e(TAG, "Error getting documents from firestore : $exception")
            }
    }
}