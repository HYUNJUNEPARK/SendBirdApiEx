package com.konai.sendbirdapisampleapp.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.konai.sendbirdapisampleapp.R
import com.konai.sendbirdapisampleapp.databinding.ActivityMain2Binding
import com.konai.sendbirdapisampleapp.fragment.BlankFragment
import com.konai.sendbirdapisampleapp.fragment.ChannelFragment
import com.konai.sendbirdapisampleapp.fragment.FriendFragment

class MainActivity2 : AppCompatActivity() {
    private lateinit var binding: ActivityMain2Binding

    private val fragmentList = arrayListOf<Fragment>(
        FriendFragment(),
        ChannelFragment(),
        BlankFragment()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main2)

        initFragment()
        replaceFragment(fragmentList[0])
    }

    private fun initFragment() {
        binding.bottomNavigationView.setOnItemSelectedListener { menu ->
            when(menu.itemId) {
                R.id.friend -> replaceFragment(fragmentList[0])
                R.id.channel -> replaceFragment(fragmentList[1])
                R.id.blank -> replaceFragment(fragmentList[2])
            }
            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .apply {
                replace(R.id.fragmentContainer, fragment)
                commit()
            }
    }
}