package com.videocall.app.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * QR kod için minimal bağlantı bilgileri (SDP yok, sadece IP+port+token)
 */
data class QRConnectionInfo(
    @SerializedName("ip")
    val ip: String,
    
    @SerializedName("port")
    val port: Int,
    
    @SerializedName("token")
    val token: String,
    
    @SerializedName("phoneHash")
    val phoneHash: String? = null,
    
    @SerializedName("version")
    val version: String = "2.0"
) {
    fun toJson(): String {
        return Gson().toJson(this)
    }
    
    companion object {
        fun fromJson(json: String): QRConnectionInfo? {
            return try {
                Gson().fromJson(json, QRConnectionInfo::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
}

