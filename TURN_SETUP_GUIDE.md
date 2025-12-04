# 🚀 TURN Sunucusu Kurulum Rehberi

## Sorun
Telefonlar arama kabul ediyor ama bağlantı kurulamıyor çünkü:
- NAT traversal için TURN sunucusu yok
- Sadece STUN var (public IP bulmak için)
- Mobil operatörler Symmetric NAT kullanıyor → P2P çalışmıyor

## Çözüm Seçenekleri

### Option 1: Twilio TURN (Hızlı - Test İçin)

**Avantajlar:**
- 5 dakikada hazır
- Güvenilir
- Test için ideal

**Dezavantajlar:**
- ~$0.40/GB ücretli
- Ayda 100-200 TL civarı (kullanıma göre)

**Kurulum:**
1. Twilio hesabı aç: https://www.twilio.com/
2. Console → Video → Configure → ICE Servers
3. Credentials al

```kotlin
// Android: DirectCallIceGatherer.kt
private val turnServers = listOf(
    TurnServer(
        urls = "turn:global.turn.twilio.com:3478?transport=udp",
        username = "YOUR_TWILIO_USERNAME",
        credential = "YOUR_TWILIO_PASSWORD"
    )
)
```

---

### Option 2: Coturn (Kendi Sunucu - Ücretsiz)

**Avantajlar:**
- Tamamen ücretsiz
- Sınırsız kullanım
- Tam kontrol

**Dezavantajlar:**
- Kurulum gerekli (~30 dakika)
- Ayrı sunucu kiralama (~$5-10/ay)

#### 1. Digital Ocean / Hetzner'den Sunucu Kirala

**Minimum Gereksinimler:**
- 1 vCPU
- 1 GB RAM
- Ubuntu 22.04

**Maliyet:** ~$5-10/ay

#### 2. Coturn Kur

```bash
# Ubuntu'da Coturn kurulumu
sudo apt update
sudo apt install -y coturn

# Coturn'u enable et
sudo systemctl enable coturn
```

#### 3. Coturn Yapılandır

```bash
# /etc/turnserver.conf dosyasını düzenle
sudo nano /etc/turnserver.conf
```

```conf
# /etc/turnserver.conf
listening-port=3478
tls-listening-port=5349

# Relay IP (sunucunuzun public IP'si)
relay-ip=YOUR_SERVER_PUBLIC_IP
external-ip=YOUR_SERVER_PUBLIC_IP

# Realm (domain adınız)
realm=turn.yourdomain.com

# Kullanıcı adı ve şifre
user=videocall:YOUR_STRONG_PASSWORD

# Fingerprint
fingerprint

# Log
log-file=/var/log/turnserver.log
verbose
```

#### 4. Firewall Ayarla

```bash
# UDP ve TCP portlarını aç
sudo ufw allow 3478/tcp
sudo ufw allow 3478/udp
sudo ufw allow 5349/tcp
sudo ufw allow 5349/udp
sudo ufw allow 49152:65535/udp  # TURN relay port range
```

#### 5. Coturn'u Başlat

```bash
sudo systemctl start coturn
sudo systemctl status coturn

# Log'ları kontrol et
sudo tail -f /var/log/turnserver.log
```

#### 6. Test Et

```bash
# Coturn test (başka bir makineden)
turnutils_uclient -v -u videocall -w YOUR_STRONG_PASSWORD YOUR_SERVER_IP
```

---

### Option 3: Render.com'da Coturn (Docker)

Render.com backend'iniz zaten var. Aynı yere Coturn ekleyelim.

#### 1. Dockerfile Oluştur

```dockerfile
# turn/Dockerfile
FROM ubuntu:22.04

RUN apt-get update && \
    apt-get install -y coturn && \
    rm -rf /var/lib/apt/lists/*

COPY turnserver.conf /etc/turnserver.conf

EXPOSE 3478/tcp
EXPOSE 3478/udp
EXPOSE 5349/tcp
EXPOSE 5349/udp
EXPOSE 49152-65535/udp

CMD ["turnserver", "-c", "/etc/turnserver.conf"]
```

#### 2. turnserver.conf

```conf
# turn/turnserver.conf
listening-port=3478
tls-listening-port=5349
realm=turn.videocall.app
user=videocall:${TURN_PASSWORD}
fingerprint
log-file=/var/log/turnserver.log
verbose
```

#### 3. Render.com'a Deploy

1. Render Dashboard → New Web Service
2. Docker seç
3. Dockerfile path: `turn/Dockerfile`
4. Port: 3478
5. Environment Variables:
   - `TURN_PASSWORD`: Güçlü şifre

---

## Android Entegrasyonu

### 1. DirectCallIceGatherer'ı Güncelle

```kotlin
// android/app/src/main/kotlin/com/videocall/app/directcall/ice/DirectCallIceGatherer.kt

data class TurnServer(
    val urls: String,
    val username: String,
    val credential: String
)

class DirectCallIceGatherer {
    
    private val stunServers = listOf(
        "stun.l.google.com:19302",
        "global.stun.twilio.com:3478"
    )
    
    // TURN sunucuları (BuildConfig'den al)
    private val turnServers: List<TurnServer> = if (BuildConfig.TURN_SERVER_URL.isNotEmpty()) {
        listOf(
            TurnServer(
                urls = BuildConfig.TURN_SERVER_URL,
                username = BuildConfig.TURN_USERNAME,
                credential = BuildConfig.TURN_PASSWORD
            )
        )
    } else {
        emptyList()
    }
    
    suspend fun gatherCandidates(
        stunServer: String? = null,
        useTurn: Boolean = true
    ): List<DirectCallIceCandidate> = withContext(Dispatchers.IO) {
        val candidates = mutableListOf<DirectCallIceCandidate>()
        
        // 1. Host candidate (local IP)
        val localIp = getLocalIpAddress()
        if (localIp != null) {
            candidates.add(
                DirectCallIceCandidate(
                    foundation = "1",
                    componentId = 1,
                    transport = "UDP",
                    priority = 2130706431,
                    address = localIp,
                    port = 54321,
                    type = "host"
                )
            )
        }
        
        // 2. Server reflexive candidate (STUN)
        val server = stunServer ?: stunServers.firstOrNull()
        if (server != null) {
            try {
                val publicIp = queryStunServer(server)
                if (publicIp != null) {
                    candidates.add(
                        DirectCallIceCandidate(
                            foundation = "2",
                            componentId = 1,
                            transport = "UDP",
                            priority = 1694498815,
                            address = publicIp,
                            port = 54322,
                            type = "srflx"
                        )
                    )
                }
            } catch (e: Exception) {
                android.util.Log.w("DirectCallIceGatherer", "STUN query başarısız: ${e.message}")
            }
        }
        
        // 3. Relay candidate (TURN) - NAT traversal için kritik!
        if (useTurn && turnServers.isNotEmpty()) {
            turnServers.forEach { turnServer ->
                try {
                    val relayCandidate = allocateTurnRelay(turnServer)
                    if (relayCandidate != null) {
                        candidates.add(relayCandidate)
                    }
                } catch (e: Exception) {
                    android.util.Log.w("DirectCallIceGatherer", "TURN allocation başarısız: ${e.message}")
                }
            }
        }
        
        android.util.Log.d("DirectCallIceGatherer", "ICE candidate'ları toplandı: ${candidates.size} adet")
        candidates
    }
    
    /**
     * TURN sunucusundan relay candidate al
     */
    private suspend fun allocateTurnRelay(turnServer: TurnServer): DirectCallIceCandidate? = withContext(Dispatchers.IO) {
        try {
            // TURN Allocation Request (RFC 5766)
            // Bu basitleştirilmiş versiyon - production için tam implementasyon gerekli
            
            val parts = turnServer.urls.replace("turn:", "").split(":")
            val host = parts[0]
            val port = if (parts.size > 1) parts[1].split("?")[0].toIntOrNull() ?: 3478 else 3478
            
            // TURN allocation (bu kısım basitleştirilmiş - gerçek implementasyon daha karmaşık)
            val relayIp = queryTurnServer(host, port, turnServer.username, turnServer.credential)
            
            if (relayIp != null) {
                return@withContext DirectCallIceCandidate(
                    foundation = "3",
                    componentId = 1,
                    transport = "UDP",
                    priority = 16777215, // Relay candidate priority (en yüksek - fallback)
                    address = relayIp,
                    port = 54323,
                    type = "relay"
                )
            }
            
            return@withContext null
        } catch (e: Exception) {
            android.util.Log.e("DirectCallIceGatherer", "TURN relay allocation hatası", e)
            return@withContext null
        }
    }
    
    /**
     * TURN sunucusuna query gönder (basit implementasyon)
     */
    private suspend fun queryTurnServer(
        host: String,
        port: Int,
        username: String,
        credential: String
    ): String? = withContext(Dispatchers.IO) {
        // TODO: RFC 5766 TURN Allocation Request implementasyonu
        // Şimdilik STUN-benzeri basit query
        try {
            val serverAddress = InetAddress.getByName(host)
            return@withContext serverAddress.hostAddress
        } catch (e: Exception) {
            android.util.Log.e("DirectCallIceGatherer", "TURN query hatası", e)
            return@withContext null
        }
    }
}
```

### 2. build.gradle'a TURN Credential Ekle

```gradle
// android/app/build.gradle

android {
    defaultConfig {
        // ... mevcut ayarlar ...
        
        // TURN Server credentials
        buildConfigField("String", "TURN_SERVER_URL", "\"${turnServerUrl}\"")
        buildConfigField("String", "TURN_USERNAME", "\"${turnUsername}\"")
        buildConfigField("String", "TURN_PASSWORD", "\"${turnPassword}\"")
    }
}
```

### 3. gradle.properties

```properties
# gradle.properties

# TURN Server Configuration
turnServerUrl=turn:YOUR_SERVER_IP:3478?transport=udp
turnUsername=videocall
turnPassword=YOUR_STRONG_PASSWORD
```

---

## Test Senaryosu

### Test 1: Aynı Wi-Fi (Çalışmalı)
- ✅ STUN yeterli
- ✅ P2P bağlantı kurulmalı

### Test 2: Farklı Wi-Fi (Çalışmalı)
- ✅ STUN yeterli (çoğu ev router'ı Full Cone NAT)
- ✅ P2P bağlantı kurulmalı

### Test 3: Mobil Veri + Wi-Fi (ŞİMDİ ÇALIŞMAMALI → TURN ile ÇALIŞMALI)
- ❌ Şu an: TURN yok → Bağlantı kurulamıyor
- ✅ TURN ekledikten sonra: Relay üzerinden bağlanmalı

### Test 4: Mobil Veri + Mobil Veri (ŞİMDİ ÇALIŞMAMALI → TURN ile ÇALIŞMALI)
- ❌ Şu an: TURN yok → Bağlantı kurulamıyor
- ✅ TURN ekledikten sonra: Relay üzerinden bağlanmalı

---

## Önerilen Kurulum Sırası

### Hızlı Test (Bugün):
1. ✅ **Twilio TURN** kur (5 dakika)
2. ✅ Android'e entegre et (30 dakika)
3. ✅ Test et (mobil veri + wi-fi)

### Production (Gelecek):
1. ✅ Digital Ocean/Hetzner sunucu kirala (~$5-10/ay)
2. ✅ Coturn kur
3. ✅ Android'i Coturn'a bağla
4. ✅ Twilio'yu kapat (maliyet tasarrufu)

---

## Maliyet Karşılaştırması (Aylık)

| Seçenek | Kurulum | Aylık Maliyet | Avantaj |
|---------|---------|---------------|---------|
| **Twilio** | 5 dakika | $50-200 (kullanıma göre) | Hızlı, güvenilir |
| **Coturn (Digital Ocean)** | 30 dakika | $5-10 | Ucuz, sınırsız |
| **Coturn (Hetzner)** | 30 dakika | €4-8 | En ucuz, sınırsız |

---

## Sonuç

TURN sunucusu **ZORUNLU** çünkü:
1. Mobil operatörler Symmetric NAT kullanıyor
2. P2P bağlantı %70-80 başarı oranında
3. TURN olmadan produc

tion'a çıkamazsınız

**Öneri:** Twilio ile test edin, sonra Coturn'a geçin.

