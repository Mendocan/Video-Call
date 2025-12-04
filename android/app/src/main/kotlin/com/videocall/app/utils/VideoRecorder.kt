package com.videocall.app.utils

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.util.Log
// import org.webrtc.VideoFrame // DirectCall'da şimdilik kullanılmıyor
// import org.webrtc.VideoSink // DirectCall'da şimdilik kullanılmıyor
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * WebRTC VideoSink kullanarak video kaydı yapan sınıf
 * Local ve remote video track'lerden frame'leri yakalar ve MediaMuxer ile birleştirir
 */
class VideoRecorder(
    private val context: Context,
    private val width: Int = 1280,
    private val height: Int = 720,
    private val frameRate: Int = 30
) {
    private var mediaMuxer: MediaMuxer? = null
    private var videoEncoder: MediaCodec? = null
    private var outputFile: File? = null
    private val isRecording = AtomicBoolean(false)
    private val isMuxerStarted = AtomicBoolean(false)
    private var videoTrackIndex = -1
    private var audioTrackIndex = -1
    private val frameCount = AtomicInteger(0)
    
    // DirectCall'da şimdilik kullanılmıyor
    /*
    private val localVideoSink = object : VideoSink {
        override fun onFrame(frame: VideoFrame) {
            if (isRecording.get()) {
                encodeFrame(frame)
            }
        }
    }
    
    private val remoteVideoSink = object : VideoSink {
        override fun onFrame(frame: VideoFrame) {
            if (isRecording.get()) {
                encodeFrame(frame)
            }
        }
    }
    */
    
    companion object {
        private const val TAG = "VideoRecorder"
        private const val MIME_TYPE = "video/avc"
        private const val IFRAME_INTERVAL = 1
        private const val TIMEOUT_USEC = 10000L
    }
    
    /**
     * Video kaydını başlatır
     * @param includeLocalVideo Local video dahil edilsin mi?
     * @param includeRemoteVideo Remote video dahil edilsin mi?
     * @return Kayıt dosyasının yolu, başarısız olursa null
     */
    fun startRecording(includeLocalVideo: Boolean = true, includeRemoteVideo: Boolean = true): String? {
        if (isRecording.get()) {
            Log.w(TAG, "Kayıt zaten devam ediyor")
            return null
        }
        
        try {
            val recordingsDir = File(context.getExternalFilesDir(null), "recordings")
            if (!recordingsDir.exists()) {
                recordingsDir.mkdirs()
            }
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            outputFile = File(recordingsDir, "video_call_$timestamp.mp4")
            
            // MediaMuxer oluştur
            mediaMuxer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                MediaMuxer(outputFile!!.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            } else {
                @Suppress("DEPRECATION")
                MediaMuxer(outputFile!!.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            }
            
            // Video encoder oluştur
            val format = MediaFormat.createVideoFormat(MIME_TYPE, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, 2_000_000) // 2 Mbps
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL)
            }
            
            val encoder = MediaCodec.createEncoderByType(MIME_TYPE)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            videoEncoder = encoder
            
            isRecording.set(true)
            frameCount.set(0)
            
            Log.i(TAG, "Video kaydı başlatıldı: ${outputFile!!.absolutePath}")
            return outputFile!!.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Video kaydı başlatılamadı", e)
            stopRecording()
            return null
        }
    }
    
    /**
     * Local video track için VideoSink döndürür (DirectCall'da şimdilik kullanılmıyor)
     */
    // fun getLocalVideoSink(): VideoSink = localVideoSink
    
    /**
     * Remote video track için VideoSink döndürür (DirectCall'da şimdilik kullanılmıyor)
     */
    // fun getRemoteVideoSink(): VideoSink = remoteVideoSink
    
    /**
     * Video frame'ini encode eder (DirectCall'da şimdilik kullanılmıyor)
     */
    private fun encodeFrame(frame: Any) { // VideoFrame yerine Any
        // DirectCall'da şimdilik kullanılmıyor
        return
        /*
        val encoder = videoEncoder ?: return
        
        try {
            val inputBufferIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC)
            if (inputBufferIndex >= 0) {
                val inputBuffer = encoder.getInputBuffer(inputBufferIndex)
                // Frame'i input buffer'a yaz
                // Not: Bu basit implementasyon, gerçek implementasyonda frame'i Surface'e render etmek gerekir
                frameCount.incrementAndGet()
            }
            
            // Encoded frame'leri al
            val bufferInfo = MediaCodec.BufferInfo()
            var outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
            while (outputBufferIndex >= 0) {
                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val newFormat = encoder.outputFormat
                    if (!isMuxerStarted.get()) {
                        videoTrackIndex = mediaMuxer!!.addTrack(newFormat)
                        // Audio track eklenirse burada eklenir
                        mediaMuxer!!.start()
                        isMuxerStarted.set(true)
                    }
                } else if (outputBufferIndex >= 0) {
                    val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)
                    if (outputBuffer != null && isMuxerStarted.get()) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        mediaMuxer!!.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo)
                    }
                    encoder.releaseOutputBuffer(outputBufferIndex, false)
                }
                outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Frame encode hatası", e)
        }
        */
    }
    
    /**
     * Kaydı durdurur ve dosyayı döndürür
     */
    fun stopRecording(): File? {
        if (!isRecording.get()) {
            Log.w(TAG, "Kayıt devam etmiyor")
            return null
        }
        
        isRecording.set(false)
        
        try {
            videoEncoder?.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    signalEndOfInputStream()
                }
                stop()
                release()
            }
            videoEncoder = null
            
            if (isMuxerStarted.get()) {
                mediaMuxer?.stop()
            }
            mediaMuxer?.release()
            mediaMuxer = null
            
            isMuxerStarted.set(false)
            videoTrackIndex = -1
            audioTrackIndex = -1
            
            val file = outputFile
            outputFile = null
            
            Log.i(TAG, "Video kaydı durduruldu: ${file?.absolutePath}, Frame sayısı: ${frameCount.get()}")
            return file
        } catch (e: Exception) {
            Log.e(TAG, "Video kaydı durdurulamadı", e)
            release()
            return null
        }
    }
    
    /**
     * Kayıt durumunu kontrol eder
     */
    fun isRecording(): Boolean = isRecording.get()
    
    /**
     * Kayıt dosyasının yolunu döndürür
     */
    fun getRecordingPath(): String? = outputFile?.absolutePath
    
    private fun release() {
        try {
            videoEncoder?.release()
            mediaMuxer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Release hatası", e)
        }
        videoEncoder = null
        mediaMuxer = null
        isRecording.set(false)
        isMuxerStarted.set(false)
    }
}

