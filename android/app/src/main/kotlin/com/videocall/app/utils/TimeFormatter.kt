package com.videocall.app.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * Zaman formatlama utility fonksiyonları
 * Her kullanıcı için timezone desteği ile
 */
object TimeFormatter {
    
    /**
     * Chat mesajı için zaman formatı
     * - Bugün: "14:30"
     * - Dün: "Dün 14:30"
     * - Bu hafta: "Pazartesi 14:30"
     * - Daha eski: "15.01.2025 14:30"
     */
    fun formatChatTime(timestamp: Long, timezone: TimeZone = TimeZone.getDefault()): String {
        val date = Date(timestamp)
        val now = Date()
        val calendar = Calendar.getInstance(timezone)
        val messageCalendar = Calendar.getInstance(timezone).apply {
            time = date
        }
        
        val diff = now.time - timestamp
        val daysDiff = (diff / (24 * 60 * 60 * 1000)).toInt()
        
        return when {
            diff < 60_000 -> "Az önce" // 1 dakikadan az
            diff < 3600_000 -> "${diff / 60_000} dk önce" // 1 saatten az
            daysDiff == 0 -> {
                // Bugün - sadece saat
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                sdf.timeZone = timezone
                sdf.format(date)
            }
            daysDiff == 1 -> {
                // Dün
                val sdf = SimpleDateFormat("'Dün' HH:mm", Locale.getDefault())
                sdf.timeZone = timezone
                sdf.format(date)
            }
            daysDiff < 7 -> {
                // Bu hafta - gün adı
                val dayNames = arrayOf("Pazar", "Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi")
                val dayName = dayNames[messageCalendar.get(Calendar.DAY_OF_WEEK) - 1]
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                sdf.timeZone = timezone
                "$dayName ${sdf.format(date)}"
            }
            else -> {
                // Daha eski - tam tarih
                val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                sdf.timeZone = timezone
                sdf.format(date)
            }
        }
    }
    
    /**
     * Görüşme süresi formatı
     * "00:05:30" formatında
     */
    fun formatCallDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, secs)
        }
    }
    
    /**
     * Görüşme tarihi formatı
     */
    fun formatCallDate(timestamp: Long, timezone: TimeZone = TimeZone.getDefault()): String {
        val date = Date(timestamp)
        val now = Date()
        val diff = now.time - timestamp
        val daysDiff = (diff / (24 * 60 * 60 * 1000)).toInt()
        
        return when {
            diff < 60_000 -> "Az önce"
            diff < 3600_000 -> "${diff / 60_000} dakika önce"
            diff < 86400_000 -> "${diff / 3600_000} saat önce"
            daysDiff == 1 -> "Dün"
            daysDiff < 7 -> "${daysDiff} gün önce"
            else -> {
                val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                sdf.timeZone = timezone
                sdf.format(date)
            }
        }
    }
    
    /**
     * Canlı yayın zamanı formatı
     * "2 saat önce başladı" veya "14:30'da başladı"
     */
    fun formatLiveTime(timestamp: Long, timezone: TimeZone = TimeZone.getDefault()): String {
        val date = Date(timestamp)
        val now = Date()
        val diff = now.time - timestamp
        
        return when {
            diff < 60_000 -> "Az önce başladı"
            diff < 3600_000 -> "${diff / 60_000} dakika önce başladı"
            diff < 86400_000 -> "${diff / 3600_000} saat önce başladı"
            else -> {
                val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                sdf.timeZone = timezone
                "${sdf.format(date)}'da başladı"
            }
        }
    }
    
    /**
     * Hikaye kalan süre formatı
     * "2s 30dk kaldı" veya "30dk kaldı"
     */
    fun formatRemainingTime(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        
        return if (hours > 0) {
            "${hours}s ${minutes}dk kaldı"
        } else {
            "${minutes}dk kaldı"
        }
    }
    
    /**
     * Tam tarih ve saat formatı
     * "15.01.2025 14:30:25"
     */
    fun formatFullDateTime(timestamp: Long, timezone: TimeZone = TimeZone.getDefault()): String {
        val date = Date(timestamp)
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
        sdf.timeZone = timezone
        return sdf.format(date)
    }
    
    /**
     * Sadece saat formatı
     * "14:30"
     */
    fun formatTime(timestamp: Long, timezone: TimeZone = TimeZone.getDefault()): String {
        val date = Date(timestamp)
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.timeZone = timezone
        return sdf.format(date)
    }
    
    /**
     * Sadece tarih formatı
     * "15.01.2025"
     */
    fun formatDate(timestamp: Long, timezone: TimeZone = TimeZone.getDefault()): String {
        val date = Date(timestamp)
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        sdf.timeZone = timezone
        return sdf.format(date)
    }
}

