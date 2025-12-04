/*
 * Copyright (c) 2025 DirectCall Project
 * 
 * Licensed under the MIT License
 * See LICENSE file for details
 */

package com.videocall.app.directcall

import android.content.Context
import android.view.SurfaceView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.videocall.app.directcall.sdp.DirectCallSdpParser
import com.videocall.app.directcall.ice.DirectCallIceGatherer
import com.videocall.app.directcall.ice.DirectCallIceCandidate
import com.videocall.app.directcall.rtp.DirectCallRtpSender
import com.videocall.app.directcall.rtp.DirectCallRtpReceiver
import com.videocall.app.directcall.codec.DirectCallVideoCodec
import com.videocall.app.directcall.codec.DirectCallAudioCodec
import com.videocall.app.directcall.security.DirectCallDtlsHandler
import com.videocall.app.directcall.media.DirectCallVideoCapturer
import com.videocall.app.directcall.media.DirectCallVideoRenderer
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean

/**
 * DirectCall Engine - Ana motor
 * 
 * Tüm bileşenleri yönetir:
 * - SDP Parser
 * - ICE Gatherer
 * - RTP Sender/Receiver
 * - Codec'ler
 * - DTLS Handler
 * - Video/Audio capture/render
 */
class DirectCallEngine(
    private val context: Context,
    private val scope: CoroutineScope
) {
    // Bileşenler
    private val sdpParser = DirectCallSdpParser()
    private val iceGatherer = DirectCallIceGatherer()
    private val videoCodec = DirectCallVideoCodec(context)
    private val audioCodec = DirectCallAudioCodec(context)
    private val dtlsHandler = DirectCallDtlsHandler()
    private val videoCapturer = DirectCallVideoCapturer(context)
    private val videoRenderer = DirectCallVideoRenderer()
    
    // RTP
    private var rtpSender: DirectCallRtpSender? = null
    private var rtpReceiver: DirectCallRtpReceiver? = null
    private var rtpSocket: DatagramSocket? = null
    
    // State
    private val isConnected = AtomicBoolean(false)
    private var remoteAddress: InetAddress? = null
    private var remotePort: Int = 0
    private var localPort: Int = 0
    
    // SDP
    private var localSdp: String? = null
    private var remoteSdp: String? = null
    
    /**
     * Local video renderer'ı ekle
     */
    fun attachLocalRenderer(surfaceView: SurfaceView) {
        videoCapturer.attachRenderer(surfaceView)
        videoCapturer.startCapture()
    }
    
    /**
     * Remote video renderer'ı ekle
     */
    fun attachRemoteRenderer(surfaceView: SurfaceView) {
        videoRenderer.attachRenderer(surfaceView)
    }
    
    /**
     * Offer (SDP) oluştur
     */
    suspend fun createOffer(audioOnly: Boolean = false): String {
        // 1. ICE candidate'ları topla
        val iceCandidates = iceGatherer.gatherCandidates()
        
        // 2. SDP oluştur
        val sdp = sdpParser.createOffer(
            audioOnly = audioOnly,
            iceCandidates = iceCandidates,
            videoCodec = if (!audioOnly) "VP8" else null,
            audioCodec = "opus"
        )
        
        localSdp = sdp
        return sdp
    }
    
    /**
     * Answer (SDP) oluştur
     */
    suspend fun createAnswer(offer: String, audioOnly: Boolean = false): String {
        // 1. Remote offer'ı parse et
        val offerInfo = sdpParser.parse(offer)
        remoteSdp = offer
        
        // 2. ICE candidate'ları topla
        val iceCandidates = iceGatherer.gatherCandidates()
        
        // 3. Answer SDP oluştur
        val answer = sdpParser.createAnswer(
            offer = offerInfo,
            audioOnly = audioOnly,
            iceCandidates = iceCandidates,
            videoCodec = if (!audioOnly) "VP8" else null,
            audioCodec = "opus"
        )
        
        localSdp = answer
        
        // 4. Remote description'ı ayarla
        setRemoteDescription(offer, isOffer = true)
        
        return answer
    }
    
    /**
     * Remote description (Offer/Answer) ayarla
     */
    fun setRemoteDescription(sdp: String, isOffer: Boolean) {
        remoteSdp = sdp
        
        // SDP'yi parse et
        val sdpInfo = sdpParser.parse(sdp)
        
        // Remote ICE candidate'larını al
        sdpInfo.iceCandidates.forEach { candidate ->
            addIceCandidate(candidate)
        }
        
        // Remote address ve port'u al (ilk candidate'dan)
        if (sdpInfo.iceCandidates.isNotEmpty()) {
            val firstCandidate = sdpInfo.iceCandidates.first()
            remoteAddress = InetAddress.getByName(firstCandidate.address)
            remotePort = firstCandidate.port
            
            // RTP bağlantısını kur
            establishRtpConnection()
        }
    }
    
    /**
     * ICE candidate ekle
     */
    fun addIceCandidate(candidate: DirectCallIceCandidate) {
        // Remote address ve port'u güncelle (daha iyi candidate varsa)
        val candidateAddress = InetAddress.getByName(candidate.address)
        
        // Priority'ye göre en iyi candidate'ı seç
        // (basit implementasyon: srflx > host)
        if (candidate.type == "srflx" || remoteAddress == null) {
            remoteAddress = candidateAddress
            remotePort = candidate.port
            
            // RTP bağlantısını güncelle
            rtpSender?.updateRemoteAddress(candidateAddress, candidate.port)
        }
    }
    
    /**
     * RTP bağlantısını kur
     */
    private fun establishRtpConnection() {
        if (remoteAddress == null || remotePort == 0) return
        
        scope.launch {
            try {
                // UDP socket oluştur
                rtpSocket = DatagramSocket(0) // Sistem otomatik port atar
                
                // DTLS handshake yap
                val dtlsSession = dtlsHandler.performHandshake(
                    socket = rtpSocket!!,
                    remoteAddress = remoteAddress!!,
                    remotePort = remotePort
                )
                
                // RTP Sender oluştur
                rtpSender = DirectCallRtpSender(
                    socket = rtpSocket!!,
                    remoteAddress = remoteAddress!!,
                    remotePort = remotePort,
                    dtlsHandler = dtlsHandler,
                    videoCodec = videoCodec,
                    audioCodec = audioCodec,
                    videoCapturer = videoCapturer
                )
                
                // RTP Receiver oluştur
                rtpReceiver = DirectCallRtpReceiver(
                    socket = rtpSocket!!,
                    dtlsHandler = dtlsHandler,
                    videoCodec = videoCodec,
                    audioCodec = audioCodec,
                    videoRenderer = videoRenderer,
                    onFrameReceived = { frame ->
                        // Remote video frame alındı
                        videoRenderer.renderFrame(frame)
                    }
                )
                
                // Receiver'ı başlat
                rtpReceiver?.startReceiving()
                
                // Sender'ı başlat
                rtpSender?.startSending()
                
                isConnected.set(true)
            } catch (e: Exception) {
                android.util.Log.e("DirectCallEngine", "RTP bağlantısı kurulamadı", e)
                isConnected.set(false)
            }
        }
    }
    
    /**
     * Video'yu enable/disable et
     */
    fun setVideoEnabled(enabled: Boolean) {
        videoCapturer.setEnabled(enabled)
        rtpSender?.setVideoEnabled(enabled)
    }
    
    /**
     * Audio'yu enable/disable et
     */
    fun setAudioEnabled(enabled: Boolean) {
        rtpSender?.setAudioEnabled(enabled)
    }
    
    /**
     * Kamerayı değiştir
     */
    fun switchCamera() {
        videoCapturer.switchCamera()
    }
    
    /**
     * Bağlantıyı kes
     */
    fun disconnect() {
        rtpSender?.stopSending()
        rtpReceiver?.stopReceiving()
        rtpSocket?.close()
        rtpSocket = null
        rtpSender = null
        rtpReceiver = null
        isConnected.set(false)
    }
    
    /**
     * Kaynakları temizle
     */
    fun dispose() {
        disconnect()
        videoCapturer.dispose()
        videoRenderer.dispose()
        videoCodec.dispose()
        audioCodec.dispose()
    }
    
    /**
     * Aktif bağlantı var mı?
     */
    fun hasActiveConnection(): Boolean {
        return isConnected.get()
    }
}

