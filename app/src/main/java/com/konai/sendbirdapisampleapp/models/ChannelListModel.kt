package com.konai.sendbirdapisampleapp.models

data class ChannelListModel(
    val name: String?,
    val url: String?,
    val lastMessage: String?,
    val lastMessageTime: Long?,
    val memberSize: Int?
)
