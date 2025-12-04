# MongoDB Atlas Network Access (IP Whitelist) Kurulumu

## 📍 Network Access Sayfasına Gitme

1. **Sol sidebar'da** "NETWORK ACCESS" bölümünü bulun
2. **"IP Access List"** seçeneğine tıklayın
3. Bu sayfada IP adreslerini yönetebilirsiniz

---

## 🔓 IP Erişimini Açma

### ⚠️ ÖNEMLİ: Geçici vs Kalıcı IP Erişimi

**"ALLOW ACCESS FROM ANYWHERE"** seçeneği **geçici** bir erişim oluşturur (6 saat sonra silinir).

**Kalıcı erişim için:** Manuel olarak `0.0.0.0/0` ekleyin!

### Yöntem 1: Kalıcı IP Erişimi (Önerilen)

1. **"IP Access List"** sayfasında
2. **"ADD IP ADDRESS"** butonuna tıklayın
3. **"Access List Entry"** alanına **manuel olarak** şunu yazın:
   ```
   0.0.0.0/0
   ```
4. **"Comment"** alanına (opsiyonel):
   ```
   Allow all IPs for Render.com
   ```
5. **"Confirm"** butonuna tıklayın

Bu şekilde **kalıcı** bir IP erişimi oluşturmuş olursunuz.

### Yöntem 2: Geçici IP Erişimi (Test için)

1. **"ADD IP ADDRESS"** butonuna tıklayın
2. **"ALLOW ACCESS FROM ANYWHERE"** seçeneğini seçin
3. ⚠️ **Uyarı:** Bu geçici bir erişimdir, 6 saat sonra silinir
4. **"Confirm"** butonuna tıklayın

### Yöntem 2: Sadece Belirli IP'lere İzin Ver (Production için önerilen)

1. **"ADD IP ADDRESS"** butonuna tıklayın
2. **"Current IP Address"** seçeneğini seçin (otomatik IP'nizi ekler)
3. Veya manuel olarak IP adresinizi girin
4. **"Confirm"** butonuna tıklayın

---

## ⚠️ ÖNEMLİ NOTLAR

- **Geliştirme için:** `0.0.0.0/0` (tüm IP'ler) kullanabilirsiniz
- **Production için:** Sadece Render.com IP'lerini veya belirli IP'leri ekleyin
- **Render.com için:** Render.com'un IP adresleri değişken olduğu için `0.0.0.0/0` kullanmak daha pratik olabilir

---

## 🔍 Eğer "ADD IP ADDRESS" Butonunu Göremiyorsanız

1. Sayfanın **sağ üst köşesine** bakın
2. **Yeşil bir buton** olmalı: "ADD IP ADDRESS" veya "+ ADD IP ADDRESS"
3. Eğer hala göremiyorsanız, sayfayı yenileyin (F5)

---

## 📸 Görsel Rehber

Network Access sayfasında şunları görmelisiniz:
- Sol üstte: "IP Access List" başlığı
- Sağ üstte: Yeşil "ADD IP ADDRESS" butonu
- Liste: Mevcut IP adresleri (eğer varsa)

---

**Son Güncelleme:** 03 Aralık 2025

