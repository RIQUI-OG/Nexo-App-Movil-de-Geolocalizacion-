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
    val chatId: String = "",
    val title: String = "",
    val isGroup: Boolean = false,
    val participants: List<String>? = null,
    val lastMessage: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val unreadCounts: Map<String, Int> = emptyMap()
) {
    constructor() : this("", "", false, null, "", System.currentTimeMillis(), emptyMap())
}
