package com.konai.sendbirdapisampleapp.util

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.*

object Extension {
    fun Context.showToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    fun Fragment.showToast(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

    fun Long.convertLongToTime(): String {
        val date = Date(this)
        val format = SimpleDateFormat("HH:mm")
        return format.format(date)
    }
}