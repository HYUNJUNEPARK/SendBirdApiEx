package com.konai.sendbirdapisampleapp

object Constants {
    //user info
    lateinit var USER_ID: String
    lateinit var USER_NICKNAME: String

    //sendbird
    const val CHANNEL_META_DATA = "metadata"
    const val SENDBIRD_API_KEY = "D4FCF442-A653-49B3-9D87-6134CD87CA81"

    //intent
    const val CHANNEL_ACTIVITY_INTENT_ACTION ="com.konai.sendbirdapisampleapp.adapter.Action"
    const val INTENT_NAME_USER_ID = "userId"
    const val INTENT_NAME_USER_NICK = "userNick"
    const val INTENT_NAME_CHANNEL_URL = "channelURL"

    //Message Item ViewType
    const val MY_MESSAGE = 0
    const val PARTNER_MESSAGE = 1

    //Message handler id
    const val ALL_MESSAGE_RECEIVE_HANDLER = "allMessageHandler"
    const val LOGIN_ACCOUNT_MESSAGE_RECEIVE_HANDLER = "accountMessageHandler"

    //firebase
    const val FIRESTORE_FIELD_USER_ID = "userId"
    const val FIRESTORE_FIELD_AFFINE_X = "affineX"
    const val FIRESTORE_FIELD_AFFINE_Y = "affineY"
    const val FIRESTORE_DOCUMENT_PUBLIC_KEY = "publicKey"

    //etc
    const val TAG = "testLog"

    //deprecated
    //shared preference
    const val PREFERENCE_NAME_HASH = "hash"
}
