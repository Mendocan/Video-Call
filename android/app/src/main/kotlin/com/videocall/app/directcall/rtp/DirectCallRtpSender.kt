/*
 * Copyright (c) 2025 DirectCall Project
 * 
 * Licensed under the MIT License
 * See LICENSE file for details
 */

package com.videocall.app.directcall.rtp

import com.videocall.app.directcall.codec.DirectCallVideoCodec
import com.videocall.app.directcall.codec.DirectCallAudioCodec
import com.videocall.app.directcall.media.DirectCallVideoCapturer
import com.videocall.app.directcall.security.DirectCallDtlsHandler
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.Random
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * DirectCall RTP Sender
 * 
 * RTP paketlerini gönderir.
 * - Video frame'leri encode edip gönderir
 * - Audio sample'ları encode edip gönderir
 * - SRTP ile şifreler
 * 
 * WebRTC'ye bağımlı değil, kendi implementasyonumuz.
 */
class DirectCallRtpSender(
    private val socket: DatagramSocket,
    private var remoteAddress: InetAddress,
    private var remotePort: Int,
    private val dtlsHandler: DirectCallDtlsHandler,
    private val videoCodec: DirectCallVideoCodec,
    private val audioCodec: DirectCallAudioCodec,
    private val videoCapturer: DirectCallVideoCapturer
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // RTP state
    private val videoSequenceNumber = AtomicInteger(Random().nextInt(65536))
    private val audioSequenceNumber = AtomicInteger(Random().nextInt(65536))
    private val videoTimestamp = AtomicLong(0L)
    private val audioTimestamp = AtomicLong(0L)
    private val ssrc = Random().nextInt()
    
    // Control
    private val isSending = AtomicBoolean(false)
    private val isVideoEnabled = AtomicBoolean(true)
    private val isAudioEnabled = AtomicBoolean(true)
    
    // Video frame rate (30 fps)
    private val videoFrameInterval = 33_333_333L // nanoseconds (90kHz için 3000 timestamp)
    private var lastVideoFrameTime = System.nanoTime()
    
    // Audio sample rate (48kHz)
    private val audioSampleRate = 48000
    private val audioTimestampIncrement = 960L // 20ms frame için (48000 * 0.02)
    
    /**
     * Remote address ve port'u güncelle
     */
    fun updateRemoteAddress(address: InetAddress, port: Int) {
        remoteAddress = address
        remotePort = port
    }
    
    /**
     * RTP göndermeyi başlat
     */
    fun startSending() {
        if (isSending.compareAndSet(false, true)) {
            scope.launch {
                startVideoSending()
            }
            scope.launch {
                startAudioSending()
            }
        }
    }
    
    /**
     * RTP göndermeyi durdur
     */
    fun stopSending() {
        isSending.set(false)
        scope.cancel()
    }
    
    /**
     * Video göndermeyi başlat
     */
    private suspend fun startVideoSending() {
        while (isSending.get()) {
            if (isVideoEnabled.get()) {
                try {
                    // Video frame al
                    val frame = videoCapturer.getNextFrame()
                    if (frame != null) {
                        // Frame'i encode et
                        val encodedFrame = videoCodec.encode(frame)
                        if (encodedFrame != null) {
                            // RTP paketi oluştur
                            val packet = createVideoRtpPacket(encodedFrame)
                            
                            // SRTP ile şifrele
                            val encryptedPacket = dtlsHandler.encryptSrtp(
                                packet.toByteArray(),
                                ssrc,
                                videoSequenceNumber.get()
                            )
                            
                            // Gönder
                            sendPacket(encryptedPacket)
                            
                            // Sequence number ve timestamp güncelle
                            videoSequenceNumber.set((videoSequenceNumber.get() + 1) % 65536)
                            videoTimestamp.addAndGet(3000) // 90kHz için 30fps
                        }
                    }
                    
                    // Frame rate kontrolü (30 fps)
                    val currentTime = System.nanoTime()
                    val elapsed = currentTime - lastVideoFrameTime
                    if (elapsed < videoFrameInterval) {
                        delay((videoFrameInterval - elapsed) / 1_000_000) // nanoseconds to milliseconds
                    }
                    lastVideoFrameTime = System.nanoTime()
                    
                } catch (e: Exception) {
                    android.util.Log.e("DirectCallRtpSender", "Video gönderme hatası", e)
                    delay(100) // Hata durumunda bekle
                }
            } else {
                delay(100) // Video kapalıysa bekle
            }
        }
    }
    
    /**
     * Audio göndermeyi başlat
     */
    private suspend fun startAudioSending() {
        // TODO: Audio capture implementasyonu
        // Şimdilik placeholder
        while (isSending.get()) {
            if (isAudioEnabled.get()) {
                try {
                    // Audio sample al (20ms frame)
                    // val audioSamples = audioCapturer.getNextFrame()
                    // if (audioSamples != null) {
                    //     // Encode et
                    //     val encodedAudio = audioCodec.encode(audioSamples)
                    //     if (encodedAudio != null) {
                    //         // RTP paketi oluştur
                    //         val packet = createAudioRtpPacket(encodedAudio)
                    //         
                    //         // SRTP ile şifrele
                    //         val encryptedPacket = dtlsHandler.encryptSrtp(
                    //             packet.toByteArray(),
                    //             ssrc,
                    //             audioSequenceNumber.get()
                    //         )
                    //         
                    //         // Gönder
                    //         sendPacket(encryptedPacket)
                    //         
                    //         // Sequence number ve timestamp güncelle
                    //         audioSequenceNumber.set((audioSequenceNumber.get() + 1) % 65536)
                    //         audioTimestamp.addAndGet(audioTimestampIncrement)
                    //     }
                    // }
                    
                    delay(20) // 20ms frame interval
                } catch (e: Exception) {
                    android.util.Log.e("DirectCallRtpSender", "Audio gönderme hatası", e)
                    delay(100)
                }
            } else {
                delay(100)
            }
        }
    }
    
    /**
     * Video RTP paketi oluştur
     */
    private fun createVideoRtpPacket(encodedFrame: ByteArray): DirectCallRtpPacket {
        return DirectCallRtpPacket(
            payloadType = 96, // Dynamic video payload type
            sequenceNumber = videoSequenceNumber.get(),
            timestamp = videoTimestamp.get(),
            ssrc = ssrc,
            payload = encodedFrame,
            marker = false // VP8 için marker bit (keyframe kontrolü yapılabilir)
        )
    }
    
    /**
     * Audio RTP paketi oluştur
     */
    private fun createAudioRtpPacket(encodedAudio: ByteArray): DirectCallRtpPacket {
        return DirectCallRtpPacket(
            payloadType = 111, // Dynamic audio payload type (Opus)
            sequenceNumber = audioSequenceNumber.get(),
            timestamp = audioTimestamp.get(),
            ssrc = ssrc,
            payload = encodedAudio,
            marker = false
        )
    }
    
    /**
     * Paketi gönder
     */
    private fun sendPacket(packet: ByteArray) {
        try {
            val datagramPacket = DatagramPacket(
                packet,
                packet.size,
                remoteAddress,
                remotePort
            )
            socket.send(datagramPacket)
        } catch (e: Exception) {
            android.util.Log.e("DirectCallRtpSender", "Paket gönderme hatası", e)
        }
    }
    
    /**
     * Video'yu enable/disable et
     */
    fun setVideoEnabled(enabled: Boolean) {
        isVideoEnabled.set(enabled)
    }
    
    /**
     * Audio'yu enable/disable et
     */
    fun setAudioEnabled(enabled: Boolean) {
        isAudioEnabled.set(enabled)
    }
}

