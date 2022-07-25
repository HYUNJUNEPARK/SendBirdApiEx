package com.konai.sendbirdapisampleapp.model

data class MessageModel(
    val message: String?,
    val sender: String?,
    val messageId: Long?,
    val createdAt: Long?
)
