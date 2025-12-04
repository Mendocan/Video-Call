package com.videocall.app.utils

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
// import org.webrtc.ScreenCapturerAndroid // DirectCall'da şimdilik kullanılmıyor
// import org.webrtc.VideoCapturer // DirectCall'da şimdilik kullanılmıyor

class ScreenCapturerManager(
    private val context: Context,
    private val mediaProjectionManager: MediaProjectionManager
) {
    private var mediaProjection: MediaProjection? = null
    // private var screenCapturer: ScreenCapturerAndroid? = null // DirectCall'da şimdilik kullanılmıyor
    
    fun createScreenCapturer(resultCode: Int, data: Intent): Any? { // VideoCapturer yerine Any
        // DirectCall'da şimdilik kullanılmıyor
        android.util.Log.w("ScreenCapturerManager", "Screen capture DirectCall'da henüz desteklenmiyor")
        return null
        /*
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
        */
    }
    
    fun stopScreenCapture() {
        // screenCapturer?.stopCapture() // DirectCall'da şimdilik kullanılmıyor
        mediaProjection?.stop()
        // screenCapturer = null
        mediaProjection = null
    }
    
    fun isScreenCapturing(): Boolean {
        // return screenCapturer != null && mediaProjection != null // DirectCall'da şimdilik kullanılmıyor
        return false
    }
}

