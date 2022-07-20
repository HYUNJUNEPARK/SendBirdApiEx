package com.konai.sendbirdapisampleapp.fragment

import androidx.recyclerview.widget.LinearLayoutManager
import com.konai.sendbirdapisampleapp.R
import com.konai.sendbirdapisampleapp.Util.toast
import com.konai.sendbirdapisampleapp.adapter.ChannelListAdapter
import com.konai.sendbirdapisampleapp.databinding.FragmentChannelBinding
import com.konai.sendbirdapisampleapp.model.ChannelListModel
import com.sendbird.android.channel.GroupChannel
import com.sendbird.android.channel.query.GroupChannelListQueryOrder
import com.sendbird.android.channel.query.MyMemberStateFilter
import com.sendbird.android.params.GroupChannelListQueryParams

class ChannelFragment : BaseFragment<FragmentChannelBinding>(R.layout.fragment_channel) {
    private var _channelList: MutableList<ChannelListModel> = mutableListOf()

    override fun initView() {
        super.initView()
        initRecyclerView()
    }

    private fun initRecyclerView() {
        initChannelList()
        val adapter = ChannelListAdapter()
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
                //CHRONOLOGICAL, LATEST_LAST_MESSAGE, CHANNEL_NAME_ALPHABETICAL, METADATA_VALUE_ALPHABETICAL.
            }
        )
        query.next { channels, e ->
            if (e != null) {
                requireContext().toast("$e")
                return@next
            }

            //to make the empty list
            _channelList = mutableListOf()

            for (i in 0 until channels!!.size) {
                _channelList.add(
                    ChannelListModel(
                        name = channels!![i].name,
                        url = channels!![i].url,
                        lastMessage = channels!![i].lastMessage.toString()
                    )
                )
            }
        }
        if (_channelList == null) {
            return
        }
    }

    private fun createChannelButton() {

    }
}