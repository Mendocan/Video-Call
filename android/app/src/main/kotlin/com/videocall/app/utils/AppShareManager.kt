package com.videocall.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Uygulama paylaşım yönetimi
 * Sosyal medya ve diğer platformlarda uygulama davetiyesi paylaşımı
 */
class AppShareManager(private val context: Context) {
    
    companion object {
        private const val APP_URL = "https://videocall.app"
        private const val APP_NAME = "Video Call"
        private const val APP_DESCRIPTION = "Özel. Güvenli. Gizli. Sunucusuz P2P video görüşme uygulaması."
    }
    
    /**
     * Uygulamayı paylaş (genel paylaşım)
     */
    fun shareApp(): Intent {
        val shareText = """
            ${APP_NAME} - ${APP_DESCRIPTION}
            
            ${APP_URL}
            
            Özellikler:
            • End-to-end şifreleme
            • Sunucusuz P2P bağlantı
            • Sınırsız görüşme
            • QR kod ile kolay bağlantı
        """.trimIndent()
        
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "$APP_NAME - Özel ve Güvenli Video Görüşme")
            type = "text/plain"
        }
        
        return Intent.createChooser(sendIntent, "Uygulamayı Paylaş")
    }
    
    /**
     * WhatsApp ile paylaş
     */
    fun shareViaWhatsApp(): Intent? {
        return try {
            val shareText = """
                ${APP_NAME} - ${APP_DESCRIPTION}
                
                ${APP_URL}
            """.trimIndent()
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage("com.whatsapp")
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            
            if (intent.resolveActivity(context.packageManager) != null) {
                intent
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Telegram ile paylaş
     */
    fun shareViaTelegram(): Intent? {
        return try {
            val shareText = """
                ${APP_NAME} - ${APP_DESCRIPTION}
                
                ${APP_URL}
            """.trimIndent()
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage("org.telegram.messenger")
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            
            if (intent.resolveActivity(context.packageManager) != null) {
                intent
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * E-posta ile paylaş
     */
    fun shareViaEmail(): Intent {
        val shareText = """
            ${APP_NAME} - ${APP_DESCRIPTION}
            
            ${APP_URL}
            
            Özellikler:
            • End-to-end şifreleme
            • Sunucusuz P2P bağlantı
            • Sınırsız görüşme
            • QR kod ile kolay bağlantı
        """.trimIndent()
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_SUBJECT, "$APP_NAME - Özel ve Güvenli Video Görüşme")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        
        return Intent.createChooser(intent, "E-posta ile Paylaş")
    }
    
    /**
     * SMS ile paylaş
     */
    fun shareViaSMS(): Intent {
        val shareText = """
            ${APP_NAME} - ${APP_DESCRIPTION}
            ${APP_URL}
        """.trimIndent()
        
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:")
            putExtra("sms_body", shareText)
        }
        
        return intent
    }
    
    /**
     * QR kod ile paylaş (uygulama linki için QR kod oluştur)
     */
    fun getAppQRCodeData(): String {
        return APP_URL
    }
}

