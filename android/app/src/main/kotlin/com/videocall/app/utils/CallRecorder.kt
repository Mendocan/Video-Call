package com.videocall.app.utils

import android.content.Context
import android.media.MediaRecorder
import android.media.MediaMuxer
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.webrtc.VideoTrack

class CallRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var videoRecorder: VideoRecorder? = null
    private var outputFile: File? = null
    private var isRecording = false
    private var isVideoRecording = false
    
    companion object {
        private const val TAG = "CallRecorder"
    }
    
    /**
     * Görüşme kaydını başlatır (sadece ses)
     * @param includeVideo Video kaydı dahil edilsin mi? (şu an desteklenmiyor, ayrı fonksiyon kullanın)
     * @return Kayıt dosyasının yolu, başarısız olursa null
     */
    fun startRecording(includeVideo: Boolean = false): String? {
        if (includeVideo) {
            Log.w(TAG, "Video kaydı için startVideoRecording() kullanın")
            return null
        }
        if (isRecording) {
            Log.w(TAG, "Kayıt zaten devam ediyor")
            return null
        }
        
        try {
            val recordingsDir = File(context.getExternalFilesDir(null), "recordings")
            if (!recordingsDir.exists()) {
                recordingsDir.mkdirs()
            }
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val extension = if (includeVideo) "mp4" else "m4a"
            outputFile = File(recordingsDir, "call_$timestamp.$extension")
            
            @Suppress("DEPRECATION")
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }
            
            mediaRecorder?.apply {
                if (includeVideo) {
                    setVideoSource(MediaRecorder.VideoSource.SURFACE)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                    setVideoEncodingBitRate(2_000_000) // 2 Mbps
                    setVideoFrameRate(30)
                    setVideoSize(1280, 720)
                } else {
                    setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(128_000) // 128 kbps
                    setAudioSamplingRate(44100)
                }
                
                setOutputFile(outputFile!!.absolutePath)
                
                try {
                    prepare()
                    start()
                    isRecording = true
                    Log.i(TAG, "Kayıt başlatıldı: ${outputFile!!.absolutePath}")
                    return outputFile!!.absolutePath
                } catch (e: IOException) {
                    Log.e(TAG, "Kayıt hazırlanamadı", e)
                    release()
                    mediaRecorder = null
                    return null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Kayıt başlatılamadı", e)
            release()
            return null
        }
        
        return null
    }
    
    /**
     * Video kaydını başlatır (WebRTC VideoSink ile)
     * @param localVideoTrack Local video track
     * @param remoteVideoTrack Remote video track (opsiyonel)
     * @return Kayıt dosyasının yolu, başarısız olursa null
     */
    fun startVideoRecording(
        localVideoTrack: VideoTrack? = null,
        remoteVideoTrack: VideoTrack? = null
    ): String? {
        if (isVideoRecording) {
            Log.w(TAG, "Video kaydı zaten devam ediyor")
            return null
        }
        
        try {
            videoRecorder = VideoRecorder(context)
            val recordingPath = videoRecorder?.startRecording(
                includeLocalVideo = localVideoTrack != null,
                includeRemoteVideo = remoteVideoTrack != null
            )
            
            // VideoSink'leri video track'lere ekle
            localVideoTrack?.addSink(videoRecorder?.getLocalVideoSink() ?: return null)
            remoteVideoTrack?.addSink(videoRecorder?.getRemoteVideoSink() ?: return null)
            
            isVideoRecording = true
            Log.i(TAG, "Video kaydı başlatıldı: $recordingPath")
            return recordingPath
        } catch (e: Exception) {
            Log.e(TAG, "Video kaydı başlatılamadı", e)
            stopVideoRecording()
            return null
        }
    }
    
    /**
     * Video kaydını durdurur
     */
    fun stopVideoRecording(): File? {
        if (!isVideoRecording) {
            Log.w(TAG, "Video kaydı devam etmiyor")
            return null
        }
        
        try {
            val file = videoRecorder?.stopRecording()
            videoRecorder = null
            isVideoRecording = false
            Log.i(TAG, "Video kaydı durduruldu")
            return file
        } catch (e: Exception) {
            Log.e(TAG, "Video kaydı durdurulamadı", e)
            videoRecorder = null
            isVideoRecording = false
            return null
        }
    }
    
    /**
     * Video kaydı durumunu kontrol eder
     */
    fun isVideoRecording(): Boolean = isVideoRecording
    
    /**
     * Video kaydı için Surface sağlar (eski API, kullanılmıyor)
     */
    @Deprecated("Video kaydı için startVideoRecording() kullanın")
    fun getRecordingSurface(): android.view.Surface? {
        return mediaRecorder?.surface
    }
    
    /**
     * Kaydı durdurur ve dosyayı döndürür
     */
    fun stopRecording(): File? {
        if (!isRecording) {
            Log.w(TAG, "Kayıt devam etmiyor")
            return null
        }
        
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            
            val file = outputFile
            outputFile = null
            
            Log.i(TAG, "Kayıt durduruldu: ${file?.absolutePath}")
            return file
        } catch (e: Exception) {
            Log.e(TAG, "Kayıt durdurulamadı", e)
            release()
            return null
        }
    }
    
    /**
     * Kaydı iptal eder ve dosyayı siler
     */
    fun cancelRecording() {
        stopRecording()?.delete()
        Log.i(TAG, "Kayıt iptal edildi ve dosya silindi")
    }
    
    /**
     * Kayıt durumunu kontrol eder
     */
    fun isRecording(): Boolean = isRecording
    
    /**
     * Kayıt dosyasının yolunu döndürür
     */
    fun getRecordingPath(): String? = outputFile?.absolutePath
    
    private fun release() {
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.e(TAG, "MediaRecorder release hatası", e)
        }
        mediaRecorder = null
        isRecording = false
    }
}

