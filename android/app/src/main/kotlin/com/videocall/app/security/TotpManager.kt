package com.videocall.app.security

import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.nio.ByteBuffer
import kotlin.math.pow

/**
 * TOTP (Time-based One-Time Password) Manager
 * RFC 6238 standardına uygun TOTP üretimi ve doğrulama
 */
class TotpManager {
    companion object {
        private const val TOTP_PERIOD = 30L // 30 saniye
        private const val TOTP_DIGITS = 6 // 6 haneli kod
        private const val HMAC_ALGORITHM = "HmacSHA1"
        
        /**
         * Secret key oluştur (Base32 formatında)
         */
        fun generateSecretKey(): String {
            val random = SecureRandom()
            val bytes = ByteArray(20) // 160 bit
            random.nextBytes(bytes)
            return Base32.encode(bytes)
        }
        
        /**
         * TOTP kodu üret
         */
        fun generateTotp(secretKey: String, timeStep: Long = System.currentTimeMillis() / 1000 / TOTP_PERIOD): String {
            val key = Base32.decode(secretKey)
            val time = ByteBuffer.allocate(8).putLong(timeStep).array()
            
            val mac = Mac.getInstance(HMAC_ALGORITHM)
            mac.init(SecretKeySpec(key, HMAC_ALGORITHM))
            val hash = mac.doFinal(time)
            
            val offset = hash[hash.size - 1].toInt() and 0x0F
            val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                    ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                    ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                    (hash[offset + 3].toInt() and 0xFF)
            
            val otp = binary % (10.0.pow(TOTP_DIGITS).toInt())
            return String.format("%0${TOTP_DIGITS}d", otp)
        }
        
        /**
         * TOTP kodunu doğrula (30 saniye tolerans)
         */
        fun verifyTotp(secretKey: String, code: String, timeWindow: Int = 1): Boolean {
            val currentTimeStep = System.currentTimeMillis() / 1000 / TOTP_PERIOD
            
            // Mevcut zaman penceresi ve önceki/sonraki pencereleri kontrol et
            for (i in -timeWindow..timeWindow) {
                val timeStep = currentTimeStep + i
                val expectedCode = generateTotp(secretKey, timeStep)
                if (expectedCode == code) {
                    return true
                }
            }
            return false
        }
        
        /**
         * QR kod için otpauth URL oluştur
         */
        fun generateOtpauthUrl(secret: String, accountName: String, issuer: String = "Video Call"): String {
            return "otpauth://totp/$issuer:$accountName?secret=$secret&issuer=$issuer&algorithm=SHA1&digits=6&period=30"
        }
    }
}

/**
 * Base32 encoding/decoding (TOTP için)
 */
object Base32 {
    private const val BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    
    fun encode(data: ByteArray): String {
        val result = StringBuilder()
        var buffer = 0
        var bitsLeft = 0
        
        for (byte in data) {
            buffer = (buffer shl 8) or (byte.toInt() and 0xFF)
            bitsLeft += 8
            
            while (bitsLeft >= 5) {
                val index = (buffer shr (bitsLeft - 5)) and 0x1F
                result.append(BASE32_CHARS[index])
                bitsLeft -= 5
            }
        }
        
        if (bitsLeft > 0) {
            val index = (buffer shl (5 - bitsLeft)) and 0x1F
            result.append(BASE32_CHARS[index])
        }
        
        return result.toString()
    }
    
    fun decode(encoded: String): ByteArray {
        val upperEncoded = encoded.uppercase()
        val result = mutableListOf<Byte>()
        var buffer = 0
        var bitsLeft = 0
        
        for (char in upperEncoded) {
            val index = BASE32_CHARS.indexOf(char)
            if (index == -1) continue
            
            buffer = (buffer shl 5) or index
            bitsLeft += 5
            
            while (bitsLeft >= 8) {
                result.add(((buffer shr (bitsLeft - 8)) and 0xFF).toByte())
                bitsLeft -= 8
            }
        }
        
        return result.toByteArray()
    }
}

