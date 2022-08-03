package com.konai.sendbirdapisampleapp.fragment

import android.util.Log
import com.konai.sendbirdapisampleapp.R
import com.konai.sendbirdapisampleapp.databinding.FragmentBlankBinding
import com.konai.sendbirdapisampleapp.strongbox.KeyStoreUtil
import com.konai.sendbirdapisampleapp.util.Constants
import com.konai.sendbirdapisampleapp.util.Constants.FIRE_STORE_FIELD_USER_ID
import com.konai.sendbirdapisampleapp.util.Constants.TAG
import com.konai.sendbirdapisampleapp.util.Constants.USER_ID
import com.konai.sendbirdapisampleapp.util.Extension.showToast

class KeyFragment : BaseFragment<FragmentBlankBinding>(R.layout.fragment_blank) {
    override fun initView() {
        super.initView()

        if(db != null) {
            initKeyStoreKeyState()
            initServerKeyStateIcon()
        }
        else {
            showToast("Firebase DB initialize failed")
        }
    }

    private fun initKeyStoreKeyState() {
        if (KeyStoreUtil().isKeyInKeyStore(USER_ID)) {
            //public
            KeyStoreUtil().getPublicKeyFromKeyStore(USER_ID).let { publicKey ->
                if (publicKey == null) binding.publicKeyStateImageView.setImageResource(R.drawable.ic_baseline_error_24)
                else binding.publicKeyStateImageView.setImageResource(R.drawable.ic_baseline_check_circle_24)
            }
            //private
            KeyStoreUtil().getPrivateKeyFromKeyStore(USER_ID).let { privateKey ->
                if (privateKey == null) binding.privateKeyStateImageView.setImageResource(R.drawable.ic_baseline_error_24)
                else binding.privateKeyStateImageView.setImageResource(R.drawable.ic_baseline_check_circle_24)
            }
        }
    }

    private fun initServerKeyStateIcon() {
        db!!.collection(Constants.FIRE_STORE_DOCUMENT_PUBLIC_KEY)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    Log.e(TAG, "Error : Empty Firebase DB")
                    return@addOnSuccessListener
                }
                for (document in result) {
                    if (document.data[FIRE_STORE_FIELD_USER_ID] == USER_ID) {
                        binding.serverPublicKeyStateImageView.setImageResource(R.drawable.ic_baseline_check_circle_24)
                    }
                }
            }
        .addOnFailureListener { exception ->
            showToast("서버키 상태 로드 실패")
            Log.e(TAG, "Error getting key state from firestore : $exception")
        }
    }
}