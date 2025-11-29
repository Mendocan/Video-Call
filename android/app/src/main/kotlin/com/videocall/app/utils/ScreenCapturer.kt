package com.videocall.app.utils

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import org.webrtc.ScreenCapturerAndroid
import org.webrtc.VideoCapturer

class ScreenCapturerManager(
    private val context: Context,
    private val mediaProjectionManager: MediaProjectionManager
) {
    private var mediaProjection: MediaProjection? = null
    private var screenCapturer: ScreenCapturerAndroid? = null
    
    fun createScreenCapturer(resultCode: Int, data: Intent): VideoCapturer? {
        return try {
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
            screenCapturer = ScreenCapturerAndroid(data, object : MediaProjection.Callback() {
                override fun onStop() {
                    screenCapturer = null
                    mediaProjection = null
                }
            })
            screenCapturer
        } catch (e: Exception) {
            null
        }
    }
    
    fun stopScreenCapture() {
        screenCapturer?.stopCapture()
        mediaProjection?.stop()
        screenCapturer = null
        mediaProjection = null
    }
    
    fun isScreenCapturing(): Boolean {
        return screenCapturer != null && mediaProjection != null
    }
}

