package com.videocall.app.security

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.videocall.app.model.QRConnectionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * QR kod oluşturma ve okuma yönetimi
 * SDP QR kodda değil, WebSocket üzerinden gönderilir
 */
class QRCodeManager {

    data class QRCodeResult(
        val bitmap: Bitmap,
        val info: QRConnectionInfo
    )
    
    private val securityManager = SecurityManager()
    
    /**
     * Minimal QR kod oluştur (sadece IP+port+token, SDP yok)
     */
    suspend fun createSecureQR(
        publicIp: String?,
        port: Int,
        token: String,
        phoneNumber: String? = null
    ): QRCodeResult = withContext(Dispatchers.Default) {
        val phoneHash = phoneNumber?.let { securityManager.hashPhoneNumber(it) }
        
        val info = QRConnectionInfo(
            ip = publicIp ?: "",
            port = port,
            token = token,
            phoneHash = phoneHash
        )
        
        val json = info.toJson()
        val bitmap = createQRCodeBitmap(json, 512, 512)
        QRCodeResult(bitmap = bitmap, info = info)
    }
    
    /**
     * 2FA için QR kod oluştur (otpauth URL'den)
     */
    suspend fun create2FAQRCode(otpauthUrl: String): Bitmap = withContext(Dispatchers.Default) {
        createQRCodeBitmap(otpauthUrl, 512, 512)
    }
    
    /**
     * QR kod bitmap oluştur
     */
    private fun createQRCodeBitmap(
        content: String,
        width: Int,
        height: Int
    ): Bitmap {
        val hints = hashMapOf<EncodeHintType, Any>().apply {
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
            put(EncodeHintType.MARGIN, 1)
        }
        
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints)
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        
        return bitmap
    }
    
    /**
     * Kişi listesi için QR kod oluştur
     */
    suspend fun createContactsQRCode(contactsJson: String): Bitmap = withContext(Dispatchers.Default) {
        // Base64 encode edilmiş JSON string'i QR kod içine koy
        val base64Encoded = android.util.Base64.encodeToString(
            contactsJson.toByteArray(Charsets.UTF_8),
            android.util.Base64.NO_WRAP
        )
        // QR kod formatı: "CONTACTS:" prefix ile başla
        val qrContent = "CONTACTS:$base64Encoded"
        createQRCodeBitmap(qrContent, 512, 512)
    }
    
    /**
     * QR kod içeriğini doğrula (minimal format)
     */
    fun verifyQRContent(qrData: String): Pair<QRConnectionInfo?, String?> {
        val info = QRConnectionInfo.fromJson(qrData)
            ?: return Pair(null, "QR kod formatı geçersiz")
        
        if (info.ip.isBlank()) {
            return Pair(null, "IP adresi eksik")
        }
        
        if (info.port <= 0 || info.port > 65535) {
            return Pair(null, "Port numarası geçersiz")
        }
        
        if (info.token.isBlank()) {
            return Pair(null, "Token eksik")
        }
        
        return Pair(info, null)
    }
}

