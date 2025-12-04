# Kayıt Formatları ve Dosya Yapısı

## Genel Bakış

Uygulama, yazılı (chat), sesli ve görüntülü görüşmeleri kaydetme özelliğine sahiptir. Her kayıt türü için farklı dosya formatları ve kayıt hedefleri kullanılmaktadır.

## Kayıt Türleri ve Formatları

### 1. Yazılı (Chat) Kayıtları

**Format:** Plain Text (`.txt`)
**Encoding:** UTF-8
**Konum:** `Android/data/com.videocall.app/files/recordings/chat/`
**Dosya Adı Formatı:** `chat_{phoneNumber}_{timestamp}.txt`

**İçerik Yapısı:**
```
=== Chat Kaydı ===
Kişi: {Contact Name}
Telefon: {Phone Number}
Tarih: {Date}
Saat: {Time}
==================

[{Timestamp}] {Sender Name}: {Message}
[{Timestamp}] {Sender Name}: {Message}
...
```

**Örnek:**
```
=== Chat Kaydı ===
Kişi: Ahmet Yılmaz
Telefon: +905551234567
Tarih: 2025-01-15
Saat: 14:30:00
==================

[14:30:15] Ben: Merhaba!
[14:30:20] Ahmet Yılmaz: Selam, nasılsın?
[14:30:25] Ben: İyiyim, teşekkürler
```

### 2. Sesli Görüşme Kayıtları

**Format:** AAC (`.m4a`)
**Codec:** AAC-LC
**Sample Rate:** 44100 Hz
**Bitrate:** 128 kbps
**Channels:** Mono (1 kanal)
**Konum:** `Android/data/com.videocall.app/files/recordings/audio/`
**Dosya Adı Formatı:** `call_{phoneNumber}_{timestamp}.m4a`

**Teknik Detaylar:**
- Android MediaRecorder API kullanılarak kaydedilir
- AAC formatı, yüksek kalite ve küçük dosya boyutu sağlar
- Mono kanal, telefon görüşmeleri için yeterlidir

### 3. Görüntülü Görüşme Kayıtları

**Format:** MP4 (`.mp4`)
**Video Codec:** H.264 (AVC)
**Audio Codec:** AAC
**Video Resolution:** 1280x720 (HD)
**Frame Rate:** 30 fps
**Video Bitrate:** 2 Mbps
**Audio Sample Rate:** 44100 Hz
**Audio Bitrate:** 128 kbps
**Konum:** `Android/data/com.videocall.app/files/recordings/video/`
**Dosya Adı Formatı:** `video_call_{phoneNumber}_{timestamp}.mp4`

**Teknik Detaylar:**
- Android MediaMuxer API kullanılarak kaydedilir
- Video ve ses track'leri ayrı ayrı encode edilip birleştirilir
- H.264 codec, yaygın uyumluluk sağlar

## Kayıt Göstergesi

### Yeşil Nokta (🟢)
- **Anlamı:** Kayıt yapılmıyor
- **Durum:** Normal görüşme/chat durumu
- **Gösterim:** Telefonun üst kısmında, bağlantı durumunun yanında

### Kırmızı Nokta (🔴)
- **Anlamı:** Kayıt yapılıyor
- **Durum:** Kendi cihazımız veya karşı taraf kayıt yapıyor
- **Gösterim:** Telefonun üst kısmında, bağlantı durumunun yanında
- **Bildirim:** Karşı taraf kayıt yapıyorsa, `RecordingStatus` mesajı ile bildirilir

## Kayıt Durumu Bildirimi

Uygulama, kayıt durumunu karşı tarafa bildirmek için `RecordingStatus` mesajı kullanır:

```json
{
  "type": "recording-status",
  "isRecording": true,
  "senderPhoneNumber": "+905551234567",
  "roomCode": "room_abc123"
}
```

- `isRecording: true` → Kırmızı nokta gösterilir
- `isRecording: false` → Yeşil nokta gösterilir

## Dosya Yönetimi

### Kayıt Klasör Yapısı
```
Android/data/com.videocall.app/files/
└── recordings/
    ├── chat/
    │   └── chat_{phoneNumber}_{timestamp}.txt
    ├── audio/
    │   └── call_{phoneNumber}_{timestamp}.m4a
    └── video/
        └── video_call_{phoneNumber}_{timestamp}.mp4
```

### Dosya Boyut Tahminleri

**Chat (Text):**
- Ortalama: 1-10 KB (100 mesaj için)
- Maksimum: ~100 KB (1000 mesaj için)

**Sesli Görüşme (AAC):**
- 1 dakika: ~1 MB
- 10 dakika: ~10 MB
- 1 saat: ~60 MB

**Görüntülü Görüşme (MP4):**
- 1 dakika: ~15 MB
- 10 dakika: ~150 MB
- 1 saat: ~900 MB

## Gizlilik ve Güvenlik

1. **Kayıt Bildirimi:** Tüm kayıtlar, karşı tarafa bildirilir (yeşil/kırmızı nokta)
2. **Yerel Depolama:** Kayıtlar sadece cihazda saklanır, sunucuya gönderilmez
3. **Şifreleme:** Kayıtlar şifrelenmez (yerel dosya sistemi)
4. **Silme:** Kullanıcı istediği zaman kayıtları silebilir

## Kullanıcı Deneyimi

- Kayıt başladığında otomatik olarak başlatılır
- Kayıt durumu her zaman görünür (yeşil/kırmızı nokta)
- Kayıt tamamlandığında kullanıcıya bildirim gösterilir
- Kayıtlar, görüşme geçmişinden erişilebilir

