package com.videocall.app.model

data class TwoFactorAuth(
    val isEnabled: Boolean = false,
    val secretKey: String? = null, // Base32 encoded secret
    val backupCodes: List<String> = emptyList(), // Backup recovery codes
    val createdAt: Long? = null
) {
    fun hasBackupCodes(): Boolean = backupCodes.isNotEmpty()
}

