package com.videocall.app.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MessageStatus {
    SENDING,      // Gönderiliyor
    SENT,         // Gönderildi (tek çentik - gri)
    DELIVERED,    // Ulaştı (çift çentik - mavi)
    READ,         // Okundu (çift çentik - mavi, içi dolu)
    FAILED        // Başarısız
}

data class ChatMessage(
    val id: String,
    val senderPhoneNumber: String,
    val senderName: String?,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFromMe: Boolean = false,
    val status: MessageStatus = MessageStatus.SENDING,
    val editedAt: Long? = null, // Düzenleme zamanı
    val isEdited: Boolean = false // Düzenlendi mi?
) {
    fun getFormattedTime(): String {
        val date = Date(timestamp)
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(date)
    }
}

