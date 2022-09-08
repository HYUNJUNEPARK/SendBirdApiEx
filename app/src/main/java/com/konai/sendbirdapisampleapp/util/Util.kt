package com.konai.sendbirdapisampleapp.util

object Util {
    fun displayUserInfo(nickname: String?, id: String?, channelType: Int): String {
        if(channelType == 0){
            return "$nickname (ID : $id) [ 나 ]"
        }
        else{
            return "$nickname (ID : $id)"
        }
    }
}