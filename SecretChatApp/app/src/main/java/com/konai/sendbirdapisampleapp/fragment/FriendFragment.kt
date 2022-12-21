package com.konai.sendbirdapisampleapp.fragment

import android.content.Intent
import com.konai.sendbirdapisampleapp.Constants.INTENT_ACTION_MY_CHANNEL
import com.konai.sendbirdapisampleapp.Constants.INTENT_NAME_CHANNEL_URL
import com.konai.sendbirdapisampleapp.Constants.INTENT_NAME_USER_ID
import com.konai.sendbirdapisampleapp.Constants.INTENT_NAME_USER_NICK
import com.konai.sendbirdapisampleapp.Constants.USER_ID
import com.konai.sendbirdapisampleapp.Constants.USER_NICKNAME
import com.konai.sendbirdapisampleapp.R
import com.konai.sendbirdapisampleapp.activity.ChannelActivity
import com.konai.sendbirdapisampleapp.databinding.FragmentFriendBinding
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
                e.printStackTrace()
                return@createChannel
            }

            if (channel != null) {
                val intent = Intent(requireContext(), ChannelActivity::class.java).apply {
                    putExtra(INTENT_NAME_CHANNEL_URL, channel.url)
                    putExtra(INTENT_NAME_USER_ID, USER_ID)
                    putExtra(INTENT_NAME_USER_NICK, USER_NICKNAME)
                    action = INTENT_ACTION_MY_CHANNEL
                }
                startActivity(intent)
            }
        }
    }
}