/*
 * Copyright (c) 2025 DirectCall Project
 * 
 * Licensed under the MIT License
 * See LICENSE file for details
 */

package com.videocall.app.directcall.sdp

import com.videocall.app.directcall.ice.DirectCallIceCandidate

/**
 * DirectCall SDP Parser
 * 
 * SDP (Session Description Protocol) formatını parse eder ve oluşturur.
 * WebRTC'ye bağımlı değil, kendi implementasyonumuz.
 */
class DirectCallSdpParser {
    
    /**
     * SDP Info - Parse edilmiş SDP bilgileri
     */
    data class SdpInfo(
        val version: String,
        val origin: String,
        val sessionName: String,
        val audio: AudioInfo?,
        val video: VideoInfo?,
        val iceCandidates: List<DirectCallIceCandidate>
    )
    
    data class AudioInfo(
        val port: Int,
        val codec: String,
        val payloadType: Int
    )
    
    data class VideoInfo(
        val port: Int,
        val codec: String,
        val payloadType: Int
    )
    
    /**
     * SDP'yi parse et
     */
    fun parse(sdp: String): SdpInfo {
        val lines = sdp.split("\r\n")
        var version = "0"
        var origin = ""
        var sessionName = "-"
        var audioInfo: AudioInfo? = null
        var videoInfo: VideoInfo? = null
        val iceCandidates = mutableListOf<DirectCallIceCandidate>()
        
        var currentMedia: String? = null
        var currentPayloadType: Int = 96 // Default dynamic payload type
        
        for (line in lines) {
            when {
                line.startsWith("v=") -> {
                    version = line.substring(2)
                }
                line.startsWith("o=") -> {
                    origin = line.substring(2)
                }
                line.startsWith("s=") -> {
                    sessionName = line.substring(2)
                }
                line.startsWith("m=audio") -> {
                    currentMedia = "audio"
                    val parts = line.substring(7).trim().split(" ")
                    if (parts.isNotEmpty()) {
                        val port = parts[0].toIntOrNull() ?: 0
                        // Payload type'ı bul (ilk payload type)
                        if (parts.size > 3) {
                            currentPayloadType = parts[3].toIntOrNull() ?: 111
                        }
                        audioInfo = AudioInfo(
                            port = port,
                            codec = "opus", // Default, rtpmap'ten alınabilir
                            payloadType = currentPayloadType
                        )
                    }
                }
                line.startsWith("m=video") -> {
                    currentMedia = "video"
                    val parts = line.substring(7).trim().split(" ")
                    if (parts.isNotEmpty()) {
                        val port = parts[0].toIntOrNull() ?: 0
                        // Payload type'ı bul (ilk payload type)
                        if (parts.size > 3) {
                            currentPayloadType = parts[3].toIntOrNull() ?: 96
                        }
                        videoInfo = VideoInfo(
                            port = port,
                            codec = "VP8", // Default, rtpmap'ten alınabilir
                            payloadType = currentPayloadType
                        )
                    }
                }
                line.startsWith("a=rtpmap:") -> {
                    // Codec bilgisini güncelle
                    val rtpmap = line.substring(9)
                    val parts = rtpmap.split(" ")
                    if (parts.size >= 2) {
                        val payloadType = parts[0].toIntOrNull()
                        val codecInfo = parts[1].split("/")
                        if (codecInfo.isNotEmpty()) {
                            val codec = codecInfo[0]
                            when (currentMedia) {
                                "audio" -> {
                                    audioInfo = audioInfo?.copy(
                                        codec = codec,
                                        payloadType = payloadType ?: audioInfo.payloadType
                                    )
                                }
                                "video" -> {
                                    videoInfo = videoInfo?.copy(
                                        codec = codec,
                                        payloadType = payloadType ?: videoInfo.payloadType
                                    )
                                }
                            }
                        }
                    }
                }
                line.startsWith("a=candidate:") -> {
                    // ICE candidate parse et
                    val candidate = parseIceCandidate(line)
                    candidate?.let { iceCandidates.add(it) }
                }
            }
        }
        
        return SdpInfo(
            version = version,
            origin = origin,
            sessionName = sessionName,
            audio = audioInfo,
            video = videoInfo,
            iceCandidates = iceCandidates
        )
    }
    
    /**
     * ICE candidate parse et
     * Format: a=candidate:1 1 UDP 2130706431 192.168.1.100 54321 typ host
     */
    private fun parseIceCandidate(line: String): DirectCallIceCandidate? {
        try {
            val candidatePart = line.substring(13) // "a=candidate:" sonrası
            val parts = candidatePart.split(" ")
            
            if (parts.size >= 8) {
                val foundation = parts[0]
                val componentId = parts[1].toIntOrNull() ?: 1
                val transport = parts[2]
                val priority = parts[3].toLongOrNull() ?: 0L
                val address = parts[4]
                val port = parts[5].toIntOrNull() ?: 0
                val type = parts[7] // "typ" sonrası
                
                return DirectCallIceCandidate(
                    foundation = foundation,
                    componentId = componentId,
                    transport = transport,
                    priority = priority,
                    address = address,
                    port = port,
                    type = type
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("DirectCallSdpParser", "ICE candidate parse hatası: $line", e)
        }
        return null
    }
    
    /**
     * Offer SDP oluştur
     */
    fun createOffer(
        audioOnly: Boolean = false,
        iceCandidates: List<DirectCallIceCandidate>,
        videoCodec: String? = "VP8",
        audioCodec: String = "opus"
    ): String {
        val sdp = StringBuilder()
        val timestamp = System.currentTimeMillis()
        
        // Session description
        sdp.append("v=0\r\n")
        sdp.append("o=- $timestamp $timestamp IN IP4 0.0.0.0\r\n")
        sdp.append("s=-\r\n")
        sdp.append("t=0 0\r\n")
        
        // Audio
        sdp.append("m=audio 54321 RTP/SAVPF 111\r\n")
        sdp.append("a=rtpmap:111 $audioCodec/48000/2\r\n")
        sdp.append("a=sendrecv\r\n")
        
        // Video (audioOnly değilse)
        if (!audioOnly && videoCodec != null) {
            sdp.append("m=video 54322 RTP/SAVPF 96\r\n")
            sdp.append("a=rtpmap:96 $videoCodec/90000\r\n")
            sdp.append("a=sendrecv\r\n")
        }
        
        // ICE candidates
        iceCandidates.forEach { candidate ->
            sdp.append("a=candidate:${candidate.foundation} ${candidate.componentId} ${candidate.transport} ${candidate.priority} ${candidate.address} ${candidate.port} typ ${candidate.type}\r\n")
        }
        
        return sdp.toString()
    }
    
    /**
     * Answer SDP oluştur
     */
    fun createAnswer(
        offer: SdpInfo,
        audioOnly: Boolean = false,
        iceCandidates: List<DirectCallIceCandidate>,
        videoCodec: String? = "VP8",
        audioCodec: String = "opus"
    ): String {
        val sdp = StringBuilder()
        val timestamp = System.currentTimeMillis()
        
        // Session description
        sdp.append("v=0\r\n")
        sdp.append("o=- $timestamp $timestamp IN IP4 0.0.0.0\r\n")
        sdp.append("s=-\r\n")
        sdp.append("t=0 0\r\n")
        
        // Audio (offer'dan al)
        offer.audio?.let { audio ->
            sdp.append("m=audio ${audio.port} RTP/SAVPF ${audio.payloadType}\r\n")
            sdp.append("a=rtpmap:${audio.payloadType} $audioCodec/48000/2\r\n")
            sdp.append("a=sendrecv\r\n")
        }
        
        // Video (offer'dan al, audioOnly değilse)
        if (!audioOnly && offer.video != null && videoCodec != null) {
            val video = offer.video
            sdp.append("m=video ${video.port} RTP/SAVPF ${video.payloadType}\r\n")
            sdp.append("a=rtpmap:${video.payloadType} $videoCodec/90000\r\n")
            sdp.append("a=sendrecv\r\n")
        }
        
        // ICE candidates
        iceCandidates.forEach { candidate ->
            sdp.append("a=candidate:${candidate.foundation} ${candidate.componentId} ${candidate.transport} ${candidate.priority} ${candidate.address} ${candidate.port} typ ${candidate.type}\r\n")
        }
        
        return sdp.toString()
    }
}

