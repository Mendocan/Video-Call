# 📞 Ringback Tone (Erişim Sesi) - Teknik Rehber

## 🎯 Ringback Tone Nedir?

**Ringback tone**, arayan kişinin duyduğu sestir. Karşı telefona ulaşıldığını ve aranan kişinin telefonunun çaldığını gösterir.

### **Operatör Aramalarında:**
- "Tu tu tu..." sesi (Türkiye'de)
- "Ring ring..." sesi (ABD'de)
- Operatör sunucusu tarafından sağlanır

### **WhatsApp/Telegram'da:**
- "Ring ring..." sesi
- Uygulama tarafından sağlanır

## 🔧 Teknik Detaylar

### 1. **SIP (Session Initiation Protocol)**
SIP protokolünde ringback tone, **180 Ringing** mesajı ile başlar:

```
INVITE → 180 Ringing (ringback tone başlar)
       → 200 OK (arama kabul edildi, ringback tone durur)
```

### 2. **WebRTC/DirectCall**
WebRTC'de ringback tone, **early media** veya **local playback** ile sağlanır:

- **Early Media:** Backend'den gelen ses akışı
- **Local Playback:** Uygulama tarafında çalınan ses

## 📱 Bizim Uygulamada Eksik Olan

### **Şu Anki Durum:**
- ❌ Arama yapıldığında ringback tone yok
- ❌ Karşı telefona ulaşıldığının sesi yok
- ❌ Sadece "Arama yapılıyor..." mesajı var

### **Olması Gereken:**
- ✅ Arama yapıldığında ringback tone çalmalı
- ✅ Karşı telefon açıldığında ringback tone durmalı
- ✅ "Tu tu tu..." veya "Ring ring..." sesi

## 🛠️ Implementasyon Planı

### **1. Backend (signalingServer.js)**

**Değişiklikler:**
- `handleCallRequest` fonksiyonunda, hedef kullanıcıya `incoming-call` mesajı gönderildikten sonra
- Arayan kullanıcıya `ringing` mesajı gönderilmeli
- Arayan kullanıcı, `ringing` mesajını aldığında ringback tone başlatmalı

**Kod:**
```javascript
// handleCallRequest içinde
// Hedef kullanıcıya incoming-call gönderildikten sonra
ws.send(JSON.stringify({
    type: 'ringing',
    targetPhoneNumber: normalizedTargetPhoneNumber,
    timestamp: new Date().toISOString()
}));
```

### **2. Android (SignalingMessage.kt)**

**Yeni Mesaj Tipi:**
```kotlin
data class Ringing(
    val targetPhoneNumber: String,
    val timestamp: String,
    override val type: String = "ringing"
) : SignalingMessage()
```

### **3. Android (VideoCallViewModel.kt)**

**Ringback Tone Fonksiyonları:**
```kotlin
private var ringbackTone: Ringtone? = null

private fun startRingbackTone() {
    try {
        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ringbackTone = RingtoneManager.getRingtone(context, ringtoneUri)
        ringbackTone?.isLooping = true
        ringbackTone?.play()
        android.util.Log.d("VideoCallViewModel", "Ringback tone başlatıldı")
    } catch (e: Exception) {
        android.util.Log.e("VideoCallViewModel", "Ringback tone çalınamadı", e)
    }
}

private fun stopRingbackTone() {
    try {
        ringbackTone?.stop()
        ringbackTone = null
        android.util.Log.d("VideoCallViewModel", "Ringback tone durduruldu")
    } catch (e: Exception) {
        android.util.Log.e("VideoCallViewModel", "Ringback tone durdurulamadı", e)
    }
}
```

**Mesaj İşleme:**
```kotlin
is SignalingMessage.Ringing -> {
    startRingbackTone()
    _uiState.update { it.copy(statusMessage = "Aranıyor...") }
}

is SignalingMessage.CallAccepted -> {
    stopRingbackTone()
    // Arama kabul edildi, görüşme başlat
}

is SignalingMessage.CallRejected -> {
    stopRingbackTone()
    // Arama reddedildi
}
```

### **4. Backend (signalingServer.js) - handleCallAccept**

**Değişiklikler:**
- `handleCallAccept` fonksiyonunda, arama kabul edildiğinde
- Arayan kullanıcıya `call-accepted` mesajı gönderilmeli
- Ringback tone durmalı

**Kod:**
```javascript
// handleCallAccept içinde
// Arayan kullanıcıya call-accepted gönder
callerUser.ws.send(JSON.stringify({
    type: 'call-accepted',
    targetPhoneNumber: normalizedTargetPhoneNumber,
    timestamp: new Date().toISOString()
}));
```

## 🎵 Ringback Tone Ses Dosyası

### **Seçenekler:**

1. **Sistem Ringtone Kullan:**
   - `RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTone)`
   - Kullanıcının telefonundaki ringtone

2. **Özel Ses Dosyası:**
   - `res/raw/ringback_tone.mp3`
   - Uygulamaya özel ringback tone

3. **Operatör Sesi (Türkiye):**
   - "Tu tu tu..." sesi
   - `res/raw/ringback_turkey.mp3`

## 📋 Test Senaryosu

### **1. Arama Yapıldığında:**
- ✅ Ringback tone başlamalı
- ✅ "Aranıyor..." mesajı görünmeli
- ✅ Ringback tone sürekli çalmalı

### **2. Karşı Telefon Açıldığında:**
- ✅ Ringback tone durmalı
- ✅ Görüşme başlamalı
- ✅ Video/ses akışı başlamalı

### **3. Karşı Telefon Reddedildiğinde:**
- ✅ Ringback tone durmalı
- ✅ "Arama reddedildi" mesajı görünmeli

### **4. Karşı Telefon Açılmadığında (Timeout):**
- ✅ Ringback tone durmalı
- ✅ "Arama yanıtlanmadı" mesajı görünmeli

## 🔍 Backend Log Kontrolü

**Beklenen Log'lar:**
```
[Signaling] call-request alındı: caller=..., target=...
[Signaling] Hedef kullanıcı bulundu: ...
[Signaling] incoming-call mesajı gönderildi: target=...
[Signaling] ringing mesajı gönderildi: caller=...
[Signaling] call-accepted alındı: target=...
[Signaling] call-accepted mesajı gönderildi: caller=...
```

## ✅ Sonuç

Ringback tone implementasyonu ile:
- ✅ Kullanıcı, aramanın ilerlediğini duyacak
- ✅ Operatör aramalarına benzer deneyim
- ✅ WhatsApp/Telegram'a benzer deneyim
- ✅ Daha profesyonel görünecek

