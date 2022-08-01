package com.konai.sendbirdapisampleapp.fragment

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.konai.sendbirdapisampleapp.R
import com.konai.sendbirdapisampleapp.databinding.FragmentBlankBinding
import com.konai.sendbirdapisampleapp.strongbox.KeyStoreUtil
import com.konai.sendbirdapisampleapp.util.Constants
import com.konai.sendbirdapisampleapp.util.Constants.FIRE_STORE_FIELD_USER_ID
import com.konai.sendbirdapisampleapp.util.Constants.TAG
import com.konai.sendbirdapisampleapp.util.Constants.USER_ID
import com.konai.sendbirdapisampleapp.util.Extension.showToast

class BlankFragment : BaseFragment<FragmentBlankBinding>(R.layout.fragment_blank) {
    //private var db: FirebaseFirestore? = null

    override fun initView() {
        super.initView()

        //db = Firebase.firestore
        initKeyStoreKeyState()
        initServerKeyState()
    }

    private fun initKeyStoreKeyState() {
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

    private fun initServerKeyState() {
        db?.collection(Constants.FIRE_STORE_DOCUMENT_PUBLIC_KEY)
            ?.get()
            ?.addOnSuccessListener { result ->
                for (document in result) {
                    if (document.data[FIRE_STORE_FIELD_USER_ID] == USER_ID) {
                        binding.serverPublicKeyStateImageView.setImageResource(R.drawable.ic_baseline_check_circle_24)
                    }
                }
            }
        ?.addOnFailureListener { exception ->
            showToast("서버키 상태 로드 실패")
            Log.e(TAG, "Error getting key state from firestore : $exception")
        }
    }
}