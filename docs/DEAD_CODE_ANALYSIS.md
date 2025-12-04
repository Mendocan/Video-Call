# Gereksiz, Atıl ve Ölü Kod Analizi

**Tarih:** 03 Aralık 2025  
**Kapsam:** Android uygulaması ve backend kod tabanı

---

## 📋 ÖZET

Bu doküman, kod tabanında tespit edilen gereksiz, atıl ve ölü kodları detaylı olarak listeler.

---

## 🔴 KULLANILMAYAN DOSYALAR

### 1. **Boş `rtc/` Dizini**

**Dosya:** `android/app/src/main/kotlin/com/videocall/app/rtc/`

**Durum:** ⚠️ **Boş dizin**  
**Etki:** Gereksiz dizin yapısı  
**Öneri:** Dizini silin.

---

### 2. **Eski Build Hata Dosyaları**

**Dosyalar:**
- `android/build_error.txt`
- `android/build_error2.txt`
- `android/build_error3.txt`
- `android/build_errors.txt`
- `android/build_warnings.txt`

**Durum:** ⚠️ **Geçici debug dosyaları**  
**Etki:** Projeyi kirletiyor, git'te yer kaplıyor  
**Öneri:** Bu dosyaları silin veya `.gitignore`'a ekleyin.

---

### 3. **Firebase Kurulum Rehberi (Artık Gereksiz)**

**Dosya:** `FCM_KURULUM_REHBERI.md`

**Durum:** ⚠️ **Firebase kaldırıldı, doküman gereksiz**  
**Etki:** Yanıltıcı dokümantasyon  
**Öneri:** Dosyayı silin.

---

## 🟡 KULLANILMAYAN SINIFLAR/FONKSİYONLAR

### 1. **LocalSignalingServer ve PeerSignalingClient**

**Dosyalar:**
- `android/app/src/main/kotlin/com/videocall/app/signaling/LocalSignalingServer.kt`
- `android/app/src/main/kotlin/com/videocall/app/signaling/PeerSignalingClient.kt`

**Durum:** ⚠️ **Kullanılıyor ama gereksiz olabilir**  
**Kullanım:** `VideoCallViewModel.kt` içinde `ensureLocalSignalingServer()` ve `PeerSignalingClient` kullanılıyor  
**Etki:** P2P bağlantı için tasarlanmış ama şu anda backend signaling server kullanılıyor  
**Öneri:** Bu dosyalar gerçekten kullanılıyor mu kontrol edin. Eğer kullanılmıyorsa kaldırın.

---

### 2. **BluetoothAudioManager**

**Dosya:** `android/app/src/main/kotlin/com/videocall/app/audio/BluetoothAudioManager.kt`

**Durum:** ⚠️ **Kullanılıyor ama deprecated API'ler kullanıyor**  
**Kullanım:** `VideoCallViewModel.kt` içinde `bluetoothAudioManager` kullanılıyor  
**Etki:** Deprecated API'ler kullanılıyor (`BluetoothAdapter.getDefaultAdapter()`, `isSpeakerphoneOn`)  
**Öneri:** Deprecated API'leri güncelleyin veya kullanılmıyorsa kaldırın.

---

### 3. **VoiceCommandManager**

**Dosya:** `android/app/src/main/kotlin/com/videocall/app/voice/VoiceCommandManager.kt`

**Durum:** ⚠️ **Kullanılıyor ama fonksiyonelliği belirsiz**  
**Kullanım:** `VideoCallViewModel.kt` içinde `voiceCommandManager` kullanılıyor  
**Etki:** Ses komutları özelliği aktif mi kontrol edin  
**Öneri:** Kullanılmıyorsa kaldırın.

---

### 4. **VideoCallWidgetProvider**

**Dosya:** `android/app/src/main/kotlin/com/videocall/app/widget/VideoCallWidgetProvider.kt`

**Durum:** ⚠️ **Widget özelliği kullanılıyor mu?**  
**Kullanım:** `VideoCallViewModel.kt` içinde `VideoCallWidgetProvider.updateAllWidgets()` çağrılıyor  
**Etki:** Android widget özelliği aktif mi kontrol edin  
**Öneri:** Widget kullanılmıyorsa kaldırın.

---

### 5. **InMemoryPresenceRepository**

**Dosya:** `android/app/src/main/kotlin/com/videocall/app/data/InMemoryPresenceRepository.kt`

**Durum:** ⚠️ **Mock implementation, gerçek backend yok**  
**Kullanım:** `VideoCallViewModel.kt` içinde kullanılıyor  
**Etki:** In-memory mock, uygulama kapanınca veri kaybolur  
**Öneri:** Gerçek backend servisi ile değiştirin veya kullanılmıyorsa kaldırın.

---

## 🟢 KULLANILMAYAN BACKEND SERVİSLERİ

### 1. **emailService, paymentService, smsService**

**Dosyalar:**
- `backend/src/services/emailService.js`
- `backend/src/services/paymentService.js`
- `backend/src/services/smsService.js`
- `backend/src/jobs/notificationJobs.js`

**Durum:** ⚠️ **Kullanılıyor ama aktif mi?**  
**Kullanım:** `backend/src/index.js` içinde kullanılıyor  
**Etki:** Bu servisler production'da aktif mi kontrol edin  
**Öneri:** 
- Eğer kullanılmıyorsa kaldırın
- Eğer kullanılıyorsa environment variable kontrolü ekleyin

---

## 🔵 TODO VE PLACEHOLDER KODLAR

### 1. **WebRTC TODO'ları (DirectCall Kullanılıyor)**

**Dosya:** `android/app/src/main/kotlin/com/videocall/app/viewmodel/VideoCallViewModel.kt`

**TODO'lar:**
- `// TODO: WebRTC'de yeni peer connection oluştur ve bağlantı kur`
- `// TODO: WebRTC'de peer connection'ı kapat`
- `// TODO: WebRTC'de audio track'i mute/unmute et`
- `// TODO: WebRTC'de video track'i enable/disable et`

**Durum:** ⚠️ **WebRTC kullanılmıyor, DirectCall kullanılıyor**  
**Etki:** Yanıltıcı TODO'lar  
**Öneri:** Bu TODO'ları kaldırın veya "DirectCall için" olarak güncelleyin.

---

### 2. **DirectCall Placeholder Kodlar**

**Dosyalar:**
- `android/app/src/main/kotlin/com/videocall/app/directcall/security/DirectCallDtlsHandler.kt` - DTLS placeholder
- `android/app/src/main/kotlin/com/videocall/app/directcall/rtp/DirectCallRtpSender.kt` - Audio capture placeholder
- `android/app/src/main/kotlin/com/videocall/app/directcall/rtp/DirectCallRtpReceiver.kt` - Audio playback placeholder

**Durum:** ⚠️ **Placeholder implementasyonlar**  
**Etki:** Güvenlik ve fonksiyonellik eksiklikleri  
**Öneri:** Gerçek implementasyonları ekleyin veya kullanılmıyorsa kaldırın.

---

### 3. **Production TODO'ları**

**Dosya:** `android/app/src/main/kotlin/com/videocall/app/ui/VideoCallApp.kt`

**TODO'lar:**
- `// TODO: Production'da backend'e güvenlik raporu gönder`
- `// TODO: Production'da güvenilir olmayan cihazlarda uygulamayı kısıtla`
- `// TODO: Production'da abonelik kontrolünü aktif et`

**Durum:** ⚠️ **Production için kritik**  
**Etki:** Production'da güvenlik ve abonelik kontrolleri eksik  
**Öncelik:** 🔴 **YÜKSEK**

---

### 4. **SubscriptionManager localhost URL**

**Dosya:** `android/app/src/main/kotlin/com/videocall/app/data/SubscriptionManager.kt`

**Kod:**
```kotlin
private val apiBaseUrl = "http://localhost:3000" // TODO: Production URL'e değiştirilecek
```

**Durum:** ⚠️ **Production'da çalışmayacak**  
**Etki:** Production'da subscription API çağrıları başarısız olacak  
**Öncelik:** 🔴 **YÜKSEK**

---

## 🟣 DEPRECATED KODLAR

### 1. **CallUiState.participants (Deprecated)**

**Dosya:** `android/app/src/main/kotlin/com/videocall/app/model/CallUiState.kt`

**Kod:**
```kotlin
val participants: Int = 1, // Deprecated: Use participantsList.size instead
```

**Durum:** ⚠️ **Deprecated field**  
**Etki:** Kullanılmıyorsa kaldırılmalı  
**Öneri:** Kullanılmıyorsa kaldırın, kullanılıyorsa `participantsList.size` ile değiştirin.

---

### 2. **CallRecorder.startRecording (Deprecated)**

**Dosya:** `android/app/src/main/kotlin/com/videocall/app/utils/CallRecorder.kt`

**Kod:**
```kotlin
@Deprecated("Video kaydı için startVideoRecording() kullanın")
```

**Durum:** ⚠️ **Deprecated method**  
**Etki:** Kullanılmıyorsa kaldırılmalı  
**Öneri:** Kullanılmıyorsa kaldırın.

---

### 3. **MainActivity.onPictureInPictureModeChanged (Deprecated)**

**Dosya:** `android/app/src/main/kotlin/com/videocall/app/MainActivity.kt`

**Durum:** ⚠️ **Deprecated API**  
**Etki:** Android API güncellemesi gerekiyor  
**Öneri:** `onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)` ile değiştirin.

---

## 📊 ÖNCELİK MATRİSİ

### 🔴 YÜKSEK ÖNCELİK (Hemen Temizlenmeli)

1. ✅ **Build hata dosyaları** - Projeyi kirletiyor
2. ✅ **FCM_KURULUM_REHBERI.md** - Yanıltıcı dokümantasyon
3. ✅ **SubscriptionManager localhost URL** - Production'da çalışmaz
4. ✅ **Production TODO'ları** - Güvenlik ve abonelik kontrolleri eksik

### 🟡 ORTA ÖNCELİK (Kontrol Edilmeli)

1. ✅ **LocalSignalingServer/PeerSignalingClient** - Kullanılıyor mu?
2. ✅ **InMemoryPresenceRepository** - Mock implementation
3. ✅ **BluetoothAudioManager deprecated API'ler** - Güncellenmeli
4. ✅ **WebRTC TODO'ları** - DirectCall kullanılıyor, TODO'lar güncel değil
5. ✅ **Backend servisleri (email/payment/sms)** - Aktif mi kontrol edin

### 🟢 DÜŞÜK ÖNCELİK (Nice-to-have)

1. ✅ **Boş `rtc/` dizini** - Temizlik
2. ✅ **Deprecated field/method'lar** - Kullanılmıyorsa kaldırın
3. ✅ **VoiceCommandManager** - Kullanılıyor mu?
4. ✅ **VideoCallWidgetProvider** - Widget aktif mi?

---

## 🛠️ ÖNERİLER

### 1. **Hemen Temizlenmeli**

```bash
# Build hata dosyalarını sil
rm android/build_error*.txt
rm android/build_warnings.txt

# Gereksiz dokümantasyonu sil
rm FCM_KURULUM_REHBERI.md

# Boş dizini sil
rmdir android/app/src/main/kotlin/com/videocall/app/rtc
```

### 2. **Kod Temizliği**

- [ ] `SubscriptionManager` localhost URL'ini environment variable ile değiştir
- [ ] WebRTC TODO'larını kaldır veya DirectCall için güncelle
- [ ] Production TODO'larını implemente et
- [ ] Deprecated field/method'ları kaldır veya güncelle
- [ ] `LocalSignalingServer` ve `PeerSignalingClient` kullanılıyor mu kontrol et

### 3. **Backend Kontrolü**

- [ ] `emailService`, `paymentService`, `smsService` aktif mi kontrol et
- [ ] Eğer kullanılmıyorsa kaldır veya environment variable kontrolü ekle

### 4. **Mock Data Temizliği**

- [ ] `InMemoryPresenceRepository` için gerçek backend servisi implementasyonu
- [ ] Veya kullanılmıyorsa kaldır

---

## 📝 NOTLAR

- **DirectCall:** Uygulama şu anda DirectCall kullanıyor, WebRTC kullanılmıyor. WebRTC ile ilgili TODO'lar güncel değil.
- **Production Hazırlığı:** Production'a geçmeden önce tüm 🔴 yüksek öncelikli maddelerin tamamlanması gerekiyor.
- **Backend Servisleri:** Email, payment ve SMS servisleri kullanılıyor ama aktif mi kontrol edilmeli.

---

---

## ✅ TEMİZLENEN KODLAR (03 Aralık 2025)

### Tamamlanan Temizlikler

1. ✅ **Build hata dosyaları silindi**
   - `android/build_error.txt`
   - `android/build_error2.txt`
   - `android/build_error3.txt`
   - `android/build_errors.txt`
   - `android/build_warnings.txt`

2. ✅ **FCM_KURULUM_REHBERI.md silindi** - Firebase kaldırıldı

3. ✅ **SubscriptionManager localhost URL düzeltildi**
   - `BuildConfig.BACKEND_API_URL` kullanılıyor
   - `local.properties`'ten `BACKEND_API_URL` alınıyor

4. ✅ **WebRTC TODO'ları temizlendi**
   - WebRTC TODO'ları kaldırıldı veya DirectCall için güncellendi
   - Screen sharing TODO'su güncellendi

5. ✅ **Deprecated kodlar temizlendi**
   - `CallUiState.participants` field'ı kaldırıldı
   - `CallRecorder.getRecordingSurface()` kaldırıldı
   - `favoriteContact` TODO'su güncellendi

**Son Güncelleme:** 03 Aralık 2025

