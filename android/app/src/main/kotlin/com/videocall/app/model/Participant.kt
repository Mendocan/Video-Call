package com.videocall.app.model

data class Participant(
    val id: String, // Unique participant ID
    val name: String? = null, // Participant name
    val phoneNumber: String? = null, // Participant phone number
    val isLocal: Boolean = false, // Is this the local user?
    val isVideoEnabled: Boolean = true,
    val isAudioEnabled: Boolean = true,
    val isMuted: Boolean = false, // Muted by host
    val isVideoVisible: Boolean = true,
    val joinedAt: Long = System.currentTimeMillis()
)

