package com.konai.sendbirdapisampleapp

import android.content.Context
import android.widget.Toast

object Util {
    fun Context.toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}