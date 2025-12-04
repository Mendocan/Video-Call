# MongoDB Connection String Düzeltme

## 🔧 Mevcut Connection String

```
mongodb+srv://mendokurtoglu_db_user:<db_password>@videocall.v6zwh6z.mongodb.net/?appName=VideoCall
```

## ✅ Düzeltilmiş Connection String

```
mongodb+srv://mendokurtoglu_db_user:GERÇEK_ŞİFRE@videocall.v6zwh6z.mongodb.net/videocall?retryWrites=true&w=majority
```

## 📝 Değişiklikler

1. **`<db_password>`** → Gerçek şifrenizle değiştirin (Database User şifresi)
2. **Database adı eklendi:** `/videocall` (cluster adından sonra)
3. **Query parametreleri eklendi:** `?retryWrites=true&w=majority`
4. **`appName=VideoCall`** kaldırıldı (opsiyonel, gerekli değil)

## 🔐 .env Dosyasına Ekleyin

```env
MONGODB_URI=mongodb+srv://mendokurtoglu_db_user:GERÇEK_ŞİFRE@videocall.v6zwh6z.mongodb.net/videocall?retryWrites=true&w=majority
```

**⚠️ ÖNEMLİ:** `GERÇEK_ŞİFRE` kısmını Database User şifrenizle değiştirin!

---

**Son Güncelleme:** 03 Aralık 2025

