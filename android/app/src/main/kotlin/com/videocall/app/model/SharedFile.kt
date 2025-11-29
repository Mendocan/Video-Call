package com.videocall.app.model

data class SharedFile(
    val id: String,
    val fileName: String,
    val fileSize: Long, // bytes
    val mimeType: String,
    val senderPhoneNumber: String,
    val senderName: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val isFromMe: Boolean = false,
    val transferState: FileTransferState = FileTransferState.PENDING,
    val progress: Float = 0f, // 0.0 - 1.0
    val fileUri: String? = null // Local file URI (alıcı için)
) {
    fun getFormattedSize(): String {
        return when {
            fileSize < 1024 -> "$fileSize B"
            fileSize < 1024 * 1024 -> "${fileSize / 1024} KB"
            else -> "${fileSize / (1024 * 1024)} MB"
        }
    }
}

enum class FileTransferState {
    PENDING,      // Beklemede
    SENDING,      // Gönderiliyor
    RECEIVING,    // Alınıyor
    COMPLETED,    // Tamamlandı
    FAILED,       // Başarısız
    CANCELLED     // İptal edildi
}

