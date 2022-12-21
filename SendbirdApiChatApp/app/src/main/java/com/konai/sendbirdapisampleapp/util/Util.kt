package com.konai.sendbirdapisampleapp.util

object Util {
    fun displayUserInfo(nickname: String?, id: String?, channelType: Int): String {
        return if(channelType == 0) "$nickname ($id) [ 나 ]" else "$nickname ($id)"
    }
}