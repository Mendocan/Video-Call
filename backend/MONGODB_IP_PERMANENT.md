# MongoDB Atlas - Kalıcı IP Erişimi Ayarlama

## ⚠️ Geçici IP Erişimi Sorunu

Eğer "This entry is temporary and will be deleted in / 6 hours" uyarısını görüyorsanız, bu **geçici bir IP erişimi** demektir. 6 saat sonra otomatik olarak silinir.

---

## ✅ Kalıcı IP Erişimi İçin Çözüm

### Yöntem 1: Manuel Olarak `0.0.0.0/0` Ekleyin (Önerilen)

1. **"IP Access List"** sayfasında
2. **"ADD IP ADDRESS"** butonuna tıklayın
3. **"Access List Entry"** alanına manuel olarak şunu yazın:
   ```
   0.0.0.0/0
   ```
4. **"Comment"** alanına (opsiyonel):
   ```
   Allow all IPs for Render.com
   ```
5. **"Confirm"** butonuna tıklayın

Bu şekilde **kalıcı** bir IP erişimi oluşturmuş olursunuz.

---

### Yöntem 2: Geçici Erişimi Kalıcıya Çevirin

1. Mevcut geçici IP erişimini bulun
2. **"EDIT"** (kalem ikonu) butonuna tıklayın
3. **"Delete After"** veya **"Temporary"** seçeneğini kaldırın
4. **"Save"** veya **"Confirm"** butonuna tıklayın

---

## 🔍 Kontrol

Kalıcı IP erişimi ekledikten sonra:
- **"IP Access List"** sayfasında
- IP adresinin yanında **"Temporary"** veya **"Expires in X hours"** yazısı **olmamalı**
- Sadece IP adresi ve durum görünmeli: `0.0.0.0/0` - **Active**

---

## 📝 Notlar

- **Geçici erişim:** Test için kullanışlıdır, 6 saat sonra otomatik silinir
- **Kalıcı erişim:** Production için gereklidir, manuel silinene kadar kalır
- **Güvenlik:** `0.0.0.0/0` tüm IP'lere izin verir, production'da daha kısıtlayıcı IP'ler kullanabilirsiniz

---

**Son Güncelleme:** 03 Aralık 2025

