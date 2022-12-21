package com.konai.sendbirdapisampleapp.models

data class MessageModel(
    val message: String?,
    val sender: String?,
    val messageId: Long?,
    val createdAt: Long?
)
