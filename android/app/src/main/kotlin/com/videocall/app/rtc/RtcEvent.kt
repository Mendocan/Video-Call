package com.videocall.app.rtc

import org.webrtc.IceCandidate
import org.webrtc.PeerConnection

sealed interface RtcEvent {
    data class ConnectionStateChanged(val state: PeerConnection.PeerConnectionState) : RtcEvent
    data class IceCandidateGenerated(val candidate: IceCandidate) : RtcEvent
    data class Error(val message: String, val throwable: Throwable? = null) : RtcEvent
    data object LocalVideoStarted : RtcEvent
    data object RemoteVideoAvailable : RtcEvent
}

