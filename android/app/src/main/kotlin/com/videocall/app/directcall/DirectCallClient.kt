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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.videocall.app.directcall.ice.DirectCallIceCandidate

/**
 * DirectCall Client - Kendi bağımsız video görüşme sistemi
 * 
 * WebRTC'ye bağımlı değil, kendi implementasyonumuz.
 * 
 * Bileşenler:
 * - SDP Parser (kendi kodumuz)
 * - ICE Gatherer (kendi kodumuz)
 * - RTP Sender/Receiver (kendi kodumuz)
 * - Codec (libvpx/libopus - açık kaynak)
 * - DTLS Handler (OpenSSL - açık kaynak)
 */
class DirectCallClient(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val appContext = context.applicationContext
    
    // DirectCall Engine (ana motor)
    private val engine = DirectCallEngine(context, scope)
    
    // Event flow
    private val _events = MutableSharedFlow<DirectCallEvent>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<DirectCallEvent> = _events.asSharedFlow()
    
    /**
     * Local video renderer'ı ekle
     */
    fun attachLocalRenderer(surfaceView: SurfaceView) {
        engine.attachLocalRenderer(surfaceView)
    }
    
    /**
     * Remote video renderer'ı ekle
     */
    fun attachRemoteRenderer(surfaceView: SurfaceView) {
        engine.attachRemoteRenderer(surfaceView)
    }
    
    /**
     * Offer (SDP) oluştur
     * @param audioOnly Sadece sesli görüşme mi?
     * @return SDP string
     */
    suspend fun createOffer(audioOnly: Boolean = false): String {
        return engine.createOffer(audioOnly)
    }
    
    /**
     * Answer (SDP) oluştur
     * @param offer Remote offer
     * @param audioOnly Sadece sesli görüşme mi?
     * @return SDP string
     */
    suspend fun createAnswer(offer: String, audioOnly: Boolean = false): String {
        return engine.createAnswer(offer, audioOnly)
    }
    
    /**
     * Remote description (Offer/Answer) ayarla
     */
    fun setRemoteDescription(sdp: String, isOffer: Boolean) {
        engine.setRemoteDescription(sdp, isOffer)
    }
    
    /**
     * ICE candidate ekle
     */
    fun addIceCandidate(candidate: DirectCallIceCandidate) {
        engine.addIceCandidate(candidate)
    }
    
    /**
     * Video'yu enable/disable et
     */
    fun setVideoEnabled(enabled: Boolean) {
        engine.setVideoEnabled(enabled)
    }
    
    /**
     * Audio'yu enable/disable et
     */
    fun setAudioEnabled(enabled: Boolean) {
        engine.setAudioEnabled(enabled)
    }
    
    /**
     * Kamerayı değiştir (ön/arka)
     */
    fun switchCamera() {
        engine.switchCamera()
    }
    
    /**
     * Bağlantıyı kes
     */
    fun disconnect() {
        engine.disconnect()
    }
    
    /**
     * Kaynakları temizle
     */
    fun dispose() {
        engine.dispose()
        scope.cancel()
    }
    
    /**
     * Aktif bağlantı var mı?
     */
    fun hasActiveConnection(): Boolean {
        return engine.hasActiveConnection()
    }
}

/**
 * DirectCall Event'leri
 */
sealed class DirectCallEvent {
    data class IceCandidateGenerated(val candidate: DirectCallIceCandidate) : DirectCallEvent()
    data class ConnectionStateChanged(val state: DirectCallConnectionState) : DirectCallEvent()
    object RemoteVideoAvailable : DirectCallEvent()
    object LocalVideoStarted : DirectCallEvent()
    data class Error(val message: String, val throwable: Throwable?) : DirectCallEvent()
}

/**
 * DirectCall Connection State
 */
enum class DirectCallConnectionState {
    NEW,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    FAILED,
    CLOSED
}

