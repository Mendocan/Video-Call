package com.videocall.app.utils

import android.content.Context
import android.provider.Settings
import android.util.Log
import java.security.MessageDigest

/**
 * Cihaz için benzersiz ID oluşturur ve yönetir
 * APK paylaşımını önlemek için kullanılır
 */
class DeviceIdManager(private val context: Context) {
    
    companion object {
        private const val TAG = "DeviceIdManager"
    }
    
    /**
     * Cihaz için benzersiz ID oluştur
     * Android ID + Build bilgileri kombinasyonu
     */
    fun getDeviceId(): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown"
        
        // Serial numarasına erişim güvenlik izni gerektirir, try-catch ile güvenli hale getir
        val serial = try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                android.os.Build.getSerial()
            } else {
                @Suppress("DEPRECATION")
                android.os.Build.SERIAL
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Cihaz seri numarasına erişilemedi, alternatif ID kullanılıyor", e)
            // Serial erişilemezse, Android ID + diğer build bilgileri ile benzersiz ID oluştur
            "${android.os.Build.MANUFACTURER}_${android.os.Build.MODEL}_${android.os.Build.DEVICE}_${android.os.Build.HARDWARE}"
        }
        
        val buildInfo = "${android.os.Build.MANUFACTURER}_${android.os.Build.MODEL}_$serial"
        
        // Kombinasyonu hash'le (SHA-256)
        val combined = "${androidId}_$buildInfo"
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(combined.toByteArray())
        
        return hash.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Cihaz ID'yi hash'le (backend'e gönderirken)
     */
    fun getHashedDeviceId(): String {
        val deviceId = getDeviceId()
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(deviceId.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}

