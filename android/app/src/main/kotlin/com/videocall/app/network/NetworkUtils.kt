package com.videocall.app.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Ağ yardımcı fonksiyonları: IP adresi alma
 */
object NetworkUtils {
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
    
    /**
     * Public IP adresini al (STUN benzeri servisler)
     */
    suspend fun getPublicIP(): String? = withContext(Dispatchers.IO) {
        val services = listOf(
            "https://api.ipify.org",
            "https://icanhazip.com",
            "https://ifconfig.me/ip"
        )
        
        for (service in services) {
            try {
                val request = Request.Builder()
                    .url(service)
                    .build()
                
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val ip = response.body?.string()?.trim()
                    if (ip != null && isValidIP(ip)) {
                        return@withContext ip
                    }
                }
            } catch (e: Exception) {
                // Bir servis başarısız olursa diğerini dene
                continue
            }
        }
        
        null
    }
    
    /**
     * IP adresi formatını doğrula
     */
    private fun isValidIP(ip: String): Boolean {
        val parts = ip.split(".")
        if (parts.size != 4) return false
        
        return parts.all { part ->
            try {
                val num = part.toInt()
                num in 0..255
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Rastgele port oluştur (dinamik port aralığı)
     */
    fun generateRandomPort(): Int {
        return (49152..65535).random()
    }
}

