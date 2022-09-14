package com.konai.sendbirdapisampleapp.models

data class ChannelListModel(
    var name: String?,
    var url: String?,
    var lastMessage: String?,
    var lastMessageTime: Long?,
    var memberSize: Int?
)
