package com.videocall.app.security

import android.content.Context
import android.util.Log
import java.security.MessageDigest

/**
 * API Key yönetimi - Güvenli saklama ve kullanım
 * 
 * NOT: Production'da native kod (JNI) ile saklanmalı veya
 * backend'den dinamik olarak alınmalı
 */
class ApiKeyManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ApiKeyManager"
        
        // TODO: Production'da bu değerler native kod ile saklanmalı
        // veya backend'den dinamik olarak alınmalı
        // Şimdilik obfuscation ile korunuyor (ProGuard)
        private const val API_KEY_PREFIX = "vc_" // Video Call prefix
        private const val API_KEY_SUFFIX = "_2025" // Yıl suffix
    }
    
    /**
     * API key'i güvenli bir şekilde al
     * Production'da native kod veya backend'den alınmalı
     */
    fun getApiKey(): String {
        // TODO: Production'da native kod ile saklanmalı
        // Şimdilik obfuscation ile korunuyor
        val baseKey = getBaseApiKey()
        return hashApiKey(baseKey)
    }
    
    /**
     * Base API key (obfuscation ile korunuyor)
     * Production'da native kod ile saklanmalı
     */
    private fun getBaseApiKey(): String {
        // Bu değer ProGuard ile obfuscate edilecek
        // Production'da native kod (JNI) ile saklanmalı
        return "${API_KEY_PREFIX}${getDeviceFingerprint()}${API_KEY_SUFFIX}"
    }
    
    /**
     * Cihaz fingerprint'i al (cihaza özel)
     */
    private fun getDeviceFingerprint(): String {
        val androidId = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown"
        
        // Cihaz bilgilerini hash'le
        val deviceInfo = "${android.os.Build.MANUFACTURER}_${android.os.Build.MODEL}_$androidId"
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(deviceInfo.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }.take(16) // İlk 16 karakter
    }
    
    /**
     * API key'i hash'le (backend'e gönderirken)
     */
    private fun hashApiKey(key: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(key.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * API key'i doğrula (backend'den gelen doğrulama için)
     */
    fun verifyApiKey(receivedKey: String): Boolean {
        val expectedKey = getApiKey()
        return expectedKey == receivedKey
    }
}

