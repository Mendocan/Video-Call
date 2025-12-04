/*
 * Copyright (c) 2025 DirectCall Project
 * 
 * Licensed under the MIT License
 * See LICENSE file for details
 */

package com.videocall.app.directcall.codec

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * DirectCall Audio Codec
 * 
 * Android MediaCodec API kullanarak gerçek audio encoding/decoding.
 * AAC codec kullanır (Android'de native destekleniyor, Opus yerine).
 * 
 * Not: Opus Android'de native olarak desteklenmiyor, bu yüzden AAC kullanıyoruz.
 * Alternatif: Opus için native library (libopus) eklenebilir.
 */
class DirectCallAudioCodec(context: Context) {
    private val appContext = context.applicationContext
    
    // Codec state
    private var encoder: MediaCodec? = null
    private var decoder: MediaCodec? = null
    private val encoderInitialized = AtomicBoolean(false)
    private val decoderInitialized = AtomicBoolean(false)
    
    // Audio settings
    private val sampleRate = 48000
    private val channels = 2 // Stereo
    private val bitrate = 64000 // 64 kbps
    private val frameSize = 960 // 20ms frame (48000 * 0.02)
    
    /**
     * Encoder'ı başlat
     */
    fun initializeEncoder() {
        if (encoderInitialized.get()) {
            releaseEncoder()
        }
        
        try {
            // AAC codec oluştur
            val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channels)
            val codecName = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                MediaCodecList(MediaCodecList.ALL_CODECS).findEncoderForFormat(format)
            } else {
                // API 29 öncesi için MediaCodec.createEncoderByType kullan
                try {
                    val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
                    val name = codec?.name
                    codec?.release() // Hemen release et, sadece ismi aldık
                    name
                } catch (e: Exception) {
                    android.util.Log.e("DirectCallAudioCodec", "AAC encoder bulunamadı", e)
                    null
                }
            } ?: throw IllegalStateException("AAC encoder bulunamadı")
            
            encoder = MediaCodec.createByCodecName(codecName)
            
            // MediaFormat oluştur (format zaten yukarıda oluşturuldu)
            format.apply {
                setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
                setInteger(MediaFormat.KEY_AAC_PROFILE, android.media.MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, frameSize * 2) // 16-bit samples
            }
            
            encoder?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder?.start()
            
            encoderInitialized.set(true)
            
            android.util.Log.d("DirectCallAudioCodec", "Audio encoder başlatıldı: ${sampleRate}Hz, ${channels}ch, ${bitrate}bps")
        } catch (e: Exception) {
            android.util.Log.e("DirectCallAudioCodec", "Audio encoder başlatılamadı", e)
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
            // AAC codec oluştur
            val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channels)
            val codecName = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                MediaCodecList(MediaCodecList.ALL_CODECS).findDecoderForFormat(format)
            } else {
                // API 29 öncesi için MediaCodec.createDecoderByType kullan
                try {
                    val codec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
                    val name = codec?.name
                    codec?.release() // Hemen release et, sadece ismi aldık
                    name
                } catch (e: Exception) {
                    android.util.Log.e("DirectCallAudioCodec", "AAC decoder bulunamadı", e)
                    null
                }
            } ?: throw IllegalStateException("AAC decoder bulunamadı")
            
            decoder = MediaCodec.createByCodecName(codecName)
            
            // MediaFormat oluştur (format zaten yukarıda oluşturuldu)
            decoder?.configure(format, null, null, 0)
            decoder?.start()
            
            decoderInitialized.set(true)
            
            android.util.Log.d("DirectCallAudioCodec", "Audio decoder başlatıldı")
        } catch (e: Exception) {
            android.util.Log.e("DirectCallAudioCodec", "Audio decoder başlatılamadı", e)
            decoderInitialized.set(false)
            throw e
        }
    }
    
    /**
     * Audio sample'ları encode et
     * @param samples PCM audio samples (16-bit, 48kHz, stereo)
     * @return Encoded audio (AAC formatında)
     */
    fun encode(samples: ByteArray): ByteArray? {
        if (!encoderInitialized.get()) {
            initializeEncoder()
        }
        
        val codec = encoder ?: return null
        
        try {
            // Input buffer al
            val inputBufferIndex = codec.dequeueInputBuffer(10000) // 10ms timeout
            if (inputBufferIndex < 0) {
                android.util.Log.w("DirectCallAudioCodec", "Encoder input buffer alınamadı")
                return null
            }
            
            val inputBuffer = codec.getInputBuffer(inputBufferIndex)
            inputBuffer?.clear()
            inputBuffer?.put(samples)
            
            // Sample'ları encoder'a gönder
            val presentationTimeUs = System.nanoTime() / 1000
            codec.queueInputBuffer(
                inputBufferIndex,
                0,
                samples.size,
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
                    // Encoded audio'yu kopyala
                    val encodedAudio = ByteArray(bufferInfo.size)
                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                    outputBuffer.get(encodedAudio)
                    
                    // Buffer'ı serbest bırak
                    codec.releaseOutputBuffer(outputBufferIndex, false)
                    
                    android.util.Log.d("DirectCallAudioCodec", "Audio encode edildi: ${encodedAudio.size} bytes")
                    return encodedAudio
                }
                
                codec.releaseOutputBuffer(outputBufferIndex, false)
            }
            
            return null
        } catch (e: Exception) {
            android.util.Log.e("DirectCallAudioCodec", "Audio encode hatası", e)
            return null
        }
    }
    
    /**
     * Encoded audio'yu decode et
     * @param encodedAudio AAC formatında encoded audio
     * @return Decoded audio (PCM formatında)
     */
    fun decode(encodedAudio: ByteArray): ByteArray? {
        if (!decoderInitialized.get()) {
            initializeDecoder()
        }
        
        val codec = decoder ?: return null
        
        try {
            // Input buffer al
            val inputBufferIndex = codec.dequeueInputBuffer(10000) // 10ms timeout
            if (inputBufferIndex < 0) {
                android.util.Log.w("DirectCallAudioCodec", "Decoder input buffer alınamadı")
                return null
            }
            
            val inputBuffer = codec.getInputBuffer(inputBufferIndex)
            inputBuffer?.clear()
            inputBuffer?.put(encodedAudio)
            
            // Audio'yu decoder'a gönder
            codec.queueInputBuffer(
                inputBufferIndex,
                0,
                encodedAudio.size,
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
                    // Decoded audio'yu kopyala
                    val decodedAudio = ByteArray(bufferInfo.size)
                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                    outputBuffer.get(decodedAudio)
                    
                    // Buffer'ı serbest bırak
                    codec.releaseOutputBuffer(outputBufferIndex, false)
                    
                    android.util.Log.d("DirectCallAudioCodec", "Audio decode edildi: ${decodedAudio.size} bytes")
                    return decodedAudio
                }
                
                codec.releaseOutputBuffer(outputBufferIndex, false)
            }
            
            return null
        } catch (e: Exception) {
            android.util.Log.e("DirectCallAudioCodec", "Audio decode hatası", e)
            return null
        }
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
            android.util.Log.e("DirectCallAudioCodec", "Encoder serbest bırakılamadı", e)
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
            android.util.Log.e("DirectCallAudioCodec", "Decoder serbest bırakılamadı", e)
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
