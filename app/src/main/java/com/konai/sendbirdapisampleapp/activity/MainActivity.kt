package com.konai.sendbirdapisampleapp.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.konai.sendbirdapisampleapp.Constants.TAG
import com.konai.sendbirdapisampleapp.adapter.ChannelListAdapter
import com.konai.sendbirdapisampleapp.R
import com.konai.sendbirdapisampleapp.api.SendBirdInit
import com.konai.sendbirdapisampleapp.Util.toast
import com.konai.sendbirdapisampleapp.databinding.ActivityMainBinding
import com.konai.sendbirdapisampleapp.model.ChannelListModel
import com.sendbird.android.SendbirdChat
import com.sendbird.android.channel.GroupChannel
import com.sendbird.android.channel.query.GroupChannelListQueryOrder
import com.sendbird.android.channel.query.MyMemberStateFilter
import com.sendbird.android.params.GroupChannelCreateParams
import com.sendbird.android.params.GroupChannelListQueryParams
import com.sendbird.android.params.UserUpdateParams

class MainActivity : AppCompatActivity() {
    //TODO MERGE VARIATION WITH CONSTANTS
    private var currentUserId: String? = null
    private var currentUserNick: String? = null

    private lateinit var binding: ActivityMainBinding
    private var _channelList: MutableList<ChannelListModel> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.mainActivity = this@MainActivity

        SendBirdInit().initializeChatSdk(this)
    }

    fun logInButtonClicked() {
        //TODO ProgressBar
        val userId = binding.userIdEditText.text.toString()

        SendbirdChat.connect(userId) { user, e ->
            val userInputNickName = binding.nickNameEditText.text.toString()
            currentUserNick = if(userInputNickName == "") "-" else userInputNickName
            currentUserId = user?.userId.toString()

            if (e != null) {
                toast("로그인 에러 : $e")
                Log.e(TAG, ": $e")
                return@connect

            }

            val params = UserUpdateParams().apply {
                nickname = currentUserNick
            }

            SendbirdChat.updateCurrentUserInfo(params) { e ->
                Log.e(TAG, ": updateCurrentUserInfo Error : $e")
            }


            binding.logInLayout.visibility = View.INVISIBLE
            binding.userIdTextView.text = "$currentUserNick [ID : ${currentUserId}]"

            initChannelList()
        }
    }


    fun initChannelList() {
        _channelList = mutableListOf()

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
                toast("$e")
                Log.e(TAG, "initChannelList Error : $e", )
                return@next
            }
            //Log.d(TAG, "initChannelList: ${channels?.size}")
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
            binding.chatListRecyclerViewCover.visibility = View.VISIBLE
            return
        }
        //Log.d(TAG, "initChannelList: outside query dataList22 $_channelList")
        val adapter = ChannelListAdapter()
        adapter.channelList = _channelList
        binding.chatListRecyclerView.adapter = adapter
        binding.chatListRecyclerView.layoutManager = LinearLayoutManager(this)
    }












    fun inviteButtonClicked() {
        val invitedUserId = binding.inviteEditText.text.toString()
        val users: List<String> = listOf(currentUserId!!, invitedUserId)

        val params = GroupChannelCreateParams().apply {
            userIds = users
            isDistinct = true
            name = "$currentUserId / $invitedUserId"
            isSuper = false
        }


        GroupChannel.createChannel(params) { channel, e ->
            if (e != null) {
                toast("$e")
            }
            if (channel != null) {
                Toast.makeText(this, "채팅방 생성", Toast.LENGTH_SHORT).show()
                //TODO refresh UI
            }
        }
    }

    fun uiKitButtonClicked() {
        //TODO ProgressBar
        //TODO MERGE THIS PART WITH UPPER CODES
        val userId = binding.userIdEditText.text.toString()
        val intent = Intent(this, UiKitActivity::class.java)
        startActivity(intent)
    }
}