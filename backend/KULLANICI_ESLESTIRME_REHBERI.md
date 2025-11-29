# Kullanıcı Eşleştirme Sistemi - Teknik Rehber

## Genel Bakış

Bu sistem, internet üzerinden telefon numaralarına göre kullanıcıları otomatik olarak eşleştirir. **Her defasında yeniden eşleştirme gerekmez** - bir kez kayıt olduktan sonra, uygulama açık olduğu sürece eşleştirme aktif kalır.

## Nasıl Çalışır?

### 1. Kullanıcı Kaydı (Register)

**Dosya:** `backend/src/signalingServer.js`

**Fonksiyon:** `handleRegister(ws, message)`

**Ne zaman çalışır:**
- Uygulama açıldığında otomatik olarak
- WebSocket bağlantısı kurulduğunda

**Nasıl çalışır:**
```javascript
// Kullanıcı kayıt sistemi: phoneNumber -> { ws, phoneNumber, name, connectedAt, lastSeen }
const userRegistry = new Map();

// WebSocket bağlantısı: ws -> phoneNumber (ters mapping)
const wsToPhoneNumber = new Map();
```

1. Android uygulaması açıldığında `SignalingClient.register(phoneNumber, name)` çağrılır
2. Backend'e `register` mesajı gönderilir
3. Backend `userRegistry` Map'ine kullanıcıyı ekler:
   - Key: `phoneNumber` (örn: "05351234567")
   - Value: `{ ws, phoneNumber, name, connectedAt, lastSeen }`
4. Backend `wsToPhoneNumber` Map'ine de ekler (ters mapping için)

**Önemli:** 
- Aynı telefon numarası ile yeni kayıt gelirse, eski bağlantı kapatılır
- Kayıt, uygulama açık olduğu sürece aktif kalır
- WebSocket bağlantısı kapandığında otomatik temizlenir

### 2. Arama Eşleştirmesi (Call Request)

**Dosya:** `backend/src/signalingServer.js`

**Fonksiyon:** `handleCallRequest(ws, message)`

**Ne zaman çalışır:**
- Kullanıcı bir kişiyi aradığında
- `SignalingClient.startCall(targetPhoneNumber, ...)` çağrıldığında

**Nasıl çalışır:**

1. **Hedef kullanıcıyı bul:**
```javascript
const targetUser = userRegistry.get(targetPhoneNumber);
```

2. **Kullanıcı kontrolü:**
   - Kullanıcı kayıtlı mı? (`userRegistry.has(targetPhoneNumber)`)
   - Kullanıcı online mı? (`targetUser.ws.readyState === 1`)
   - Kullanıcı engellenmiş mi? (`blockedUsers` kontrolü)

3. **Eşleştirme:**
   - Hedef kullanıcı bulunduysa, `incoming-call` mesajı gönderilir
   - Geçici bir grup oluşturulur (2 kişilik görüşme için)
   - Room code oluşturulur

### 3. Chat Eşleştirmesi

**Dosya:** `backend/src/signalingServer.js`

**Mesaj tipi:** `chat`

**Nasıl çalışır:**
- Chat mesajları da aynı `userRegistry` kullanılır
- Hedef kullanıcı `userRegistry.get(targetPhoneNumber)` ile bulunur
- Mesaj, hedef kullanıcının WebSocket'ine gönderilir

## Veri Yapıları

### userRegistry Map
```javascript
// phoneNumber -> { ws, phoneNumber, name, connectedAt, lastSeen }
userRegistry.set("05351234567", {
  ws: WebSocket,
  phoneNumber: "05351234567",
  name: "Ahmet Yılmaz",
  connectedAt: Date,
  lastSeen: Date
});
```

### wsToPhoneNumber Map
```javascript
// ws -> phoneNumber (ters mapping)
wsToPhoneNumber.set(webSocket, "05351234567");
```

## Önemli Noktalar

### ✅ Bir Kez Kayıt Yeterli
- Uygulama açıldığında otomatik kayıt olur
- Uygulama açık olduğu sürece kayıt aktif kalır
- Her arama için yeniden kayıt gerekmez

### ✅ Otomatik Temizlik
- WebSocket bağlantısı kapandığında `cleanupConnection()` çağrılır
- Kullanıcı `userRegistry`'den otomatik silinir
- Uygulama yeniden açıldığında otomatik yeniden kayıt olur

### ✅ Çoklu Cihaz Desteği
- Aynı telefon numarası ile yeni cihaz bağlanırsa, eski bağlantı kapatılır
- Son bağlanan cihaz aktif olur

### ✅ Online/Offline Durumu
- `targetUser.ws.readyState === 1` kontrolü ile online/offline durumu kontrol edilir
- Offline kullanıcılara mesaj gönderilemez

## Backend Dosyaları

### Ana Dosya
- **`backend/src/signalingServer.js`** - Tüm eşleştirme mantığı burada

### İlgili Fonksiyonlar
1. **`handleRegister(ws, message)`** - Kullanıcı kaydı
2. **`handleCallRequest(ws, message)`** - Arama eşleştirmesi
3. **`handleUserStatus(ws, message)`** - Kullanıcı durumu sorgulama
4. **`cleanupConnection(ws)`** - Bağlantı temizleme

### İlgili Map'ler
1. **`userRegistry`** - Kullanıcı kayıtları (phoneNumber -> userInfo)
2. **`wsToPhoneNumber`** - Ters mapping (ws -> phoneNumber)
3. **`connections`** - WebSocket bağlantı bilgileri
4. **`blockedUsers`** - Engellenenler listesi

## Android Client Tarafı

### Kayıt
- **Dosya:** `android/app/src/main/kotlin/com/videocall/app/signaling/SignalingClient.kt`
- **Fonksiyon:** `register(phoneNumber: String, name: String?)`
- **Ne zaman:** Uygulama açıldığında (`VideoCallViewModel.init`)

### Arama
- **Dosya:** `android/app/src/main/kotlin/com/videocall/app/viewmodel/VideoCallViewModel.kt`
- **Fonksiyon:** `startCallWithContact(contact: Contact)`
- **Nasıl:** `signalingClient.startCall(targetPhoneNumber, ...)`

## Özet

1. **Kayıt:** Uygulama açıldığında otomatik (bir kez)
2. **Eşleştirme:** `userRegistry.get(phoneNumber)` ile anında
3. **Temizlik:** WebSocket kapandığında otomatik
4. **Yeniden Kayıt:** Uygulama yeniden açıldığında otomatik

**Sonuç:** Her defasında yeniden eşleştirme gerekmez. Bir kez kayıt olduktan sonra, uygulama açık olduğu sürece eşleştirme aktif kalır.

