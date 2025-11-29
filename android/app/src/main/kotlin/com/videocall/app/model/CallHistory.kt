package com.videocall.app.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CallType {
    INCOMING,  // Gelen
    OUTGOING   // Giden
}

data class CallHistory(
    val id: Long,
    val contactName: String?,
    val phoneNumber: String,
    val callType: CallType,
    val timestamp: Long,
    val duration: Long = 0, // saniye cinsinden
    val roomCode: String = ""
) {
    fun getFormattedDate(): String {
        val date = Date(timestamp)
        val now = Date()
        val diff = now.time - date.time
        
        return when {
            diff < 60_000 -> "Az önce" // 1 dakikadan az
            diff < 3600_000 -> "${diff / 60_000} dakika önce" // 1 saatten az
            diff < 86400_000 -> "${diff / 3600_000} saat önce" // 1 günden az
            diff < 604800_000 -> "${diff / 86400_000} gün önce" // 1 haftadan az
            else -> {
                val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                sdf.format(date)
            }
        }
    }
}

