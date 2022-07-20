package com.konai.sendbirdapisampleapp.util

import android.content.Context
import android.widget.Toast

object Extension {
    fun Context.toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}