package com.konai.sendbirdapisampleapp.fragment

import android.util.Log
import android.view.View
import android.widget.Toast
import com.konai.sendbirdapisampleapp.R
import com.konai.sendbirdapisampleapp.db.DBProvider
import com.konai.sendbirdapisampleapp.db.keyid.KeyIdDatabase
import com.konai.sendbirdapisampleapp.strongbox.EncryptedSharedPreferencesManager
import com.konai.sendbirdapisampleapp.strongbox.StrongBox
import com.konai.sendbirdapisampleapp.Constants.FIRESTORE_DOCUMENT_PUBLIC_KEY
import com.konai.sendbirdapisampleapp.Constants.FIRESTORE_FIELD_USER_ID
import com.konai.sendbirdapisampleapp.Constants.TAG
import com.konai.sendbirdapisampleapp.Constants.USER_ID
import com.konai.sendbirdapisampleapp.databinding.FragmentKeyBinding
import com.sendbird.android.channel.GroupChannel
import com.sendbird.android.channel.query.GroupChannelListQueryOrder
import com.sendbird.android.channel.query.MyMemberStateFilter
import com.sendbird.android.params.GroupChannelListQueryParams
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class KeyFragment : BaseFragment<FragmentKeyBinding>(R.layout.fragment_key), CoroutineScope {
    lateinit var strongBox: StrongBox
    private lateinit var encryptedSharedPreferencesManager: EncryptedSharedPreferencesManager
    private lateinit var localDB: KeyIdDatabase
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + Job()

    override fun initView() {
        super.initView()

        if (remoteDB == null) {
            Log.e(TAG, "Firebase DB initialize error")
            return
        }

        try {
            binding.keyFragment = this
            strongBox = StrongBox.getInstance(requireContext())
            encryptedSharedPreferencesManager =
                EncryptedSharedPreferencesManager.getInstance(requireContext())!!
            localDB = DBProvider.getInstance(requireContext())!!
            displayECKeyPairState()
            displayKeyIdInESP()
            launch {
                showServerKeyState()
                //displayAvailableKeyIds()
                displayUrlInLocalDB()
            }
            //suspend 함수 databinding 으로 처리 못함 ?
            binding.localDBRefreshButton.setOnClickListener {
                launch {
                    displayUrlInLocalDB()
                }
            }
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //서버(Firebase DB)에 로그인한 사용자의 publicKey 가 등록되어 있는지 확인
    private suspend fun showServerKeyState()= withContext(Dispatchers.IO) {
        remoteDB!!.collection(FIRESTORE_DOCUMENT_PUBLIC_KEY)
            .get()
            .addOnSuccessListener { result ->
                //1. 서버 Empty
                if (result.isEmpty) {
                    Log.e(TAG, "Error : Empty Firebase DB")
                    return@addOnSuccessListener
                }
                //2. 서버 not Empty
                for (document in result) {
                    //서버에 로그인한 유저의 publicKey 가 등록되어 있는 상태
                    if (document.data[FIRESTORE_FIELD_USER_ID] == USER_ID) {
                        binding.serverPublicKeyStateImageView.setImageResource(R.drawable.ic_baseline_check_circle_24)
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "서버 PublicKey 상태 확인 실패", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
    }

    //키스토어에 로그인한 사용자의 ECKeyPair 가 등록되어 있는지 확인
    private fun displayECKeyPairState()= with(binding) {
        try {
            strongBox.getECPublicKey(USER_ID).let {
                publicKeyStateImageView.setImageResource(R.drawable.ic_baseline_check_circle_24)
                privateKeyStateImageView.setImageResource(R.drawable.ic_baseline_check_circle_24)
            }
        }
        catch (e: Exception) {
            publicKeyStateImageView.setImageResource(R.drawable.ic_baseline_error_24)
            privateKeyStateImageView.setImageResource(R.drawable.ic_baseline_error_24)
        }
    }

    //encryptedSharedPreferences(ESP)에 저장된 모든 키Id 를 UI로 보여줌
    fun displayKeyIdInESP() {
        encryptedSharedPreferencesManager.getKeyIdList().let { keyIdList ->
            binding.devicePublicKeyCountTextView.text = keyIdList!!.size.toString()

            val keyIds = StringBuffer("")
            for (keyId in keyIdList) {
                keyIds.append("$keyId\n")
            }
            binding.allESPKeysTextView.text = keyIds.toString()
        }
    }

    private suspend fun displayUrlInLocalDB() = withContext(Dispatchers.IO) {
        //로그인한 유저가 참여하고 있는 채널 리스트
        val userChannelList: MutableList<String> = mutableListOf()

        val query = GroupChannel.createMyGroupChannelListQuery(
            GroupChannelListQueryParams().apply {
                includeEmpty = true
                myMemberStateFilter = MyMemberStateFilter.JOINED
                order = GroupChannelListQueryOrder.LATEST_LAST_MESSAGE
            }
        )
        query.next { channelList, e ->
            if (e != null) {
                e.printStackTrace()
                return@next
            }
            for (channel in channelList!!) {
                userChannelList.add(channel.url)
            }

            CoroutineScope(Dispatchers.IO).launch {
                localDB.keyIdDao().getAll().let { urlList ->
                    val urls = StringBuffer("")
                    for (url in urlList) {

                        if (userChannelList.contains(url.urlHash)) {
                            urls.append("\n[* 로그인한 계정 사용 가능]\nurl: ${url.urlHash}\nkeyId:\n${url.keyId}\n")
                        }
                        else {
                            urls.append("url: ${url.urlHash}\nkeyId:\n${url.keyId}\n")
                        }
                    }
                    withContext(Dispatchers.Main) {
                        binding.localDBUrlCountTextView.text = urlList.size.toString()
                        binding.localDBUrlTextView.text = urls
                    }
                }
            }
        }
    }
}