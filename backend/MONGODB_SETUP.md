# MongoDB Atlas Bağlantı Rehberi

## 📋 ADIM ADIM KURULUM

### 1. MongoDB Atlas'tan Connection String Alın

1. MongoDB Atlas dashboard'a gidin
2. "Connect" butonuna tıklayın
3. **"Drivers" (Sürücüler)** seçeneğini seçin
4. **"Node.js"** sürücüsünü seçin
5. Connection string'i kopyalayın

**Örnek Format:**
```
mongodb+srv://<username>:<password>@cluster0.xxxxx.mongodb.net/videocall?retryWrites=true&w=majority
```

### 2. Network Access Ayarları

1. MongoDB Atlas dashboard'da **"Network Access"** sekmesine gidin
2. **"Add IP Address"** butonuna tıklayın
3. **"Allow Access from Anywhere"** (0.0.0.0/0) seçin (veya Render.com IP'lerini ekleyin)
4. **"Confirm"** butonuna tıklayın

### 3. Database User Oluşturun

1. **"Database Access"** sekmesine gidin
2. **"Add New Database User"** butonuna tıklayın
3. Username ve Password belirleyin
4. **"Read and write to any database"** yetkisi verin
5. **"Add User"** butonuna tıklayın

### 4. Connection String'i Güncelleyin

Connection string'deki `<username>` ve `<password>` kısımlarını oluşturduğunuz kullanıcı bilgileriyle değiştirin:

```
mongodb+srv://videocall_user:your_password@cluster0.xxxxx.mongodb.net/videocall?retryWrites=true&w=majority
```

### 5. Backend'e Ekleyin

Connection string'i `.env` dosyasına ekleyin:

```env
MONGODB_URI=mongodb+srv://videocall_user:your_password@cluster0.xxxxx.mongodb.net/videocall?retryWrites=true&w=majority
```

### 6. Render.com'a Ekleyin

1. Render.com dashboard'a gidin
2. Service'inizi seçin
3. **"Environment"** sekmesine gidin
4. **"Add Environment Variable"** butonuna tıklayın
5. Key: `MONGODB_URI`
6. Value: Connection string'inizi yapıştırın
7. **"Save Changes"** butonuna tıklayın

---

## ✅ SONRAKI ADIMLAR

1. `mongoose` paketini yükleyin
2. Connection kodunu oluşturun
3. Model'leri tanımlayın
4. Mevcut Map'leri MongoDB ile değiştirin

---

**Son Güncelleme:** 03 Aralık 2025

