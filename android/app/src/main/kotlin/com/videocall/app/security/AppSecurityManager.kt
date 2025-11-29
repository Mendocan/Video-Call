package com.videocall.app.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.io.File
import java.security.MessageDigest

/**
 * Uygulama güvenlik yönetimi:
 * - Root/Jailbreak detection
 * - Anti-tampering (APK bütünlük kontrolü)
 * - Debug detection
 * - Emulator detection
 * - App integrity check
 */
class AppSecurityManager(private val context: Context) {
    
    companion object {
        private const val TAG = "AppSecurityManager"
        
        // Bilinen root binary'leri
        private val ROOT_BINARIES = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        
        // Bilinen root package'ları
        private val ROOT_PACKAGES = arrayOf(
            "com.noshufou.android.su",
            "com.noshufou.android.su.elite",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.thirdparty.superuser",
            "com.yellowes.su",
            "com.topjohnwu.magisk",
            "com.kingroot.kinguser",
            "com.kingo.root",
            "com.smedialink.oneclickroot",
            "com.zhiqupk.root.global",
            "com.alephzain.framaroot"
        )
        
        // Bilinen emulator özellikleri
        private val EMULATOR_PROPERTIES = mapOf(
            "ro.kernel.qemu" to "1",
            "ro.hardware" to "goldfish",
            "ro.product.model" to "sdk",
            "ro.product.device" to "generic"
        )
    }
    
    /**
     * Uygulama güvenlik kontrolü - Tüm kontrolleri yapar
     */
    fun performSecurityCheck(): SecurityCheckResult {
        val issues = mutableListOf<String>()
        
        // 1. Root detection
        if (isRooted()) {
            issues.add("Root tespit edildi")
            Log.w(TAG, "⚠️ Root tespit edildi!")
        }
        
        // 2. Debug detection
        if (isDebugging()) {
            issues.add("Debug mod aktif")
            Log.w(TAG, "⚠️ Debug mod aktif!")
        }
        
        // 3. Emulator detection
        if (isEmulator()) {
            issues.add("Emülatör tespit edildi")
            Log.w(TAG, "⚠️ Emülatör tespit edildi!")
        }
        
        // 4. Anti-tampering (APK bütünlük kontrolü)
        if (!verifyAppIntegrity()) {
            issues.add("APK bütünlüğü bozulmuş")
            Log.w(TAG, "⚠️ APK bütünlüğü bozulmuş!")
        }
        
        return SecurityCheckResult(
            isSecure = issues.isEmpty(),
            issues = issues
        )
    }
    
    /**
     * Root cihaz tespiti
     */
    private fun isRooted(): Boolean {
        // 1. Root binary'lerini kontrol et
        for (binary in ROOT_BINARIES) {
            if (File(binary).exists()) {
                return true
            }
        }
        
        // 2. Root package'larını kontrol et
        val packageManager = context.packageManager
        for (packageName in ROOT_PACKAGES) {
            try {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
                return true
            } catch (e: PackageManager.NameNotFoundException) {
                // Package yok, devam et
            }
        }
        
        // 3. "su" komutunu çalıştırmayı dene
        return try {
            Runtime.getRuntime().exec("su")
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Debug mod tespiti
     */
    private fun isDebugging(): Boolean {
        // Debugger bağlı mı?
        return android.os.Debug.isDebuggerConnected()
    }
    
    /**
     * Emülatör tespiti
     */
    private fun isEmulator(): Boolean {
        // Build özelliklerini kontrol et
        for ((key, value) in EMULATOR_PROPERTIES) {
            val prop = getSystemProperty(key)
            if (prop == value) {
                return true
            }
        }
        
        // Model ve manufacturer kontrolü
        val model = Build.MODEL.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()
        val fingerprint = Build.FINGERPRINT.lowercase()
        
        return model.contains("sdk") ||
               model.contains("emulator") ||
               manufacturer.contains("genymotion") ||
               fingerprint.contains("generic") ||
               fingerprint.contains("unknown")
    }
    
    /**
     * APK bütünlük kontrolü (Anti-tampering)
     */
    private fun verifyAppIntegrity(): Boolean {
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }
            
            // İmza kontrolü (basit versiyon)
            // Production'da daha detaylı kontrol yapılmalı
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }
            if (signatures == null || signatures.isEmpty()) {
                return false
            }
            
            // APK dosyasının hash'ini kontrol et
            val apkPath = context.packageCodePath
            val apkFile = File(apkPath)
            
            if (!apkFile.exists()) {
                return false
            }
            
            // Basit hash kontrolü (production'da daha güvenli yöntem kullanılmalı)
            val expectedHash = getExpectedApkHash()
            if (expectedHash != null) {
                val actualHash = calculateFileHash(apkFile)
                return expectedHash == actualHash
            }
            
            // Hash yoksa, en azından dosyanın varlığını kontrol et
            return true
        } catch (e: Exception) {
            Log.e(TAG, "APK bütünlük kontrolü hatası", e)
            return false
        }
    }
    
    /**
     * Sistem özelliği al
     */
    private fun getSystemProperty(key: String): String? {
        return try {
            val process = Runtime.getRuntime().exec("getprop $key")
            process.inputStream.bufferedReader().readText().trim()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Dosya hash'i hesapla (SHA-256)
     */
    private fun calculateFileHash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Beklenen APK hash'i (production'da backend'den alınmalı)
     */
    private fun getExpectedApkHash(): String? {
        // TODO: Production'da backend'den alınmalı veya güvenli bir şekilde saklanmalı
        // Şimdilik null döndürüyoruz (sadece dosya varlığını kontrol ediyoruz)
        return null
    }
    
    /**
     * Güvenlik kontrol sonucu
     */
    data class SecurityCheckResult(
        val isSecure: Boolean,
        val issues: List<String>
    )
}

