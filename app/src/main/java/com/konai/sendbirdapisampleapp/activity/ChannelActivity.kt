package com.konai.sendbirdapisampleapp.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.konai.sendbirdapisampleapp.R
import com.konai.sendbirdapisampleapp.databinding.ActivityChannelBinding

class ChannelActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChannelBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_channel)
    }
}