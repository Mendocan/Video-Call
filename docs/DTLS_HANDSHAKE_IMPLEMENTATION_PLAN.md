# DirectCall DTLS Handshake Implementasyon Planı

**Tarih:** 03 Aralık 2025  
**Durum:** Planlama Aşaması  
**Öncelik:** 🔴 YÜKSEK (Güvenlik Kritik)

---

## 📋 ÖZET

DirectCall'da şu anda placeholder DTLS handshake implementasyonu var. Gerçek DTLS handshake implementasyonu güvenlik için kritik.

**⚠️ ÖNEMLİ:** Domain gerekmez! Bu cihazlar arası güvenli bağlantı için, domain ile ilgisi yok.

---

## 🎯 HEDEF

Placeholder DTLS handshake yerine gerçek DTLS handshake implementasyonu:
- ✅ OpenSSL veya Android'in kriptografi kütüphaneleri kullanılacak
- ✅ Gerçek certificate exchange
- ✅ Güvenli key derivation (HKDF)
- ✅ SRTP encryption/decryption

---

## 🔧 ANDROID DTLS SEÇENEKLERİ

### 1. Android Native DTLS (Önerilen) ⭐

**Avantajlar:**
- ✅ Android'in kendi kriptografi kütüphaneleri
- ✅ Conscrypt (BoringSSL wrapper) kullanılabilir
- ✅ Daha az bağımlılık
- ✅ Google tarafından destekleniyor

**Kütüphaneler:**
- `org.conscrypt:conscrypt-android` - BoringSSL wrapper
- Android'in kendi `javax.net.ssl` paketi

### 2. OpenSSL Android (Alternatif)

**Avantajlar:**
- ✅ Tam kontrol
- ✅ WebRTC ile uyumlu
- ⚠️ Daha karmaşık kurulum

**Kütüphaneler:**
- `org.webrtc:google-webrtc` - WebRTC kütüphanesi (DTLS içerir)

### 3. JNI ile OpenSSL (Gelişmiş)

**Avantajlar:**
- ✅ En yüksek performans
- ✅ Tam kontrol
- ⚠️ Çok karmaşık, C/C++ kodu gerekir

---

## 📦 GEREKLİ BAĞIMLILIKLAR

### Seçenek 1: Conscrypt (Önerilen)

```gradle
dependencies {
    implementation 'org.conscrypt:conscrypt-android:2.5.2'
}
```

### Seçenek 2: WebRTC (DTLS içerir)

```gradle
dependencies {
    implementation 'org.webrtc:google-webrtc:1.0.32006'
}
```

---

## 🔐 DTLS HANDSHAKE ADIMLARI

### 1. ClientHello
- Client random oluştur
- Cipher suites listesi gönder
- Compression methods gönder
- Extensions gönder

### 2. ServerHello
- Server random oluştur
- Cipher suite seç
- Compression method seç
- Extensions gönder

### 3. Certificate
- Server certificate gönder
- Certificate chain gönder

### 4. ServerHelloDone
- Server handshake tamamlandı

### 5. ClientKeyExchange
- Pre-master secret oluştur
- Server public key ile şifrele
- Gönder

### 6. ChangeCipherSpec
- Şifreleme başlatılacak sinyali

### 7. Finished
- Handshake mesajlarının hash'i
- Şifreli gönderilir

### 8. Server: ChangeCipherSpec + Finished
- Server da şifrelemeyi başlatır
- Finished mesajı gönderir

---

## 🗄️ SRTP KEY DERIVATION

### HKDF (HMAC-based Key Derivation Function)

```kotlin
// Master secret'tan SRTP key'leri türet
val srtpEncryptionKey = hkdf(masterSecret, "SRTPEncryptionKey", 16)
val srtpEncryptionSalt = hkdf(masterSecret, "SRTPEncryptionSalt", 14)
val srtpAuthenticationKey = hkdf(masterSecret, "SRTPAuthenticationKey", 20)
```

---

## 📝 ÖRNEK IMPLEMENTASYON YAPISI

### 1. DTLS Client (DirectCallDtlsHandler.kt)

```kotlin
class DirectCallDtlsHandler {
    private var sslContext: SSLContext? = null
    private var sslSocket: SSLSocket? = null
    private var masterSecret: ByteArray? = null
    
    suspend fun performHandshake(
        remoteAddress: InetAddress,
        remotePort: Int
    ): DirectCallDtlsSession {
        // SSL Context oluştur
        sslContext = SSLContext.getInstance("TLS")
        sslContext?.init(null, null, null)
        
        // SSL Socket oluştur
        val socket = Socket(remoteAddress, remotePort)
        sslSocket = sslContext?.socketFactory?.createSocket(
            socket,
            remoteAddress.hostAddress,
            remotePort,
            true
        ) as? SSLSocket
        
        // DTLS için özelleştir
        sslSocket?.useClientMode = true
        sslSocket?.enabledProtocols = arrayOf("TLSv1.2", "TLSv1.3")
        
        // Handshake yap
        sslSocket?.startHandshake()
        
        // Master secret al
        masterSecret = extractMasterSecret(sslSocket)
        
        // SRTP key'leri türet
        val srtpKeys = deriveSrtpKeys(masterSecret!!)
        
        return DirectCallDtlsSession(
            masterSecret = masterSecret!!,
            clientRandom = extractClientRandom(sslSocket),
            serverRandom = extractServerRandom(sslSocket),
            srtpKeys = srtpKeys
        )
    }
    
    private fun extractMasterSecret(socket: SSLSocket): ByteArray {
        // Conscrypt veya reflection ile master secret al
        // NOT: Bu platform-specific olabilir
    }
    
    private fun deriveSrtpKeys(masterSecret: ByteArray): SrtpKeys {
        // HKDF kullanarak SRTP key'leri türet
        val hkdf = Hkdf.fromHmacSha256()
        val info = "EXTRACTOR-dtls_srtp".toByteArray()
        
        val keyMaterial = hkdf.expand(
            masterSecret,
            info,
            60 // 16 + 14 + 20 + 10 (client + server keys)
        )
        
        return SrtpKeys(
            clientEncryptionKey = keyMaterial.sliceArray(0..15),
            clientEncryptionSalt = keyMaterial.sliceArray(16..29),
            clientAuthenticationKey = keyMaterial.sliceArray(30..49),
            serverEncryptionKey = keyMaterial.sliceArray(50..65),
            serverEncryptionSalt = keyMaterial.sliceArray(66..79),
            serverAuthenticationKey = keyMaterial.sliceArray(80..99)
        )
    }
}
```

### 2. HKDF Implementasyonu

```kotlin
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object Hkdf {
    fun fromHmacSha256(): HkdfImpl {
        return HkdfImpl("HmacSHA256")
    }
    
    class HkdfImpl(private val algorithm: String) {
        fun expand(ikm: ByteArray, info: ByteArray, length: Int): ByteArray {
            val mac = Mac.getInstance(algorithm)
            val prk = extract(ikm, ByteArray(32)) // Salt olarak 32 byte sıfır
            mac.init(SecretKeySpec(prk, algorithm))
            
            val result = ByteArray(length)
            var offset = 0
            var counter = 1
            
            while (offset < length) {
                mac.update(info)
                mac.update(counter.toByte())
                val hash = mac.doFinal()
                
                val copyLength = minOf(hash.size, length - offset)
                System.arraycopy(hash, 0, result, offset, copyLength)
                offset += copyLength
                counter++
                
                mac.reset()
                mac.init(SecretKeySpec(prk, algorithm))
            }
            
            return result
        }
        
        private fun extract(ikm: ByteArray, salt: ByteArray): ByteArray {
            val mac = Mac.getInstance(algorithm)
            mac.init(SecretKeySpec(salt, algorithm))
            return mac.doFinal(ikm)
        }
    }
}
```

---

## ⚠️ ZORLUKLAR VE ÇÖZÜMLER

### 1. Master Secret Erişimi

**Sorun:** Android'de master secret'a doğrudan erişim yok.

**Çözümler:**
- Conscrypt kullan (reflection ile erişim)
- WebRTC kütüphanesi kullan (DTLS içerir)
- JNI ile OpenSSL kullan

### 2. UDP vs TCP

**Sorun:** DTLS genellikle UDP üzerinde çalışır, ama Android SSLSocket TCP kullanır.

**Çözüm:**
- UDP socket üzerinde manuel DTLS implementasyonu
- Veya TCP üzerinde TLS kullan (daha kolay)

### 3. Certificate Management

**Sorun:** Self-signed certificate veya CA certificate yönetimi.

**Çözüm:**
- Self-signed certificate oluştur
- Veya Let's Encrypt kullan (domain gerekir - ama DTLS için gerekmez)

---

## 🚀 BAŞLANGIÇ ADIMLARI

### Faz 1: Conscrypt Entegrasyonu
1. `build.gradle`'a Conscrypt ekle
2. SSL Context oluştur
3. Basic handshake test et

### Faz 2: Master Secret Extraction
1. Conscrypt ile master secret al
2. Test et

### Faz 3: HKDF Implementasyonu
1. HKDF kütüphanesi ekle veya implement et
2. SRTP key derivation test et

### Faz 4: SRTP Entegrasyonu
1. SRTP encryption/decryption ekle
2. End-to-end test et

---

## 📚 KAYNAKLAR

- [Conscrypt Documentation](https://github.com/google/conscrypt)
- [RFC 5764 - DTLS for SRTP](https://tools.ietf.org/html/rfc5764)
- [RFC 8446 - TLS 1.3](https://tools.ietf.org/html/rfc8446)
- [WebRTC DTLS Implementation](https://webrtc.org/)

---

## ⚠️ ÖNEMLİ NOTLAR

1. **Domain Gerekmez:** DTLS handshake cihazlar arası, domain gerekmez.
2. **Certificate:** Self-signed certificate kullanılabilir (P2P için yeterli).
3. **Performance:** DTLS handshake ilk bağlantıda bir kez yapılır.
4. **Security:** Master secret güvenli saklanmalı, memory'den temizlenmeli.

---

**Son Güncelleme:** 03 Aralık 2025

