# 🚀 Railway.app - Hızlı Başlangıç Rehberi

## ⚡ 5 Dakikada Deploy

### Adım 1: Railway Hesabı Oluştur

1. https://railway.app → **Start a New Project**
2. **Login with GitHub** (GitHub hesabınızla giriş yapın)
3. Email doğrulaması yapın (gerekirse)

### Adım 2: Proje Deploy Et

1. **New Project** → **Deploy from GitHub repo**
2. GitHub repo'nuzu seçin (`Video Call` projesi)
3. **Root Directory:** `backend` seçin
4. **Deploy!** (Otomatik başlar)

### Adım 3: Environment Variables

**Settings** → **Variables** → **New Variable**:

```
SIGNALING_PORT=8080
NODE_ENV=production
```

**Save** → Otomatik restart olur.

### Adım 4: Domain'i Al

**Settings** → **Networking** → **Generate Domain**

Örnek: `signaling-production.up.railway.app`

**WebSocket URL:**
```
wss://signaling-production.up.railway.app/ws
```

### Adım 5: Test Et

**Health Check:**
```
https://signaling-production.up.railway.app/health
```

**Beklenen yanıt:**
```json
{
  "status": "ok",
  "activeRooms": 0,
  "activeConnections": 0
}
```

---

## ✅ Tamamlandı!

**WebSocket URL:** `wss://signaling-production.up.railway.app/ws`

Bu URL'yi Android uygulamanızda kullanın:
```kotlin
val signalingUrl = "wss://signaling-production.up.railway.app/ws"
```

---

## 📊 Monitoring

**Dashboard'da görebilirsiniz:**
- ✅ Logs (gerçek zamanlı)
- ✅ Metrics (CPU, RAM, Network)
- ✅ Deployments (geçmiş)
- ✅ Restart (tek tıkla)

---

## 💰 Maliyet

**Free Tier:**
- $5 kredi/ay
- Test için yeterli

**Paid:**
- $0.000463/GB RAM/saat
- ~$5-10/ay (1GB RAM, 1 vCPU)

---

## 🔧 Sorun Giderme

**Deploy başarısız:**
- ✅ `package.json` var mı?
- ✅ `src/signalingServer.js` var mı?
- ✅ Environment variables doğru mu?

**WebSocket bağlanmıyor:**
- ✅ `wss://` kullanın (https:// değil)
- ✅ `/ws` path'i var mı?
- ✅ Logs'u kontrol edin

**Logs görünmüyor:**
- ✅ **Deployments** → **View Logs**

---

## 🎯 Sonraki Adımlar

1. ✅ Railway'de deploy tamamlandı
2. ⏭️ Android uygulamasını güncelle (yeni WebSocket URL)
3. ⏭️ Test et (mobil veri + Wi-Fi)
4. ⏭️ Production'a geç

---

## 📝 Notlar

- ✅ SSL otomatik (HTTPS/WSS)
- ✅ Port forwarding GEREKMEZ
- ✅ Domain otomatik oluşturulur
- ✅ Auto-deploy (Git push ile güncelleme)

