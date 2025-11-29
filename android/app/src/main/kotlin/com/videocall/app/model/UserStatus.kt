package com.videocall.app.model

enum class UserStatus {
    ONLINE,           // Çevrimiçi
    BUSY,             // Meşgul
    DO_NOT_DISTURB,   // Rahatsız Etmeyin
    INVISIBLE         // Görünmez
}

data class UserPresence(
    val status: UserStatus,
    val customMessage: String? = null, // Özel durum mesajı (opsiyonel)
    val lastSeen: Long = System.currentTimeMillis()
) {
    fun getStatusText(): String {
        return when (status) {
            UserStatus.ONLINE -> "Çevrimiçi"
            UserStatus.BUSY -> "Meşgul"
            UserStatus.DO_NOT_DISTURB -> "Rahatsız Etmeyin"
            UserStatus.INVISIBLE -> "Görünmez"
        }
    }
}

