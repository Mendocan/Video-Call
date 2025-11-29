# 🖥️ Video Call Desktop Application

Electron ile desktop uygulaması.

## 🚀 Geliştirme

### Gereksinimler
- Node.js 18+
- npm veya yarn

### Kurulum
```bash
cd web
npm install
```

### Geliştirme Modu
```bash
# Next.js dev server + Electron'u birlikte başlat
npm run electron:dev
```

Bu komut:
1. Next.js dev server'ı başlatır (http://localhost:3000)
2. Server hazır olunca Electron'u açar

### Sadece Next.js (Web)
```bash
npm run dev
```

### Sadece Electron (Build sonrası)
```bash
npm run build
npm run electron
```

## 📦 Build (Production)

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
Çıktı: `dist/Video Call.AppImage` ve `dist/videocall-desktop.deb`

### Tüm Platformlar
```bash
npm run electron:build
```

## 📁 Dosya Yapısı

```
web/
├── electron/
│   ├── main.js          # Electron main process
│   └── preload.js       # Preload script (güvenlik)
├── app/                 # Next.js app
├── public/              # Static dosyalar
└── package.json
```

## 🔧 Yapılandırma

### Electron Ayarları
`package.json` içindeki `build` bölümünden:
- App ID
- Ürün adı
- Icon
- Installer ayarları

### Next.js Ayarları
`next.config.ts` içinde:
- `output: 'standalone'` (production build için)

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
2. `http://localhost:3000` adresine tarayıcıdan erişebildiğinizi kontrol edin
3. Console'da hata mesajlarını kontrol edin

### Build hatası
1. `npm run build` ile Next.js build'inin başarılı olduğunu kontrol edin
2. `out/` klasörünün oluştuğunu kontrol edin
3. Electron builder loglarını kontrol edin

## 📝 Notlar

- Development modunda Next.js dev server ayrı çalışır
- Production build'de Next.js standalone mode kullanılır
- Electron main process Next.js server'ı spawn eder
- Preload script güvenlik için contextIsolation kullanır

