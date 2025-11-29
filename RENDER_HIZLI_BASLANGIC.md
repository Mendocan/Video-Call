# 🚀 Render.com - Hızlı Başlangıç Rehberi

## ⚡ Render.com'da Docker Gerekli mi?

**Hayır!** Docker opsiyonel. Node.js projesini direkt deploy edebilirsiniz.

**İki Seçenek:**
1. **Native Build** (Önerilen) - Node.js direkt, daha kolay
2. **Docker** - Opsiyonel, daha fazla kontrol

---

## 🎯 Seçenek 1: Native Build (Önerilen)

### Adım 1: Yeni Web Service Oluştur

1. Render Dashboard → **New** → **Web Service**
2. **Connect GitHub** → Repo'nuzu seçin (`Video Call`)
3. **Next**

### Adım 2: Ayarları Yapılandır

**Name:** `videocall-signaling` (veya istediğiniz isim)

**Region:** 
- **Frankfurt** (Türkiye'ye yakın, önerilen)
- veya **Oregon** (ABD)

**Branch:** `main` (veya hangi branch'teyseniz)

**Root Directory:** `backend` ⚠️ **ÖNEMLİ!**

**Runtime:** `Node`

**Build Command:**
```bash
npm install
```

**Start Command:**
```bash
node src/signalingServer.js
```

### Adım 3: Environment Variables

**Environment Variables** → **Add Environment Variable**:

```
SIGNALING_PORT=8080
NODE_ENV=production
SIGNALING_HOST=0.0.0.0
```

### Adım 4: Plan Seç

**Free Plan:**
- ✅ 750 saat/ay ücretsiz
- ⚠️ 15 dakika kullanılmazsa uykuya geçer (ilk istek yavaş)
- ✅ Test için yeterli

**Starter Plan:** $7/ay (uyku modu yok)

**Öneri:** Önce Free Plan ile test edin.

### Adım 5: Deploy!

**Create Web Service** → Otomatik deploy başlar (2-3 dakika)

**Deploy tamamlandığında:**
- ✅ Otomatik domain: `videocall-signaling.onrender.com`
- ✅ Otomatik SSL: `https://videocall-signaling.onrender.com`
- ✅ WebSocket: `wss://videocall-signaling.onrender.com/ws`

---

## 🐳 Seçenek 2: Docker ile (Opsiyonel)

Docker kullanmak isterseniz:

### Adım 1: Dockerfile Kontrol

`backend/Dockerfile` zaten var (oluşturduk).

### Adım 2: Render'da Docker Seç

1. **New** → **Web Service**
2. **Connect GitHub** → Repo seç
3. **Root Directory:** `backend`
4. **Runtime:** `Docker` (Node yerine)
5. **Dockerfile Path:** `backend/Dockerfile` (veya sadece `Dockerfile`)

**Build Command:** (Gerekmez, Docker otomatik build eder)

**Start Command:** (Gerekmez, Dockerfile'daki CMD kullanılır)

### Adım 3: Environment Variables

Aynı environment variables:
```
SIGNALING_PORT=8080
NODE_ENV=production
SIGNALING_HOST=0.0.0.0
```

### Adım 4: Deploy!

**Create Web Service** → Docker build başlar (3-5 dakika)

---

## ✅ Test

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
  "activeConnections": 0,
  "registeredUsers": 0,
  "onlineUsers": 0
}
```

### 2. WebSocket URL

Android uygulamanızda kullanın:
```kotlin
val signalingUrl = "wss://videocall-signaling.onrender.com/ws"
```

---

## ⚠️ Önemli Notlar

### Free Plan - Uyku Modu

**Sorun:** 15 dakika kullanılmazsa uykuya geçer.

**Çözüm 1:** Ücretsiz "ping" servisleri kullanın:
- https://uptimerobot.com (ücretsiz, 50 monitor)
- https://cron-job.org (ücretsiz)
- Her 10 dakikada bir `https://videocall-signaling.onrender.com/health` ping atın

**Çözüm 2:** Starter Plan ($7/ay) - Uyku modu yok

### WebSocket Timeout

Render.com'da WebSocket timeout 55 saniye. Signaling server'ınızda keep-alive ping/pong kullanın (zaten var).

### Logs

**Dashboard** → **Logs** → Gerçek zamanlı loglar görüntülenir.

---

## 🔧 Sorun Giderme

### Deploy Başarısız

**Hata:** `npm install` başarısız
- ✅ `package.json` var mı?
- ✅ `backend/` klasöründe mi?

**Hata:** `Cannot find module`
- ✅ `Root Directory: backend` ayarlandı mı?
- ✅ `Start Command: node src/signalingServer.js` doğru mu?

### WebSocket Bağlanmıyor

**Hata:** Connection timeout
- ✅ Uyku modunda mı? (ilk istek yavaş, 30-60 saniye bekle)
- ✅ `wss://` kullanın (https:// değil)
- ✅ `/ws` path'i var mı?

**Hata:** 404 Not Found
- ✅ `Start Command` doğru mu?
- ✅ Port 8080 açık mı?

### Logs Kontrol

**Dashboard** → **Logs** → Hata mesajlarını kontrol edin.

---

## 📊 Plan Karşılaştırma

| Plan | Maliyet | Uyku Modu | RAM | CPU |
|------|---------|-----------|-----|-----|
| Free | Ücretsiz | ✅ (15 dk) | 512MB | Shared |
| Starter | $7/ay | ❌ | 512MB | Shared |
| Standard | $25/ay | ❌ | 2GB | Dedicated |

**Öneri:** 
- Test için: **Free Plan** + UptimeRobot ping
- Production için: **Starter Plan** ($7/ay)

---

## 🎯 Sonraki Adımlar

1. ✅ Render'da deploy tamamlandı
2. ⏭️ UptimeRobot kur (uyku modunu önlemek için)
3. ⏭️ Android uygulamasını güncelle (yeni WebSocket URL)
4. ⏭️ Test et (mobil veri + Wi-Fi)
5. ⏭️ Production'a geç (Starter Plan)

---

## 💡 İpuçları

### UptimeRobot Kurulumu (Ücretsiz)

1. https://uptimerobot.com → Sign up (ücretsiz)
2. **Add New Monitor**
3. **Monitor Type:** HTTP(s)
4. **URL:** `https://videocall-signaling.onrender.com/health`
5. **Monitoring Interval:** 5 dakika
6. **Save**

**Sonuç:** Her 5 dakikada bir ping atılır, uyku modu önlenir.

### Custom Domain (Opsiyonel)

**Settings** → **Custom Domains** → Domain ekleyebilirsiniz.

Örnek: `signaling.videocall.app`

**DNS Ayarları:**
- Type: **CNAME**
- Name: `signaling`
- Value: `videocall-signaling.onrender.com`

---

## ✅ Tamamlandı!

**WebSocket URL:** `wss://videocall-signaling.onrender.com/ws`

Bu URL'yi Android uygulamanızda kullanın!

---

## 📝 Özet

- ✅ Docker **GEREKMEZ** (opsiyonel)
- ✅ Native Build daha kolay (önerilen)
- ✅ Free Plan: 750 saat/ay
- ✅ Uyku modu var (UptimeRobot ile çözülür)
- ✅ Otomatik SSL (WSS)

