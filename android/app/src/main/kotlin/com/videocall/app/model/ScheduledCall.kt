package com.videocall.app.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ScheduledCall(
    val id: Long,
    val contactName: String,
    val contactPhoneNumber: String?,
    val scheduledTime: Long, // Unix timestamp
    val roomCode: String? = null,
    val notes: String? = null,
    val isCompleted: Boolean = false,
    val isCancelled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getFormattedDateTime(): String {
        val date = Date(scheduledTime)
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return sdf.format(date)
    }
    
    fun getFormattedDate(): String {
        val date = Date(scheduledTime)
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return sdf.format(date)
    }
    
    fun getFormattedTime(): String {
        val date = Date(scheduledTime)
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(date)
    }
    
    fun isUpcoming(): Boolean {
        return !isCompleted && !isCancelled && scheduledTime > System.currentTimeMillis()
    }
    
    fun isPast(): Boolean {
        return scheduledTime < System.currentTimeMillis()
    }
    
    fun getTimeUntilCall(): Long {
        return scheduledTime - System.currentTimeMillis()
    }
}

