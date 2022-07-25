package com.konai.sendbirdapisampleapp.fragment

import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.konai.sendbirdapisampleapp.R
import com.konai.sendbirdapisampleapp.adapter.ChannelListAdapter
import com.konai.sendbirdapisampleapp.databinding.FragmentChannelBinding
import com.konai.sendbirdapisampleapp.model.ChannelListModel
import com.konai.sendbirdapisampleapp.util.Constants.TAG
import com.konai.sendbirdapisampleapp.util.Constants.USER_ID
import com.konai.sendbirdapisampleapp.util.Constants.USER_NICKNAME
import com.konai.sendbirdapisampleapp.util.Extension.convertLongToTime
import com.konai.sendbirdapisampleapp.util.Extension.toast
import com.sendbird.android.channel.GroupChannel
import com.sendbird.android.channel.query.GroupChannelListQueryOrder
import com.sendbird.android.channel.query.MyMemberStateFilter
import com.sendbird.android.params.GroupChannelCreateParams
import com.sendbird.android.params.GroupChannelListQueryParams

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
                requireContext().toast("$e")
                return@next
            }

            _channelList.clear() //to make the list empty

            if (channels!!.isEmpty()) return@next

            for (i in channels.indices) {
                var partnerId: String
                var partnerNickname: String

//                //채널에 해당하는 대화 상대의 정보(아이디, 닉네임)만 데이터 클래스에 저장
//                //TODO 문제점 : private 채널 일 때 앱 크래시
//                if (channels!![i].members.size == 1) {
//                    partnerId = "private"
//                    partnerNickname = "private"
//                    return@next
//                }

                //private channel
                if(channels[i].members.size == 1) {
                    partnerId = USER_ID
                    partnerNickname = USER_NICKNAME
                }
                else {
                    if(channels!![i].members[0].userId == USER_ID) {
                        partnerId = channels!![i].members[1].userId
                        partnerNickname = channels!![i].members[1].nickname
                    }
                    else {
                        partnerId = channels!![i].members[0].userId
                        partnerNickname = channels!![i].members[0].nickname
                    }
                }

                _channelList.add(
                    ChannelListModel(
                        name = channels[i].name,
                        url = channels[i].url,
                        lastMessage = channels[i].lastMessage?.message,
                        lastMessageTime = (channels[i].lastMessage?.createdAt)?.convertLongToTime(),
                        partnerMemberId = partnerId,
                        partnerMemberNick = partnerNickname
                    )
                )
            }
            binding.chatListRecyclerView.adapter?.notifyDataSetChanged()
        }
    }

    //TODO dataBinding onClicked
    private fun onCreateChannelButtonClicked() {
        val invitedUserId = binding.userIdInputEditText.text.toString()
        val users: List<String> = listOf(USER_ID, invitedUserId)
        val params = GroupChannelCreateParams().apply {
            userIds = users
            isDistinct = true
            name = "$USER_ID, $invitedUserId"
            isSuper = false
        }

        GroupChannel.createChannel(params) { channel, e ->
            if (e != null) {
                requireContext().toast("$e")
            }
            Log.d(TAG, "onCreateChannelButtonClicked: $channel")
            if (channel != null) {
                Toast.makeText(requireContext(), "채널 생성", Toast.LENGTH_SHORT).show()
                initChannelList()
            }
        }
        binding.userIdInputEditText.text = null
    }
}