# Video Call Backend API

Backend API for Video Call application pricing and subscription management.

## Kurulum

```bash
npm install
```

## Çalıştırma

Development:
```bash
npm run dev
```

Production:
```bash
npm start
```

## API Endpoints

### Health Check
- `GET /api/health` - Server durumu

### Pricing
- `GET /api/pricing` - Tüm fiyatlandırma planları
- `GET /api/pricing/:planId` - Belirli bir plan (monthly/yearly)

### Subscription
- `POST /api/subscribe` - Yeni abonelik oluştur

## Fiyatlandırma

- **Aylık Plan**: 99 TRY/ay
- **Yıllık Plan**: 990 TRY/yıl (%17 indirim)

