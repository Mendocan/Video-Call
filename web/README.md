# 🖥️ Video Call Desktop Application

Electron ile geliştirilmiş desktop görüntülü görüşme uygulaması.

## 🚀 Özellikler

- ✅ Görüntülü görüşme (WebRTC)
- ✅ Sesli görüşme
- ✅ Ekran paylaşımı
- ✅ Chat mesajlaşma
- ✅ Dosya paylaşımı
- ✅ Kişi yönetimi
- ✅ Türkçe/İngilizce dil desteği
- ✅ Cross-platform (Windows, macOS, Linux)

## 📋 Gereksinimler

- Node.js 18+
- npm veya yarn

## 🔧 Kurulum

```bash
cd web
npm install
```

## 🛠️ Geliştirme

### Desktop Uygulaması (Electron)
```bash
npm run electron:dev
```

Bu komut:
1. Next.js dev server'ı başlatır (http://localhost:3000)
2. Server hazır olunca Electron'u açar

### Sadece Web (Next.js)
```bash
npm run dev
```

## 📦 Production Build

### Windows
```bash
npm run electron:build:win
```
Çıktı: `dist/Video Call Setup.exe`

### macOS
```bash
npm run electron:build:mac
```
Çıktı: `dist/Video Call.dmg`

### Linux
```bash
npm run electron:build:linux
```
Çıktı: `dist/Video Call.AppImage`

## 📁 Proje Yapısı

```
web/
├── electron/
│   ├── main.js          # Electron main process
│   └── preload.js       # Preload script (güvenlik)
├── app/
│   ├── call/            # Görüşme sayfası
│   ├── contacts/        # Kişiler sayfası
│   ├── login/           # Giriş sayfası
│   └── register/        # Kayıt sayfası
├── components/           # React bileşenleri
├── contexts/            # React context'ler
├── lib/
│   ├── signaling/       # Signaling client
│   ├── webrtc/          # WebRTC client
│   └── i18n/            # Dil dosyaları
└── public/              # Static dosyalar
```

## 🌐 Sayfalar

- `/` - Ana sayfa (otomatik yönlendirme)
- `/call` - Görüşme sayfası
- `/contacts` - Kişiler sayfası
- `/login` - Giriş sayfası
- `/register` - Kayıt sayfası

## 🔐 Güvenlik

- Context Isolation aktif
- Node Integration kapalı
- Preload script ile güvenli IPC
- HTTPS/WSS bağlantıları

## 📝 Notlar

- Development modunda Next.js dev server ayrı çalışır
- Production build'de Next.js standalone mode kullanılır
- Electron main process Next.js server'ı spawn eder
- Signaling server: `wss://signaling.videocall.com/ws` (varsayılan)

## 🐛 Sorun Giderme

### Port 3000 zaten kullanılıyor
```bash
# Windows
netstat -ano | findstr :3000
taskkill /PID <PID> /F

# macOS/Linux
lsof -ti:3000 | xargs kill
```

### Electron açılmıyor
1. Next.js dev server'ın çalıştığından emin olun
2. `http://localhost:3000` adresine erişebildiğinizi kontrol edin
3. Console'da hata mesajlarını kontrol edin

## 📄 Lisans

Özel proje - Tüm hakları saklıdır.
