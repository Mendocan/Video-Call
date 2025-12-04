package com.videocall.app

import android.app.Application

class VideoCallApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // DirectCall kullanılıyor - WebRtcEnvironment gerekmez
        // Firebase kaldırıldı - WebSocket kullanıyoruz (bağımsız yapı)
    }
}

