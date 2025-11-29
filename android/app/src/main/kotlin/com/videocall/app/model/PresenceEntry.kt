package com.videocall.app.model

/**
 * Telefon hash'i üzerinden yayınlanan eşleşme bilgisi.
 * Gerçek backend'e taşınana kadar geçici olarak cihaz içinde tutulur.
 */
data class PresenceEntry(
    val phoneHash: String,
    val publicIp: String?,
    val port: Int,
    val token: String,
    val updatedAt: Long
)

