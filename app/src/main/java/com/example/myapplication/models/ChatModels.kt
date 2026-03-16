package com.example.myapplication

import java.text.SimpleDateFormat
import java.util.*

data class Message(
    val text: String,
    val isUser: Boolean,
    val time: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
)

data class Chat(
    val id: String = UUID.randomUUID().toString(),
    val messages: MutableList<Message> = mutableListOf(),
    val createdAt: String = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date())
) {
    fun getTitle(): String {
        val lastUserMessage = messages.lastOrNull { it.isUser }
        return if (lastUserMessage != null) {
            val text = lastUserMessage.text
            if (text.length > 40) text.substring(0, 40) + "..." else text
        } else {
            "Новый чат"
        }
    }

    fun getLastMessageTime(): String {
        return messages.lastOrNull()?.time ?: createdAt
    }

    fun getShortPreview(): String {
        return messages.firstOrNull()?.text?.take(60) ?: "Пустой чат"
    }

    fun isUnread(): Boolean {
        return messages.size == 1 && messages[0].isUser
    }
}