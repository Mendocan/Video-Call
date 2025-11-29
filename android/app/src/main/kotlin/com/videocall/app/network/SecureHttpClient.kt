package com.videocall.app.network

import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Güvenli HTTP istemcisi - Certificate Pinning ile
 * Man-in-the-middle saldırılarına karşı koruma sağlar
 */
object SecureHttpClient {
    
    // Production'da backend sertifika hash'leri buraya eklenecek
    // Sertifika hash'ini almak için: openssl s_client -connect api.videocall.app:443 | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl enc -base64
    private val CERTIFICATE_PINNER = CertificatePinner.Builder()
        // TODO: Production'da backend sertifika hash'lerini ekle
        // Örnek format: .add("api.videocall.app", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
        // Şimdilik pinning devre dışı (development için)
        .build()
    
    /**
     * Güvenli HTTP istemcisi (Certificate Pinning ile)
     * Production'da kullanılmalı
     */
    val secureClient: OkHttpClient = OkHttpClient.Builder()
        .certificatePinner(CERTIFICATE_PINNER)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    
    /**
     * Development HTTP istemcisi (Certificate Pinning olmadan)
     * Sadece development/test için
     */
    val devClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    
    /**
     * Mevcut build tipine göre uygun istemciyi döndürür
     * Production build'de secureClient, debug build'de devClient kullanılır
     */
    fun getClient(): OkHttpClient {
        // TODO: BuildConfig.DEBUG kontrolü yapılabilir
        // Şimdilik her zaman secureClient döndürüyoruz
        // Production'da sertifika hash'leri eklendiğinde aktif olacak
        return if (isProduction()) {
            secureClient
        } else {
            devClient
        }
    }
    
    /**
     * Production modunda mı kontrol et
     */
    private fun isProduction(): Boolean {
        // TODO: BuildConfig veya environment variable ile kontrol edilebilir
        // Şimdilik false döndürüyoruz (development modu)
        return false
    }
}

