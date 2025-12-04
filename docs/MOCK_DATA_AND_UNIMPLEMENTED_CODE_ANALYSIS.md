# Mock Data ve Implemente Edilmemiş Kod Analizi

**Tarih:** 03 Aralık 2025  
**Kapsam:** Android uygulaması ve backend kod tabanı

---

## 📋 ÖZET

Bu doküman, kod tabanında kullanılan mock data ve implemente edilmemiş kod yapılarını detaylı olarak listeler.

---

## 🔴 MOCK DATA KULLANIMI

### 1. **PresenceRepository (In-Memory Mock)**

**Dosya:** `android/app/src/main/kotlin/com/videocall/app/data/PresenceRepository.kt`

```kotlin
/**
 * Cihazların birbirini bulması için kullanılan kısa ömürlü rehber.
 * Şimdilik in-memory mock, ileride gerçek backend ile değiştirilecek.
 */
interface PresenceRepository {
    suspend fun publish(entry: PresenceEntry)
    suspend fun fetch(phoneHash: String): PresenceEntry?
    suspend fun clear(phoneHash: String)
}
```

**Durum:** ⚠️ **In-memory mock kullanılıyor**  
**Etki:** Cihazlar arası presence discovery şu anda sadece bellek içinde çalışıyor. Uygulama kapanınca veri kaybolur.  
**Öneri:** Gerçek backend servisi ile değiştirilmeli.

---

### 2. **SignalingMessage - Dummy Parametreler**

**Dosya:** `android/app/src/main/kotlin/com/videocall/app/signaling/SignalingMessage.kt`

```kotlin
data class GetGroups(val dummy: String = "") : SignalingMessage {
    override val type: String = "get-groups"
}

data class GetBlockedUsers(val dummy: String = "") : SignalingMessage {
    override val type: String = "get-blocked-users"
}
```

**Durum:** ⚠️ **Dummy parametre kullanılıyor**  
**Etki:** Bu mesaj tipleri için gereksiz bir `dummy` parametresi var.  
**Öneri:** Parametre kaldırılmalı veya gerçek parametreler eklenmeli.

---

### 3. **SubscriptionManager - Localhost URL**

**Dosya:** `android/app/src/main/kotlin/com/videocall/app/data/SubscriptionManager.kt`

```kotlin
private val apiBaseUrl = "http://localhost:3000" // TODO: Production URL'e değiştirilecek
```

**Durum:** ⚠️ **Localhost URL kullanılıyor**  
**Etki:** Production'da çalışmayacak.  
**Öneri:** Environment-based configuration ile production URL'e geçilmeli.

---

### 4. **OTPScreen - Test OTP**

**Dosya:** `android/app/src/main/kotlin/com/videocall/app/ui/OTPScreen.kt`

```kotlin
testOTP: String? = null, // Development için test OTP
```

**Durum:** ⚠️ **Development-only özellik**  
**Etki:** Production build'de bu parametre kullanılmamalı.  
**Öneri:** Build variant'a göre kontrol edilmeli veya kaldırılmalı.

---

## 🟡 IMPLEMENTE EDİLMEMİŞ KODLAR

### 1. **DirectCall DTLS Handshake (Placeholder)**

**Dosya:** `android/app/src/main/kotlin/com/videocall/app/directcall/security/DirectCallDtlsHandler.kt`

```kotlin
// TODO: Gerçek DTLS handshake implementasyonu
// OpenSSL kullanarak:
// 1. ClientHello gönder
// 2. ServerHello al
// 3. Certificate al
// 4. ServerHelloDone al
// 5. ClientKeyExchange gönder
// 6. ChangeCipherSpec + Finished gönder
// 7. Finished al

// Placeholder: Basit key generation
val random = SecureRandom()
masterSecret = ByteArray(48).apply { random.nextBytes(this) }
```

**Durum:** ⚠️ **Placeholder implementasyon**  
**Etki:** Güvenlik açığı riski. Gerçek DTLS handshake yapılmıyor.  
**Öncelik:** 🔴 **YÜKSEK** (Güvenlik kritik)

---

### 2. **DirectCall Audio Capture (Commented Out)**

**Dosya:** `android/app/src/main/kotlin/com/videocall/app/directcall/rtp/DirectCallRtpSender.kt`

```kotlin
// TODO: Audio capture implementasyonu
// Şimdilik placeholder
while (isSending.get()) {
    if (isAudioEnabled.get()) {
        try {
            // Audio sample al (20ms frame)
            // val audioSamples = audioCapturer.getNextFrame()
            // if (audioSamples != null) {
            //     // Encode et
            //     val encodedAudio = audioCodec.encode(audioSamples)
            //     ...
            // }
            delay(20) // 20ms frame interval
```

**Durum:** ⚠️ **Kod yorum satırında**  
**Etki:** DirectCall'da ses gönderimi çalışmıyor.  
**Öncelik:** 🟡 **ORTA**

---

### 3. **Video Processor (DirectCall için yok)**

**Dosya:** `android/app/src/main/kotlin/com/videocall/app/viewmodel/VideoCallViewModel.kt`

```kotlin
// TODO: DirectCall için video processor implementasyonu
android.util.Log.w("VideoCallViewModel", "Video processor DirectCall'da henüz desteklenmiyor")
```

**Durum:** ⚠️ **Desteklenmiyor**  
**Etki:** Video filtreleri ve efektler DirectCall'da çalışmıyor.  
**Öncelik:** 🟢 **DÜŞÜK** (Nice-to-have)

---

### 4. **Screen Sharing (DirectCall için yok)**

**Dosya:** `android/app/src/main/kotlin/com/videocall/app/viewmodel/VideoCallViewModel.kt`

```kotlin
fun toggleScreenSharing(resultCode: Int? = null, data: Intent? = null) {
    // DirectCall'da screen sharing şimdilik yok
    _uiState.update { it.copy(isScreenSharing = false) }
    android.util.Log.w("VideoCallViewModel", "Screen sharing DirectCall'da henüz desteklenmiyor")
    // TODO: Screen sharing implementasyonu
}
```

**Durum:** ⚠️ **Desteklenmiyor**  
**Etki:** Ekran paylaşımı özelliği çalışmıyor.  
**Öncelik:** 🟡 **ORTA**

---

### 5. **Video Recording (TODO)**

**Dosya:** `android/app/src/main/kotlin/com/videocall/app/viewmodel/VideoCallViewModel.kt`

```kotlin
// TODO: Video ve audio kayıtlarını birleştir (MediaMuxer ile)
// TODO: Kullanıcıya kayıt tamamlandı bildirimi göster
```

**Durum:** ⚠️ **Kısmen implemente edilmiş**  
**Etki:** Video kayıt tamamlanmamış.  
**Öncelik:** 🟡 **ORTA**

---

### 6. **WebRTC Audio/Video Track Operations (TODO)**

**Dosya:** `android/app/src/main/kotlin/com/videocall/app/viewmodel/VideoCallViewModel.kt`

```kotlin
// TODO: WebRTC'de audio track'i mute/unmute et
// TODO: WebRTC'de video track'i enable/disable et
```

**Durum:** ⚠️ **DirectCall kullanılıyor, WebRTC TODO'ları eski**  
**Etki:** WebRTC implementasyonu kullanılmıyor, bu TODO'lar güncel değil.  
**Öncelik:** 🟢 **DÜŞÜK** (WebRTC kullanılmıyor)

---

### 7. **Favorite Contact Calculation (TODO)**

**Dosya:** `android/app/src/main/kotlin/com/videocall/app/viewmodel/VideoCallViewModel.kt`

```kotlin
favoriteContact = null // TODO: Calculate favorite contact
```

**Durum:** ⚠️ **Implemente edilmemiş**  
**Etki:** Favori kişi özelliği çalışmıyor.  
**Öncelik:** 🟢 **DÜŞÜK**

---

### 8. **Production Güvenlik Kontrolleri (TODO)**

**Dosya:** `android/app/src/main/kotlin/com/videocall/app/ui/VideoCallApp.kt`

```kotlin
// TODO: Production'da backend'e güvenlik raporu gönder
// TODO: Production'da güvenilir olmayan cihazlarda uygulamayı kısıtla
// TODO: Production'da abonelik kontrolünü aktif et
```

**Durum:** ⚠️ **Test için devre dışı**  
**Etki:** Production güvenlik kontrolleri eksik.  
**Öncelik:** 🔴 **YÜKSEK** (Production için kritik)

---

### 9. **Abonelik Kontrolleri (Test için devre dışı)**

**Dosya:** `android/app/src/main/kotlin/com/videocall/app/ui/VideoCallApp.kt`

```kotlin
// Abonelik kontrolü - Test için geçici olarak devre dışı
```

**Durum:** ⚠️ **Test için devre dışı**  
**Etki:** Production'da abonelik kontrolü yapılmıyor.  
**Öncelik:** 🔴 **YÜKSEK** (Production için kritik)

---

### 10. **Video Processing (Placeholder)**

**Dosya:** `android/app/src/main/kotlin/com/videocall/app/viewmodel/VideoCallViewModel.kt`

```kotlin
// TODO: Gerçek video işleme implementasyonu eklenecek
```

**Durum:** ⚠️ **Placeholder**  
**Etki:** Video işleme özellikleri eksik.  
**Öncelik:** 🟡 **ORTA**

---

### 11. **WebRTC Peer Connection (TODO - Kullanılmıyor)**

**Dosya:** `android/app/src/main/kotlin/com/videocall/app/viewmodel/VideoCallViewModel.kt`

```kotlin
// TODO: WebRTC'de yeni peer connection oluştur ve bağlantı kur
// TODO: WebRTC'de peer connection'ı kapat
```

**Durum:** ⚠️ **WebRTC kullanılmıyor, DirectCall kullanılıyor**  
**Etki:** Bu TODO'lar güncel değil.  
**Öncelik:** 🟢 **DÜŞÜK** (WebRTC kullanılmıyor)

---

## 📊 ÖNCELİK MATRİSİ

### 🔴 YÜKSEK ÖNCELİK (Güvenlik ve Production Kritik)

1. ✅ **DirectCall DTLS Handshake** - Güvenlik açığı
2. ✅ **Production Güvenlik Kontrolleri** - Production için kritik
3. ✅ **Abonelik Kontrolleri** - Production için kritik
4. ✅ **SubscriptionManager localhost URL** - Production'da çalışmaz

### 🟡 ORTA ÖNCELİK (Fonksiyonel Özellikler)

1. ✅ **DirectCall Audio Capture** - Ses gönderimi çalışmıyor
2. ✅ **Screen Sharing** - Özellik eksik
3. ✅ **Video Recording** - Kısmen implemente edilmiş
4. ✅ **Video Processing** - Placeholder

### 🟢 DÜŞÜK ÖNCELİK (Nice-to-have)

1. ✅ **Video Processor (DirectCall)** - Filtreler/efektler
2. ✅ **Favorite Contact Calculation** - Özellik eksik
3. ✅ **WebRTC TODO'ları** - WebRTC kullanılmıyor
4. ✅ **GetGroups/GetBlockedUsers dummy parametreler** - Temizlik

---

## 🛠️ ÖNERİLER

### 1. **Mock Data Temizliği**

- [ ] `PresenceRepository` için gerçek backend servisi implementasyonu
- [ ] `GetGroups` ve `GetBlockedUsers` için dummy parametrelerin kaldırılması
- [ ] `SubscriptionManager` için environment-based URL configuration
- [ ] `testOTP` için build variant kontrolü veya kaldırma

### 2. **Kritik Implementasyonlar**

- [ ] **DirectCall DTLS Handshake** - OpenSSL entegrasyonu
- [ ] **DirectCall Audio Capture** - Audio capturer implementasyonu
- [ ] **Production Güvenlik** - Tüm güvenlik kontrollerinin aktif edilmesi
- [ ] **Abonelik Kontrolleri** - Production'da aktif edilmesi

### 3. **Kod Temizliği**

- [ ] Kullanılmayan WebRTC TODO'larının kaldırılması
- [ ] Commented-out kodların temizlenmesi veya implementasyonunun tamamlanması
- [ ] Placeholder implementasyonların gerçek implementasyonlarla değiştirilmesi

---

## 📝 NOTLAR

- **DirectCall:** Uygulama şu anda DirectCall kullanıyor, WebRTC kullanılmıyor. WebRTC ile ilgili TODO'lar güncel değil.
- **Production Hazırlığı:** Production'a geçmeden önce tüm 🔴 yüksek öncelikli maddelerin tamamlanması gerekiyor.
- **Test vs Production:** Test için devre dışı bırakılan özelliklerin production build'de aktif edilmesi gerekiyor.

---

## 🔵 BACKEND - MOCK DATA VE TODO'LAR

### 1. **Kullanıcı Bildirim Gönderimi (TODO)**

**Dosya:** `backend/src/index.js`

```javascript
// TODO: Kullanıcı bilgilerini al ve bildirim gönder
```

**Durum:** ⚠️ **Implemente edilmemiş**  
**Etki:** Bazı durumlarda kullanıcıya bildirim gönderilmiyor.  
**Öncelik:** 🟡 **ORTA**

---

### 2. **Kullanıcı Eşleştirme (TODO)**

**Dosya:** `backend/src/index.js`

```javascript
// TODO: Production'da users Map'inde telefon numarası ile eşleştirme yapılmalı
```

**Durum:** ⚠️ **In-memory Map kullanılıyor**  
**Etki:** Production'da veritabanı kullanılmalı.  
**Öncelik:** 🔴 **YÜKSEK** (Production için kritik)

---

### 3. **Kayıt Tarihi (TODO)**

**Dosya:** `backend/src/index.js`

```javascript
registeredAt: new Date().toISOString() // TODO: Production'da gerçek kayıt tarihi
```

**Durum:** ⚠️ **Şu anki tarih kullanılıyor**  
**Etki:** Gerçek kayıt tarihi kaydedilmiyor.  
**Öncelik:** 🟡 **ORTA**

---

### 4. **Veritabanı Kaydı (TODO)**

**Dosya:** `backend/src/index.js`

```javascript
// TODO: Production'da veritabanına kaydet
```

**Durum:** ⚠️ **In-memory storage**  
**Etki:** Veriler kalıcı değil, sunucu restart'ta kaybolur.  
**Öncelik:** 🔴 **YÜKSEK** (Production için kritik)

---

### 5. **Admin Rol Kontrolü (TODO)**

**Dosya:** `backend/src/index.js`

```javascript
// TODO: Production'da admin rol kontrolü ekle
```

**Durum:** ⚠️ **Implemente edilmemiş**  
**Etki:** Admin işlemleri için güvenlik eksik.  
**Öncelik:** 🔴 **YÜKSEK** (Güvenlik kritik)

---

### 6. **Anomaly Detection Alerting (TODO)**

**Dosya:** `backend/src/middleware/anomalyDetection.js`

```javascript
// TODO: Production'da alerting sistemi (email, SMS, Slack, vb.)
```

**Durum:** ⚠️ **Implemente edilmemiş**  
**Etki:** Anomali tespit edildiğinde bildirim gönderilmiyor.  
**Öncelik:** 🟡 **ORTA**

---

## 📊 BACKEND ÖNCELİK MATRİSİ

### 🔴 YÜKSEK ÖNCELİK (Production Kritik)

1. ✅ **Kullanıcı Eşleştirme (Veritabanı)** - In-memory yerine DB
2. ✅ **Veritabanı Kaydı** - Kalıcı veri depolama
3. ✅ **Admin Rol Kontrolü** - Güvenlik

### 🟡 ORTA ÖNCELİK

1. ✅ **Kullanıcı Bildirim Gönderimi** - Eksik bildirimler
2. ✅ **Kayıt Tarihi** - Gerçek tarih kaydı
3. ✅ **Anomaly Detection Alerting** - Bildirim sistemi

---

## 🎯 GENEL ÖNERİLER

### Android Tarafı

1. **Kritik Güvenlik:**
   - DirectCall DTLS Handshake implementasyonu
   - Production güvenlik kontrollerinin aktif edilmesi
   - Abonelik kontrollerinin production'da aktif edilmesi

2. **Fonksiyonel Özellikler:**
   - DirectCall Audio Capture implementasyonu
   - Screen Sharing implementasyonu
   - Video Recording tamamlama

3. **Mock Data Temizliği:**
   - PresenceRepository için gerçek backend
   - SubscriptionManager localhost URL düzeltmesi
   - Dummy parametrelerin kaldırılması

### Backend Tarafı

1. **Veritabanı Entegrasyonu:**
   - In-memory Map'ler yerine veritabanı kullanımı
   - Kalıcı veri depolama
   - Kullanıcı kayıt tarihi doğru kaydı

2. **Güvenlik:**
   - Admin rol kontrolü
   - Production güvenlik kontrolleri

3. **Bildirim Sistemi:**
   - Kullanıcı bildirimleri
   - Anomaly detection alerting

---

**Son Güncelleme:** 03 Aralık 2025

