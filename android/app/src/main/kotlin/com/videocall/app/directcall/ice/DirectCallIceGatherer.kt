/*
 * Copyright (c) 2025 DirectCall Project
 * 
 * Licensed under the MIT License
 * See LICENSE file for details
 */

package com.videocall.app.directcall.ice

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.*
import java.util.*

/**
 * DirectCall ICE Gatherer
 * 
 * ICE candidate'ları toplar.
 * - Host candidate (local IP)
 * - Server reflexive candidate (STUN ile - RFC 5389 uyumlu)
 * 
 * WebRTC'ye bağımlı değil, kendi implementasyonumuz.
 */
class DirectCallIceGatherer {
    
    private val stunServers = listOf(
        "stun.l.google.com:19302",
        "global.stun.twilio.com:3478"
    )
    
    // STUN Magic Cookie (RFC 5389)
    private val STUN_MAGIC_COOKIE = 0x2112A442L
    
    /**
     * ICE candidate'ları topla
     * @param stunServer STUN server (opsiyonel, null ise sadece host candidate)
     */
    suspend fun gatherCandidates(
        stunServer: String? = null
    ): List<DirectCallIceCandidate> = withContext(Dispatchers.IO) {
        val candidates = mutableListOf<DirectCallIceCandidate>()
        
        // 1. Host candidate (local IP)
        val localIp = getLocalIpAddress()
        if (localIp != null) {
            val hostPort = 54321 // Default port
            candidates.add(
                DirectCallIceCandidate(
                    foundation = "1",
                    componentId = 1,
                    transport = "UDP",
                    priority = 2130706431, // Host candidate priority
                    address = localIp,
                    port = hostPort,
                    type = "host"
                )
            )
        }
        
        // 2. Server reflexive candidate (STUN)
        val server = stunServer ?: stunServers.firstOrNull()
        if (server != null) {
            try {
                val publicIp = queryStunServer(server)
                if (publicIp != null) {
                    val srflxPort = 54322 // Default port
                    candidates.add(
                        DirectCallIceCandidate(
                            foundation = "2",
                            componentId = 1,
                            transport = "UDP",
                            priority = 1694498815, // Server reflexive priority
                            address = publicIp,
                            port = srflxPort,
                            type = "srflx"
                        )
                    )
                }
            } catch (e: Exception) {
                android.util.Log.w("DirectCallIceGatherer", "STUN query başarısız: ${e.message}")
            }
        }
        
        android.util.Log.d("DirectCallIceGatherer", "ICE candidate'ları toplandı: ${candidates.size} adet")
        candidates
    }
    
    /**
     * Local IP adresini al
     */
    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        // Wi-Fi veya mobil veri IP'si
                        val hostAddress = address.hostAddress
                        if (hostAddress != null && !hostAddress.startsWith("169.254")) {
                            // Link-local adresi değilse
                            return hostAddress
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DirectCallIceGatherer", "Local IP alınamadı", e)
        }
        return null
    }
    
    /**
     * STUN server'a query gönder ve public IP al
     * 
     * RFC 5389 uyumlu STUN Binding Request/Response implementasyonu.
     */
    private suspend fun queryStunServer(server: String): String? = withContext(Dispatchers.IO) {
        try {
            val parts = server.split(":")
            val host = parts[0]
            val port = if (parts.size > 1) parts[1].toIntOrNull() ?: 19302 else 19302
            
            // Transaction ID oluştur (12 bytes)
            val transactionId = ByteArray(12).apply {
                Random().nextBytes(this)
            }
            
            // STUN Binding Request oluştur (RFC 5389)
            val request = ByteArray(20).apply {
                // Message type: Binding Request (0x0001)
                this[0] = 0x00
                this[1] = 0x01
                
                // Message length: 0 (no attributes)
                this[2] = 0x00
                this[3] = 0x00
                
                // Magic cookie (0x2112A442)
                this[4] = 0x21.toByte()
                this[5] = 0x12.toByte()
                this[6] = 0xA4.toByte()
                this[7] = 0x42.toByte()
                
                // Transaction ID (12 bytes)
                System.arraycopy(transactionId, 0, this, 8, 12)
            }
            
            // UDP socket oluştur
            val socket = DatagramSocket()
            socket.soTimeout = 5000 // 5 saniye timeout
            
            // STUN server'a gönder
            val serverAddress = InetAddress.getByName(host)
            val packet = DatagramPacket(request, request.size, serverAddress, port)
            socket.send(packet)
            
            // Response al
            val response = ByteArray(1024)
            val responsePacket = DatagramPacket(response, response.size)
            socket.receive(responsePacket)
            
            // Response'u parse et (RFC 5389)
            val publicIp = parseStunResponse(response, responsePacket.length, transactionId)
            
            socket.close()
            
            return@withContext publicIp
            
        } catch (e: Exception) {
            android.util.Log.e("DirectCallIceGatherer", "STUN query hatası", e)
            return@withContext null
        }
    }
    
    /**
     * STUN Response'u parse et (RFC 5389)
     * 
     * XOR-MAPPED-ADDRESS attribute'unu bulur ve public IP'yi döndürür.
     */
    private fun parseStunResponse(response: ByteArray, length: Int, transactionId: ByteArray): String? {
        try {
            if (length < 20) {
                return null // Minimum STUN message size
            }
            
            // Message type kontrolü (Binding Response: 0x0101)
            val messageType = ((response[0].toInt() and 0xFF) shl 8) or (response[1].toInt() and 0xFF)
            if (messageType != 0x0101) {
                android.util.Log.w("DirectCallIceGatherer", "STUN response type yanlış: $messageType")
                return null
            }
            
            // Transaction ID kontrolü
            for (i in 0 until 12) {
                if (response[8 + i] != transactionId[i]) {
                    android.util.Log.w("DirectCallIceGatherer", "STUN transaction ID eşleşmedi")
                    return null
                }
            }
            
            // Attributes'ları parse et
            val messageLength = ((response[2].toInt() and 0xFF) shl 8) or (response[3].toInt() and 0xFF)
            var offset = 20 // STUN header size
            
            while (offset < 20 + messageLength) {
                if (offset + 4 > length) break
                
                // Attribute type
                val attrType = ((response[offset].toInt() and 0xFF) shl 8) or (response[offset + 1].toInt() and 0xFF)
                // Attribute length
                val attrLength = ((response[offset + 2].toInt() and 0xFF) shl 8) or (response[offset + 3].toInt() and 0xFF)
                
                offset += 4
                
                // XOR-MAPPED-ADDRESS (0x0020)
                if (attrType == 0x0020) {
                    if (offset + attrLength > length) break
                    
                    // XOR-MAPPED-ADDRESS format:
                    // 0x00 (unused)
                    // Family (0x01 = IPv4, 0x02 = IPv6)
                    // X-Port (XOR'd port)
                    // X-Address (XOR'd address)
                    
                    val family = response[offset + 1].toInt() and 0xFF
                    if (family == 0x01) { // IPv4
                        // Port (XOR'd with magic cookie high 16 bits)
                        val xPort = ((response[offset + 2].toInt() and 0xFF) shl 8) or (response[offset + 3].toInt() and 0xFF)
                        val port = xPort xor ((STUN_MAGIC_COOKIE shr 16).toInt() and 0xFFFF)
                        
                        // Address (XOR'd with magic cookie)
                        val xAddr1 = response[offset + 4].toInt() and 0xFF
                        val xAddr2 = response[offset + 5].toInt() and 0xFF
                        val xAddr3 = response[offset + 6].toInt() and 0xFF
                        val xAddr4 = response[offset + 7].toInt() and 0xFF
                        
                        val addr1 = xAddr1 xor ((STUN_MAGIC_COOKIE shr 24).toInt() and 0xFF)
                        val addr2 = xAddr2 xor ((STUN_MAGIC_COOKIE shr 16).toInt() and 0xFF)
                        val addr3 = xAddr3 xor ((STUN_MAGIC_COOKIE shr 8).toInt() and 0xFF)
                        val addr4 = xAddr4 xor (STUN_MAGIC_COOKIE.toInt() and 0xFF)
                        
                        val publicIp = "$addr1.$addr2.$addr3.$addr4"
                        android.util.Log.d("DirectCallIceGatherer", "STUN public IP bulundu: $publicIp:$port")
                        return publicIp
                    }
                }
                
                // Next attribute (4-byte aligned)
                offset += attrLength
                if (attrLength % 4 != 0) {
                    offset += (4 - (attrLength % 4))
                }
            }
            
            return null
        } catch (e: Exception) {
            android.util.Log.e("DirectCallIceGatherer", "STUN response parse hatası", e)
            return null
        }
    }
}
