# MongoDB Entegrasyon Planı

**Tarih:** 03 Aralık 2025  
**Durum:** Planlama Aşaması

---

## 📋 ÖZET

Backend'deki in-memory Map'ler yerine MongoDB kullanılacak. Domain gerekmez, MongoDB Atlas veya Render.com MongoDB addon kullanılabilir.

---

## 🎯 HEDEF

In-memory storage'ları MongoDB'ye taşımak:
- ✅ Kullanıcı verileri (`users` Map)
- ✅ Abonelikler (`subscriptions` Map)
- ✅ Abonelik kodları (`subscriptionCodes` Map)
- ✅ Cihaz kayıtları (`deviceRegistrations` Map)
- ✅ Geri bildirimler (`feedbacks` array)
- ✅ Signaling server verileri (opsiyonel - geçici veriler için in-memory kalabilir)

---

## 🔧 MONGODB SEÇENEKLERİ

### 1. MongoDB Atlas (Önerilen) ⭐

**Avantajlar:**
- ✅ Ücretsiz tier (M0) - 512MB storage, shared cluster
- ✅ Domain gerekmez
- ✅ Connection string ile bağlanılır
- ✅ Otomatik yedekleme
- ✅ Kolay ölçeklendirme
- ✅ Render.com'dan bağlanılabilir

**Kurulum:**
1. MongoDB Atlas hesabı oluştur (https://www.mongodb.com/cloud/atlas)
2. Free tier cluster oluştur
3. Database user oluştur
4. Network Access ayarla (Render.com IP'leri veya 0.0.0.0/0 - tüm IP'ler)
5. Connection string al

**Connection String Format:**
```
mongodb+srv://<username>:<password>@cluster0.xxxxx.mongodb.net/videocall?retryWrites=true&w=majority
```

### 2. Render.com MongoDB Addon

**Avantajlar:**
- ✅ Render.com içinde entegre
- ✅ Domain gerekmez
- ✅ Kolay kurulum
- ⚠️ Ücretli (ücretsiz tier yok)

**Kurulum:**
1. Render.com dashboard'a git
2. "New" > "MongoDB"
3. Plan seç (ücretli)
4. Connection string otomatik oluşturulur

---

## 📦 GEREKLİ PAKETLER

```bash
npm install mongodb
# veya
npm install mongoose  # ODM (Object Document Mapper) - daha kolay kullanım
```

**Öneri:** `mongoose` kullanmak daha kolay ve güvenli.

---

## 🗄️ VERİTABANI ŞEMASI

### Collections (Tablo benzeri)

#### 1. `users`
```javascript
{
  _id: ObjectId,
  userId: String, // "user_1234567890_abc123"
  name: String,
  email: String,
  phone: String,
  password: String, // hashed
  role: String, // "user" veya "admin"
  createdAt: Date,
  subscription: {
    planId: String,
    status: String,
    expiresAt: Date
  }
}
```

#### 2. `subscriptions`
```javascript
{
  _id: ObjectId,
  subscriptionId: String,
  userId: String,
  phoneNumber: String,
  planId: String,
  status: String, // "active", "expired", "cancelled"
  paymentId: String,
  createdAt: Date,
  expiresAt: Date
}
```

#### 3. `subscriptionCodes`
```javascript
{
  _id: ObjectId,
  code: String, // "VC-XXXX-XXXX-XXXX"
  phoneNumber: String | null,
  planId: String,
  expiresAt: Date | null,
  used: Boolean,
  usedAt: Date | null,
  usedBy: String | null,
  deviceType: String | null,
  subscriptionId: String | null,
  createdAt: Date
}
```

#### 4. `deviceRegistrations`
```javascript
{
  _id: ObjectId,
  phoneNumber: String,
  deviceId: String, // hashed
  registeredAt: Date,
  lastSeen: Date
}
```

#### 5. `feedbacks`
```javascript
{
  _id: ObjectId,
  userId: String | null,
  email: String | null,
  phoneNumber: String | null,
  type: String, // "bug", "feature", "general"
  subject: String,
  message: String,
  rating: Number | null,
  createdAt: Date
}
```

---

## 🔄 MİGRASYON PLANI

### Faz 1: MongoDB Bağlantısı
1. MongoDB Atlas hesabı oluştur
2. `mongoose` paketini yükle
3. Connection string'i `.env` dosyasına ekle
4. `backend/src/db/connection.js` oluştur
5. Bağlantıyı test et

### Faz 2: Model Tanımlamaları
1. `backend/src/models/User.js` oluştur
2. `backend/src/models/Subscription.js` oluştur
3. `backend/src/models/SubscriptionCode.js` oluştur
4. `backend/src/models/DeviceRegistration.js` oluştur
5. `backend/src/models/Feedback.js` oluştur

### Faz 3: Auth.js Güncellemesi
1. `users` Map yerine MongoDB kullan
2. `registerUser` fonksiyonunu güncelle
3. `loginUser` fonksiyonunu güncelle
4. `getUserById` fonksiyonunu güncelle

### Faz 4: Index.js Güncellemesi
1. `subscriptions` Map yerine MongoDB kullan
2. `subscriptionCodes` Map yerine MongoDB kullan
3. `deviceRegistrations` Map yerine MongoDB kullan
4. `feedbacks` array yerine MongoDB kullan

### Faz 5: Test ve Doğrulama
1. Tüm endpoint'leri test et
2. Veri tutarlılığını kontrol et
3. Performance testleri yap

---

## 🔐 GÜVENLİK

### Environment Variables
```env
MONGODB_URI=mongodb+srv://username:password@cluster0.xxxxx.mongodb.net/videocall?retryWrites=true&w=majority
```

**⚠️ ÖNEMLİ:** Connection string'i `.env` dosyasına ekle, asla kod içine yazma!

### Render.com Environment Variables
1. Render.com dashboard'a git
2. Service > Environment
3. `MONGODB_URI` ekle
4. Connection string'i yapıştır

---

## 📝 ÖRNEK KOD

### Connection (backend/src/db/connection.js)
```javascript
import mongoose from 'mongoose';

const MONGODB_URI = process.env.MONGODB_URI;

if (!MONGODB_URI) {
  throw new Error('MONGODB_URI environment variable is required');
}

let cached = global.mongoose;

if (!cached) {
  cached = global.mongoose = { conn: null, promise: null };
}

async function connectDB() {
  if (cached.conn) {
    return cached.conn;
  }

  if (!cached.promise) {
    const opts = {
      bufferCommands: false,
    };

    cached.promise = mongoose.connect(MONGODB_URI, opts).then((mongoose) => {
      console.log('✅ MongoDB bağlantısı başarılı');
      return mongoose;
    });
  }

  try {
    cached.conn = await cached.promise;
  } catch (e) {
    cached.promise = null;
    throw e;
  }

  return cached.conn;
}

export default connectDB;
```

### Model Örneği (backend/src/models/User.js)
```javascript
import mongoose from 'mongoose';

const userSchema = new mongoose.Schema({
  userId: {
    type: String,
    required: true,
    unique: true
  },
  name: {
    type: String,
    required: true
  },
  email: {
    type: String,
    required: true,
    unique: true
  },
  phone: {
    type: String,
    unique: true,
    sparse: true // null değerlere izin ver
  },
  password: {
    type: String,
    required: true
  },
  role: {
    type: String,
    enum: ['user', 'admin'],
    default: 'user'
  },
  subscription: {
    planId: String,
    status: String,
    expiresAt: Date
  },
  createdAt: {
    type: Date,
    default: Date.now
  }
});

// Index'ler
userSchema.index({ email: 1 });
userSchema.index({ phone: 1 });
userSchema.index({ userId: 1 });

export default mongoose.models.User || mongoose.model('User', userSchema);
```

---

## ⚠️ ÖNEMLİ NOTLAR

1. **Domain Gerekmez:** MongoDB Atlas connection string ile bağlanılır, domain gerekmez.
2. **Render.com IP Whitelist:** MongoDB Atlas Network Access'te Render.com IP'lerini ekle veya 0.0.0.0/0 (tüm IP'ler) yap.
3. **Connection Pooling:** `mongoose` otomatik connection pooling yapar.
4. **Error Handling:** Bağlantı hatalarını yakala ve logla.
5. **Backward Compatibility:** Geçiş sırasında hem Map hem MongoDB kullanılabilir (gradual migration).

---

## 🚀 BAŞLANGIÇ ADIMLARI

1. MongoDB Atlas hesabı oluştur (ücretsiz)
2. Cluster oluştur
3. Database user oluştur
4. Network Access ayarla
5. Connection string al
6. `.env` dosyasına ekle
7. `mongoose` paketini yükle
8. Connection kodunu yaz
9. İlk model'i oluştur (User)
10. Test et

---

**Son Güncelleme:** 03 Aralık 2025

