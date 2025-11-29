# Cloudflare DDNS Kurulum Rehberi

## Ön Hazırlık

### 1. Cloudflare Hesabı Oluştur
1. https://dash.cloudflare.com/sign-up adresine git
2. E-posta ile hesap oluştur
3. E-posta doğrulaması yap

### 2. Domain Ekle (Varsa)
- Eğer domain'in varsa Cloudflare'e ekle
- Nameserver'ları Cloudflare'e yönlendir
- Yoksa: Ücretsiz subdomain kullanabilirsin (örnek: `videocall.workers.dev`)

### 3. API Token Oluştur
1. Cloudflare Dashboard → **My Profile** → **API Tokens**
2. **Create Token** → **Edit zone DNS** template'ini kullan
3. Zone ve Account Resources'u seç
4. Token'ı kopyala (bir daha gösterilmeyecek!)

---

## Seçenek 1: Cloudflare Tunnel (Önerilen) ⭐

### Avantajlar
- ✅ Port forwarding **GEREKMEZ**
- ✅ HTTPS otomatik
- ✅ Güvenli (Cloudflare arkasında)
- ✅ Ücretsiz

### Kurulum

#### Adım 1: Cloudflared İndir
```powershell
# Windows için
# https://github.com/cloudflare/cloudflared/releases
# cloudflared-windows-amd64.exe indir
# C:\Program Files\cloudflared\ dizinine kopyala
```

#### Adım 2: Tunnel Oluştur
```powershell
cd "C:\Program Files\cloudflared"
.\cloudflared.exe tunnel login
# Tarayıcı açılacak, Cloudflare'e giriş yap
```

#### Adım 3: Tunnel Oluştur ve Başlat
```powershell
# Tunnel oluştur
.\cloudflared.exe tunnel create videocall-signaling

# Config dosyası oluştur (C:\Users\<Kullanıcı>\.cloudflared\config.yml)
# Manuel oluştur:
```

**config.yml içeriği:**
```yaml
tunnel: videocall-signaling
credentials-file: C:\Users\<Kullanıcı>\.cloudflared\<TUNNEL-ID>.json

ingress:
  # Signaling Server (WebSocket)
  - hostname: signaling.videocall.app
    service: ws://localhost:8080
  
  # Backend API
  - hostname: api.videocall.app
    service: http://localhost:3000
  
  # Catch-all
  - service: http_status:404
```

#### Adım 4: DNS Kaydı Ekle
```powershell
# Cloudflare Dashboard'dan veya CLI ile:
.\cloudflared.exe tunnel route dns videocall-signaling signaling.videocall.app
.\cloudflared.exe tunnel route dns videocall-signaling api.videocall.app
```

#### Adım 5: Tunnel'ı Başlat
```powershell
# Manuel başlatma
.\cloudflared.exe tunnel run videocall-signaling

# Veya Windows Service olarak:
.\cloudflared.exe service install
.\cloudflared.exe service start
```

### Sonuç
- `wss://signaling.videocall.app/ws` → WebSocket bağlantısı
- `https://api.videocall.app` → Backend API
- Port forwarding **GEREKMEZ** ✅

---

## Seçenek 2: Cloudflare API + DDNS Client

### Gereksinimler
- ✅ Port forwarding gerekli (modem/router)
- ✅ Cloudflare API Token
- ✅ Domain veya subdomain

### Kurulum

#### Adım 1: DDNS Client Script Oluştur
`backend/scripts/cloudflare-ddns.js` dosyası oluştur:

```javascript
import fetch from 'node-fetch';
import dotenv from 'dotenv';

dotenv.config();

const CLOUDFLARE_API_TOKEN = process.env.CLOUDFLARE_API_TOKEN;
const CLOUDFLARE_ZONE_ID = process.env.CLOUDFLARE_ZONE_ID;
const DOMAIN = process.env.CLOUDFLARE_DOMAIN || 'signaling.videocall.app';

// Public IP'yi al
async function getPublicIP() {
  try {
    const response = await fetch('https://api.ipify.org?format=json');
    const data = await response.json();
    return data.ip;
  } catch (error) {
    console.error('Public IP alınamadı:', error);
    return null;
  }
}

// Cloudflare DNS kaydını güncelle
async function updateDNSRecord(ip) {
  try {
    // Önce mevcut kaydı bul
    const listResponse = await fetch(
      `https://api.cloudflare.com/client/v4/zones/${CLOUDFLARE_ZONE_ID}/dns_records?name=${DOMAIN}`,
      {
        headers: {
          'Authorization': `Bearer ${CLOUDFLARE_API_TOKEN}`,
          'Content-Type': 'application/json'
        }
      }
    );

    const listData = await listResponse.json();
    
    if (listData.result && listData.result.length > 0) {
      // Mevcut kaydı güncelle
      const recordId = listData.result[0].id;
      const updateResponse = await fetch(
        `https://api.cloudflare.com/client/v4/zones/${CLOUDFLARE_ZONE_ID}/dns_records/${recordId}`,
        {
          method: 'PUT',
          headers: {
            'Authorization': `Bearer ${CLOUDFLARE_API_TOKEN}`,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            type: 'A',
            name: DOMAIN,
            content: ip,
            ttl: 300 // 5 dakika
          })
        }
      );

      const updateData = await updateResponse.json();
      if (updateData.success) {
        console.log(`✅ DNS kaydı güncellendi: ${DOMAIN} → ${ip}`);
        return true;
      } else {
        console.error('DNS güncelleme hatası:', updateData.errors);
        return false;
      }
    } else {
      // Yeni kayıt oluştur
      const createResponse = await fetch(
        `https://api.cloudflare.com/client/v4/zones/${CLOUDFLARE_ZONE_ID}/dns_records`,
        {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${CLOUDFLARE_API_TOKEN}`,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            type: 'A',
            name: DOMAIN,
            content: ip,
            ttl: 300
          })
        }
      );

      const createData = await createResponse.json();
      if (createData.success) {
        console.log(`✅ DNS kaydı oluşturuldu: ${DOMAIN} → ${ip}`);
        return true;
      } else {
        console.error('DNS oluşturma hatası:', createData.errors);
        return false;
      }
    }
  } catch (error) {
    console.error('Cloudflare API hatası:', error);
    return false;
  }
}

// Ana fonksiyon
async function main() {
  console.log('🔄 Cloudflare DDNS güncelleme başlatılıyor...');
  
  const publicIP = await getPublicIP();
  if (!publicIP) {
    console.error('❌ Public IP alınamadı');
    process.exit(1);
  }

  console.log(`🌐 Public IP: ${publicIP}`);
  
  const success = await updateDNSRecord(publicIP);
  if (success) {
    console.log('✅ DDNS güncelleme başarılı');
  } else {
    console.error('❌ DDNS güncelleme başarısız');
    process.exit(1);
  }
}

// Her 5 dakikada bir çalıştır
main();
setInterval(main, 5 * 60 * 1000); // 5 dakika
```

#### Adım 2: Environment Variables Ekle
`backend/.env` dosyasına ekle:

```env
CLOUDFLARE_API_TOKEN=your_api_token_here
CLOUDFLARE_ZONE_ID=your_zone_id_here
CLOUDFLARE_DOMAIN=signaling.videocall.app
```

#### Adım 3: Zone ID'yi Bul
1. Cloudflare Dashboard → Domain seç
2. Sağ tarafta **Zone ID** görünecek
3. Kopyala ve `.env` dosyasına ekle

#### Adım 4: DNS Kaydı Oluştur (İlk Kez)
Cloudflare Dashboard'dan:
1. **DNS** → **Records**
2. **Add record**
3. Type: **A**
4. Name: `signaling` (veya istediğin subdomain)
5. IPv4: Şu anki public IP (176.33.104.214)
6. Proxy: **DNS only** (turuncu bulut kapalı - WebSocket için)
7. Save

#### Adım 5: DDNS Client'ı Çalıştır
```powershell
cd backend
node scripts/cloudflare-ddns.js
```

#### Adım 6: Otomatik Başlatma (Windows Service)
`backend/scripts/cloudflare-ddns-service.js` oluştur ve Windows Task Scheduler ile her 5 dakikada bir çalıştır.

### Sonuç
- `ws://signaling.videocall.app:8080/ws` → WebSocket bağlantısı
- Port forwarding **GEREKLİ** (modem/router'da 8080 ve 3000 portları)

---

## Hangi Seçeneği Seçmeliyim?

### Cloudflare Tunnel Seç Eğer:
- ✅ Port forwarding yapmak istemiyorsan
- ✅ HTTPS istiyorsan (otomatik)
- ✅ Daha güvenli bir çözüm istiyorsan
- ✅ Kolay kurulum istiyorsan

### API + DDNS Seç Eğer:
- ✅ Port forwarding yapabilirsin
- ✅ Doğrudan IP bağlantısı istiyorsan
- ✅ Daha fazla kontrol istiyorsan

---

## Test

### Cloudflare Tunnel:
```powershell
# Tunnel durumunu kontrol et
.\cloudflared.exe tunnel list

# Tunnel loglarını gör
.\cloudflared.exe tunnel info videocall-signaling
```

### API + DDNS:
```powershell
# DNS kaydını kontrol et
nslookup signaling.videocall.app

# Public IP ile karşılaştır
curl https://api.ipify.org
```

---

## Sorun Giderme

### Tunnel Bağlanmıyor
- Cloudflare hesabını kontrol et
- `config.yml` dosyasını kontrol et
- Tunnel ID'yi kontrol et

### DNS Güncellenmiyor
- API Token'ı kontrol et
- Zone ID'yi kontrol et
- Cloudflare Dashboard'dan manuel kontrol et

### Port Forwarding Çalışmıyor
- Modem/router ayarlarını kontrol et
- Windows Firewall'ı kontrol et
- Public IP değişmiş olabilir (DDNS kontrol et)

