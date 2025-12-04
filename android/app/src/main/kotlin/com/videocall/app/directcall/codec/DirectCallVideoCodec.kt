/*
 * Copyright (c) 2025 DirectCall Project
 * 
 * Licensed under the MIT License
 * See LICENSE file for details
 */

package com.videocall.app.directcall.codec

import android.content.Context
import android.graphics.ImageFormat
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * DirectCall Video Codec
 * 
 * Android MediaCodec API kullanarak gerçek video encoding/decoding.
 * VP8 codec kullanır (Android'de native destekleniyor).
 */
class DirectCallVideoCodec(context: Context) {
    private val appContext = context.applicationContext
    
    // Codec state
    private var encoder: MediaCodec? = null
    private var decoder: MediaCodec? = null
    private val encoderInitialized = AtomicBoolean(false)
    private val decoderInitialized = AtomicBoolean(false)
    
    // Video settings
    private var width = 640
    private var height = 480
    private var fps = 30
    private var bitrate = 500_000 // 500 kbps
    
    // Encoder buffer tracking
    private val encoderInputBuffers = mutableListOf<ByteBuffer>()
    private val encoderOutputBuffers = mutableListOf<ByteBuffer>()
    private var encoderInputBufferIndex = -1
    private var encoderOutputBufferIndex = -1
    
    // Decoder buffer tracking
    private val decoderInputBuffers = mutableListOf<ByteBuffer>()
    private val decoderOutputBuffers = mutableListOf<ByteBuffer>()
    private var decoderInputBufferIndex = -1
    private var decoderOutputBufferIndex = -1
    
    // Sequence number for RTP
    private val sequenceNumber = AtomicInteger(0)
    
    /**
     * Encoder'ı başlat
     */
    fun initializeEncoder(width: Int, height: Int, fps: Int, bitrate: Int) {
        if (encoderInitialized.get()) {
            releaseEncoder()
        }
        
        this.width = width
        this.height = height
        this.fps = fps
        this.bitrate = bitrate
        
        try {
            // VP8 codec oluştur
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_VP8, width, height)
            val codecName = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                MediaCodecList(MediaCodecList.ALL_CODECS).findEncoderForFormat(format)
            } else {
                // API 29 öncesi için MediaCodec.createEncoderByType kullan
                try {
                    val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_VP8)
                    val name = codec?.name
                    codec?.release() // Hemen release et, sadece ismi aldık
                    name
                } catch (e: Exception) {
                    android.util.Log.e("DirectCallVideoCodec", "VP8 encoder bulunamadı", e)
                    null
                }
            } ?: throw IllegalStateException("VP8 encoder bulunamadı")
            
            encoder = MediaCodec.createByCodecName(codecName)
            
            // MediaFormat oluştur (format zaten yukarıda oluşturuldu)
            format.apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // Her saniye bir keyframe
            }
            
            encoder?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder?.start()
            
            encoderInitialized.set(true)
            
            android.util.Log.d("DirectCallVideoCodec", "Video encoder başlatıldı: ${width}x${height}@${fps}fps, ${bitrate}bps")
        } catch (e: Exception) {
            android.util.Log.e("DirectCallVideoCodec", "Video encoder başlatılamadı", e)
            encoderInitialized.set(false)
            throw e
        }
    }
    
    /**
     * Decoder'ı başlat
     */
    fun initializeDecoder() {
        if (decoderInitialized.get()) {
            releaseDecoder()
        }
        
        try {
            // VP8 codec oluştur
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_VP8, width, height)
            val codecName = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                MediaCodecList(MediaCodecList.ALL_CODECS).findDecoderForFormat(format)
            } else {
                // API 29 öncesi için MediaCodec.createDecoderByType kullan
                try {
                    val codec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_VP8)
                    val name = codec?.name
                    codec?.release() // Hemen release et, sadece ismi aldık
                    name
                } catch (e: Exception) {
                    android.util.Log.e("DirectCallVideoCodec", "VP8 decoder bulunamadı", e)
                    null
                }
            } ?: throw IllegalStateException("VP8 decoder bulunamadı")
            
            decoder = MediaCodec.createByCodecName(codecName)
            
            // MediaFormat oluştur (format zaten yukarıda oluşturuldu)
            decoder?.configure(format, null, null, 0)
            decoder?.start()
            
            decoderInitialized.set(true)
            
            android.util.Log.d("DirectCallVideoCodec", "Video decoder başlatıldı")
        } catch (e: Exception) {
            android.util.Log.e("DirectCallVideoCodec", "Video decoder başlatılamadı", e)
            decoderInitialized.set(false)
            throw e
        }
    }
    
    /**
     * Video frame'i encode et
     * @param frame YUV420 formatında frame (ByteArray veya Image)
     * @return Encoded frame (VP8 formatında)
     */
    fun encode(frame: Any): ByteArray? {
        if (!encoderInitialized.get()) {
            initializeEncoder(width, height, fps, bitrate)
        }
        
        val codec = encoder ?: return null
        
        try {
            // Frame'i YUV420 formatına çevir
            val yuvFrame = when (frame) {
                is Image -> imageToYuv420(frame)
                is ByteArray -> frame
                else -> return null
            }
            
            // Input buffer al
            val inputBufferIndex = codec.dequeueInputBuffer(10000) // 10ms timeout
            if (inputBufferIndex < 0) {
                android.util.Log.w("DirectCallVideoCodec", "Encoder input buffer alınamadı")
                return null
            }
            
            val inputBuffer = codec.getInputBuffer(inputBufferIndex)
            inputBuffer?.clear()
            inputBuffer?.put(yuvFrame)
            
            // Frame'i encoder'a gönder
            val presentationTimeUs = System.nanoTime() / 1000
            codec.queueInputBuffer(
                inputBufferIndex,
                0,
                yuvFrame.size,
                presentationTimeUs,
                0
            )
            
            // Output buffer al
            val bufferInfo = MediaCodec.BufferInfo()
            var outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000) // 10ms timeout
            
            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // Format değişti, tekrar dene
                outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
            }
            
            if (outputBufferIndex >= 0) {
                val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                if (outputBuffer != null && bufferInfo.size > 0) {
                    // Encoded frame'i kopyala
                    val encodedFrame = ByteArray(bufferInfo.size)
                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                    outputBuffer.get(encodedFrame)
                    
                    // Buffer'ı serbest bırak
                    codec.releaseOutputBuffer(outputBufferIndex, false)
                    
                    android.util.Log.d("DirectCallVideoCodec", "Video frame encode edildi: ${encodedFrame.size} bytes")
                    return encodedFrame
                }
                
                codec.releaseOutputBuffer(outputBufferIndex, false)
            }
            
            return null
        } catch (e: Exception) {
            android.util.Log.e("DirectCallVideoCodec", "Video encode hatası", e)
            return null
        }
    }
    
    /**
     * Encoded frame'i decode et
     * @param encodedFrame VP8 formatında encoded frame
     * @return Decoded frame (YUV420 formatında)
     */
    fun decode(encodedFrame: ByteArray): ByteArray? {
        if (!decoderInitialized.get()) {
            initializeDecoder()
        }
        
        val codec = decoder ?: return null
        
        try {
            // Input buffer al
            val inputBufferIndex = codec.dequeueInputBuffer(10000) // 10ms timeout
            if (inputBufferIndex < 0) {
                android.util.Log.w("DirectCallVideoCodec", "Decoder input buffer alınamadı")
                return null
            }
            
            val inputBuffer = codec.getInputBuffer(inputBufferIndex)
            inputBuffer?.clear()
            inputBuffer?.put(encodedFrame)
            
            // Frame'i decoder'a gönder
            codec.queueInputBuffer(
                inputBufferIndex,
                0,
                encodedFrame.size,
                0,
                0
            )
            
            // Output buffer al
            val bufferInfo = MediaCodec.BufferInfo()
            var outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000) // 10ms timeout
            
            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // Format değişti, tekrar dene
                outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
            }
            
            if (outputBufferIndex >= 0) {
                val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                if (outputBuffer != null && bufferInfo.size > 0) {
                    // Decoded frame'i kopyala
                    val decodedFrame = ByteArray(bufferInfo.size)
                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                    outputBuffer.get(decodedFrame)
                    
                    // Buffer'ı serbest bırak
                    codec.releaseOutputBuffer(outputBufferIndex, false)
                    
                    android.util.Log.d("DirectCallVideoCodec", "Video frame decode edildi: ${decodedFrame.size} bytes")
                    return decodedFrame
                }
                
                codec.releaseOutputBuffer(outputBufferIndex, false)
            }
            
            return null
        } catch (e: Exception) {
            android.util.Log.e("DirectCallVideoCodec", "Video decode hatası", e)
            return null
        }
    }
    
    /**
     * Android Image'i YUV420 formatına çevir
     */
    private fun imageToYuv420(image: Image): ByteArray {
        val planes = image.planes
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer
        
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        
        val yuv = ByteArray(ySize + uSize + vSize)
        
        yBuffer.get(yuv, 0, ySize)
        uBuffer.get(yuv, ySize, uSize)
        vBuffer.get(yuv, ySize + uSize, vSize)
        
        return yuv
    }
    
    /**
     * Encoder'ı serbest bırak
     */
    private fun releaseEncoder() {
        try {
            encoder?.stop()
            encoder?.release()
            encoder = null
            encoderInitialized.set(false)
        } catch (e: Exception) {
            android.util.Log.e("DirectCallVideoCodec", "Encoder serbest bırakılamadı", e)
        }
    }
    
    /**
     * Decoder'ı serbest bırak
     */
    private fun releaseDecoder() {
        try {
            decoder?.stop()
            decoder?.release()
            decoder = null
            decoderInitialized.set(false)
        } catch (e: Exception) {
            android.util.Log.e("DirectCallVideoCodec", "Decoder serbest bırakılamadı", e)
        }
    }
    
    /**
     * Kaynakları temizle
     */
    fun dispose() {
        releaseEncoder()
        releaseDecoder()
    }
}
