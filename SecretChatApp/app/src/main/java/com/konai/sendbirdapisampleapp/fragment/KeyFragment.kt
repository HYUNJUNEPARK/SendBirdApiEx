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
            //suspend ?????? databinding ?????? ?????? ?????? ?
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

    //??????(Firebase DB)??? ???????????? ???????????? publicKey ??? ???????????? ????????? ??????
    private suspend fun showServerKeyState()= withContext(Dispatchers.IO) {
        remoteDB!!.collection(FIRESTORE_DOCUMENT_PUBLIC_KEY)
            .get()
            .addOnSuccessListener { result ->
                //1. ?????? Empty
                if (result.isEmpty) {
                    Log.e(TAG, "Error : Empty Firebase DB")
                    return@addOnSuccessListener
                }
                //2. ?????? not Empty
                for (document in result) {
                    //????????? ???????????? ????????? publicKey ??? ???????????? ?????? ??????
                    if (document.data[FIRESTORE_FIELD_USER_ID] == USER_ID) {
                        binding.serverPublicKeyStateImageView.setImageResource(R.drawable.ic_baseline_check_circle_24)
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "?????? PublicKey ?????? ?????? ??????", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
    }

    //??????????????? ???????????? ???????????? ECKeyPair ??? ???????????? ????????? ??????
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

    //encryptedSharedPreferences(ESP)??? ????????? ?????? ???Id ??? UI??? ?????????
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
        //???????????? ????????? ???????????? ?????? ?????? ?????????
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
                            urls.append("\n[* ???????????? ?????? ?????? ??????]\nurl: ${url.urlHash}\nkeyId:\n${url.keyId}\n")
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