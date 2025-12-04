# 🔐 Backend Kullanıcı Kayıt Sistemi ve Durum Yönetimi

## 📋 Genel Bakış

Backend'de kullanıcı kayıt sistemi, telefon numaraları üzerinden çalışan bir **in-memory** (bellek içi) sistemdir. Her kullanıcı telefon numarası ile kaydedilir ve durum bilgileri saklanır.

## 🗂️ Veri Yapıları

### 1. `userRegistry` (Kullanıcı Kayıt Sistemi)

```javascript
// phoneNumber -> { ws, phoneNumber, name, clientIP, connectedAt, lastSeen }
const userRegistry = new Map();
```

**Özellikler:**
- **Key**: Normalize edilmiş telefon numarası (örn: `05551234567`)
- **Value**: Kullanıcı bilgileri objesi
  - `ws`: WebSocket bağlantısı
  - `phoneNumber`: Normalize edilmiş telefon numarası
  - `name`: Kullanıcı adı (opsiyonel)
  - `clientIP`: İstemci IP adresi
  - `connectedAt`: Bağlantı zamanı
  - `lastSeen`: Son görülme zamanı

**Kullanım:**
- Kullanıcılar `register` mesajı ile kaydedilir
- Arama yapılırken `userRegistry` içinde aranır
- Kullanıcı çıkış yaptığında veya bağlantı kapandığında silinir

### 2. `connections` (Bağlantı Yönetimi)

```javascript
// WebSocket -> { roomCode, phoneNumber, participantId, clientIP, isRegistered, ... }
const connections = new Map();
```

**Özellikler:**
- **Key**: WebSocket bağlantısı
- **Value**: Bağlantı bilgileri
  - `roomCode`: Oda kodu (görüşme için)
  - `phoneNumber`: Telefon numarası
  - `participantId`: Katılımcı ID
  - `clientIP`: İstemci IP adresi
  - `isRegistered`: Kayıt durumu

**Kullanım:**
- Her WebSocket bağlantısı için bir kayıt oluşturulur
- Bağlantı kapandığında temizlenir

### 3. `wsToPhoneNumber` (Ters Mapping)

```javascript
// WebSocket -> phoneNumber
const wsToPhoneNumber = new Map();
```

**Kullanım:**
- WebSocket bağlantısından telefon numarasına hızlı erişim için

## 🔄 Kullanıcı Kayıt Süreci

### 1. Register Mesajı

**Android'den gelen mesaj:**
```json
{
  "type": "register",
  "phoneNumber": "05551234567",
  "name": "Ahmet Yılmaz"
}
```

**Backend işlemi:**
1. Telefon numarası normalize edilir (`normalizePhoneNumber`)
2. Eğer aynı telefon numarası zaten kayıtlıysa, eski bağlantı kapatılır
3. Kullanıcı `userRegistry`'ye eklenir
4. `connections` güncellenir
5. Başarı mesajı gönderilir: `logged-in`

### 2. Normalize İşlemi

```javascript
function normalizePhoneNumber(phoneNumber) {
  // Sadece rakamları al
  let cleaned = phoneNumber.replace(/\D/g, '');
  
  // +90 ile başlıyorsa, +90'ı kaldır ve 0 ekle
  if (cleaned.startsWith('90') && cleaned.length === 12) {
    return '0' + cleaned.substring(2);
  }
  
  // 0 ile başlıyorsa olduğu gibi döndür
  if (cleaned.startsWith('0') && cleaned.length === 11) {
    return cleaned;
  }
  
  // 10 haneli numara ise (0 olmadan), başına 0 ekle
  if (cleaned.length === 10) {
    return '0' + cleaned;
  }
  
  return cleaned;
}
```

**Desteklenen formatlar:**
- `+905551234567` → `05551234567`
- `905551234567` → `05551234567`
- `05551234567` → `05551234567`
- `5551234567` → `05551234567`

## 📞 Arama İşlemi (Call Request)

### 1. Call Request Mesajı

**Android'den gelen mesaj:**
```json
{
  "type": "call-request",
  "targetPhoneNumber": "05559876543",
  "callerPhoneNumber": "05551234567",
  "callerName": "Ahmet Yılmaz"
}
```

### 2. Kullanıcı Bulma Süreci

**Adım 1: Normalize Et**
```javascript
const normalizedTargetPhoneNumber = normalizePhoneNumber(targetPhoneNumber);
```

**Adım 2: Direkt Arama**
```javascript
let targetUser = userRegistry.get(normalizedTargetPhoneNumber);
```

**Adım 3: Alternatif Formatları Dene**
Eğer bulunamazsa, `findUserByPhoneNumber()` fonksiyonu:
- Tüm olası formatları oluşturur
- Her format için `userRegistry`'de arar
- Son 10 hane eşleştirmesi yapar

**Adım 4: Kullanıcı Bulundu mu?**
- ✅ **Bulundu**: `incoming-call` mesajı gönderilir
- ❌ **Bulunamadı**: `call-error` mesajı gönderilir

## 🟢 Kullanıcı Durum Yönetimi

### Durum Tipleri

1. **ONLINE**: Kullanıcı online ve bağlı
   - `ws.readyState === 1` (WebSocket.OPEN)
   - `userRegistry`'de kayıtlı

2. **OFFLINE**: Kullanıcı offline
   - `ws.readyState !== 1` (WebSocket.CLOSED, CLOSING, CONNECTING)
   - Veya `userRegistry`'de yok

### Durum Kontrolü

**Arama sırasında:**
```javascript
if (targetUser.ws.readyState !== 1) {
  // Kullanıcı offline
  ws.send(JSON.stringify({
    type: 'call-error',
    reason: 'User is offline',
    targetPhoneNumber: normalizedTargetPhoneNumber
  }));
  return;
}
```

**Test sayfasında:**
- "Kayıtsız": `userRegistry`'de yok
- "İsimsiz": `userRegistry`'de var ama `name` yok
- "Durum bekliyor": WebSocket bağlantısı kuruluyor

## 🔍 Kullanıcı Lookup (Keşif)

### User Lookup Mesajı

**Android'den gelen mesaj:**
```json
{
  "type": "user-lookup",
  "phoneNumber": "05559876543"
}
```

**Backend yanıtı:**
```json
{
  "type": "user-lookup",
  "phoneNumber": "05559876543",
  "isRegistered": true,
  "name": "Mehmet Demir",
  "isOnline": true,
  "lastSeen": "2024-01-15T10:30:00.000Z"
}
```

## 🧹 Temizleme İşlemleri

### 1. Bağlantı Kapandığında

```javascript
function cleanupConnection(ws) {
  const phoneNumber = wsToPhoneNumber.get(ws);
  if (phoneNumber) {
    userRegistry.delete(phoneNumber);
    wsToPhoneNumber.delete(ws);
  }
  connections.delete(ws);
}
```

### 2. Logout Mesajı

**Android'den gelen mesaj:**
```json
{
  "type": "logout"
}
```

**Backend işlemi:**
- Kullanıcı `userRegistry`'den silinir
- WebSocket bağlantısı kapatılır
- Tüm kayıtlar temizlenir

## 📊 Test Sayfası (`/test`)

Test sayfası, backend'deki tüm bağlantıları ve kullanıcıları gösterir:

**Gösterilen bilgiler:**
- Toplam bağlantı sayısı
- Kayıtlı kullanıcı sayısı
- Her kullanıcı için:
  - IP adresi
  - Telefon numarası
  - İsim
  - Kayıt durumu
  - Bağlantı zamanı

**Durum gösterimi:**
- "Kayıtsız": `userRegistry`'de yok
- "İsimsiz": `userRegistry`'de var ama `name` yok
- "Durum bekliyor": WebSocket bağlantısı kuruluyor

## 🔐 Güvenlik Özellikleri

### 1. Duplicate Bağlantı Önleme

Aynı telefon numarasından birden fazla bağlantı varsa, eski bağlantı kapatılır:
```javascript
const existingUser = userRegistry.get(normalizedPhoneNumber);
if (existingUser && existingUser.ws !== ws && existingUser.ws.readyState === 1) {
  existingUser.ws.close(1000, 'New registration from same phone number');
  cleanupConnection(existingUser.ws);
}
```

### 2. IP Adresi Takibi

Her kullanıcı için IP adresi saklanır:
```javascript
const userInfo = {
  ws,
  phoneNumber: normalizedPhoneNumber,
  name: name || null,
  clientIP: connectionInfo.clientIP,
  connectedAt: new Date(),
  lastSeen: new Date()
};
```

## 🎯 Önemli Notlar

1. **In-Memory Storage**: Tüm veriler bellekte tutulur, sunucu yeniden başlatıldığında sıfırlanır
2. **Telefon Numarası Normalizasyonu**: Tüm telefon numaraları `0` ile başlayan formata normalize edilir
3. **Fuzzy Matching**: Arama yapılırken tüm olası formatlar denenir
4. **Son 10 Hane Eşleştirmesi**: Eğer tam eşleşme bulunamazsa, son 10 hane karşılaştırılır
5. **WebSocket Durumu**: Kullanıcının online/offline durumu WebSocket bağlantısının durumuna göre belirlenir

## 📝 Örnek Senaryo

1. **Kullanıcı A** (`05551234567`) login olur
   - `register` mesajı gönderilir
   - Backend'de `userRegistry.set("05551234567", {...})` yapılır
   - Durum: "Kayıtlı" ve "Online"

2. **Kullanıcı B** (`05559876543`) login olur
   - Aynı işlem tekrarlanır
   - Durum: "Kayıtlı" ve "Online"

3. **Kullanıcı A**, **Kullanıcı B**'yi arar
   - `call-request` mesajı gönderilir
   - Backend `userRegistry.get("05559876543")` ile kullanıcıyı bulur
   - `incoming-call` mesajı **Kullanıcı B**'ye gönderilir
   - `ringing` mesajı **Kullanıcı A**'ya gönderilir

4. **Kullanıcı B** aramayı kabul eder
   - `call-accept` mesajı gönderilir
   - Görüşme başlar

5. **Kullanıcı A** logout yapar
   - `logout` mesajı gönderilir
   - Backend'de `userRegistry.delete("05551234567")` yapılır
   - Durum: "Kayıtsız"

