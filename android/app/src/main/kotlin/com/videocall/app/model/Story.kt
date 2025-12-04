package com.videocall.app.model

import java.util.Date

/**
 * Canlı yayın hikayesi (24 saat süreyle)
 */
data class Story(
    val storyId: String,
    val liveId: String,
    val broadcasterPhoneNumber: String,
    val broadcasterName: String?,
    val title: String?,
    val roomCode: String,
    val createdAt: Long, // Unix timestamp (milliseconds)
    val expiresAt: Long, // Unix timestamp (milliseconds) - createdAt + 24 saat
    val thumbnailUri: String? = null, // Hikaye önizleme görseli
    val viewerCount: Int = 0
) {
    /**
     * Hikaye hala aktif mi? (24 saat geçmedi mi?)
     */
    fun isActive(): Boolean {
        return System.currentTimeMillis() < expiresAt
    }
    
    /**
     * Kalan süre (saniye cinsinden)
     */
    fun getRemainingSeconds(): Long {
        val remaining = expiresAt - System.currentTimeMillis()
        return if (remaining > 0) remaining / 1000 else 0
    }
    
    companion object {
        /**
         * 24 saat = 86,400,000 milliseconds
         */
        const val STORY_DURATION_MS = 24 * 60 * 60 * 1000L
    }
}

