package com.videocall.app.model

data class CallUiState(
    val roomCode: String = "",
    val statusMessage: String = "Hazır",
    val isConnected: Boolean = false,
    val isMicEnabled: Boolean = false,
    val isCameraEnabled: Boolean = false,
    val remoteVideoVisible: Boolean = false,
    val localVideoVisible: Boolean = false,
    val participants: Int = 1, // Deprecated: Use participantsList.size instead
    val participantsList: List<Participant> = emptyList(), // Multi-party support
    val isGroupCall: Boolean = false, // Is this a group call?
    val callStartTime: Long? = null, // Görüşme başlangıç zamanı (süre gösterimi için)
    val callDuration: Long = 0, // Görüşme süresi (saniye cinsinden)
    val isScreenSharing: Boolean = false, // Ekran paylaşımı durumu
    val isAudioOnly: Boolean = false, // Sadece sesli arama
    val backgroundMode: BackgroundMode = BackgroundMode.NONE, // Arka plan modu
    val filter: FilterType = FilterType.NONE, // Filtre tipi
    val encryptionLevel: EncryptionLevel = EncryptionLevel.END_TO_END, // Şifreleme seviyesi
    val videoQuality: VideoQuality = VideoQuality.HIGH, // Video kalitesi
    val chatMessages: List<com.videocall.app.model.ChatMessage> = emptyList(), // Chat mesajları
    val isChatVisible: Boolean = false, // Chat paneli görünür mü?
    val sharedFiles: List<com.videocall.app.model.SharedFile> = emptyList() // Paylaşılan dosyalar
)

enum class BackgroundMode {
    NONE,
    BLUR,
    COLOR
}

enum class FilterType {
    NONE,
    SEPIA,
    BLACK_WHITE,
    VINTAGE
}

enum class EncryptionLevel {
    END_TO_END,
    STANDARD
}

enum class VideoQuality {
    HIGH,      // 1280x720, 30fps, ~2 Mbps
    MEDIUM,    // 640x480, 30fps, ~1 Mbps
    LOW        // 320x240, 15fps, ~500 Kbps
}

data class VideoQualitySettings(
    val width: Int,
    val height: Int,
    val fps: Int,
    val minBitrate: Int,  // bps
    val maxBitrate: Int   // bps
) {
    companion object {
        fun getSettings(quality: VideoQuality): VideoQualitySettings {
            return when (quality) {
                VideoQuality.HIGH -> VideoQualitySettings(1280, 720, 30, 1_000_000, 2_000_000)
                VideoQuality.MEDIUM -> VideoQualitySettings(640, 480, 30, 500_000, 1_000_000)
                VideoQuality.LOW -> VideoQualitySettings(320, 240, 15, 250_000, 500_000)
            }
        }
    }
}

