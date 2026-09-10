package com.example.avanceproyecto.models

data class ChatMessage(
    val messageId: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "", "", "", System.currentTimeMillis())
}

data class ChatConversation(
    val chatId: String = "",
    val title: String = "",
    val isGroup: Boolean = false,
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTime: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", false, emptyList(), "", System.currentTimeMillis())
}
