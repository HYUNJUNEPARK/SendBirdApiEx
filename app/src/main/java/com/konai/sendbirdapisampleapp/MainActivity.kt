package com.konai.sendbirdapisampleapp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.konai.sendbirdapisampleapp.Util.toast
import com.konai.sendbirdapisampleapp.databinding.ActivityMainBinding
import com.konai.sendbirdapisampleapp.model.ChatListModel
import com.sendbird.android.SendbirdChat
import com.sendbird.android.channel.GroupChannel
import com.sendbird.android.channel.query.GroupChannelListQueryOrder
import com.sendbird.android.channel.query.MyMemberStateFilter
import com.sendbird.android.params.GroupChannelCreateParams
import com.sendbird.android.params.GroupChannelListQueryParams
import com.sendbird.android.params.UserUpdateParams

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "applicationLog"
        const val APP_ID = "D4FCF442-A653-49B3-9D87-6134CD87CA81"
        var currentUserId: String? = null
        var currentUserNickname: String? = null
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var chatChannelList: ChatListModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.mainActivity = this@MainActivity

        SendBirdInit().initializeChatSdk(this)
    }



    //read channel list
    fun initChannelList() {
        //private
        val query = GroupChannel.createMyGroupChannelListQuery(
            GroupChannelListQueryParams().apply {
                includeEmpty = true
                myMemberStateFilter = MyMemberStateFilter.JOINED
                order = GroupChannelListQueryOrder.LATEST_LAST_MESSAGE //CHRONOLOGICAL, LATEST_LAST_MESSAGE, CHANNEL_NAME_ALPHABETICAL, and METADATA_VALUE_ALPHABETICAL.
            }
        )
        query.next { channels, e ->
            if (e != null) {
                toast("$e")
            }

            chatChannelList = ChatListModel(
                name = channels!![0].name,
                url = channels!![0].url
            )

            Log.d(TAG, "channel List : $chatChannelList")
            Log.e(TAG, "initChannelList Error : $e", )

        }
    }


    //invite
    fun inviteButtonClicked() {
        val invitedUserId = binding.inviteEditText.text.toString()

        //group channel : private, 1 to 1(distinct true ), Ephemeral?
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
            }
        }
    }



    //LogIn
    fun logInButtonClicked() {
        //TODO ProgressBar
        val userId = binding.userIdEditText.text.toString()

        SendbirdChat.connect(userId) { user, e ->
            currentUserId = user?.userId.toString() //TODO Null safety
            currentUserNickname = binding.nickNameEditText.text.toString() //TODO Null safety

            if (e != null) {
                toast("로그인 에러 : $e")
                //400305 : User ID is too long.
                Log.e(TAG, ": $e")
                return@connect
            }

            //[START update profile]
            val params = UserUpdateParams().apply {
                nickname = currentUserNickname
            }
            SendbirdChat.updateCurrentUserInfo(params) { e ->
                Log.e(TAG, ": updateCurrentUserInfo Error : $e")
            }
            //[END update profile]

            initChannelList()


            binding.logInLayout.visibility = View.INVISIBLE
            binding.userIdTextView.text = "$currentUserNickname [ID : ${currentUserId}]"
        }
    }
}