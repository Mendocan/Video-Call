# MongoDB Entegrasyonu - Sonraki Adımlar

## ✅ Tamamlanan İşlemler

1. ✅ `mongoose` paketi yüklendi
2. ✅ MongoDB connection dosyası oluşturuldu (`backend/src/db/connection.js`)
3. ✅ MongoDB Atlas bağlantısı test edildi ve başarılı
4. ✅ `.env` dosyasına `MONGODB_URI` eklendi

---

## 🚀 Sonraki Adımlar

### 1. Model'leri Oluştur

MongoDB için model dosyalarını oluşturmalıyız:

- `backend/src/models/User.js` - Kullanıcı modeli
- `backend/src/models/Subscription.js` - Abonelik modeli
- `backend/src/models/SubscriptionCode.js` - Abonelik kodu modeli
- `backend/src/models/DeviceRegistration.js` - Cihaz kayıt modeli
- `backend/src/models/Feedback.js` - Geri bildirim modeli

### 2. Mevcut Map'leri MongoDB ile Değiştir

- `backend/src/auth.js` - `users` Map → MongoDB User modeli
- `backend/src/index.js` - `subscriptions` Map → MongoDB Subscription modeli
- `backend/src/index.js` - `subscriptionCodes` Map → MongoDB SubscriptionCode modeli
- `backend/src/index.js` - `deviceRegistrations` Map → MongoDB DeviceRegistration modeli
- `backend/src/index.js` - `feedbacks` array → MongoDB Feedback modeli

### 3. Backend'i MongoDB ile Başlat

`backend/src/index.js` dosyasına MongoDB bağlantısını ekle:

```javascript
import connectDB from './db/connection.js';

// Server başlatmadan önce MongoDB'ye bağlan
await connectDB();
```

---

## 📝 Öncelik Sırası

1. **User Model** - En kritik (kullanıcı kayıt/giriş)
2. **Subscription Model** - Abonelik yönetimi
3. **SubscriptionCode Model** - Abonelik kodları
4. **DeviceRegistration Model** - Cihaz kayıtları
5. **Feedback Model** - Geri bildirimler

---

## ⚠️ ÖNEMLİ NOTLAR

- **Backward Compatibility:** Geçiş sırasında hem Map hem MongoDB kullanılabilir (gradual migration)
- **Error Handling:** MongoDB hatalarını yakala ve logla
- **Index'ler:** Model'lerde index'leri tanımla (performance için)
- **Validation:** Mongoose schema validation kullan

---

**Son Güncelleme:** 03 Aralık 2025

