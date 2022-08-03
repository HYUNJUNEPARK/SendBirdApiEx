package com.konai.sendbirdapisampleapp.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.konai.sendbirdapisampleapp.R
import com.konai.sendbirdapisampleapp.databinding.ActivityMainBinding
import com.konai.sendbirdapisampleapp.fragment.KeyFragment
import com.konai.sendbirdapisampleapp.fragment.ChannelListFragment
import com.konai.sendbirdapisampleapp.fragment.FriendFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val fragmentManager = supportFragmentManager
    private var friendFragment: FriendFragment? = null
    private var channelListFragment: ChannelListFragment? = null
    private var blankFragment: KeyFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.mainActivity = this

        initFragment()
        initBottomNavigation()
    }

    private fun initFragment() {
        friendFragment = FriendFragment() //최초로 보이는 프래그먼트 세팅
        fragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, friendFragment!!)
            .commit()
    }

    private fun initBottomNavigation() {
        binding.bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when(menuItem.itemId) {
                R.id.friend -> {
                    if (friendFragment == null) {
                        friendFragment = FriendFragment()
                        fragmentManager.beginTransaction()
                            .add(R.id.fragmentContainer, friendFragment!!)
                            .commit()
                    }
                    if (friendFragment != null) fragmentManager.beginTransaction().show(friendFragment!!).commit()
                    if (channelListFragment != null) fragmentManager.beginTransaction().hide(channelListFragment!!).commit()
                    if (blankFragment != null) fragmentManager.beginTransaction().hide(blankFragment!!).commit()
                }
                R.id.channel -> {
                    if (channelListFragment == null) {
                        channelListFragment = ChannelListFragment()
                        fragmentManager.beginTransaction()
                            .add(R.id.fragmentContainer, channelListFragment!!)
                            .commit()
                    }
                    if (friendFragment != null) fragmentManager.beginTransaction().hide(friendFragment!!).commit()
                    if (channelListFragment != null) fragmentManager.beginTransaction().show(channelListFragment!!).commit()
                    if (blankFragment != null) fragmentManager.beginTransaction().hide(blankFragment!!).commit()

                }
                R.id.blank -> {
                    if (blankFragment == null) {
                        blankFragment = KeyFragment()
                        fragmentManager.beginTransaction()
                            .add(R.id.fragmentContainer, blankFragment!!)
                            .commit()
                    }
                    if (friendFragment != null) fragmentManager.beginTransaction().hide(friendFragment!!).commit()
                    if (channelListFragment != null) fragmentManager.beginTransaction().hide(channelListFragment!!).commit()
                    if (blankFragment != null) fragmentManager.beginTransaction().show(blankFragment!!).commit()
                }
            }
            true
        }
    }
}