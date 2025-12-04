package com.videocall.app.model

data class CallUiState(
    val roomCode: String = "",
    val statusMessage: String = "Hazır",
    val isConnected: Boolean = false,
    val isMicEnabled: Boolean = false,
    val isCameraEnabled: Boolean = false,
    val remoteVideoVisible: Boolean = false,
    val localVideoVisible: Boolean = false,
    // participants field kaldırıldı - participantsList.size kullanın
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
    val sharedFiles: List<com.videocall.app.model.SharedFile> = emptyList(), // Paylaşılan dosyalar
    val isRecording: Boolean = false, // Kayıt yapılıyor mu? (kırmızı nokta için)
    val isOtherPartyRecording: Boolean = false // Karşı taraf kayıt yapıyor mu?
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
    FULLHD_60FPS,  // 1920x1080, 60fps, ~4 Mbps (FullHD)
    HIGH,          // 1280x720, 30fps, ~2 Mbps
    UHD_4K,        // 3840x2160, 30fps, ~8 Mbps (4K - telefon destekliyorsa)
    MEDIUM,        // 640x480, 30fps, ~1 Mbps
    LOW            // 320x240, 15fps, ~500 Kbps
}

data class VideoQualitySettings(
    val width: Int,
    val height: Int,
    val fps: Int,
    val minBitrate: Int,  // bps
    val maxBitrate: Int   // bps
) {
    companion object {
        fun getSettings(quality: VideoQuality): VideoQualitySettings = when (quality) {
            VideoQuality.FULLHD_60FPS -> VideoQualitySettings(1920, 1080, 60, 3_000_000, 4_000_000)
            VideoQuality.HIGH -> VideoQualitySettings(1280, 720, 30, 1_000_000, 2_000_000)
            VideoQuality.UHD_4K -> VideoQualitySettings(3840, 2160, 30, 6_000_000, 8_000_000)
            VideoQuality.MEDIUM -> VideoQualitySettings(640, 480, 30, 500_000, 1_000_000)
            VideoQuality.LOW -> VideoQualitySettings(320, 240, 15, 250_000, 500_000)
        }
    }
}

