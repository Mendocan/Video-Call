# 🚀 Deployment Guide - Signaling Server

> **💡 Yeni başlayanlar için:** Daha basit ve adım adım rehber için `BASIT_KURULUM_REHBERI.md` dosyasına bakın.

## 📋 Ön Gereksinimler

1. **Domain:** `videocall.com` (veya `signaling.videocall.com`)
2. **Sunucu:** 1 aylık VPS/Cloud sunucu (test için yeterli)
3. **Node.js:** 18+ versiyonu
4. **SSL Sertifikası:** WSS için gerekli (Let's Encrypt ücretsiz)

## 🏢 Şirket Bilgileri

- **Şirket Adı:** Tele Tek Dijital Hizmetler
- **Yetkili:** Nurcan K.
- **Mevcut Domain:** teletek.com.tr (zaten alınmış)
- **Yeni Domain:** videocall.com (alınabilir)

## 🔧 Adım Adım Kurulum

### 1. Domain Satın Al

**Domain:** `videocall.com` (alınabilir)

**Önerilen Domain Sağlayıcıları:**
- **Namecheap:** ~$10-15/yıl (önerilen)
- **GoDaddy:** ~$12-20/yıl
- **Türkiye:** Turhost, Natro (~50-100 TL/yıl)

**Not:** `teletek.com.tr` zaten alınmış, yeni domain olarak `videocall.com` kullanılacak.

### 2. Sunucu Kiralama

**Önerilen Sunucu Sağlayıcıları:**
- **DigitalOcean:** $6/ay (1GB RAM, 1 vCPU)
- **Linode:** $5/ay (1GB RAM, 1 vCPU)
- **Vultr:** $6/ay (1GB RAM, 1 vCPU)
- **Türkiye:** Turhost, Natro (yerel sunucular)

**Minimum Gereksinimler:**
- 1GB RAM
- 1 vCPU
- 25GB Disk
- Ubuntu 22.04 LTS

### 3. Sunucu Kurulumu

**SSH ile bağlan:**
```bash
ssh root@your-server-ip
```

**Node.js kurulumu:**
```bash
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs
node --version  # 18.x.x olmalı
```

**PM2 kurulumu (process manager):**
```bash
sudo npm install -g pm2
```

### 4. Domain DNS Ayarları

Domain sağlayıcınızın DNS ayarlarına gidin:

**A Record ekleyin:**
```
Type: A
Name: signaling
Value: YOUR_SERVER_IP
TTL: 3600
```

**Sonuç:** `signaling.videocall.com` → `YOUR_SERVER_IP`

**Alternatif (subdomain olmadan):**
```
Type: A
Name: @ (veya boş)
Value: YOUR_SERVER_IP
TTL: 3600
```

**Sonuç:** `videocall.com` → `YOUR_SERVER_IP` (ana domain)

### 5. SSL Sertifikası (Let's Encrypt) - ÜCRETSİZ! ✅

**ÖNEMLİ:** Let's Encrypt tamamen ücretsizdir!

**Let's Encrypt Nedir?**
- Tamamen ücretsiz SSL sertifikası
- 90 gün geçerli (otomatik yenilenebilir)
- Tüm büyük tarayıcılar tarafından güvenilir
- 1 aylık test için yeterli
- Production için de kullanılabilir

**Certbot kurulumu:**
```bash
sudo apt-get update
sudo apt-get install certbot
```

**SSL sertifikası al (ÜCRETSİZ):**
```bash
sudo certbot certonly --standalone -d signaling.videocall.com
```

**Alternatif (ana domain için):**
```bash
sudo certbot certonly --standalone -d videocall.com
```

**Not:** Certbot size e-posta adresi soracak (sadece yenileme bildirimleri için). İsteğe bağlı.

Sertifikalar şurada olacak:
- `/etc/letsencrypt/live/signaling.videocall.com/fullchain.pem`
- `/etc/letsencrypt/live/signaling.videocall.com/privkey.pem`

**Veya (ana domain için):**
- `/etc/letsencrypt/live/videocall.com/fullchain.pem`
- `/etc/letsencrypt/live/videocall.com/privkey.pem`

**Otomatik Yenileme (İsteğe Bağlı):**
```bash
# 90 günde bir otomatik yenileme için cron job ekle
sudo certbot renew --dry-run
```

### 6. Backend Kodlarını Yükle

**Sunucuya bağlan ve klasör oluştur:**
```bash
mkdir -p /opt/videocall-backend
cd /opt/videocall-backend
```

**Backend klasörünü yükle (SFTP veya Git):**
```bash
# SFTP ile (FileZilla, WinSCP)
# veya Git ile
git clone your-repo-url .
```

**Bağımlılıkları yükle:**
```bash
cd /opt/videocall-backend/backend
npm install
```

### 7. Ortam Değişkenleri

`.env` dosyası oluştur:
```bash
cd /opt/videocall-backend/backend
nano .env
```

İçeriği:
```env
SIGNALING_PORT=8080
NODE_ENV=production
```

### 8. Nginx Reverse Proxy

**Nginx kurulumu:**
```bash
sudo apt-get install nginx
```

**Nginx config oluştur:**
```bash
sudo nano /etc/nginx/sites-available/signaling
```

İçeriği:
```nginx
upstream signaling {
    server localhost:8080;
}

server {
    listen 80;
    server_name signaling.videocall.com;
    
    # HTTP'den HTTPS'e yönlendir
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name signaling.videocall.com;

    # SSL sertifikaları
    ssl_certificate /etc/letsencrypt/live/signaling.videocall.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/signaling.videocall.com/privkey.pem;
    
    # SSL ayarları
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # WebSocket endpoint
    location /ws {
        proxy_pass http://signaling;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket timeout
        proxy_read_timeout 86400;
    }

    # Health check endpoint
    location /health {
        proxy_pass http://signaling;
        proxy_set_header Host $host;
    }
}
```

**Nginx'i aktif et:**
```bash
sudo ln -s /etc/nginx/sites-available/signaling /etc/nginx/sites-enabled/
sudo nginx -t  # Test et
sudo systemctl reload nginx
```

### 9. Signaling Sunucusunu Başlat

**PM2 ile başlat:**
```bash
cd /opt/videocall-backend/backend
pm2 start src/signalingServer.js --name signaling-server
pm2 save
pm2 startup  # Otomatik başlatma için
```

**Durum kontrolü:**
```bash
pm2 status
pm2 logs signaling-server
```

### 10. Firewall Ayarları

**Portları aç:**
```bash
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw enable
```

## ✅ Test

### 1. Health Check
```bash
curl https://signaling.videocall.com/health
```

**Beklenen yanıt:**
```json
{
  "status": "ok",
  "timestamp": "...",
  "activeRooms": 0,
  "activeConnections": 0,
  "totalParticipants": 0,
  "features": {
    "groupCall": true,
    "wifiSupport": true,
    "mobileDataSupport": true,
    "chat": true,
    "fileShare": true
  }
}
```

### 2. Android Uygulaması

**local.properties dosyasını güncelle:**
```properties
SIGNALING_URL=wss://signaling.videocall.com/ws
```

**Uygulamayı derle ve test et:**
- Room code ile arama yap
- Mobil veri üzerinden test et
- Wi-Fi üzerinden test et
- Grup görüşme test et

## 🔍 Monitoring

### PM2 Monitoring
```bash
pm2 monit
```

### Logs
```bash
pm2 logs signaling-server
pm2 logs signaling-server --lines 100  # Son 100 satır
```

### Restart
```bash
pm2 restart signaling-server
```

## 🐛 Sorun Giderme

### Sunucuya bağlanamıyor
- Firewall ayarlarını kontrol et
- DNS ayarlarını kontrol et
- SSL sertifikasını kontrol et

### WebSocket bağlantısı kurulamıyor
- Nginx config'i kontrol et
- PM2 loglarını kontrol et
- Port 8080'in açık olduğundan emin ol

### Mesajlar iletilmiyor
- Room code'ların eşleştiğinden emin ol
- PM2 loglarını kontrol et
- Network bağlantısını kontrol et

## 📊 Maliyet Tahmini (1 Aylık Test)

- **Domain:** ~$10-15/yıl (ilk yıl genelde daha ucuz)
- **Sunucu:** ~$5-6/ay
- **SSL:** Ücretsiz (Let's Encrypt)
- **Toplam:** ~$15-20 (ilk ay)

## 🎯 Sonuç

Sunucu hazır! Android uygulaması `wss://signaling.videocall.com/ws` adresine bağlanabilir.

**Önemli:** 
- Sunucu çalışmazsa proje çalışmaz
- 1 aylık test süresi yeterli
- Başarılı olursa production'a geç
- Başarısız olursa alternatif çözümler düşün

