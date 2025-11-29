package com.videocall.app.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Signaling Server IP Discovery
 * 
 * Otomatik IP bulma mekanizması:
 * 1. Backend API'den signaling server IP'sini al
 * 2. Fallback: BuildConfig'deki default URL'i kullan
 */
object SignalingServerDiscovery {
    private const val BACKEND_URL = "https://api.videocall.app" // Production backend URL
    private const val DEFAULT_SIGNALING_URL = "wss://signaling.videocall.com/ws" // Fallback
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()
    
    /**
     * Backend API'den signaling server IP'sini al
     * @return Signaling server WebSocket URL (ws://IP:PORT/ws) veya null
     */
    suspend fun discoverSignalingServerUrl(): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BACKEND_URL/api/signaling/server-info")
                .get()
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    val json = JSONObject(responseBody)
                    if (json.getBoolean("success")) {
                        val serverInfo = json.getJSONObject("serverInfo")
                        val wsUrl = serverInfo.getString("wsUrl")
                        android.util.Log.d("SignalingServerDiscovery", "Signaling server bulundu: $wsUrl")
                        return@withContext wsUrl
                    }
                }
            }
            
            android.util.Log.w("SignalingServerDiscovery", "Backend'den signaling server bilgisi alınamadı: ${response.code}")
            null
        } catch (e: Exception) {
            android.util.Log.e("SignalingServerDiscovery", "Signaling server discovery hatası", e)
            null
        }
    }
    
    /**
     * Signaling server URL'ini al (discovery + fallback)
     * @param defaultUrl Fallback URL (BuildConfig'den gelebilir)
     * @return Signaling server WebSocket URL
     */
    suspend fun getSignalingServerUrl(defaultUrl: String = DEFAULT_SIGNALING_URL): String {
        val discoveredUrl = discoverSignalingServerUrl()
        return discoveredUrl ?: defaultUrl
    }
}

