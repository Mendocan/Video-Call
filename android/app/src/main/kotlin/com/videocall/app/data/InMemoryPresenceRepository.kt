package com.videocall.app.data

import com.videocall.app.model.PresenceEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Geçici in-memory presence deposu.
 * Gerçek servise geçildiğinde bu sınıf kaldırılacak.
 */
object InMemoryPresenceRepository : PresenceRepository {

    private val entries = ConcurrentHashMap<String, PresenceEntry>()

    override suspend fun publish(entry: PresenceEntry) = withContext(Dispatchers.IO) {
        entries[entry.phoneHash] = entry
    }

    override suspend fun fetch(phoneHash: String): PresenceEntry? = withContext(Dispatchers.IO) {
        entries[phoneHash]
    }

    override suspend fun clear(phoneHash: String) = withContext(Dispatchers.IO) {
        entries.remove(phoneHash)
        Unit
    }
}

