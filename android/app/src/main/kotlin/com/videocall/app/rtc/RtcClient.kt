package com.videocall.app.rtc

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.ScreenCapturerAndroid
import org.webrtc.VideoCapturer
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.audio.JavaAudioDeviceModule
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class RtcClient(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {

    private val appContext = context.applicationContext
    private val eglBase: EglBase = EglBase.create()
    private val peerConnectionFactory: PeerConnectionFactory
    private val videoCapturer: CameraVideoCapturer
    private var screenCapturer: ScreenCapturerAndroid? = null
    private var currentCapturer: VideoCapturer? = null
    private var mediaProjection: MediaProjection? = null
    private val videoSource: VideoSource
    private val audioSource: AudioSource
    private val videoTrack: VideoTrack
    private val audioTrack: AudioTrack
    private val captureStarted = AtomicBoolean(false)
    private val capturerInitialized = AtomicBoolean(false)
    private val isScreenSharing = AtomicBoolean(false)
    private val surfaceTextureHelper by lazy {
        SurfaceTextureHelper.create("videocall_camera", eglBase.eglBaseContext)
    }

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:global.stun.twilio.com:3478").createIceServer()
    )

    private val _events = MutableSharedFlow<RtcEvent>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<RtcEvent> = _events.asSharedFlow()

    private var peerConnection: PeerConnection? = null
    private var localRenderer: SurfaceViewRenderer? = null
    private var remoteRenderer: SurfaceViewRenderer? = null
    private var pendingRemoteTrack: VideoTrack? = null
    private var currentVideoQuality: com.videocall.app.model.VideoQuality = com.videocall.app.model.VideoQuality.HIGH

    init {
        WebRtcEnvironment.initialize(context)
        val audioModule = JavaAudioDeviceModule.builder(context)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .createAudioDeviceModule()

        val encoderFactory = DefaultVideoEncoderFactory(
            eglBase.eglBaseContext,
            true,
            true
        )
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setAudioDeviceModule(audioModule)
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()

        videoCapturer = createVideoCapturer(context)
        videoSource = peerConnectionFactory.createVideoSource(false)
        videoTrack = peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, videoSource)
        videoTrack.setEnabled(false)

        audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        audioTrack = peerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource)
        audioTrack.setEnabled(false)
    }

    fun attachLocalRenderer(renderer: SurfaceViewRenderer) {
        renderer.init(eglBase.eglBaseContext, null)
        renderer.setMirror(true)
        renderer.setEnableHardwareScaler(true)
        localRenderer = renderer
        videoTrack.addSink(renderer)
        startPreviewIfNeeded()
    }

    fun attachRemoteRenderer(renderer: SurfaceViewRenderer) {
        renderer.init(eglBase.eglBaseContext, null)
        renderer.setMirror(false)
        renderer.setEnableHardwareScaler(true)
        remoteRenderer = renderer
        pendingRemoteTrack?.let { track ->
            track.addSink(renderer)
            pendingRemoteTrack = null
            _events.tryEmit(RtcEvent.RemoteVideoAvailable)
        }
    }

    suspend fun createOffer(audioOnly: Boolean = false): SessionDescription {
        val peerConnection = ensurePeerConnection()
        val constraints = if (audioOnly) audioOnlyConstraints() else defaultConstraints()
        return peerConnection.createSdp(constraints, isOffer = true)
    }

    suspend fun createAnswer(audioOnly: Boolean = false): SessionDescription {
        val peerConnection = ensurePeerConnection()
        val constraints = if (audioOnly) audioOnlyConstraints() else defaultConstraints()
        return peerConnection.createSdp(constraints, isOffer = false)
    }

    fun setRemoteDescription(description: SessionDescription) {
        ensurePeerConnection().setRemoteDescription(
            object : SdpObserverAdapter() {},
            description
        )
    }

    fun addIceCandidate(candidate: IceCandidate) {
        ensurePeerConnection().addIceCandidate(candidate)
    }

    fun setVideoEnabled(enabled: Boolean) {
        videoTrack.setEnabled(enabled)
    }

    fun setAudioEnabled(enabled: Boolean) {
        audioTrack.setEnabled(enabled)
    }

    fun switchCamera() {
        (videoCapturer as? CameraVideoCapturer)?.switchCamera(null)
    }

    fun hasActivePeerConnection(): Boolean = peerConnection != null
    
    /**
     * Local video track'i döndürür (video kaydı için)
     */
    fun getLocalVideoTrack(): VideoTrack = videoTrack
    
    /**
     * Remote video track'i döndürür (video kaydı için)
     */
    fun getRemoteVideoTrack(): VideoTrack? = pendingRemoteTrack
    
    /**
     * EglBase'i döndürür (video işleme için)
     */
    fun getEglBase(): EglBase = eglBase
    
    /**
     * PeerConnectionFactory'yi döndürür (video işleme için)
     */
    fun getPeerConnectionFactory(): PeerConnectionFactory = peerConnectionFactory

    fun disconnect() {
        peerConnection?.close()
        peerConnection = null
    }

    fun dispose() {
        disconnect()
        if (captureStarted.compareAndSet(true, false)) {
            try {
                videoCapturer.stopCapture()
            } catch (_: InterruptedException) {
            }
        }
        videoCapturer.dispose()
        videoSource.dispose()
        audioSource.dispose()
        localRenderer?.release()
        remoteRenderer?.release()
        surfaceTextureHelper.dispose()
        eglBase.release()
        scope.cancel()
    }

    private fun ensurePeerConnection(): PeerConnection {
        val existing = peerConnection
        if (existing != null) return existing
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        val observer = object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                _events.tryEmit(RtcEvent.IceCandidateGenerated(candidate))
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
                _events.tryEmit(RtcEvent.ConnectionStateChanged(newState))
            }

            override fun onTrack(transceiver: RtpTransceiver) {
                val track = transceiver.receiver.track() as? VideoTrack ?: return
                remoteRenderer?.let {
                    track.addSink(it)
                    _events.tryEmit(RtcEvent.RemoteVideoAvailable)
                } ?: run {
                    pendingRemoteTrack = track
                }
            }

            override fun onStandardizedIceConnectionChange(newState: PeerConnection.IceConnectionState) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) {}
            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {}
            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState) {}
            override fun onSignalingChange(newState: PeerConnection.SignalingState) {}
            override fun onRenegotiationNeeded() {}
            override fun onDataChannel(channel: org.webrtc.DataChannel) {}
            override fun onAddStream(stream: org.webrtc.MediaStream) {}
            override fun onRemoveStream(stream: org.webrtc.MediaStream) {}
        }

        val pc = peerConnectionFactory.createPeerConnection(rtcConfig, observer)
            ?: throw IllegalStateException("PeerConnection could not be created")

        val mediaStreamLabels = listOf("VIDEOCALL_STREAM")
        pc.addTrack(videoTrack, mediaStreamLabels)
        pc.addTrack(audioTrack, mediaStreamLabels)
        peerConnection = pc
        return pc
    }

    private fun startPreviewIfNeeded() {
        if (captureStarted.get() || isScreenSharing.get()) return
        if (capturerInitialized.compareAndSet(false, true)) {
            videoCapturer.initialize(surfaceTextureHelper, appContext, videoSource.capturerObserver)
        }
        try {
            val settings = com.videocall.app.model.VideoQualitySettings.getSettings(currentVideoQuality)
            videoCapturer.startCapture(settings.width, settings.height, settings.fps)
            captureStarted.set(true)
            currentCapturer = videoCapturer
            _events.tryEmit(RtcEvent.LocalVideoStarted)
        } catch (error: Exception) {
            _events.tryEmit(RtcEvent.Error("Önizleme başlatılamadı", error))
        }
    }
    
    fun startScreenCapture(resultCode: Int, data: Intent) {
        if (isScreenSharing.get()) return
        
        try {
            val mediaProjectionManager = appContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
            
            screenCapturer = ScreenCapturerAndroid(data, object : MediaProjection.Callback() {
                override fun onStop() {
                    stopScreenCapture()
                }
            })
            
            // Mevcut capture'ı durdur
            if (captureStarted.get()) {
                videoCapturer.stopCapture()
                captureStarted.set(false)
            }
            
            // Screen capturer'ı başlat
            val screenSurfaceTextureHelper = SurfaceTextureHelper.create("screen_capture", eglBase.eglBaseContext)
            screenCapturer?.initialize(screenSurfaceTextureHelper, appContext, videoSource.capturerObserver)
            
            val settings = com.videocall.app.model.VideoQualitySettings.getSettings(currentVideoQuality)
            screenCapturer?.startCapture(settings.width, settings.height, settings.fps)
            
            isScreenSharing.set(true)
            currentCapturer = screenCapturer
            _events.tryEmit(RtcEvent.LocalVideoStarted)
        } catch (e: Exception) {
            _events.tryEmit(RtcEvent.Error("Ekran paylaşımı başlatılamadı: ${e.message}", e))
        }
    }
    
    fun stopScreenCapture() {
        if (!isScreenSharing.get()) return
        
        try {
            screenCapturer?.stopCapture()
            screenCapturer = null
            mediaProjection?.stop()
            mediaProjection = null
            isScreenSharing.set(false)
            currentCapturer = videoCapturer
            
            // Kameraya geri dön
            startPreviewIfNeeded()
        } catch (e: Exception) {
            _events.tryEmit(RtcEvent.Error("Ekran paylaşımı durdurulamadı: ${e.message}", e))
        }
    }
    
    fun isScreenSharing(): Boolean = isScreenSharing.get()
    
    fun setVideoQuality(quality: com.videocall.app.model.VideoQuality) {
        currentVideoQuality = quality
        // Eğer capture başlamışsa, yeniden başlat
        if (captureStarted.get()) {
            try {
                videoCapturer.stopCapture()
                captureStarted.set(false)
                startPreviewIfNeeded()
            } catch (e: Exception) {
                _events.tryEmit(RtcEvent.Error("Video kalitesi değiştirilemedi", e))
            }
        }
    }

    private fun defaultConstraints(): MediaConstraints = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
    }
    
    private fun audioOnlyConstraints(): MediaConstraints = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
    }
    
    fun setAudioOnlyMode(enabled: Boolean) {
        videoTrack.setEnabled(!enabled)
        if (enabled) {
            // Video capture'ı durdur
            if (captureStarted.get()) {
                try {
                    videoCapturer.stopCapture()
                    captureStarted.set(false)
                } catch (_: Exception) {}
            }
        } else {
            // Video capture'ı başlat
            startPreviewIfNeeded()
        }
    }

    private suspend fun PeerConnection.createSdp(
        constraints: MediaConstraints,
        isOffer: Boolean
    ): SessionDescription = suspendCancellableCoroutine { continuation ->
        val observer = object : SdpObserverAdapter() {
            override fun onCreateSuccess(sdp: SessionDescription) {
                setLocalDescription(object : SdpObserverAdapter() {}, sdp)
                continuation.resume(sdp)
            }

            override fun onCreateFailure(error: String?) {
                continuation.resumeWithException(IllegalStateException("SDP oluşturulamadı: $error"))
            }
        }

        try {
            if (isOffer) {
                createOffer(observer, constraints)
            } else {
                createAnswer(observer, constraints)
            }
        } catch (error: Exception) {
            continuation.resumeWithException(error)
        }
    }

    private fun createVideoCapturer(context: Context): CameraVideoCapturer {
        val enumerator = Camera2Enumerator(context)
        val frontCamera = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
        val cameraName = frontCamera ?: enumerator.deviceNames.firstOrNull()
        requireNotNull(cameraName) { "Kamera bulunamadı" }
        return enumerator.createCapturer(cameraName, null)
    }

    private companion object {
        private const val VIDEO_TRACK_ID = "VIDEOCALL_VIDEO"
        private const val AUDIO_TRACK_ID = "VIDEOCALL_AUDIO"
    }
}

open class SdpObserverAdapter : SdpObserver {
    override fun onCreateSuccess(sessionDescription: SessionDescription) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(error: String?) {}
    override fun onSetFailure(error: String?) {}
}

