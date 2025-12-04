/*
 * Copyright (c) 2025 DirectCall Project
 * 
 * Licensed under the MIT License
 * See LICENSE file for details
 */

package com.videocall.app.directcall.ice

/**
 * DirectCall ICE Candidate
 * 
 * ICE (Interactive Connectivity Establishment) candidate bilgileri.
 * WebRTC'ye bağımlı değil, kendi data class'ımız.
 */
data class DirectCallIceCandidate(
    val foundation: String,
    val componentId: Int,
    val transport: String,
    val priority: Long,
    val address: String,
    val port: Int,
    val type: String // "host", "srflx", "relay"
) {
    /**
     * SDP formatına çevir
     * Format: candidate:1 1 UDP 2130706431 192.168.1.100 54321 typ host
     */
    fun toSdpString(): String {
        return "candidate:${foundation} ${componentId} ${transport} ${priority} ${address} ${port} typ ${type}"
    }
    
    /**
     * JSON formatına çevir (signaling için)
     */
    fun toJson(): Map<String, Any> {
        return mapOf(
            "foundation" to foundation,
            "componentId" to componentId,
            "transport" to transport,
            "priority" to priority,
            "address" to address,
            "port" to port,
            "type" to type
        )
    }
    
    companion object {
        /**
         * JSON'dan oluştur
         */
        fun fromJson(json: Map<String, Any>): DirectCallIceCandidate {
            return DirectCallIceCandidate(
                foundation = json["foundation"] as? String ?: "",
                componentId = (json["componentId"] as? Number)?.toInt() ?: 1,
                transport = json["transport"] as? String ?: "UDP",
                priority = (json["priority"] as? Number)?.toLong() ?: 0L,
                address = json["address"] as? String ?: "",
                port = (json["port"] as? Number)?.toInt() ?: 0,
                type = json["type"] as? String ?: "host"
            )
        }
    }
}

