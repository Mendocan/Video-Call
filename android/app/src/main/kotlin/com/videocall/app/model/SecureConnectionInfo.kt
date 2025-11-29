package com.videocall.app.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * Güvenli bağlantı bilgileri (QR kod içeriği)
 */
data class SecureConnectionInfo(
    // WebRTC bilgileri
    @SerializedName("sdp")
    val sdp: String,
    
    @SerializedName("publicIp")
    val publicIp: String?,
    
    @SerializedName("port")
    val port: Int,
    
    // Güvenlik
    @SerializedName("fingerprint")
    val fingerprint: String, // Sertifika parmak izi (SHA-256)
    
    @SerializedName("sharedSecret")
    val sharedSecret: String? = null, // Paylaşılan şifre (opsiyonel)
    
    @SerializedName("phoneHash")
    val phoneHash: String? = null, // Telefon numarası hash (opsiyonel)
    
    @SerializedName("nonce")
    val nonce: String, // Tek kullanımlık token
    
    @SerializedName("timestamp")
    val timestamp: Long,
    
    // Metadata
    @SerializedName("version")
    val version: String = "1.0"
) {
    fun toJson(): String {
        return Gson().toJson(this)
    }
    
    companion object {
        fun fromJson(json: String): SecureConnectionInfo? {
            return try {
                Gson().fromJson(json, SecureConnectionInfo::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
}

