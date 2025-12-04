# MongoDB Server Kaldırma Rehberi

**Tarih:** 03 Aralık 2025

---

## ⚠️ ÖNEMLİ

- ✅ **MongoDB Compass'ı TUTUN** - Atlas'a bağlanmak için kullanışlı
- ❌ **MongoDB Server'ı kaldırabilirsiniz** - Atlas kullanacağız

---

## 🗑️ MongoDB Server Kaldırma Adımları

### 1. Servisi Durdur
```powershell
Stop-Service MongoDB
```

### 2. Servisi Kaldır
```powershell
sc.exe delete MongoDB
```

### 3. MongoDB Server'ı Kaldır (Programs and Features)
1. Windows Settings > Apps
2. "MongoDB" ara
3. "MongoDB Server" bul
4. Uninstall

### 4. Data Klasörünü Temizle (Opsiyonel)
```
C:\Program Files\MongoDB\Server\
C:\data\db\  (varsa)
```

---

## ✅ MongoDB Compass'ı Tutun

Compass'ı tutmanız önerilir çünkü:
- ✅ MongoDB Atlas'a bağlanmak için kullanışlı
- ✅ Veritabanını görsel olarak yönetebilirsiniz
- ✅ Collection'ları, document'ları görebilirsiniz
- ✅ Query yazabilirsiniz

---

## 🔗 MongoDB Atlas'a Compass ile Bağlanma

1. MongoDB Atlas'tan connection string al
2. Compass'ı aç
3. Connection string'i yapıştır
4. Connect

---

**Son Güncelleme:** 03 Aralık 2025

