# Video Call - Fiyatlandırma Stratejisi

## Genel Bakış

Video Call uygulaması **Tek Ürün, Tek Fiyat, Farklı Süreler** modeli ile çalışmaktadır. Bu model, tek bir özelliğe (güvenli video görüşme) sahip uygulamalar için daha uygun ve basit bir yaklaşımdır.

## Fiyatlandırma Planları

### Tek Ürün Özellikleri

Tüm planlarda aynı özellikler sunulur:
- Sınırsız video görüşme
- End-to-end şifreleme
- Sunucusuz P2P bağlantı (hiçbir sunucuda tutulmaz)
- QR kod ile kolay bağlantı
- Rehber entegrasyonu
- Sınırsız katılımcı
- Sınırsız görüşme geçmişi
- 7/24 teknik destek

**Değer Önerisi:**
Özel. Güvenli. Gizli. Görüşmeleriniz hiçbir sunucuda tutulmaz.

### 1. 3 Aylık Abonelik

**Fiyat:** 450 TRY (Aylık: 150 TRY)

**Süre:** 3 ay

**Avantajlar:**
- Kısa süreli kullanım için ideal
- Düşük başlangıç maliyeti
- Esnek yenileme

### 2. 6 Aylık Abonelik

**Fiyat:** 750 TRY (Aylık: 125 TRY)

**Süre:** 6 ay

**İndirim:** %17 (3 aylık plana göre)

**Avantajlar:**
- 1 ay bedava kullanım
- Orta vadeli taahhüt
- Daha iyi değer

### 3. Yıllık Abonelik

**Fiyat:** 1.200 TRY (Aylık: 100 TRY)

**Süre:** 12 ay

**İndirim:** %33 (3 aylık plana göre)

**Avantajlar:**
- 4 ay bedava kullanım
- En iyi değer
- Uzun vadeli kullanıcılar için ideal

## Fiyatlandırma Stratejisi

### Neden Bu Model?

1. **Tek Ürün, Tek Fiyat Mantığı:**
   - Uygulamanın tek bir özelliği var: Güvenli video görüşme
   - Freemium/premium karmaşıklığı gereksiz
   - Basit ve anlaşılır fiyatlandırma

2. **Süre Bazlı Fiyatlandırma:**
   - Kullanıcılar ihtiyaçlarına göre süre seçebilir
   - Uzun süreli taahhütler için indirim
   - Esnek yenileme seçenekleri

3. **Sunucusuz Teknoloji Değeri:**
   - Görüşmeler hiçbir sunucuda tutulmaz
   - Peer-to-peer bağlantı ile tam gizlilik
   - Bu özellik, diğer platformlardan (Zoom, Teams, WhatsApp) ayıran temel değer

4. **Pazar Araştırması:**
   - Zoom: ~$15/ay (kurumsal)
   - Microsoft Teams: ~$6-12/ay
   - Signal: Ücretsiz (ama sunucu kullanıyor)
   - Bizim farkımız: Sunucusuz + Tek ürün odaklı

5. **Türkiye Pazarı:**
   - Aylık bazda 100-150 TRY aralığı
   - Türkiye'deki orta gelirli kullanıcılar için erişilebilir
   - Farklı bütçelere uygun seçenekler

### Rekabet Avantajları

1. **Sunucusuz Mimari:**
   - Diğer platformlar sunucu kullanıyor
   - Bizim görüşmelerimiz doğrudan cihazlar arasında
   - Bu, gizlilik ve güvenlik açısından büyük avantaj

2. **Basit Fiyatlandırma:**
   - Tek ürün, tek fiyat
   - Karmaşık planlar yok
   - Net ve anlaşılır

3. **Şeffaf Fiyatlandırma:**
   - Gizli ücret yok
   - Net özellik listesi
   - Kolay iptal

## Abonelik Süresi Sonrası Stratejisi

### Sorun: Abonelik Bitince Ne Olur?

**Çözüm: Abonelik Gerekliliği**

1. **Abonelik Bitişi:**
   - Abonelik bitince uygulama kullanılamaz
   - Yeni görüşme başlatılamaz
   - Mevcut görüşmeler sonlandırılır

2. **Veri Güvenliği:**
   - Görüşmeler zaten sunucuda tutulmadığı için veri kaybı yok
   - Cihazda saklanan görüşme geçmişi korunur
   - Kullanıcı verileri güvende

3. **Yenileme Süreci:**
   - Kullanıcıya bildirim gönderilir (30 gün önce, 7 gün önce, 1 gün önce)
   - Uygulama içinde kolay yenileme
   - Ödeme işlemi tamamlandığında anında aktif

4. **Kullanıcı Deneyimi:**
   - Abonelik bitince uygulama kullanılamaz (açık ve net)
   - Yenileme kolay ve hızlı
   - Veri kaybı yok

## Teknik Altyapı Gereksinimleri

### 1. Backend API

**Gereksinimler:**
- Abonelik yönetimi (oluşturma, kontrol, iptal)
- Kullanıcı kimlik doğrulama
- Ödeme entegrasyonu (Stripe, PayPal, yerel ödeme sistemleri)
- Bildirim sistemi (e-posta, push notification)

**Mevcut Durum:**
- ✅ Temel API yapısı hazır
- ✅ Abonelik kontrol endpoint'leri
- ⏳ Ödeme entegrasyonu (TODO)
- ⏳ Bildirim sistemi (TODO)

### 2. Database

**Gereksinimler:**
- Kullanıcı veritabanı
- Abonelik veritabanı
- Ödeme geçmişi
- Kullanım metrikleri

**Önerilen Yapı:**
```sql
users (
  id, phone_number, created_at, updated_at
)

subscriptions (
  id, user_id, plan_id, status, created_at, expires_at, payment_method
)

payments (
  id, subscription_id, amount, currency, status, created_at
)

usage_metrics (
  id, user_id, date, call_minutes, call_count
)
```

### 3. Android Uygulaması

**Gereksinimler:**
- Abonelik durumu kontrolü
- Günlük kullanım limiti kontrolü
- Premium özellik kilitleme
- Abonelik yenileme UI

**Mevcut Durum:**
- ✅ PreferencesManager (kullanıcı verileri)
- ⏳ SubscriptionManager (TODO)
- ⏳ UsageTracker (TODO)
- ⏳ Premium feature locks (TODO)

### 4. Ödeme Entegrasyonu

**Seçenekler:**
1. **Stripe:** Uluslararası, kredi kartı
2. **PayPal:** Uluslararası, PayPal hesabı
3. **iyzico:** Türkiye, kredi kartı, banka kartı
4. **PayTR:** Türkiye, kredi kartı, banka kartı

**Öneri:**
- Türkiye pazarı için: iyzico veya PayTR
- Uluslararası için: Stripe
- Her ikisini de desteklemek

## Uygulama Planı

### Faz 1: Temel Altyapı (Tamamlandı ✅)
- [x] Backend API yapısı
- [x] Fiyatlandırma planları (3, 6, 12 ay)
- [x] Abonelik kontrol endpoint'leri
- [x] Web pricing sayfası
- [x] Android subscription management

### Faz 2: Ödeme Entegrasyonu (Devam Ediyor ⏳)
- [ ] Stripe entegrasyonu
- [ ] iyzico entegrasyonu
- [ ] Ödeme sayfası
- [ ] Ödeme webhook'ları

### Faz 3: Android Entegrasyonu (Planlanıyor 📋)
- [ ] SubscriptionManager sınıfı
- [ ] Günlük kullanım takibi
- [ ] Premium özellik kilitleme
- [ ] Abonelik yenileme UI

### Faz 4: Bildirim Sistemi (Planlanıyor 📋)
- [ ] E-posta bildirimleri
- [ ] Push notification
- [ ] Abonelik yenileme hatırlatmaları

## Metrikler ve KPI'lar

### Takip Edilecek Metrikler:
1. **Dönüşüm Oranı:**
   - Ücretsiz → Premium geçiş oranı
   - Hedef: %5-10

2. **Gelir:**
   - Aylık/yıllık gelir
   - Ortalama kullanıcı değeri (ARPU)

3. **Kullanıcı Tutma:**
   - Abonelik yenileme oranı
   - Churn rate (kayıp oranı)

4. **Kullanım:**
   - Günlük aktif kullanıcılar (DAU)
   - Ortalama görüşme süresi
   - Premium kullanıcı kullanımı

## Sonuç

Video Call uygulaması, sunucusuz mimarisi ve gizlilik odaklı yaklaşımı ile pazarında benzersiz bir konuma sahiptir. Freemium + Premium modeli, kullanıcıların uygulamayı denemelerine ve premium özellikler için abonelik almalarına olanak tanır. Abonelik süresi sonrası otomatik freemium'a geçiş, kullanıcı deneyimini korurken yenileme için teşvik sağlar.

