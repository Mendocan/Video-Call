package com.videocall.app.data

import com.videocall.app.model.PresenceEntry

/**
 * Cihazların birbirini bulması için kullanılan kısa ömürlü rehber.
 * Şimdilik in-memory mock, ileride gerçek backend ile değiştirilecek.
 */
interface PresenceRepository {
    suspend fun publish(entry: PresenceEntry)
    suspend fun fetch(phoneHash: String): PresenceEntry?
    suspend fun clear(phoneHash: String)
}

