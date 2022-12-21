package com.konai.sendbirdapisampleapp.models

data class ChannelModel(
    var url: String?,
    var name: String?,
    var lastMessage: String?,
    var lastMessageTime: Long?,
    var memberSize: Int?
)
