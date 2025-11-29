package com.videocall.app.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatMessage(
    val id: String,
    val senderPhoneNumber: String,
    val senderName: String?,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFromMe: Boolean = false
) {
    fun getFormattedTime(): String {
        val date = Date(timestamp)
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(date)
    }
}

