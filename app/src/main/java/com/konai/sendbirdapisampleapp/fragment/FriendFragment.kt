package com.konai.sendbirdapisampleapp.fragment

import com.konai.sendbirdapisampleapp.Constants.USER_ID
import com.konai.sendbirdapisampleapp.Constants.USER_NICKNAME
import com.konai.sendbirdapisampleapp.R
import com.konai.sendbirdapisampleapp.databinding.FragmentFriendBinding

class FriendFragment : BaseFragment<FragmentFriendBinding>(R.layout.fragment_friend) {
    override fun initView() {
        super.initView()

        initUserProfile()
    }

    private fun initUserProfile()= with(binding) {
        userNickTextView.text = USER_NICKNAME
        userIdTextView.text = USER_ID
    }
}