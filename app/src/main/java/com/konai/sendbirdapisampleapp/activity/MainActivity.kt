package com.konai.sendbirdapisampleapp.activity

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.konai.sendbirdapisampleapp.R
import com.konai.sendbirdapisampleapp.databinding.ActivityMainBinding
import com.konai.sendbirdapisampleapp.fragment.ChannelListFragment
import com.konai.sendbirdapisampleapp.fragment.FriendFragment
import com.konai.sendbirdapisampleapp.fragment.KeyFragment
import com.konai.sendbirdapisampleapp.strongbox.StrongBox
import com.konai.sendbirdapisampleapp.Constants.USER_ID

class MainActivity : AppCompatActivity() {
    private var blankFragment: KeyFragment? = null
    private var friendFragment: FriendFragment? = null
    private var channelListFragment: ChannelListFragment? = null
    private val fragmentManager = supportFragmentManager
    private lateinit var binding: ActivityMainBinding
    private lateinit var strongBox: StrongBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
            binding.mainActivity = this
            strongBox = StrongBox.getInstance(this)
            initFragment()
            initBottomNavigation()

            //다른 사람의 디바이스에 로그인한 경우 메시지를 띄움
            if (!strongBox.isEcKey(USER_ID)) {
                showAlertDialog()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
                    if (friendFragment != null)
                        fragmentManager.beginTransaction().show(friendFragment!!).commit()
                    if (channelListFragment != null)
                        fragmentManager.beginTransaction().hide(channelListFragment!!).commit()
                    if (blankFragment != null)
                        fragmentManager.beginTransaction().hide(blankFragment!!).commit()
                }
                R.id.channel -> {
                    if (channelListFragment == null) {
                        channelListFragment = ChannelListFragment()
                        fragmentManager.beginTransaction()
                            .add(R.id.fragmentContainer, channelListFragment!!)
                            .commit()
                    }
                    if (friendFragment != null) {
                        fragmentManager.beginTransaction().hide(friendFragment!!).commit()
                    }
                    if (channelListFragment != null) {
                        fragmentManager.beginTransaction().show(channelListFragment!!).commit()
                    }
                    if (blankFragment != null) {
                        fragmentManager.beginTransaction().hide(blankFragment!!).commit()
                    }
                }
                R.id.blank -> {
                    if (blankFragment == null) {
                        blankFragment = KeyFragment()
                        fragmentManager.beginTransaction()
                            .add(R.id.fragmentContainer, blankFragment!!)
                            .commit()
                    }
                    if (friendFragment != null) {
                        fragmentManager.beginTransaction().hide(friendFragment!!).commit()
                    }
                    if (channelListFragment != null) {
                        fragmentManager.beginTransaction().hide(channelListFragment!!).commit()
                    }
                    if (blankFragment != null) {
                        fragmentManager.beginTransaction().show(blankFragment!!).commit()
                    }
                }
            }
            true
        }
    }

    private fun showAlertDialog() {
        AlertDialog.Builder(this)
            .setTitle("경고")
            .setMessage("계정에 등록된 기기가 아닙니다. \n채널 생성/메시지 송신/메시지 복호화가 불가능합니다.")
            .setPositiveButton("확인") { _, _ -> }
            .create()
            .show()

    }
}