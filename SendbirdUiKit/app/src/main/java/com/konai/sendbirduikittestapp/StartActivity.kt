package com.konai.sendbirduikittestapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.konai.sendbirduikittestapp.Constants.INTENT_NAME_USER_ID
import com.konai.sendbirduikittestapp.Constants.INTENT_NAME_USER_NICK
import com.konai.sendbirduikittestapp.Constants.MY_APP_INTENT_ACTION
import com.konai.sendbirduikittestapp.Constants.USER_ID
import com.konai.sendbirduikittestapp.Constants.USER_NICK
import com.konai.sendbirduikittestapp.databinding.ActivityStartBinding

class StartActivity : AppCompatActivity() {
    private val binding by lazy { ActivityStartBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initUserInfo()
    }

    private fun initUserInfo() = with(binding) {
        if (intent.action == MY_APP_INTENT_ACTION) {
            USER_ID = intent.getStringExtra(INTENT_NAME_USER_ID)!!
            USER_NICK = intent.getStringExtra(INTENT_NAME_USER_NICK)!!

            userId.text = USER_ID
            userNick.text = USER_NICK
            startButton.isEnabled = true
        }
    }

    fun startButtonClicked(v: View) {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}