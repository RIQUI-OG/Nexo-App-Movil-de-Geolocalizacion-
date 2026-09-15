package com.example.avanceproyecto.models

import androidx.annotation.Keep
import com.google.firebase.database.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    val messageId: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val replyToText: String = "",
    val replyToName: String = ""
) {
    constructor() : this("", "", "", "", "", System.currentTimeMillis(), "", "")
}

@Keep
@IgnoreExtraProperties
data class ChatConversation(
    var chatId: String = "",
    var title: String = "",
    var isGroup: Boolean = false,
    var participants: List<String>? = null,
    var lastMessage: String = "",
    var lastMessageTime: Long = 0L,
    var unreadCounts: Map<String, Int> = HashMap()
) {
    constructor() : this("", "", false, null, "", 0L, HashMap())
}
