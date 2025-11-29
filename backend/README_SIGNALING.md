# WebRTC Signaling Server

## 📡 Açıklama

Bu sunucu, mobil veri üzerinden telefonları eşleştiren WebRTC signaling sunucusudur. Telefonlar room code ile birbirine bağlanır ve WebRTC bağlantısı kurulur.

## 🚀 Kurulum

### 1. Bağımlılıkları Yükle

```bash
cd backend
npm install
```

### 2. Ortam Değişkenlerini Ayarla

`.env` dosyasına ekleyin:

```env
SIGNALING_PORT=8080
```

### 3. Sunucuyu Başlat

**Geliştirme modu:**
```bash
npm run dev:signaling
```

**Production modu:**
```bash
node src/signalingServer.js
```

## 📋 Nasıl Çalışır?

### 1. İki Kişilik Görüşme (1-1)

**Telefon A (Arayan):**
- `startCall()` çağrılır
- Room code oluşturulur (örn: "ABC123")
- `wss://signaling.videocall.dev/ws?room=ABC123` adresine bağlanır
- Offer (SDP) gönderir

**Telefon B (Aranan):**
- `joinCall()` çağrılır
- Aynı room code kullanılır (örn: "ABC123")
- `wss://signaling.videocall.dev/ws?room=ABC123` adresine bağlanır
- Offer alır, Answer (SDP) gönderir

### 2. Grup Görüşme (3+ Kişi)

**Telefon A (Grup Kurucusu):**
- `startCall()` çağrılır
- Room code oluşturulur (örn: "ABC123")
- `wss://signaling.videocall.dev/ws?room=ABC123` adresine bağlanır

**Telefon B, C, D... (Katılımcılar):**
- `joinCall()` çağrılır
- Aynı room code kullanılır (örn: "ABC123")
- `wss://signaling.videocall.dev/ws?room=ABC123` adresine bağlanır
- Her katılımcı kendi peer connection'ını yönetir

**Sunucu:**
- Aynı room code'a sahip **TÜM** telefonları eşleştirir
- SDP (offer/answer) ve ICE candidate mesajlarını **TÜM** katılımcılara iletir
- Medya doğrudan telefonlar arasında akar (sunucu geçmez)

### 3. Ağ Desteği

**Wi-Fi:**
- Ev ağı
- İşyeri ağı
- Ortak Wi-Fi ağları
- WebRTC otomatik olarak en iyi bağlantı yolunu seçer

**Mobil Veri (Operatör Internet):**
- 4G/5G bağlantıları
- Farklı operatörler arası görüşme
- WebRTC STUN/TURN sunucuları ile NAT geçişi

## 🔧 API

### WebSocket Endpoint
```
wss://signaling.videocall.dev/ws?room=ROOM_CODE
```

### Health Check
```
GET http://signaling.videocall.dev/health
```

**Yanıt:**
```json
{
  "status": "ok",
  "timestamp": "2025-01-15T10:30:00.000Z",
  "activeRooms": 5,
  "activeConnections": 10,
  "totalParticipants": 15,
  "features": {
    "groupCall": true,
    "wifiSupport": true,
    "mobileDataSupport": true,
    "chat": true,
    "fileShare": true
  }
}
```

## 📨 Mesaj Formatları

### Offer (SDP)
```json
{
  "type": "offer",
  "sdp": "v=0\r\no=- 1234567890 1234567890 IN IP4 0.0.0.0\r\n..."
}
```

### Answer (SDP)
```json
{
  "type": "answer",
  "sdp": "v=0\r\no=- 9876543210 9876543210 IN IP4 0.0.0.0\r\n..."
}
```

### ICE Candidate
```json
{
  "type": "ice-candidate",
  "candidate": "candidate:1 1 UDP 2130706431 192.168.1.100 54321 typ host",
  "sdpMid": "0",
  "sdpMLineIndex": 0
}
```

### Chat
```json
{
  "type": "chat",
  "message": "Merhaba!",
  "senderPhoneNumber": "+905551234567",
  "senderName": "Ahmet"
}
```

### Participant Info
```json
{
  "type": "participant-info",
  "phoneNumber": "+905551234567",
  "participantId": "participant_123",
  "name": "Ahmet Yılmaz"
}
```

### Get Participants
```json
{
  "type": "get-participants"
}
```

**Yanıt:**
```json
{
  "type": "participants-list",
  "participants": [
    {
      "participantId": "participant_123",
      "phoneNumber": "+905551234567",
      "name": "Ahmet Yılmaz",
      "joinedAt": "2025-01-15T10:30:00.000Z"
    }
  ],
  "count": 1
}
```

### Leave
```json
{
  "type": "leave"
}
```

## 🌐 Hosting

### Production Deployment

1. **Sunucu Gereksinimleri:**
   - Node.js 18+
   - WebSocket desteği
   - SSL sertifikası (WSS için)

2. **Domain Ayarları:**
   - `signaling.videocall.dev` → Sunucu IP'sine yönlendir
   - SSL sertifikası kur (Let's Encrypt önerilir)

3. **PM2 ile Çalıştırma:**
   ```bash
   pm2 start src/signalingServer.js --name signaling-server
   pm2 save
   pm2 startup
   ```

4. **Nginx Reverse Proxy (Önerilen):**
   ```nginx
   upstream signaling {
       server localhost:8080;
   }

   server {
       listen 443 ssl;
       server_name signaling.videocall.dev;

       ssl_certificate /path/to/cert.pem;
       ssl_certificate_key /path/to/key.pem;

       location /ws {
           proxy_pass http://signaling;
           proxy_http_version 1.1;
           proxy_set_header Upgrade $http_upgrade;
           proxy_set_header Connection "upgrade";
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
           proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
           proxy_set_header X-Forwarded-Proto $scheme;
       }

       location /health {
           proxy_pass http://signaling;
       }
   }
   ```

## 🔒 Güvenlik

- Rate limiting eklenebilir
- Authentication token kontrolü eklenebilir
- IP whitelist eklenebilir
- DDoS koruması (Cloudflare önerilir)

## 📊 Monitoring

- Health check endpoint'i ile sunucu durumu kontrol edilebilir
- PM2 monitoring ile process durumu izlenebilir
- Log dosyaları ile hata takibi yapılabilir

## 🐛 Sorun Giderme

### Bağlantı Kurulamıyor
- SSL sertifikası kontrol edin
- Firewall ayarlarını kontrol edin
- Port 8080'in açık olduğundan emin olun

### Mesajlar İletilmiyor
- Room code'ların eşleştiğinden emin olun
- WebSocket bağlantısının açık olduğunu kontrol edin
- Log dosyalarını kontrol edin

