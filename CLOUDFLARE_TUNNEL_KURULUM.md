# Cloudflare Tunnel Kurulum Rehberi - Adım Adım

## Ön Hazırlık

### 1. Cloudflare Hesabı
- https://dash.cloudflare.com/sign-up
- E-posta ile kayıt ol
- E-posta doğrulaması yap

### 2. Domain Ekle (Varsa)
- Dashboard → **Add a Site**
- Domain'i gir (örnek: `videocall.app`)
- Nameserver'ları Cloudflare'e yönlendir
- **Yoksa**: Ücretsiz Workers subdomain kullanabilirsin (daha sonra)

---

## Kurulum Adımları

### Adım 1: cloudflared İndir

**Windows için:**
1. https://github.com/cloudflare/cloudflared/releases/latest
2. `cloudflared-windows-amd64.exe` indir
3. `C:\Program Files\cloudflared\` dizinine kopyala
4. Dosya adını `cloudflared.exe` olarak değiştir

**PowerShell (Admin):**
```powershell
# Dizin oluştur
New-Item -ItemType Directory -Path "C:\Program Files\cloudflared" -Force

# İndir (PowerShell 5.1+)
Invoke-WebRequest -Uri "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe" -OutFile "C:\Program Files\cloudflared\cloudflared.exe"

# PATH'e ekle (opsiyonel)
$env:Path += ";C:\Program Files\cloudflared"
[Environment]::SetEnvironmentVariable("Path", $env:Path, [EnvironmentVariableTarget]::Machine)
```

### Adım 2: Cloudflare'e Login

```powershell
cd "C:\Program Files\cloudflared"
.\cloudflared.exe tunnel login
```

**Ne olacak:**
1. Tarayıcı otomatik açılacak
2. Cloudflare hesabına giriş yap
3. Domain seç (varsa)
4. "Authorize" butonuna tıkla
5. Sertifika dosyası oluşturulacak: `C:\Users\<Kullanıcı>\.cloudflared\cert.pem`

### Adım 3: Tunnel Oluştur

```powershell
.\cloudflared.exe tunnel create videocall-signaling
```

**Çıktı:**
```
Tunnel credentials written to C:\Users\<Kullanıcı>\.cloudflared\<TUNNEL-ID>.json
```

**Tunnel ID'yi not al!** (örnek: `a1b2c3d4-e5f6-7890-abcd-ef1234567890`)

### Adım 4: Config Dosyası Oluştur

**Dosya yolu:** `C:\Users\<Kullanıcı>\.cloudflared\config.yml`

**İçerik:**
```yaml
tunnel: videocall-signaling
credentials-file: C:\Users\<Kullanıcı>\.cloudflared\<TUNNEL-ID>.json

ingress:
  # Signaling Server (WebSocket) - ÖNEMLİ: ws:// kullan
  - hostname: signaling.videocall.app
    service: ws://localhost:8080
  
  # Backend API
  - hostname: api.videocall.app
    service: http://localhost:3000
  
  # Catch-all (404)
  - service: http_status:404
```

**Not:** `<Kullanıcı>` yerine Windows kullanıcı adını yaz (örnek: `C:\Users\Ahmet\.cloudflared\config.yml`)

### Adım 5: DNS Kayıtları Ekle

**Cloudflare Dashboard'dan:**
1. Domain seç → **DNS** → **Records**
2. **Add record**:
   - Type: **CNAME**
   - Name: `signaling`
   - Target: `<TUNNEL-ID>.cfargotunnel.com`
   - Proxy: **DNS only** (turuncu bulut KAPALI - WebSocket için gerekli!)
   - Save

3. **Add record** (Backend API için):
   - Type: **CNAME**
   - Name: `api`
   - Target: `<TUNNEL-ID>.cfargotunnel.com`
   - Proxy: **Proxied** (turuncu bulut AÇIK - HTTPS için)
   - Save

**Veya CLI ile:**
```powershell
.\cloudflared.exe tunnel route dns videocall-signaling signaling.videocall.app
.\cloudflared.exe tunnel route dns videocall-signaling api.videocall.app
```

### Adım 6: Tunnel'ı Test Et

```powershell
.\cloudflared.exe tunnel run videocall-signaling
```

**Beklenen çıktı:**
```
+--------------------------------------------------------------------------------------------+
|  Your quick Tunnel has been created! Visit it at (it may take some time to be reachable): |
|  https://signaling.videocall.app                                                           |
+--------------------------------------------------------------------------------------------+
```

**Ctrl+C ile durdur**

### Adım 7: Windows Service Olarak Kur

```powershell
# Service'i kur
.\cloudflared.exe service install

# Service'i başlat
.\cloudflared.exe service start

# Durum kontrol
.\cloudflared.exe service status
```

**Service otomatik başlatma:**
- Windows Services (`services.msc`) → `cloudflared` → **Startup type: Automatic**

### Adım 8: Signaling Server'ı Güncelle

**Backend'e Cloudflare domain'i kaydet:**

`backend/src/signalingServer.js` dosyasında `registerWithBackend` fonksiyonunu güncelle:

```javascript
async function registerWithBackend(localIP, publicIP, port) {
  const backendUrl = process.env.BACKEND_URL || 'http://localhost:3000';
  
  // Cloudflare Tunnel kullanıyorsak
  const useCloudflareTunnel = process.env.USE_CLOUDFLARE_TUNNEL === 'true';
  
  let wsUrl, httpUrl;
  
  if (useCloudflareTunnel) {
    // Cloudflare Tunnel domain'leri
    wsUrl = `wss://signaling.videocall.app/ws`; // wss:// (secure WebSocket)
    httpUrl = `https://api.videocall.app`; // https://
  } else {
    // Normal IP bağlantısı
    wsUrl = `ws://${localIP}:${port}/ws`;
    httpUrl = `http://${localIP}:${port}`;
  }
  
  // ... geri kalan kod aynı
}
```

**Environment variable ekle:**
`backend/.env`:
```env
USE_CLOUDFLARE_TUNNEL=true
CLOUDFLARE_DOMAIN=signaling.videocall.app
```

---

## Test

### 1. Tunnel Durumunu Kontrol Et
```powershell
.\cloudflared.exe tunnel list
.\cloudflared.exe tunnel info videocall-signaling
```

### 2. DNS Kayıtlarını Kontrol Et
```powershell
nslookup signaling.videocall.app
nslookup api.videocall.app
```

### 3. WebSocket Bağlantısını Test Et
- Web uygulamasını aç
- Mobil veri ile test et
- Console'da `wss://signaling.videocall.app/ws` bağlantısını kontrol et

### 4. Backend API'yi Test Et
```powershell
curl https://api.videocall.app/api/health
```

---

## Sorun Giderme

### Tunnel Bağlanmıyor
- ✅ Cloudflare hesabını kontrol et
- ✅ `config.yml` dosyasını kontrol et
- ✅ Tunnel ID'yi kontrol et
- ✅ Service durumunu kontrol et: `.\cloudflared.exe service status`

### WebSocket Bağlanmıyor
- ✅ DNS kaydında **Proxy KAPALI** olmalı (turuncu bulut kapalı)
- ✅ `wss://` kullan (secure WebSocket)
- ✅ Browser console'da hata mesajlarını kontrol et

### Service Başlamıyor
- ✅ Windows Services'te `cloudflared` servisini kontrol et
- ✅ Event Viewer'da hataları kontrol et
- ✅ Manuel başlat: `.\cloudflared.exe tunnel run videocall-signaling`

### DNS Çözümlenmiyor
- ✅ Cloudflare Dashboard'dan DNS kayıtlarını kontrol et
- ✅ `nslookup` ile test et
- ✅ DNS propagation bekle (5-10 dakika)

---

## Önemli Notlar

1. **WebSocket için Proxy KAPALI olmalı** (DNS only)
2. **Backend API için Proxy AÇIK olabilir** (HTTPS için)
3. **Tunnel her zaman çalışmalı** (Windows Service olarak)
4. **Port forwarding GEREKMEZ** ✅
5. **HTTPS otomatik** (Cloudflare sağlar)

---

## Sonraki Adımlar

1. ✅ Tunnel kurulumu tamamlandı
2. ⏭️ Signaling server'ı Cloudflare domain ile güncelle
3. ⏭️ Client uygulamalarını güncelle (wss://signaling.videocall.app)
4. ⏭️ Mobil veri ile test et

