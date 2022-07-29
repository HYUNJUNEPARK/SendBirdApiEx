package com.konai.sendbirdapisampleapp.util

object Constants {
    //user info
    lateinit var USER_ID: String
    lateinit var USER_NICKNAME: String

    //build
    const val APP_ID = "D4FCF442-A653-49B3-9D87-6134CD87CA81"
    const val SENDBIRD_UI_KIT_APP = "com.konai.sendbirduikittestapp"

    //intent
    const val MY_APP_INTENT_ACTION = "com.konai.sendbirdapisampleapp.activity.ACTION"
    const val CHANNEL_ACTIVITY_INTENT_ACTION ="com.konai.sendbirdapisampleapp.adapter.Action"
    const val INTENT_NAME_USER_ID = "userId"
    const val INTENT_NAME_USER_NICK = "userNick"
    const val INTENT_NAME_CHANNEL_URL = "channelURL"

    //etc
    const val TAG = "applicationLog"

    //Message Item ViewType
    const val MY_MESSAGE = 0
    const val PARTNER_MESSAGE = 1

    //Channel Type
    const val MY_PERSONAL_CHANNEL = 0
    const val CONVERSATION_CHANNEL = 1

    //handler id
    const val RECEIVE_MESSAGE_HANDLER = "messageHandler"

    //firebase
    const val FIRE_STORE_FIELD_USER_ID = "userId"
    const val FIRE_STORE_FIELD_AFFINE_X = "affineX"
    const val FIRE_STORE_FIELD_AFFINE_Y = "affineY"
    const val FIRE_STORE_DOCUMENT_PUBLIC_KEY = "publicKey"
}
