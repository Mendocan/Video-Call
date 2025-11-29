package com.videocall.app.security

import android.util.Log
import com.videocall.app.model.SecureConnectionInfo
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Güvenlik yönetimi: Fingerprint, Nonce, Rate Limiting
 */
class SecurityManager {
    
    private val usedNonces = ConcurrentHashMap<String, Long>()
    private val connectionAttempts = ConcurrentHashMap<String, MutableList<Long>>()
    
    companion object {
        private const val TAG = "SecurityManager"
        private const val NONCE_EXPIRY = 60 * 60 * 1000L // 1 saat
        private const val MAX_ATTEMPTS = 5
        private const val TIME_WINDOW = 60 * 1000L // 1 dakika
        private const val TIMESTAMP_MAX_AGE = 5 * 60 * 1000L // 5 dakika
    }
    
    /**
     * SDP'den fingerprint hesapla (SHA-256)
     */
    fun calculateFingerprint(sdp: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(sdp.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Tek kullanımlık token (nonce) oluştur
     */
    fun generateNonce(): String {
        return UUID.randomUUID().toString()
    }
    
    /**
     * Nonce doğrula (tek kullanımlık kontrolü)
     */
    fun verifyNonce(nonce: String): Boolean {
        val now = System.currentTimeMillis()
        
        // Eski nonce'ları temizle
        usedNonces.entries.removeIf { (_, timestamp) -> now - timestamp > NONCE_EXPIRY }
        
        // Zaten kullanılmış mı?
        if (usedNonces.containsKey(nonce)) {
            Log.w(TAG, "Nonce zaten kullanılmış: $nonce")
            return false
        }
        
        // Kullanıldı olarak işaretle
        usedNonces[nonce] = now
        return true
    }
    
    /**
     * Timestamp doğrula (5 dakika geçerli)
     */
    fun verifyTimestamp(timestamp: Long): Boolean {
        val age = System.currentTimeMillis() - timestamp
        val isValid = age >= 0 && age <= TIMESTAMP_MAX_AGE
        
        if (!isValid) {
            Log.w(TAG, "Timestamp geçersiz: age=$age ms")
        }
        
        return isValid
    }
    
    /**
     * Rate limiting kontrolü
     */
    fun canConnect(ipAddress: String): Boolean {
        val now = System.currentTimeMillis()
        val attempts = connectionAttempts[ipAddress] ?: mutableListOf()
        
        // Eski kayıtları temizle
        attempts.removeAll { now - it > TIME_WINDOW }
        
        if (attempts.size >= MAX_ATTEMPTS) {
            Log.w(TAG, "Rate limit aşıldı: $ipAddress (${attempts.size} deneme)")
            return false
        }
        
        attempts.add(now)
        connectionAttempts[ipAddress] = attempts
        return true
    }
    
    /**
     * Güvenli bağlantı bilgilerini doğrula
     */
    fun verifyConnectionInfo(
        info: SecureConnectionInfo,
        expectedSecret: String? = null
    ): VerificationResult {
        // 1. Timestamp kontrolü
        if (!verifyTimestamp(info.timestamp)) {
            return VerificationResult(
                isValid = false,
                reason = "QR kod süresi dolmuş (5 dakika geçerli)"
            )
        }
        
        // 2. Fingerprint kontrolü
        val expectedFingerprint = calculateFingerprint(info.sdp)
        if (info.fingerprint != expectedFingerprint) {
            return VerificationResult(
                isValid = false,
                reason = "Fingerprint eşleşmiyor (sahte QR kod olabilir)"
            )
        }
        
        // 3. Secret kontrolü (varsa)
        if (expectedSecret != null && info.sharedSecret != expectedSecret) {
            return VerificationResult(
                isValid = false,
                reason = "Paylaşılan şifre eşleşmiyor"
            )
        }
        
        // 4. Nonce kontrolü
        if (!verifyNonce(info.nonce)) {
            return VerificationResult(
                isValid = false,
                reason = "QR kod zaten kullanılmış"
            )
        }
        
        // 5. IP rate limiting
        val ip = info.publicIp ?: "unknown"
        if (!canConnect(ip)) {
            return VerificationResult(
                isValid = false,
                reason = "Çok fazla bağlantı denemesi (1 dakika bekleyin)"
            )
        }
        
        return VerificationResult(isValid = true, reason = null)
    }
    
    /**
     * Telefon numarası hash'i hesapla
     */
    fun hashPhoneNumber(phoneNumber: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(phoneNumber.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Temizleme (bellek yönetimi)
     */
    fun cleanup() {
        val now = System.currentTimeMillis()
        usedNonces.entries.removeIf { (_, timestamp) -> now - timestamp > NONCE_EXPIRY }
        connectionAttempts.values.forEach { it.removeAll { timestamp -> now - timestamp > TIME_WINDOW } }
    }
    
    data class VerificationResult(
        val isValid: Boolean,
        val reason: String?
    )
}

