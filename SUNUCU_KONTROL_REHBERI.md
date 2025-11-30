# 🔍 Sunucu Kontrol Rehberi

## ✅ Kontrol Listesi

### 1. GitHub Kontrolü

#### A. Backend Klasörü GitHub'da Var mı?

**Kontrol:**
1. GitHub'da repo'yu açın: `https://github.com/Mendocan/Video-Call`
2. `backend/` klasörünün var olduğunu kontrol edin
3. `backend/src/signalingServer.js` dosyasının var olduğunu kontrol edin
4. `backend/package.json` dosyasının var olduğunu kontrol edin

**Beklenen Dosya Yapısı:**
```
Video-Call/
├── backend/
│   ├── src/
│   │   └── signalingServer.js ✅
│   ├── package.json ✅
│   ├── Dockerfile ✅
│   └── .dockerignore (opsiyonel)
├── android/
└── web/
```

#### B. Son Commit'ler GitHub'da mı?

**Kontrol:**
1. GitHub'da **Commits** sekmesine gidin
2. Son commit'in ne zaman yapıldığını kontrol edin
3. `backend/` klasöründe değişiklik var mı kontrol edin

**Komut (yerel):**
```powershell
cd "C:\Video Call"
git log --oneline -10
git status
```

---

### 2. Render.com Kontrolü

#### A. Render.com Dashboard Kontrolü

**Kontrol:**
1. Render.com dashboard'a gidin: https://dashboard.render.com
2. **Web Services** bölümünde `video-call-dyx9` (veya servis adınız) var mı?
3. **Status** nedir? (Live, Building, Failed?)
4. **Last Deploy** ne zaman?

#### B. Render.com Ayarları Kontrolü

**Kontrol:**
1. Servis → **Settings** → **Build & Deploy**
2. **Root Directory:** `backend` ✅ olmalı
3. **Build Command:** `npm install` ✅ olmalı
4. **Start Command:** `node src/signalingServer.js` ✅ olmalı
5. **Branch:** `main` (veya hangi branch'teyseniz)

#### C. Environment Variables Kontrolü

**Kontrol:**
1. Servis → **Environment** → **Environment Variables**
2. Şu değişkenler var mı?
   - `SIGNALING_PORT=8080` ✅
   - `NODE_ENV=production` ✅
   - `SIGNALING_HOST=0.0.0.0` ✅

#### D. Render.com Logs Kontrolü

**Kontrol:**
1. Servis → **Logs** sekmesine gidin
2. Son logları kontrol edin:
   - ✅ `🚀 Signaling Server running on 0.0.0.0:8080` görünüyor mu?
   - ✅ `📡 WebSocket endpoint: ws://...` görünüyor mu?
   - ❌ Hata mesajları var mı?

**Beklenen Log:**
```
🚀 Signaling Server running on 0.0.0.0:8080
📡 WebSocket endpoint: ws://...
💡 Health check: http://...:8080/health
```

---

### 3. Health Check Testi

#### A. Tarayıcıdan Test

**URL:**
```
https://video-call-dyx9.onrender.com/health
```

**Beklenen Yanıt:**
```json
{
  "status": "ok",
  "timestamp": "2025-01-XX...",
  "activeRooms": 0,
  "activeConnections": 0,
  "registeredUsers": 0,
  "onlineUsers": 0,
  "activeGroups": 0,
  "totalParticipants": 0
}
```

**Sorun Varsa:**
- ❌ 404 Not Found → Root Directory yanlış
- ❌ 500 Internal Server Error → Kod hatası
- ❌ Timeout → Server uyku modunda (30-60 saniye bekle)

#### B. WebSocket Test (Tarayıcı Console)

**Test:**
1. Tarayıcıda F12 → Console
2. Şu kodu çalıştırın:

```javascript
const ws = new WebSocket('wss://video-call-dyx9.onrender.com/ws');
ws.onopen = () => console.log('✅ WebSocket bağlandı!');
ws.onerror = (e) => console.error('❌ WebSocket hatası:', e);
ws.onmessage = (e) => console.log('📨 Mesaj:', e.data);
```

**Beklenen:**
- ✅ `WebSocket bağlandı!` mesajı görünmeli
- ❌ Hata varsa logları kontrol edin

---

### 4. Kod Kontrolü

#### A. Yerel Dosyalar Kontrolü

**Kontrol:**
1. `backend/src/signalingServer.js` dosyası var mı?
2. `backend/package.json` dosyası var mı?
3. `ws` dependency var mı? (`package.json` içinde)

**Komut:**
```powershell
cd "C:\Video Call\backend"
Get-Content package.json | Select-String "ws"
```

**Beklenen:**
```json
"ws": "^8.16.0"
```

#### B. GitHub ile Yerel Karşılaştırma

**Komut:**
```powershell
cd "C:\Video Call"
git status
git diff backend/src/signalingServer.js
```

**Beklenen:**
- `nothing to commit, working tree clean` → Tüm değişiklikler GitHub'da
- Değişiklik varsa → `git add .` ve `git commit` yapın

---

### 5. Deploy Kontrolü

#### A. Render.com'da Hangi Commit Deploy Edildi?

**Kontrol:**
1. Render.com → Servis → **Events** sekmesi
2. Son deploy'da hangi commit hash'i kullanıldı?
3. GitHub'daki commit hash'i ile eşleşiyor mu?

**Komut (yerel):**
```powershell
cd "C:\Video Call"
git log -1 --format="%H"
```

**Render.com'da:**
- Events → Son deploy → Commit hash'i görünür

#### B. Manuel Deploy

**Eğer sorun varsa:**
1. Render.com → Servis → **Manual Deploy** → **Deploy latest commit**
2. Deploy tamamlanana kadar bekleyin (2-3 dakika)
3. Logs'u kontrol edin

---

## 🔧 Sorun Giderme

### Sorun 1: Health Check 404 Veriyor

**Neden:**
- Root Directory yanlış (`backend` olmalı)
- Start Command yanlış (`node src/signalingServer.js` olmalı)

**Çözüm:**
1. Render.com → Settings → Build & Deploy
2. Root Directory: `backend` ✅
3. Start Command: `node src/signalingServer.js` ✅
4. Save → Manual Deploy

---

### Sorun 2: WebSocket Bağlanmıyor

**Neden:**
- Server uyku modunda (Free Plan)
- Timeout ayarları yetersiz
- Render.com WebSocket timeout (55 saniye)

**Çözüm:**
1. İlk bağlantıda 30-60 saniye bekleyin
2. Android timeout'ları 60 saniyeye çıkarıldı (yapıldı ✅)
3. Health check'i önce çağırın (server'ı uyandırır)

---

### Sorun 3: GitHub'da Dosyalar Yok

**Neden:**
- Değişiklikler commit edilmemiş
- Push edilmemiş

**Çözüm:**
```powershell
cd "C:\Video Call"
git add .
git commit -m "Backend dosyaları güncellendi"
git push origin main
```

---

### Sorun 4: Render.com Eski Kod Kullanıyor

**Neden:**
- Eski commit deploy edilmiş
- Cache sorunu

**Çözüm:**
1. Render.com → Manual Deploy → Deploy latest commit
2. Veya Settings → Clear build cache → Deploy

---

## 📋 Hızlı Kontrol Komutları

### Yerel Kontrol
```powershell
# Git durumu
cd "C:\Video Call"
git status

# Son commit
git log -1

# Backend dosyaları
ls backend/src/

# Package.json kontrolü
Get-Content backend/package.json | Select-String "ws"
```

### Render.com Kontrol (Tarayıcı)
1. Health Check: https://video-call-dyx9.onrender.com/health
2. Dashboard: https://dashboard.render.com
3. Logs: Servis → Logs sekmesi

---

## ✅ Kontrol Sonucu

Kontrolleri yaptıktan sonra şunları paylaşın:

1. ✅/❌ GitHub'da backend klasörü var mı?
2. ✅/❌ Render.com'da servis çalışıyor mu?
3. ✅/❌ Health check çalışıyor mu?
4. ✅/❌ Logs'ta hata var mı?
5. ✅/❌ Son commit GitHub'da mı?

Bu bilgilerle sorunu tespit edip çözebiliriz!

