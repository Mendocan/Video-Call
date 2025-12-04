# MongoDB Atlas Connection String Alma

## 📍 Connection String'i Alma

### 1. MongoDB Atlas Dashboard'a Dönün

1. Sol sidebar'da **"DEPLOYMENT"** bölümüne gidin
2. **"Database"** seçeneğine tıklayın
3. Cluster'ınızı seçin (genellikle "Cluster0")

### 2. "Connect" Butonuna Tıklayın

1. Cluster'ınızın yanında **"Connect"** butonunu bulun
2. **"Connect"** butonuna tıklayın

### 3. "Drivers" (Sürücüler) Seçeneğini Seçin

1. Açılan modal'da **"Drivers"** seçeneğine tıklayın
2. **"Node.js"** sürücüsünü seçin
3. **Version:** En son sürümü seçin (genellikle 6.x veya üzeri)

### 4. Connection String'i Kopyalayın

Connection string şu formatta olacak:
```
mongodb+srv://<username>:<password>@cluster0.xxxxx.mongodb.net/videocall?retryWrites=true&w=majority
```

**ÖNEMLİ:** 
- `<username>` ve `<password>` kısımlarını Database User bilgilerinizle değiştirin
- `mendokurtoglu_db_user` kullanıcı adınızı kullanın
- Şifrenizi girin

**Örnek:**
```
mongodb+srv://mendokurtoglu_db_user:your_password@cluster0.xxxxx.mongodb.net/videocall?retryWrites=true&w=majority
```

### 5. Connection String'i Backend'e Ekleyin

1. `backend/.env` dosyasını açın
2. Şu satırı ekleyin veya güncelleyin:
```env
MONGODB_URI=mongodb+srv://mendokurtoglu_db_user:your_password@cluster0.xxxxx.mongodb.net/videocall?retryWrites=true&w=majority
```

**⚠️ ÖNEMLİ:** 
- `your_password` kısmını gerçek şifrenizle değiştirin
- Connection string'i asla git'e commit etmeyin (`.env` zaten `.gitignore`'da)

---

## ✅ Sonraki Adım: Bağlantıyı Test Et

Connection string'i ekledikten sonra bağlantıyı test edelim!

---

**Son Güncelleme:** 03 Aralık 2025

