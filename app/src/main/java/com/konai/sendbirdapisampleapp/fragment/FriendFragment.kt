package com.konai.sendbirdapisampleapp.fragment

import android.content.Intent
import com.konai.sendbirdapisampleapp.R
import com.konai.sendbirdapisampleapp.activity.MyChannelActivity
import com.konai.sendbirdapisampleapp.databinding.FragmentFriendBinding
import com.konai.sendbirdapisampleapp.util.Constants.CHANNEL_ACTIVITY_INTENT_ACTION
import com.konai.sendbirdapisampleapp.util.Constants.INTENT_NAME_CHANNEL_URL
import com.konai.sendbirdapisampleapp.util.Constants.INTENT_NAME_USER_ID
import com.konai.sendbirdapisampleapp.util.Constants.INTENT_NAME_USER_NICK
import com.konai.sendbirdapisampleapp.util.Constants.USER_ID
import com.konai.sendbirdapisampleapp.util.Constants.USER_NICKNAME
import com.konai.sendbirdapisampleapp.util.Extension.showToast
import com.sendbird.android.channel.GroupChannel
import com.sendbird.android.params.GroupChannelCreateParams

class FriendFragment : BaseFragment<FragmentFriendBinding>(R.layout.fragment_friend) {
    override fun initView() {
        super.initView()

        initUserProfile()
        binding.myChannelButton.setOnClickListener {
            createMyChannel()
        }
    }

    private fun initUserProfile()= with(binding) {
        userNickTextView.text = USER_NICKNAME
        userIdTextView.text = USER_ID
        myIdTextView2.text = USER_ID
    }

    private fun createMyChannel() {
        val users: List<String> = listOf(USER_ID)
        val params = GroupChannelCreateParams().apply {
            userIds = users
            operatorUserIds = users
            isDistinct = true
            name = "나와의 채팅"
            isSuper = false
        }
        GroupChannel.createChannel(params) { channel, e ->
            if (e != null) {
                showToast("$e")
            }
            if (channel != null) {
                val intent = Intent(requireContext(), MyChannelActivity::class.java)
                intent.putExtra(INTENT_NAME_CHANNEL_URL, channel.url)
                intent.putExtra(INTENT_NAME_USER_ID, USER_ID)
                intent.putExtra(INTENT_NAME_USER_NICK, USER_NICKNAME)
                intent.action = CHANNEL_ACTIVITY_INTENT_ACTION
                startActivity(intent)
            }
        }
    }
}