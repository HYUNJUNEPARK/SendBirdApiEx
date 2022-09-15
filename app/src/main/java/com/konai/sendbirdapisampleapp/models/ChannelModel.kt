package com.konai.sendbirdapisampleapp.models

data class ChannelModel(
    var name: String?,
    var url: String?,
    var lastMessage: String?,
    var lastMessageTime: Long?,
    var memberSize: Int?
)
