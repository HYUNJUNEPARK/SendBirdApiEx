package com.konai.sendbirdapisampleapp.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.konai.sendbirdapisampleapp.R
import com.konai.sendbirdapisampleapp.adapter.ChannelMessageAdapter
import com.konai.sendbirdapisampleapp.databinding.ActivityMyChannelBinding
import com.konai.sendbirdapisampleapp.util.Constants

class MyChannelActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMyChannelBinding
    private lateinit var adapter: ChannelMessageAdapter
    private lateinit var channelURL: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_my_channel)
        binding.myChannelActivity = this

        if(intent.action != Constants.CHANNEL_ACTIVITY_INTENT_ACTION) return
        channelURL = intent.getStringExtra(Constants.INTENT_NAME_CHANNEL_URL)!!

        //initMessageRecyclerView()
    }
//[START Init]
    private fun initMessageRecyclerView() {
        adapter = ChannelMessageAdapter()
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
    }
//[END Init]

//[START Read message]
    private fun readAllMessages() {

    }
    private fun messageReceived() {

    }
//[END Read message]

//[START Click Event]
    fun onSendButtonClicked() {

    }
//[END Click Event]

//[START Util]
    private fun adjustRecyclerViewPosition() {
        binding.recyclerView.run { //리사이클러뷰 위치 조정
            postDelayed({
                scrollToPosition(adapter!!.itemCount - 1)
            }, 300)
        }
    }
//[END Util]
}