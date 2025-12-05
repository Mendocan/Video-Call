# 🏗️ Servisler ve Deployment Rehberi

## 📊 Servislerin Rolleri

### 1. **MongoDB Atlas** 🗄️
**Rol:** Veritabanı (Database)

**Ne İşe Yarar:**
- Kullanıcı bilgileri (User)
- Abonelik bilgileri (Subscription)
- Abonelik kodları (SubscriptionCode)
- Cihaz kayıtları (DeviceRegistration)
- Geri bildirimler (Feedback)

**Nerede Kullanılıyor:**
- `backend/src/db/connection.js` - MongoDB bağlantısı
- `backend/src/models/` - Mongoose modelleri
- `backend/src/index.js` - API endpoint'leri

**Bağlantı:**
- Connection String: `MONGODB_URI` environment variable
- Format: `mongodb+srv://username:password@cluster.mongodb.net/videocall`
- Cloud servisi (Atlas) - sunucu kurulumu gerekmez

**Maliyet:**
- Ücretsiz tier: 512MB storage (test için yeterli)
- Production: ~$9-25/ay (kullanıma göre)

---

### 2. **Render.com** 🚀
**Rol:** Hosting/Deployment Platformu

**Ne İşe Yarar:**
- Backend API'yi host eder (`backend/src/index.js`)
- Signaling Server'ı host eder (`backend/src/signalingServer.js`)
- WebSocket bağlantılarını yönetir
- HTTPS/SSL sağlar

**Nerede Kullanılıyor:**
- Backend API: `https://video-call-dyx9.onrender.com`
- Signaling Server: WebSocket endpoint
- Test sayfası: `https://video-call-dyx9.onrender.com/test`

**Deployment:**
- **Otomatik Deploy:** ✅ Evet (Git push ile)
- **Manuel Deploy:** Render.com dashboard'dan

**Maliyet:**
- Ücretsiz tier: 750 saat/ay (yeterli)
- Production: ~$7-25/ay (kullanıma göre)

---

### 3. **Twilio** 📞
**Rol:** TURN Sunucusu (WebRTC için)

**Ne İşe Yarar:**
- WebRTC görüşmelerinde NAT/Firewall sorunlarını çözer
- P2P bağlantı kurulamadığında relay sunucusu olarak çalışır
- Global CDN ile düşük latency sağlar

**Nerede Kullanılıyor:**
- `android/app/src/main/kotlin/com/videocall/app/directcall/ice/DirectCallIceGatherer.kt`
- TURN server credentials: `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`

**Maliyet:**
- İlk 10GB/ay: **ÜCRETSİZ** ✅
- Sonrası: $0.40/GB
- Test aşaması: **0 TL**

**Not:** Twilio SMS servisi kullanılmıyor. SMS için NETGSM kullanılıyor.

---

### 4. **NETGSM** (SMS Servisi) 📱
**Rol:** SMS Gönderimi

**Ne İşe Yarar:**
- SMS bildirimleri gönderir
- Abonelik hatırlatıcıları
- Görüşme randevu hatırlatıcıları

**Nerede Kullanılıyor:**
- `backend/src/services/smsService.js`
- `backend/src/index.js` - SMS bildirimleri
- `backend/src/jobs/notificationJobs.js` - Zamanlanmış SMS'ler

**Maliyet:**
- NETGSM fiyatlandırmasına göre (Türkiye'de yaygın)

---

## 🔄 Deployment Süreci

### Render.com'a Otomatik Deploy

**Nasıl Çalışır:**
1. **Git Repository:** Render.com, GitHub/GitLab repository'nize bağlanır
2. **Auto-Deploy:** `main` branch'e push yaptığınızda otomatik deploy başlar
3. **Build:** `npm install` ve `npm start` komutları çalışır
4. **Deploy:** Yeni kod production'a yüklenir

**Deployment Adımları:**
```bash
# 1. Kod değişikliklerini yap
# 2. Git'e commit et
git add .
git commit -m "Backend kod değişiklikleri"

# 3. Main branch'e push et
git push origin main

# 4. Render.com otomatik olarak deploy eder
#    - Build log'larını Render.com dashboard'da görebilirsiniz
#    - Deploy tamamlandığında yeni kod aktif olur
```

**Manuel Deploy:**
- Render.com dashboard → Service → "Manual Deploy" butonu

---

### Environment Variables (Render.com)

**Backend için gerekli environment variables:**

1. **MongoDB:**
   ```
   MONGODB_URI=mongodb+srv://username:password@cluster.mongodb.net/videocall
   ```

2. **Port:**
   ```
   PORT=3000
   SIGNALING_PORT=8080
   ```

3. **SMS (NETGSM):**
   ```
   NETGSM_USERNAME=your_username
   NETGSM_PASSWORD=your_password
   NETGSM_API_URL=https://api.netgsm.com.tr/sms/send/get
   ```

4. **Email (opsiyonel):**
   ```
   EMAIL_HOST=smtp.gmail.com
   EMAIL_PORT=587
   EMAIL_USER=your_email@gmail.com
   EMAIL_PASS=your_app_password
   ```

**Render.com'da Nasıl Eklenir:**
1. Render.com dashboard → Service seçin
2. "Environment" sekmesine gidin
3. "Add Environment Variable" butonuna tıklayın
4. Key ve Value'yu girin
5. "Save Changes" butonuna tıklayın
6. **ÖNEMLİ:** Değişiklikler için yeniden deploy gerekir

---

## 📝 Kod Değişiklikleri ve Deploy

### Backend Kod Değişiklikleri

**Yapılan Değişiklikler:**
- ✅ MongoDB duplicate index uyarıları düzeltildi
- ✅ Register işlemi iyileştirildi
- ✅ WebSocket bağlantı log'ları eklendi
- ✅ Direct call devre dışı bırakıldı

**Deploy Edildi mi?**
- **Hayır, henüz deploy edilmedi!**
- Kod değişiklikleri sadece local'de
- Render.com'a push yapıldığında deploy edilir

**Deploy Etmek İçin:**
```bash
# 1. Değişiklikleri commit et
git add .
git commit -m "MongoDB index düzeltmeleri ve bağlantı iyileştirmeleri"

# 2. Main branch'e push et
git push origin main

# 3. Render.com otomatik deploy başlatır
#    - Dashboard'da "Deploying..." görünür
#    - 2-5 dakika sürer
#    - "Live" olduğunda yeni kod aktif
```

---

## 🔍 Servisler Arası İlişki

```
┌─────────────┐
│   Android   │
│   Uygulama  │
└──────┬──────┘
       │
       │ WebSocket
       │ HTTPS
       ▼
┌─────────────────┐
│   Render.com    │
│  (Backend API)  │
│  (Signaling)    │
└──────┬──────────┘
       │
       ├─────────────┐
       │             │
       ▼             ▼
┌──────────┐  ┌──────────┐
│ MongoDB  │  │  Twilio  │
│  Atlas   │  │  (TURN)  │
└──────────┘  └──────────┘
```

**Akış:**
1. Android uygulama → Render.com (WebSocket bağlantısı)
2. Render.com → MongoDB (veri kaydetme/okuma)
3. Android uygulama → Twilio (TURN server, WebRTC için)
4. Render.com → NETGSM (SMS gönderimi)

---

## ⚠️ Önemli Notlar

### 1. Environment Variables
- **Asla Git'e commit etmeyin!** (`.env` zaten `.gitignore`'da)
- Render.com dashboard'dan ekleyin
- Değişiklikler için yeniden deploy gerekir

### 2. MongoDB Connection
- MongoDB Atlas'ta Network Access ayarlarını kontrol edin
- Render.com IP'lerini whitelist'e ekleyin (veya 0.0.0.0/0)

### 3. Twilio TURN
- Twilio credentials Android uygulamasında hardcode edilmiş
- Production'da güvenli bir şekilde saklanmalı (backend'den alınmalı)

### 4. Deployment
- Her push otomatik deploy başlatır
- Deploy sırasında uygulama kısa süreliğine offline olabilir
- Blue-Green deployment yok (tek instance)

---

## 🎯 Sonuç

**MongoDB:** Veritabanı (Atlas cloud)
**Render.com:** Hosting/Deployment (backend'i host eder)
**Twilio:** TURN sunucusu (WebRTC için)
**NETGSM:** SMS servisi

**Deployment:** Git push → Render.com otomatik deploy eder

**Şu Anki Durum:**
- ✅ Kod değişiklikleri yapıldı (local'de)
- ⏳ Render.com'a push edilmedi
- ⏳ Deploy edilmedi

**Sonraki Adım:**
```bash
git add .
git commit -m "Backend iyileştirmeleri"
git push origin main
```
