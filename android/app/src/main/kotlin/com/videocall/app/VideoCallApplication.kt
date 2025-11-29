package com.videocall.app

import android.app.Application
import com.videocall.app.rtc.WebRtcEnvironment

class VideoCallApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        WebRtcEnvironment.initialize(this)
    }
}

