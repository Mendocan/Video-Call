/*
 * Copyright (c) 2025 DirectCall Project
 * 
 * Licensed under the MIT License
 * See LICENSE file for details
 */

package com.videocall.app.directcall.rtp

/**
 * DirectCall RTP Packet
 * 
 * RTP (Real-Time Transport Protocol) paketi.
 * WebRTC'ye bağımlı değil, kendi implementasyonumuz.
 */
data class DirectCallRtpPacket(
    val version: Int = 2, // RTP version (always 2)
    val padding: Boolean = false,
    val extension: Boolean = false,
    val csrcCount: Int = 0, // Contributing source count
    val marker: Boolean = false,
    val payloadType: Int, // 96 = dynamic video, 111 = dynamic audio
    val sequenceNumber: Int,
    val timestamp: Long,
    val ssrc: Int, // Synchronization source identifier
    val payload: ByteArray
) {
    /**
     * RTP paketini byte array'e çevir
     */
    fun toByteArray(): ByteArray {
        val header = ByteArray(12)
        
        // Byte 0: V(2) P(1) X(1) CC(4)
        header[0] = ((version shl 6) or 
                   (if (padding) 0x20 else 0) or 
                   (if (extension) 0x10 else 0) or 
                   (csrcCount and 0x0F)).toByte()
        
        // Byte 1: M(1) PT(7)
        header[1] = ((if (marker) 0x80 else 0) or (payloadType and 0x7F)).toByte()
        
        // Byte 2-3: Sequence number
        header[2] = ((sequenceNumber shr 8) and 0xFF).toByte()
        header[3] = (sequenceNumber and 0xFF).toByte()
        
        // Byte 4-7: Timestamp
        header[4] = ((timestamp shr 24) and 0xFF).toByte()
        header[5] = ((timestamp shr 16) and 0xFF).toByte()
        header[6] = ((timestamp shr 8) and 0xFF).toByte()
        header[7] = (timestamp and 0xFF).toByte()
        
        // Byte 8-11: SSRC
        header[8] = ((ssrc shr 24) and 0xFF).toByte()
        header[9] = ((ssrc shr 16) and 0xFF).toByte()
        header[10] = ((ssrc shr 8) and 0xFF).toByte()
        header[11] = (ssrc and 0xFF).toByte()
        
        // Header + Payload
        return header + payload
    }
    
    companion object {
        /**
         * Byte array'den RTP paketi oluştur
         */
        fun fromByteArray(data: ByteArray): DirectCallRtpPacket? {
            if (data.size < 12) return null
            
            try {
                val version = (data[0].toInt() shr 6) and 0x03
                val padding = (data[0].toInt() and 0x20) != 0
                val extension = (data[0].toInt() and 0x10) != 0
                val csrcCount = data[0].toInt() and 0x0F
                
                val marker = (data[1].toInt() and 0x80) != 0
                val payloadType = data[1].toInt() and 0x7F
                
                val sequenceNumber = ((data[2].toInt() and 0xFF) shl 8) or (data[3].toInt() and 0xFF)
                
                val timestamp = ((data[4].toInt() and 0xFF).toLong() shl 24) or
                               ((data[5].toInt() and 0xFF).toLong() shl 16) or
                               ((data[6].toInt() and 0xFF).toLong() shl 8) or
                               (data[7].toInt() and 0xFF).toLong()
                
                val ssrc = ((data[8].toInt() and 0xFF) shl 24) or
                          ((data[9].toInt() and 0xFF) shl 16) or
                          ((data[10].toInt() and 0xFF) shl 8) or
                          (data[11].toInt() and 0xFF)
                
                val payload = data.sliceArray(12 until data.size)
                
                return DirectCallRtpPacket(
                    version = version,
                    padding = padding,
                    extension = extension,
                    csrcCount = csrcCount,
                    marker = marker,
                    payloadType = payloadType,
                    sequenceNumber = sequenceNumber,
                    timestamp = timestamp,
                    ssrc = ssrc,
                    payload = payload
                )
            } catch (e: Exception) {
                android.util.Log.e("DirectCallRtpPacket", "RTP paketi parse hatası", e)
                return null
            }
        }
    }
}

