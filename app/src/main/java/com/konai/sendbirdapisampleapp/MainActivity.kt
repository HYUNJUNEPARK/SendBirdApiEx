package com.konai.sendbirdapisampleapp

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.konai.sendbirdapisampleapp.databinding.ActivityMainBinding
import com.sendbird.android.SendbirdChat
import com.sendbird.android.channel.GroupChannel
import com.sendbird.android.channel.query.GroupChannelListQueryOrder
import com.sendbird.android.channel.query.MyMemberStateFilter
import com.sendbird.android.exception.SendbirdException
import com.sendbird.android.handler.InitResultHandler
import com.sendbird.android.params.GroupChannelCreateParams
import com.sendbird.android.params.GroupChannelListQueryParams
import com.sendbird.android.params.InitParams
import com.sendbird.android.params.UserUpdateParams

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "applicationLog"
        const val APP_ID = "D4FCF442-A653-49B3-9D87-6134CD87CA81"
        var currentUserId: String? = null
        var currentUserNickname: String? = null
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.mainActivity = this@MainActivity
        initializeChatSdk()
    }

    //read channel list
    fun initChannelList() {
        //private
        val query = GroupChannel.createMyGroupChannelListQuery(
            GroupChannelListQueryParams().apply {
                includeEmpty = true
                myMemberStateFilter = MyMemberStateFilter.JOINED
                order = GroupChannelListQueryOrder.LATEST_LAST_MESSAGE //CHRONOLOGICAL, LATEST_LAST_MESSAGE, CHANNEL_NAME_ALPHABETICAL, and METADATA_VALUE_ALPHABETICAL.
                limit = 15
            }
        )
        query.next { channels, e ->
            Log.d(TAG, "${className()} channel List : $channels")
            Log.e(TAG, "${className()} initChannelList Error : $e", )
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
                Log.e(TAG, "${className()}: $e")
                return@connect
            }

            //[START update profile]
            val params = UserUpdateParams().apply {
                nickname = currentUserNickname
            }
            SendbirdChat.updateCurrentUserInfo(params) { e ->
                Log.e(TAG, "${className()}: updateCurrentUserInfo Error $e")
            }
            //[END]

            initChannelList()
            binding.logInLayout.visibility = View.INVISIBLE
            binding.userIdTextView.text = "$currentUserNickname[${currentUserId}]"
        }
    }




    fun Context.toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }



    fun className(): String {
        return localClassName
    }



    //TODO ASYNC By Coroutine
    private fun initializeChatSdk() {
        SendbirdChat.init(
            InitParams(APP_ID, applicationContext, useCaching = true),
            object : InitResultHandler {
                override fun onMigrationStarted() {
                    Log.i(TAG, "${className()} : Called when there's an update in Sendbird server.")
                }

                override fun onInitFailed(e: SendbirdException) {
                    Log.e(TAG,"${className()} : Called when initialize failed. $e \n SDK will still operate properly as if useLocalCaching is set to false.")
                }

                override fun onInitSucceed() {
                    toast("Called when initialization is completed.")
                    Log.i(TAG, "${className()} : Called when initialization is completed.")
                }
            }
        )
    }
}