/*
 * Copyright (c) 2025 DirectCall Project
 * 
 * Licensed under the MIT License
 * See LICENSE file for details
 */

package com.videocall.app.directcall.rtp

import com.videocall.app.directcall.codec.DirectCallVideoCodec
import com.videocall.app.directcall.codec.DirectCallAudioCodec
import com.videocall.app.directcall.media.DirectCallVideoRenderer
import com.videocall.app.directcall.security.DirectCallDtlsHandler
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * DirectCall RTP Receiver
 * 
 * RTP paketlerini alır ve işler.
 * - RTP paketlerini alır
 * - SRTP ile decrypt eder
 * - Video frame'leri decode edip render eder
 * - Audio sample'ları decode edip çalar
 * - Jitter buffer yönetimi
 * 
 * WebRTC'ye bağımlı değil, kendi implementasyonumuz.
 */
class DirectCallRtpReceiver(
    private val socket: DatagramSocket,
    private val dtlsHandler: DirectCallDtlsHandler,
    private val videoCodec: DirectCallVideoCodec,
    private val audioCodec: DirectCallAudioCodec,
    private val videoRenderer: DirectCallVideoRenderer,
    private val onFrameReceived: (ByteArray) -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // State
    private val isReceiving = AtomicBoolean(false)
    private val expectedVideoSequenceNumber = AtomicInteger(0)
    private val expectedAudioSequenceNumber = AtomicInteger(0)
    
    // Jitter buffer (basit implementasyon - sequence number tracking)
    private var lastVideoSequenceNumber = -1
    private var lastAudioSequenceNumber = -1
    private var videoPacketLossCount = 0
    private var audioPacketLossCount = 0
    
    /**
     * RTP almayı başlat
     */
    fun startReceiving() {
        if (isReceiving.compareAndSet(false, true)) {
            scope.launch {
                receiveLoop()
            }
        }
    }
    
    /**
     * RTP almayı durdur
     */
    fun stopReceiving() {
        isReceiving.set(false)
        scope.cancel()
    }
    
    /**
     * RTP alım döngüsü
     */
    private suspend fun receiveLoop() {
        val buffer = ByteArray(1500) // MTU size
        
        while (isReceiving.get()) {
            try {
                val packet = DatagramPacket(buffer, buffer.size)
                socket.soTimeout = 1000 // 1 saniye timeout
                socket.receive(packet)
                
                // SRTP decrypt et
                val decryptedPacket = dtlsHandler.decryptSrtp(
                    packet.data.sliceArray(0 until packet.length)
                )
                
                if (decryptedPacket != null) {
                    // RTP paketini parse et
                    val rtpPacket = DirectCallRtpPacket.fromByteArray(decryptedPacket)
                    
                    if (rtpPacket != null) {
                        // Paketi işle
                        processRtpPacket(rtpPacket)
                    }
                }
            } catch (e: java.net.SocketTimeoutException) {
                // Timeout normal, devam et
                continue
            } catch (e: Exception) {
                if (isReceiving.get()) {
                    android.util.Log.e("DirectCallRtpReceiver", "RTP alma hatası", e)
                    delay(100) // Hata durumunda kısa bekle
                }
            }
        }
    }
    
    /**
     * RTP paketini işle
     */
    private suspend fun processRtpPacket(packet: DirectCallRtpPacket) {
        when (packet.payloadType) {
            96 -> {
                // Video packet
                processVideoPacket(packet)
            }
            111 -> {
                // Audio packet (AAC)
                processAudioPacket(packet)
            }
            else -> {
                android.util.Log.w("DirectCallRtpReceiver", "Bilinmeyen payload type: ${packet.payloadType}")
            }
        }
    }
    
    /**
     * Video paketini işle
     */
    private suspend fun processVideoPacket(packet: DirectCallRtpPacket) {
        // Sequence number kontrolü
        val receivedSeq = packet.sequenceNumber
        
        if (lastVideoSequenceNumber >= 0) {
            val expectedSeq = (lastVideoSequenceNumber + 1) % 65536
            if (receivedSeq != expectedSeq) {
                // Packet loss veya out-of-order
                if (receivedSeq > expectedSeq) {
                    val lost = receivedSeq - expectedSeq
                    videoPacketLossCount += lost
                    android.util.Log.w("DirectCallRtpReceiver", "Video packet loss: expected=$expectedSeq, received=$receivedSeq, lost=$lost")
                } else {
                    // Out-of-order (geç paket)
                    android.util.Log.d("DirectCallRtpReceiver", "Video out-of-order packet: expected=$expectedSeq, received=$receivedSeq")
                    // Geç paketi atla
                    return
                }
            }
        }
        
        lastVideoSequenceNumber = receivedSeq
        
        // Frame'i decode et
        val decodedFrame = videoCodec.decode(packet.payload)
        if (decodedFrame != null) {
            // Frame'i render et
            videoRenderer.renderFrame(decodedFrame)
            onFrameReceived(decodedFrame)
        }
    }
    
    /**
     * Audio paketini işle
     */
    private suspend fun processAudioPacket(packet: DirectCallRtpPacket) {
        // Sequence number kontrolü
        val receivedSeq = packet.sequenceNumber
        
        if (lastAudioSequenceNumber >= 0) {
            val expectedSeq = (lastAudioSequenceNumber + 1) % 65536
            if (receivedSeq != expectedSeq) {
                if (receivedSeq > expectedSeq) {
                    val lost = receivedSeq - expectedSeq
                    audioPacketLossCount += lost
                    android.util.Log.w("DirectCallRtpReceiver", "Audio packet loss: expected=$expectedSeq, received=$receivedSeq, lost=$lost")
                } else {
                    // Out-of-order
                    android.util.Log.d("DirectCallRtpReceiver", "Audio out-of-order packet: expected=$expectedSeq, received=$receivedSeq")
                    return
                }
            }
        }
        
        lastAudioSequenceNumber = receivedSeq
        
        // Audio'yu decode et
        val decodedAudio = audioCodec.decode(packet.payload)
        if (decodedAudio != null) {
            // Audio'yu çal (AudioTrack kullanılabilir)
            // TODO: Audio playback implementasyonu
            android.util.Log.d("DirectCallRtpReceiver", "Audio decode edildi: ${decodedAudio.size} bytes")
        }
    }
    
    /**
     * Packet loss istatistiklerini al
     */
    fun getPacketLossStats(): Pair<Int, Int> {
        return Pair(videoPacketLossCount, audioPacketLossCount)
    }
    
    /**
     * İstatistikleri sıfırla
     */
    fun resetStats() {
        videoPacketLossCount = 0
        audioPacketLossCount = 0
        lastVideoSequenceNumber = -1
        lastAudioSequenceNumber = -1
    }
}
