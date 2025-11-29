# 🚀 Render.com - Adım Adım Kurulum Rehberi

## 📸 Ekran Görüntüsü: Git Provider Seçimi

Şu anda **"Connect Git provider"** ekranındasınız.

---

## ✅ Adım 1: GitHub'ı Seçin

1. **GitHub** butonuna tıklayın (beyaz buton, GitHub logosu var)
2. GitHub hesabınıza giriş yapın (eğer giriş yapmadıysanız)
3. Render'a GitHub erişim izni verin

**Ne olacak:**
- GitHub repo'larınız listelenecek
- `Video Call` projenizi seçebileceksiniz

---

## ✅ Adım 2: Repository Seçin

GitHub repo'larınız listelendiğinde:

1. **`Video Call`** projesini bulun
2. **Select** butonuna tıklayın

---

## ✅ Adım 3: Web Service Ayarları

Şimdi **ayarlar ekranı** açılacak. Şunları yapın:

### Name (İsim)
```
videocall-signaling
```
veya istediğiniz bir isim

### Region (Bölge)
**Frankfurt** seçin (Türkiye'ye yakın, düşük latency)

veya

**Oregon** (ABD)

### Branch
```
main
```
(veya hangi branch'teyseniz: `master`, `develop`)

### Root Directory ⚠️ ÖNEMLİ!
```
backend
```
**Bu çok önemli!** `backend` yazın, böylece Render `backend/` klasöründen deploy eder.

### Runtime
```
Node
```
(Node.js seçili olmalı)

### Build Command
```
npm install
```

### Start Command
```
node src/signalingServer.js
```

---

## ✅ Adım 4: Environment Variables

**Environment Variables** bölümüne tıklayın:

**Add Environment Variable** butonuna tıklayın ve şunları ekleyin:

### 1. SIGNALING_PORT
- **Key:** `SIGNALING_PORT`
- **Value:** `8080`

### 2. NODE_ENV
- **Key:** `NODE_ENV`
- **Value:** `production`

### 3. SIGNALING_HOST
- **Key:** `SIGNALING_HOST`
- **Value:** `0.0.0.0`

**Her birini ekledikten sonra "Save" butonuna tıklayın.**

---

## ✅ Adım 5: Plan Seçimi

**Plan** bölümünde:

**Free** seçin (test için yeterli)

**Not:** Free Plan'da 15 dakika kullanılmazsa uykuya geçer. İlk istek yavaş olabilir (30-60 saniye).

**Çözüm:** Daha sonra UptimeRobot ile ping atacağız (ücretsiz).

---

## ✅ Adım 6: Deploy!

**Create Web Service** butonuna tıklayın!

**Ne olacak:**
1. Render GitHub'dan kodları çeker
2. `npm install` çalıştırır (2-3 dakika)
3. `node src/signalingServer.js` başlatır
4. Otomatik domain oluşturur: `videocall-signaling.onrender.com`
5. Otomatik SSL ekler (HTTPS/WSS)

**Süre:** 2-5 dakika

---

## ✅ Adım 7: Deploy Tamamlandı!

Deploy tamamlandığında:

**Dashboard'da göreceksiniz:**
- ✅ **Status:** Live (yeşil)
- ✅ **URL:** `https://videocall-signaling.onrender.com`
- ✅ **WebSocket:** `wss://videocall-signaling.onrender.com/ws`

---

## 🧪 Test

### 1. Health Check

Tarayıcıda açın:
```
https://videocall-signaling.onrender.com/health
```

**Beklenen yanıt:**
```json
{
  "status": "ok",
  "timestamp": "...",
  "activeRooms": 0,
  "activeConnections": 0
}
```

### 2. WebSocket URL

Android uygulamanızda kullanın:
```kotlin
val signalingUrl = "wss://videocall-signaling.onrender.com/ws"
```

---

## ⚠️ Sorun Giderme

### Deploy Başarısız

**Hata:** `npm install` başarısız
- ✅ `Root Directory: backend` ayarlandı mı?
- ✅ `package.json` `backend/` klasöründe var mı?

**Hata:** `Cannot find module 'src/signalingServer.js'`
- ✅ `Start Command: node src/signalingServer.js` doğru mu?
- ✅ `Root Directory: backend` ayarlandı mı?

### WebSocket Bağlanmıyor

**Hata:** Connection timeout
- ✅ İlk istek yavaş olabilir (uyku modu, 30-60 saniye bekle)
- ✅ `wss://` kullanın (https:// değil)
- ✅ `/ws` path'i var mı?

**Hata:** 404 Not Found
- ✅ `Start Command` doğru mu?
- ✅ Logs'u kontrol edin (Dashboard → Logs)

---

## 📊 Logs Kontrol

**Dashboard** → **Logs** → Gerçek zamanlı loglar görüntülenir.

**Başarılı deploy'da göreceksiniz:**
```
🚀 Signaling Server running on 0.0.0.0:8080
📡 WebSocket endpoint: ws://0.0.0.0:8080/ws
💡 Health check: http://0.0.0.0:8080/health
```

---

## 🎯 Sonraki Adımlar

1. ✅ Render'da deploy tamamlandı
2. ⏭️ UptimeRobot kur (uyku modunu önlemek için) - [UptimeRobot Kurulumu](#uptimerobot-kurulumu)
3. ⏭️ Android uygulamasını güncelle (yeni WebSocket URL)
4. ⏭️ Test et (mobil veri + Wi-Fi)

---

## 🔔 UptimeRobot Kurulumu (Ücretsiz)

Uyku modunu önlemek için:

1. https://uptimerobot.com → **Sign Up** (ücretsiz)
2. Email doğrulaması yap
3. **Add New Monitor**
4. **Monitor Type:** `HTTP(s)`
5. **Friendly Name:** `VideoCall Signaling`
6. **URL:** `https://videocall-signaling.onrender.com/health`
7. **Monitoring Interval:** `5 minutes`
8. **Alert Contacts:** Email ekleyin (opsiyonel)
9. **Save**

**Sonuç:** Her 5 dakikada bir ping atılır, uyku modu önlenir.

---

## ✅ Tamamlandı!

**WebSocket URL:** `wss://videocall-signaling.onrender.com/ws`

Bu URL'yi Android uygulamanızda kullanın!

---

## 📝 Özet

1. ✅ GitHub'ı seçin
2. ✅ `Video Call` repo'sunu seçin
3. ✅ **Root Directory:** `backend`
4. ✅ **Build Command:** `npm install`
5. ✅ **Start Command:** `node src/signalingServer.js`
6. ✅ **Environment Variables:** `SIGNALING_PORT=8080`, `NODE_ENV=production`
7. ✅ **Plan:** Free
8. ✅ **Create Web Service**
9. ✅ Test edin!

