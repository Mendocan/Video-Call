# 📞 Arama İşleyiş Süreci - Detaylı Açıklama

**Son Güncelleme:** Room mantığı kaldırıldı, direkt telefon numarası eşleştirmesi kullanılıyor.

## 🎯 Sistem Mimarisi

**Room mantığı kaldırıldı!** Artık sistem **direkt telefon numarası eşleştirmesi** kullanıyor.

```
┌─────────────┐                    ┌──────────────┐                    ┌─────────────┐
│   Ali       │                    │   Backend    │                    │   Ayşe      │
│ (Arayan)    │                    │ (Signaling)  │                    │ (Aranan)    │
└──────┬──────┘                    └──────┬───────┘                    └──────┬──────┘
       │                                   │                                   │
       │ 1. call-request                   │                                   │
       │──────────────────────────────────>│                                   │
       │                                   │                                   │
       │                                   │ 2. incoming-call                 │
       │                                   │──────────────────────────────────>│
       │                                   │                                   │
       │ 3. ringing                        │                                   │
       │<──────────────────────────────────│                                   │
       │                                   │                                   │
       │                                   │ 4. call-accept                    │
       │                                   │<──────────────────────────────────│
       │                                   │                                   │
       │ 5. call-accepted                  │                                   │
       │<──────────────────────────────────│                                   │
       │                                   │                                   │
       │ 6. call-accepted-by                │                                   │
       │<──────────────────────────────────│                                   │
       │                                   │                                   │
       │ 7. offer                          │                                   │
       │──────────────────────────────────>│                                   │
       │                                   │ 7. offer (forward)                │
       │                                   │──────────────────────────────────>│
       │                                   │                                   │
       │                                   │ 8. answer                         │
       │                                   │<──────────────────────────────────│
       │ 8. answer (forward)               │                                   │
       │<──────────────────────────────────│                                   │
       │                                   │                                   │
       │ 9. ice-candidate                  │                                   │
       │──────────────────────────────────>│                                   │
       │                                   │ 9. ice-candidate (forward)        │
       │                                   │──────────────────────────────────>│
       │                                   │                                   │
       │                                   │ 10. ice-candidate                 │
       │                                   │<──────────────────────────────────│
       │ 10. ice-candidate (forward)       │                                   │
       │<──────────────────────────────────│                                   │
       │                                   │                                   │
       │ <========= WebRTC P2P ==========> │                                   │
       │                                   │                                   │
```

---

## 📋 Adım Adım İşleyiş

### 1️⃣ **Arama Başlatma (Ali - Arayan Taraf)**

#### Android (VideoCallViewModel.kt)
```kotlin
startCallWithContact(contact)
  ↓
signalingClient.startCall(
    targetPhoneNumber = "05559876543",
    callerPhoneNumber = "05551234567",
    callerName = "Ali"
)
```

#### Backend (signalingServer.js)
```javascript
handleCallRequest(ws, message)
  ↓
1. Kullanıcı kayıtlı mı kontrol et
2. Hedef kullanıcıyı bul (userRegistry'den)
3. Geçici grup oluştur:
   - groupId = "group_123..."
   - members = ["05551234567", "05559876543"]
   - currentCallGroupId = groupId (Ali için)
4. Ayşe'ye incoming-call mesajı gönder (roomCode YOK)
5. Ali'ye ringing mesajı gönder (ringback tone için)
6. Ali'ye call-request-sent mesajı gönder (roomCode YOK)
```

**ÖNEMLİ:** 
- ✅ Room oluşturulmuyor
- ✅ Room'a ekleme yok
- ✅ Sadece geçici grup oluşturuluyor (eşleştirme için)

---

### 2️⃣ **Gelen Arama (Ayşe - Aranan Taraf)**

#### Android (VideoCallViewModel.kt)
```kotlin
handleIncomingCall(message)
  ↓
1. incomingCall oluştur (roomCode = null)
2. Bildirim göster
3. Zil sesi çal
```

**Backend'den gelen mesaj:**
```json
{
  "type": "incoming-call",
  "groupId": "group_123...",
  "callerPhoneNumber": "05551234567",
  "callerName": "Ali",
  "isGroupCall": false,
  "timestamp": "..."
}
```

**ÖNEMLİ:**
- ✅ roomCode yok (null)
- ✅ Sadece groupId var

---

### 3️⃣ **Arama Kabul (Ayşe)**

#### Android (VideoCallViewModel.kt)
```kotlin
acceptIncomingCall()
  ↓
1. signalingClient.acceptCall(groupId)
2. WebSocket bağlantısını kontrol et
3. Eğer bağlı değilse: signalingClient.connect()
4. Offer oluştur ve gönder
```

#### Backend (signalingServer.js)
```javascript
handleCallAccept(ws, message)
  ↓
1. Grup bilgisini al
2. currentCallGroupId = groupId (Ayşe için)
3. Ayşe'ye call-accepted mesajı gönder (roomCode YOK)
4. Ali'ye call-accepted-by mesajı gönder (roomCode YOK)
```

**ÖNEMLİ:**
- ✅ Room'a ekleme yok
- ✅ Sadece currentCallGroupId set ediliyor

---

### 4️⃣ **WebRTC Bağlantı Kurulumu**

#### Ali Offer Gönderiyor:
```kotlin
// Android (Ali)
directCallClient.createOffer()
  ↓
sendOffer(offer)
  ↓
signalingClient.send(SignalingMessage.Offer(sdp))
```

```javascript
// Backend
handleDirectOffer(ws, message)
  ↓
1. Ali'nin currentCallGroupId: "group_123..."
2. Grup bilgisi: { members: ["05551234567", "05559876543"] }
3. Hedef kullanıcı: "05559876543" (Ali hariç)
4. Ayşe'ye direkt gönder (room broadcast YOK)
```

#### Ayşe Answer Gönderiyor:
```kotlin
// Android (Ayşe)
handleRemoteOffer(sdp)
  ↓
directCallClient.createAnswer(sdp)
  ↓
sendAnswer(answer)
  ↓
signalingClient.send(SignalingMessage.Answer(sdp))
```

```javascript
// Backend
handleDirectAnswer(ws, message)
  ↓
1. Ayşe'nin currentCallGroupId: "group_123..."
2. Grup bilgisi: { members: ["05551234567", "05559876543"] }
3. Hedef kullanıcı: "05551234567" (Ayşe hariç)
4. Ali'ye direkt gönder (room broadcast YOK)
```

#### ICE Candidates:
```kotlin
// Her iki taraf
directCallClient.addIceCandidate()
  ↓
sendIceCandidate(candidate)
  ↓
signalingClient.send(SignalingMessage.IceCandidateMessage(...))
```

```javascript
// Backend
handleDirectIceCandidate(ws, message)
  ↓
1. Gönderen'in currentCallGroupId'den grup bilgisi al
2. Hedef kullanıcıyı bul (grup üyelerinden gönderen hariç)
3. Direkt gönder (room broadcast YOK)
```

---

## ⚠️ Potansiyel Sorunlar ve Kontroller

### 1. **currentCallGroupId Set Edilmiyor**

**Sorun:** Offer/Answer gönderilirken `currentCallGroupId` null olabilir.

**Kontrol:**
- ✅ `handleCallRequest`'te: `connectionInfo.currentCallGroupId = tempGroupId` (Ali için)
- ✅ `handleCallAccept`'te: `connectionInfo.currentCallGroupId = groupId` (Ayşe için)

**Test:**
```javascript
// Backend log'larında kontrol et:
console.log(`[Signaling] ✅ Offer gönderildi: sender=${senderPhone} -> target=${targetPhone}`)
// Eğer "⚠️ Offer alındı ama aktif grup yok" görüyorsan, currentCallGroupId set edilmemiş
```

---

### 2. **WebSocket Bağlantısı Hazır Değil**

**Sorun:** Offer/Answer gönderilirken WebSocket bağlantısı kurulmamış olabilir.

**Kontrol:**
- Android'de `acceptIncomingCall()` içinde WebSocket kontrolü var
- Ama `startCallWithContact()` içinde kontrol var mı?

**Test:**
```kotlin
// Android log'larında kontrol et:
android.util.Log.d("VideoCallViewModel", "WebSocket bağlantısı yok, bağlanılıyor...")
// Eğer bu log görünmüyorsa, WebSocket zaten bağlı
```

---

### 3. **Grup Üyeleri Eşleşmiyor**

**Sorun:** `groupInfo.members.find(m => m !== senderPhone)` hedef kullanıcıyı bulamıyor.

**Kontrol:**
- Grup üyeleri normalize edilmiş telefon numaraları mı?
- `senderPhone` normalize edilmiş mi?

**Test:**
```javascript
// Backend log'larında kontrol et:
console.log(`[Signaling] ⚠️ Offer alındı ama hedef kullanıcı bulunamadı: groupId=${groupId}, sender=${senderPhone}`)
// Eğer bu log görünüyorsa, grup üyeleri eşleşmiyor
```

---

### 4. **Timing Sorunu**

**Sorun:** Offer gönderilirken karşı taraf henüz `currentCallGroupId` set etmemiş olabilir.

**Kontrol:**
- `acceptIncomingCall()` içinde `delay(300)` var
- Ama `startCallWithContact()` içinde timing kontrolü var mı?

**Test:**
- Backend log'larında offer/answer sırasını kontrol et
- Eğer offer geliyor ama answer gelmiyorsa, timing sorunu olabilir

---

### 5. **Mesaj Yönlendirme Hatası**

**Sorun:** `handleDirectOffer/Answer/IceCandidate` fonksiyonları çalışmıyor olabilir.

**Kontrol:**
- Backend'de `handleMessage` içinde `case 'offer'` doğru mu?
- `handleDirectOffer` fonksiyonu çağrılıyor mu?

**Test:**
```javascript
// Backend log'larında kontrol et:
console.log(`[Signaling] ✅ Offer gönderildi: sender=${senderPhone} -> target=${targetPhone}`)
// Eğer bu log görünmüyorsa, handleDirectOffer çalışmıyor
```

---

## 🔍 Debug Checklist

### Backend Kontrolleri:

1. **Arama başlatıldığında:**
   - ✅ `currentCallGroupId` set edildi mi? (Ali için)
   - ✅ `incoming-call` mesajı gönderildi mi? (roomCode yok mu?)
   - ✅ `ringing` mesajı gönderildi mi?

2. **Arama kabul edildiğinde:**
   - ✅ `currentCallGroupId` set edildi mi? (Ayşe için)
   - ✅ `call-accepted` mesajı gönderildi mi? (roomCode yok mu?)
   - ✅ `call-accepted-by` mesajı gönderildi mi? (Ali'ye, roomCode yok mu?)

3. **Offer gönderildiğinde:**
   - ✅ `currentCallGroupId` null değil mi?
   - ✅ Grup bilgisi bulunuyor mu?
   - ✅ Hedef kullanıcı bulunuyor mu?
   - ✅ Mesaj gönderiliyor mu?

4. **Answer gönderildiğinde:**
   - ✅ `currentCallGroupId` null değil mi?
   - ✅ Grup bilgisi bulunuyor mu?
   - ✅ Hedef kullanıcı bulunuyor mu?
   - ✅ Mesaj gönderiliyor mu?

### Android Kontrolleri:

1. **Arama başlatıldığında:**
   - ✅ WebSocket bağlantısı hazır mı?
   - ✅ `call-request` mesajı gönderildi mi?
   - ✅ `ringing` mesajı alındı mı? (ringback tone çalıyor mu?)

2. **Arama kabul edildiğinde:**
   - ✅ WebSocket bağlantısı hazır mı?
   - ✅ `call-accept` mesajı gönderildi mi?
   - ✅ `call-accepted` mesajı alındı mı?
   - ✅ Offer oluşturuldu ve gönderildi mi?

3. **Offer/Answer alındığında:**
   - ✅ Mesaj işleniyor mu?
   - ✅ Remote description set ediliyor mu?
   - ✅ Answer oluşturuluyor ve gönderiliyor mu?

---

## 🐛 Yaygın Hatalar ve Çözümler

### Hata 1: "⚠️ Offer alındı ama aktif grup yok"

**Neden:** `currentCallGroupId` set edilmemiş.

**Kontrol:**
```javascript
// Backend log'larında kontrol et:
console.log(`[Signaling] ✅ Arayan taraf room'a eklendi: caller=${normalizedCallerPhone}, groupId=${tempGroupId}`)
// Eğer bu log görünmüyorsa, currentCallGroupId set edilmemiş
```

**Çözüm:**
- `handleCallRequest`'te: `connectionInfo.currentCallGroupId = tempGroupId` (Ali için) ✅
- `handleCallAccept`'te: `connectionInfo.currentCallGroupId = groupId` (Ayşe için) ✅

---

### Hata 2: "⚠️ Offer alındı ama grup bulunamadı"

**Neden:** Grup silinmiş veya groupId yanlış.

**Kontrol:**
```javascript
// Backend log'larında kontrol et:
console.log(`[Signaling] ⚠️ Offer alındı ama grup bulunamadı: groupId=${groupId}, sender=${senderPhone}`)
```

**Çözüm:**
- Grup oluşturulduktan sonra silinmediğinden emin ol
- `groups.get(groupId)` null dönüyor mu kontrol et
- Grup oluşturulurken `groups.set(tempGroupId, tempGroupInfo)` çalışıyor mu?

---

### Hata 3: "⚠️ Offer alındı ama hedef kullanıcı bulunamadı"

**Neden:** Grup üyeleri eşleşmiyor.

**Kontrol:**
```javascript
// Backend log'larında kontrol et:
console.log(`[Signaling] ⚠️ Offer alındı ama hedef kullanıcı bulunamadı: groupId=${groupId}, sender=${senderPhone}`)
// Grup üyeleri: ${groupInfo.members.join(', ')}
```

**Çözüm:**
- Grup üyeleri normalize edilmiş telefon numaraları mı? ✅ (normalize edilmiş)
- `senderPhone` normalize edilmiş mi? ✅ (`connectionInfo.phoneNumber` zaten normalize)
- `groupInfo.members.find(m => m !== senderPhone)` çalışıyor mu? ✅

**Test:**
```javascript
// Backend'de debug log ekle:
console.log(`[Signaling] Grup üyeleri: ${groupInfo.members.join(', ')}, sender=${senderPhone}`)
```

---

### Hata 4: "⚠️ Offer gönderilemedi: hedef kullanıcı offline"

**Neden:** Hedef kullanıcı WebSocket bağlantısı kapalı.

**Kontrol:**
```javascript
// Backend log'larında kontrol et:
console.log(`[Signaling] ⚠️ Offer gönderilemedi: hedef kullanıcı offline: target=${targetPhone}, sender=${senderPhone}`)
```

**Çözüm:**
- Hedef kullanıcı kayıtlı mı? (`userRegistry.get(targetPhone)`) ✅
- WebSocket durumu OPEN mi? (`ws.readyState === 1`) ✅

**Test:**
```javascript
// Backend'de debug log ekle:
const targetUser = userRegistry.get(targetPhone);
console.log(`[Signaling] Hedef kullanıcı: phone=${targetPhone}, exists=${!!targetUser}, readyState=${targetUser?.ws?.readyState}`)
```

---

### Hata 5: Android'de "WebSocket bağlantısı kurulamadı"

**Neden:** WebSocket bağlantısı zamanında kurulmuyor.

**Kontrol:**
```kotlin
// Android log'larında kontrol et:
android.util.Log.d("VideoCallViewModel", "WebSocket bağlantısı yok, bağlanılıyor...")
// Eğer bu log görünmüyorsa, WebSocket zaten bağlı
```

**Çözüm:**
- `signalingClient.connect()` çağrılıyor mu? ✅ (startCallWithContact'ta eklendi)
- Bağlantı timeout'u yeterli mi? ✅ (50 * 100ms = 5 saniye)
- Backend WebSocket endpoint'i doğru mu? ✅

**Test:**
```kotlin
// Android'de debug log ekle:
android.util.Log.d("VideoCallViewModel", "WebSocket durumu: ${signalingClient.status.value}")
```

---

### Hata 6: "Arama başlatıldı ama karşı tarafa ulaşmıyor"

**Neden:** Hedef kullanıcı backend'de kayıtlı değil veya offline.

**Kontrol:**
```javascript
// Backend log'larında kontrol et:
console.log(`[Signaling] ❌ call-request reddedildi: ${normalizedTargetPhoneNumber} kayıtlı değil veya offline`)
console.log(`[Signaling] 📊 Kayıtlı kullanıcılar (${userRegistry.size} adet):`, Array.from(userRegistry.keys()))
```

**Çözüm:**
- Her iki telefon da backend'e kayıtlı olmalı ✅
- WebSocket bağlantıları açık olmalı ✅
- Telefon numaraları normalize edilmiş olmalı ✅

---

### Hata 7: "Offer gönderildi ama answer gelmiyor"

**Neden:** Karşı taraf offer'ı işlemiyor veya answer göndermiyor.

**Kontrol:**
```kotlin
// Android log'larında kontrol et:
android.util.Log.d("VideoCallViewModel", "✅ Offer gönderildi, answer bekleniyor...")
// Eğer bu log görünüyorsa ama answer gelmiyorsa, karşı taraf işlemiyor
```

**Çözüm:**
- Karşı tarafta `handleRemoteOffer()` çağrılıyor mu? ✅
- Answer oluşturuluyor ve gönderiliyor mu? ✅
- Backend'de `handleDirectAnswer()` çalışıyor mu? ✅

**Test:**
```kotlin
// Android'de debug log ekle:
android.util.Log.d("VideoCallViewModel", "📥 Answer mesajı alındı, remote description set ediliyor...")
```

---

## 📊 Test Senaryosu - Adım Adım

### Senaryo: Ali (05551234567) → Ayşe (05559876543) Arama Yapıyor

#### ✅ ÖN HAZIRLIK

1. **Her iki telefon da backend'e kayıtlı olmalı:**
   ```
   Ali: register("05551234567", "Ali")
   Ayşe: register("05559876543", "Ayşe")
   ```

2. **Her iki telefon da WebSocket bağlantısı kurmalı:**
   ```
   Ali: signalingClient.connect()
   Ayşe: signalingClient.connect()
   ```

3. **Backend'de kontrol:**
   ```
   [Signaling] ✅ Kullanıcı kayıt edildi: phoneNumber=05551234567
   [Signaling] ✅ Kullanıcı kayıt edildi: phoneNumber=05559876543
   ```

---

#### 📞 ADIM 1: Arama Başlatma (Ali)

**Android (Ali):**
```kotlin
startCallWithContact(ayseContact)
  ↓
1. WebSocket bağlantısı kontrol edilir
2. Eğer bağlı değilse: signalingClient.connect()
3. signalingClient.startCall(
     targetPhoneNumber = "05559876543",
     callerPhoneNumber = "05551234567",
     callerName = "Ali"
   )
```

**Backend log'larında görmeli:**
```
[Signaling] call-request alındı: {"targetPhoneNumber":"05559876543",...}
[Signaling] call-request işleniyor: caller=05551234567, target=05559876543
[Signaling] Hedef kullanıcı bulundu: 05559876543
[Signaling] Bireysel arama başlatıldı: caller=05551234567, target=05559876543, groupId=group_123...
[Signaling] incoming-call mesajı gönderildi: 05559876543
[Signaling] ringing mesajı gönderildi: caller=05551234567
```

**Android (Ali) log'larında görmeli:**
```
VideoCallViewModel: Arama başlatılıyor: target=05559876543, caller=05551234567
VideoCallViewModel: WebSocket bağlantısı yok, bağlanılıyor... (eğer bağlı değilse)
VideoCallViewModel: ✅ Arama mesajı gönderildi: target=05559876543
VideoCallViewModel: Arama isteği gönderildi: groupId=group_123...
```

**Ayşe'de görmeli:**
- ✅ Bildirim gelmeli
- ✅ Zil sesi çalmalı

---

#### 📞 ADIM 2: Arama Kabul (Ayşe)

**Android (Ayşe):**
```kotlin
acceptIncomingCall()
  ↓
1. signalingClient.acceptCall(groupId)
2. WebSocket bağlantısı kontrol edilir
3. Eğer bağlı değilse: signalingClient.connect()
4. delay(300)
5. createOffer() → sendOffer()
```

**Backend log'larında görmeli:**
```
[Signaling] call-accept alındı: groupId=group_123...
[Signaling] Arama kabul edildi: phoneNumber=05559876543, groupId=group_123...
[Signaling] call-accepted-by gönderiliyor: memberPhone=05551234567, phoneNumber=05559876543
[Signaling] ✅ WebRTC bağlantısı hazır: arayan=05551234567, aranan=05559876543
```

**Android (Ali) log'larında görmeli:**
```
VideoCallViewModel: CallAccepted: WebSocket bağlantısı yok, bağlanılıyor... (eğer bağlı değilse)
VideoCallViewModel: CallAccepted: Offer gönderiliyor...
VideoCallViewModel: CallAccepted: Offer gönderildi
```

**Android (Ayşe) log'larında görmeli:**
```
VideoCallViewModel: WebSocket bağlantısı yok, bağlanılıyor... (eğer bağlı değilse)
VideoCallViewModel: Gelen arama için offer gönderiliyor...
VideoCallViewModel: Offer gönderildi, answer bekleniyor...
```

**Ali'de görmeli:**
- ✅ Ringback tone durmalı
- ✅ "Arama kabul edildi" mesajı

---

#### 📞 ADIM 3: Offer/Answer Exchange

**Ali Offer Gönderiyor:**
```
Ali: createOffer() → sendOffer()
  ↓
Backend: handleDirectOffer()
  ↓
[Signaling] ✅ Offer gönderildi: sender=05551234567 -> target=05559876543
  ↓
Ayşe: handleSignalingMessage(Offer)
  ↓
handleRemoteOffer() → createAnswer() → sendAnswer()
```

**Ayşe Answer Gönderiyor:**
```
Ayşe: sendAnswer()
  ↓
Backend: handleDirectAnswer()
  ↓
[Signaling] ✅ Answer gönderildi: sender=05559876543 -> target=05551234567
  ↓
Ali: handleSignalingMessage(Answer)
  ↓
setRemoteDescription() → isConnected = true
```

**Backend log'larında görmeli:**
```
[Signaling] ✅ Offer gönderildi: sender=05551234567 -> target=05559876543
[Signaling] ✅ Answer gönderildi: sender=05559876543 -> target=05551234567
[Signaling] ✅ ICE candidate gönderildi: sender=05551234567 -> target=05559876543
[Signaling] ✅ ICE candidate gönderildi: sender=05559876543 -> target=05551234567
```

**Android log'larında görmeli:**
```
VideoCallViewModel: ✅ Answer oluşturuldu, gönderiliyor...
VideoCallViewModel: ✅ Answer gönderildi, görüşme başlatılıyor...
VideoCallViewModel: 📥 Answer mesajı alındı, remote description set ediliyor...
VideoCallViewModel: ✅ Answer işlendi, görüşme başlatılıyor...
```

**Her iki tarafta görmeli:**
- ✅ `isConnected = true`
- ✅ Ses/görüntü akışı başlamalı

---

## 🎯 Sonuç

**Room mantığı tamamen kaldırıldı!**

- ✅ Backend: Direkt telefon numarası eşleştirmesi
- ✅ Android: Room code kullanımı yok
- ✅ Mesajlar: Direkt hedef kullanıcıya gönderiliyor
- ✅ Basit ve direkt sistem

**Test ederken dikkat edilmesi gerekenler:**
1. Her iki telefon da backend'e kayıtlı olmalı
2. WebSocket bağlantıları hazır olmalı
3. `currentCallGroupId` doğru set edilmeli
4. Grup üyeleri normalize edilmiş telefon numaraları olmalı

**Sorun devam ederse:**
- Backend log'larını kontrol et
- Android log'larını kontrol et
- WebSocket bağlantı durumlarını kontrol et
- `currentCallGroupId` değerlerini kontrol et
