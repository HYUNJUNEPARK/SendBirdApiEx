package com.konai.sendbirdapisampleapp.fragment

import android.util.Log
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
    lateinit var encryptedSharedPreferencesManager: EncryptedSharedPreferencesManager
    lateinit var localDB: KeyIdDatabase
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + Job()

    override fun initView() {
        super.initView()

        if (remoteDB == null) {
            Log.e(TAG, "Firebase DB initialize error")
            return
        }

        try {
            strongBox = StrongBox.getInstance(requireContext())
            encryptedSharedPreferencesManager =
                EncryptedSharedPreferencesManager.getInstance(requireContext())!!
            localDB = DBProvider.getInstance(requireContext())!!
            displayECKeyPairState()
            displayKeyIdInESP()
            launch {
                showServerKeyState()
                displayAvailableKeyIds()
                displayUrlInLocalDB()
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
    private fun displayKeyIdInESP() {
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
        localDB.keyIdDao().getAll().let { urlList ->
            val urls = StringBuffer("")
            for (url in urlList) {
                urls.append("url: ${url.urlHash}\nkeyId:\n${url.keyId}\n")
            }
            //Log.d(TAG, "localDB $urls")

            withContext(Dispatchers.Main) {
                binding.localDBUrlCountTextView.text = urlList.size.toString()
                binding.localDBUrlTextView.text = urls
            }
        }
    }


    /**
     * 로컬 DB 에 저장되어 있는 키와 로그인한 사용자가 사용할 수 있는 키를 UI 로 보여주는 함수
     *
     * 1. 로그인한 계정이 참여하고 있는 채널의 모든 URL 주소를 가져옴
     * 2. 로컬 DB 에 저장된 데이터 중 참여하고 있는 URL 과 같은 일치하는 Key 를 확인
     */
    private suspend fun displayAvailableKeyIds()= withContext(Dispatchers.IO) {
        localDB.keyIdDao().getAll().let {
            //Log.d(TAG, "Local DB: $it")
        }

        //TODO 1 센드버드에서 로그인한 사용자가 참여하고 있는 채널의 URL 주소 로드
        val query = GroupChannel.createMyGroupChannelListQuery(
            GroupChannelListQueryParams().apply {
                includeEmpty = true
                myMemberStateFilter = MyMemberStateFilter.JOINED
                order = GroupChannelListQueryOrder.LATEST_LAST_MESSAGE
            }
        )
        query.next { channel, e ->
            if (e != null) {
                e.printStackTrace()
                return@next
            }
            if (channel!!.isEmpty()) {
                return@next
            }

            for (idx in channel.indices) {

//                for (url in allChannelUrlList) {
//                    if (channel[idx].url == url) {
//                        availableHashSb.append("${channel[idx].url}\n\n")
//                        availableHashNumber++
//                    }
//                }

            }

//            binding.availableHashNumberTextView.text = availableHashNumber.toString()
//            binding.availableHashTextView.text = availableHashSb
        }


        //TODO 2 Local DB 에 있는 KeyId를 로드
        localDB.keyIdDao().getAll().let {
            Log.d(TAG, "local DB Key List : $it")
        }

        //TODO 3 1,2 를 비교해서 일치하는 URL 주소를 카운팅

//        var allChannelUrlList: MutableList<String> = mutableListOf()
//        val allHashSb = StringBuffer("")
//        val availableHashSb = StringBuffer("")
//        var availableHashNumber = 0
        //기기에 저장된 모든 해시

        //        //로그인 계정이 사용할 수 있는 해시 -> 로그인 채널의 url == 기기에 저장된 hash 비교
//        val query = GroupChannel.createMyGroupChannelListQuery(
//            GroupChannelListQueryParams().apply {
//                includeEmpty = true
//                myMemberStateFilter = MyMemberStateFilter.JOINED
//                order = GroupChannelListQueryOrder.LATEST_LAST_MESSAGE
//            }
//        )
//        query.next { channels, e ->
//            if (e != null) {
//                showToast("$e")
//                return@next
//            }
//
//            if (channels!!.isEmpty()) return@next
//
//            for (idx in channels.indices) {
//                for (url in allChannelUrlList) {
//                    if (channels[idx].url == url) {
//                        availableHashSb.append("${channels[idx].url}\n\n")
//                        availableHashNumber++
//                    }
//                }
//            }
//            binding.availableHashNumberTextView.text = availableHashNumber.toString()
//            binding.availableHashTextView.text = availableHashSb
//        }
    }
}