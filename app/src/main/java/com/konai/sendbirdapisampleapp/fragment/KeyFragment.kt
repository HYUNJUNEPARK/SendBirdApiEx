package com.konai.sendbirdapisampleapp.fragment

import android.content.Context
import android.util.Log
import com.konai.sendbirdapisampleapp.R
import com.konai.sendbirdapisampleapp.databinding.FragmentBlankBinding
import com.konai.sendbirdapisampleapp.strongbox.KeyStoreUtil
import com.konai.sendbirdapisampleapp.util.Constants
import com.konai.sendbirdapisampleapp.util.Constants.FIRE_STORE_FIELD_USER_ID
import com.konai.sendbirdapisampleapp.util.Constants.PREFERENCE_NAME_HASH
import com.konai.sendbirdapisampleapp.util.Constants.TAG
import com.konai.sendbirdapisampleapp.util.Constants.USER_ID
import com.konai.sendbirdapisampleapp.util.Extension.showToast
import com.sendbird.android.channel.GroupChannel
import com.sendbird.android.channel.query.GroupChannelListQueryOrder
import com.sendbird.android.channel.query.MyMemberStateFilter
import com.sendbird.android.params.GroupChannelListQueryParams

class KeyFragment : BaseFragment<FragmentBlankBinding>(R.layout.fragment_blank) {
    override fun initView() {
        super.initView()

        if (db == null) {
            showToast("Firebase DB initialize error")
            return
        }

        initKeyStoreKeyState()
        initServerKeyStateIcon()
        initAvailableHashNumber()
    }

    private fun initAvailableHashNumber() = with(binding) {
        var allChannelUrlList: MutableList<String> = mutableListOf()
        val allHashSb = StringBuffer("")
        val availableHashSb = StringBuffer("")
        var availableHashNumber = 0
        //기기에 저장된 모든 해시
        requireContext().getSharedPreferences(PREFERENCE_NAME_HASH, Context.MODE_PRIVATE)
            .let { sharedPreference ->
                devicePublicKeyCountTextView.text = sharedPreference.all.size.toString()

                for (i in sharedPreference.all) {
                    allHashSb.append("${i.key}\n\n")
                    allChannelUrlList.add(i.key)
                }
                allHashSavedOnDeviceTextView.text = allHashSb.toString()
            }

        //로그인 계정이 사용할 수 있는 해시 -> 로그인 채널의 url == 기기에 저장된 hash 비교
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

            for (idx in channels.indices) {
                for (url in allChannelUrlList) {
                    if (channels[idx].url == url) {
                        availableHashSb.append("${channels[idx].url}\n\n")
                        availableHashNumber++
                    }
                }
            }
            availableHashNumberTextView.text = availableHashNumber.toString()
            availableHashTextView.text = availableHashSb
        }
    }

    private fun initKeyStoreKeyState() {
        if (KeyStoreUtil().isKeyInKeyStore(USER_ID)) {
            //public
            KeyStoreUtil().getPublicKeyFromKeyStore(USER_ID).let { publicKey ->
                if (publicKey == null) binding.publicKeyStateImageView.setImageResource(R.drawable.ic_baseline_error_24)
                else binding.publicKeyStateImageView.setImageResource(R.drawable.ic_baseline_check_circle_24)
            }
            //private
            KeyStoreUtil().getPrivateKeyFromKeyStore(USER_ID).let { privateKey ->
                if (privateKey == null) binding.privateKeyStateImageView.setImageResource(R.drawable.ic_baseline_error_24)
                else binding.privateKeyStateImageView.setImageResource(R.drawable.ic_baseline_check_circle_24)
            }
        }
    }

    private fun initServerKeyStateIcon() {
        db!!.collection(Constants.FIRE_STORE_DOCUMENT_PUBLIC_KEY)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    Log.e(TAG, "Error : Empty Firebase DB")
                    return@addOnSuccessListener
                }
                for (document in result) {
                    if (document.data[FIRE_STORE_FIELD_USER_ID] == USER_ID) {
                        binding.serverPublicKeyStateImageView.setImageResource(R.drawable.ic_baseline_check_circle_24)
                    }
                }
            }
        .addOnFailureListener { exception ->
            showToast("서버키 상태 로드 실패")
            Log.e(TAG, "Error getting key state from firestore : $exception")
        }
    }
}