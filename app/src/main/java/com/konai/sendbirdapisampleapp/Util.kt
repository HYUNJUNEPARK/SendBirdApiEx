package com.konai.sendbirdapisampleapp

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.konai.sendbirdapisampleapp.MainActivity.Companion.TAG

object Util {
    fun Context.toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}