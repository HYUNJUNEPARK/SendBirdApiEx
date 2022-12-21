package com.konai.sendbirdapisampleapp

import java.text.SimpleDateFormat
import java.util.*

object Extension {
    fun Long.convertLongToTime(): String {
        val date = Date(this)
        val format = SimpleDateFormat("HH:mm")
        return format.format(date)
    }
}