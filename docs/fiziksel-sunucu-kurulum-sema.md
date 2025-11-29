# Fiziksel Sunucu Kurulum Şeması
## Rendezvous Servisi için Donanım ve Ağ Yapısı

---

## 1. DONANIM ŞEMASI

```
┌─────────────────────────────────────────────────────────┐
│                    FİZİKSEL SUNUCU                      │
│  ┌──────────────────────────────────────────────────┐  │
│  │         Mini PC / NUC veya Masaüstü PC           │  │
│  │  ┌──────────────────────────────────────────┐   │  │
│  │  │ CPU: Intel i5/i7 veya AMD Ryzen (4+ core)│   │  │
│  │  │ RAM: 8-16 GB DDR4                        │   │  │
│  │  │ SSD: 256-512 GB NVMe                     │   │  │
│  │  │ Ethernet: Gigabit port                   │   │  │
│  │  └──────────────────────────────────────────┘   │  │
│  └──────────────────────────────────────────────────┘  │
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │         İşletim Sistemi: Ubuntu Server 22.04    │  │
│  │  ┌──────────────────────────────────────────┐   │  │
│  │  │ Docker Engine                            │   │  │
│  │  │ Rendezvous Service (Node.js/Go)         │   │  │
│  │  │ Nginx (Reverse Proxy - Opsiyonel)       │   │  │
│  │  └──────────────────────────────────────────┘   │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

---

## 2. GÜÇ BAĞLANTI ŞEMASI

```
┌──────────────┐
│  Elektrik    │
│  Priz (220V) │
└──────┬───────┘
       │
       │ (220V AC)
       │
┌──────▼───────┐
│     UPS      │
│  (500-1000VA)│
│  ┌─────────┐ │
│  │ Batarya │ │
│  └─────────┘ │
└──────┬───────┘
       │
       │ (220V AC - Stabilize)
       │
┌──────▼───────┐
│  Güç Kaynağı │
│  (PSU)       │
│  300-500W    │
└──────┬───────┘
       │
       │ (12V DC)
       │
┌──────▼───────┐
│  Mini PC/PC  │
│  (Sunucu)    │
└──────────────┘
```

**Güç Tüketimi:**
- Mini PC: ~20-30W (idle), ~50W (peak)
- UPS: ~10-20W (şarj)
- Toplam: ~30-70W
- Aylık Elektrik: ~₺50-150 (kullanıma göre)

---

## 3. AĞ BAĞLANTI ŞEMASI

```
┌─────────────────────────────────────────────────────────┐
│                    İNTERNET                              │
│              (Operatör: TTNET/Turkcell vb.)              │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ (Fiber/ADSL)
                     │
┌────────────────────▼────────────────────────────────────┐
│                  MODEM/ROUTER                            │
│  ┌──────────────────────────────────────────────────┐   │
│  │ Statik IP: xxx.xxx.xxx.xxx (Operatörden talep)  │   │
│  │ Port Forwarding: 8080 → Sunucu IP:8080          │   │
│  └──────────────────────────────────────────────────┘   │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ (Ethernet - Cat 6)
                     │
┌────────────────────▼────────────────────────────────────┐
│              FİZİKSEL SUNUCU                             │
│  ┌──────────────────────────────────────────────────┐   │
│  │ Ubuntu Server 22.04                              │   │
│  │ IP: 192.168.1.100 (Yerel ağ)                    │   │
│  │ Public IP: xxx.xxx.xxx.xxx (İnternet)           │   │
│  │ Port: 8080 (Rendezvous Service)                  │   │
│  │ Port: 443 (HTTPS - Opsiyonel)                   │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

**Ağ Gereksinimleri:**
- Statik IP: Operatörden talep (₺50-200/ay ek ücret)
- Port Forwarding: Router'da 8080 portunu sunucuya yönlendir
- Firewall: Ubuntu UFW ile port açma

---

## 4. YAZILIM MİMARİSİ ŞEMASI

```
┌─────────────────────────────────────────────────────────┐
│              UBUNTU SERVER 22.04 LTS                    │
│                                                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │              DOCKER ENGINE                      │  │
│  │  ┌──────────────────────────────────────────┐   │  │
│  │  │     Rendezvous Service Container         │   │  │
│  │  │  ┌────────────────────────────────────┐  │   │  │
│  │  │  │ Node.js veya Go Application       │  │  │   │
│  │  │  │ Port: 8080                        │  │  │   │
│  │  │  │ - Telefon hash → IP eşleştirme    │  │  │   │
│  │  │  │ - Geçici kayıtlar (5 dk)          │  │  │   │
│  │  │  │ - Token doğrulama                 │  │  │   │
│  │  │  └────────────────────────────────────┘  │  │   │
│  │  └──────────────────────────────────────────┘   │  │
│  └──────────────────────────────────────────────────┘  │
│                                                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │         NGINX (Opsiyonel - Reverse Proxy)         │  │
│  │  - SSL/TLS terminasyonu                          │  │
│  │  - Load balancing (gelecekte)                    │  │
│  └──────────────────────────────────────────────────┘  │
│                                                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │         SYSTEMD (Servis Yönetimi)                 │  │
│  │  - Otomatik başlatma                             │  │
│  │  - Crash recovery                                │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

---

## 5. KURULUM ADIMLARI ŞEMASI

```
ADIM 1: DONANIM KURULUMU
├── Mini PC/PC'yi fiziksel olarak yerleştir
├── UPS'e bağla
├── Ethernet kablosunu bağla
└── Güç kablosunu tak

ADIM 2: İŞLETİM SİSTEMİ KURULUMU
├── Ubuntu Server 22.04 ISO'sunu USB'ye yaz
├── Sunucuya boot et
├── Kurulum yap (disk bölümleme, kullanıcı oluştur)
└── Ağ ayarlarını yap (statik IP - opsiyonel)

ADIM 3: TEMEL YAZILIM KURULUMU
├── Sistem güncellemeleri (apt update && apt upgrade)
├── Docker kurulumu
├── Git kurulumu
└── Firewall ayarları (UFW)

ADIM 4: RENDEZVOUS SERVİSİ KURULUMU
├── Servis kodunu klonla/git'ten çek
├── Docker image oluştur
├── Docker container'ı çalıştır
└── Systemd service dosyası oluştur (otomatik başlatma)

ADIM 5: AĞ YAPILANDIRMASI
├── Router'da port forwarding (8080 → sunucu IP:8080)
├── Statik IP ayarı (operatörden talep)
└── Firewall kuralları (UFW allow 8080)

ADIM 6: TEST VE DOĞRULAMA
├── Servis çalışıyor mu? (curl localhost:8080/health)
├── İnternet'ten erişilebilir mi? (public-ip:8080)
└── Android uygulamasından test et
```

---

## 6. MALİYET ŞEMASI

```
┌─────────────────────────────────────────────────────────┐
│              İLK YATIRIM (Tek Seferlik)                 │
├─────────────────────────────────────────────────────────┤
│ Mini PC/NUC:              ₺8.000 - ₺15.000            │
│ UPS (500-1000VA):         ₺1.500 - ₺3.000             │
│ Ethernet Kablosu:         ₺100 - ₺300                 │
│ USB Flash (Kurulum için):  ₺50 - ₺100                  │
├─────────────────────────────────────────────────────────┤
│ TOPLAM İLK YATIRIM:       ₺9.650 - ₺18.400            │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│              AYLIK MALİYET (Tekrarlayan)                │
├─────────────────────────────────────────────────────────┤
│ Elektrik:                 ₺50 - ₺150                   │
│ İnternet (Statik IP):     ₺50 - ₺200 (opsiyonel)       │
│ İnternet (Normal):        Zaten mevcut                 │
├─────────────────────────────────────────────────────────┤
│ TOPLAM AYLIK:             ₺100 - ₺350                  │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│              1 YILLIK TOPLAM MALİYET                    │
├─────────────────────────────────────────────────────────┤
│ İlk Yatırım:              ₺9.650 - ₺18.400            │
│ Aylık (12 ay):            ₺1.200 - ₺4.200             │
├─────────────────────────────────────────────────────────┤
│ TOPLAM (1 YIL):           ₺10.850 - ₺22.600            │
└─────────────────────────────────────────────────────────┘
```

---

## 7. GÜVENLİK ŞEMASI

```
┌─────────────────────────────────────────────────────────┐
│              GÜVENLİK KATMANLARI                        │
├─────────────────────────────────────────────────────────┤
│ 1. FİZİKSEL GÜVENLİK                                     │
│    ├── Sunucu güvenli bir yerde (kilitli oda)          │
│    ├── UPS ile elektrik kesintisi koruması             │
│    └── Soğutma (aşırı ısınma koruması)                 │
│                                                         │
│ 2. AĞ GÜVENLİĞİ                                         │
│    ├── Firewall (UFW) - Sadece gerekli portlar açık    │
│    ├── Fail2ban - Brute force koruması                  │
│    └── SSH key authentication (şifre yerine)           │
│                                                         │
│ 3. UYGULAMA GÜVENLİĞİ                                   │
│    ├── TLS/SSL şifreleme (HTTPS)                        │
│    ├── Token doğrulama                                  │
│    ├── Rate limiting (spam koruması)                    │
│    └── Input validation                                 │
│                                                         │
│ 4. VERİ GÜVENLİĞİ                                       │
│    ├── Veri depolamaz (sadece geçici eşleştirme)       │
│    ├── Log tutmaz (gizlilik)                            │
│    └── 5 dakika sonra otomatik silme                   │
└─────────────────────────────────────────────────────────┘
```

---

## 8. BAKIM ŞEMASI

```
┌─────────────────────────────────────────────────────────┐
│              BAKIM RUTİNLERİ                            │
├─────────────────────────────────────────────────────────┤
│ GÜNLÜK:                                                  │
│    ├── Servis çalışıyor mu? (systemctl status)         │
│    └── Disk alanı kontrolü (df -h)                     │
│                                                         │
│ HAFTALIK:                                                │
│    ├── Sistem güncellemeleri (apt update)               │
│    ├── Log kontrolü (journalctl)                        │
│    └── Yedekleme (opsiyonel)                            │
│                                                         │
│ AYLIK:                                                   │
│    ├── Güvenlik güncellemeleri                          │
│    ├── Performans analizi                              │
│    └── UPS batarya kontrolü                             │
│                                                         │
│ YILLIK:                                                  │
│    ├── Donanım kontrolü                                 │
│    └── UPS batarya değişimi (gerekirse)                │
└─────────────────────────────────────────────────────────┘
```

---

## 9. SORUN GİDERME ŞEMASI

```
SORUN: Servis çalışmıyor
├── Docker container durumu: docker ps -a
├── Log kontrolü: docker logs rendezvous-service
└── Systemd durumu: systemctl status rendezvous

SORUN: İnternet'ten erişilemiyor
├── Port forwarding kontrolü (router)
├── Firewall kuralları: ufw status
└── Statik IP kontrolü: curl ifconfig.me

SORUN: Yüksek bellek kullanımı
├── Container kaynakları: docker stats
├── Sistem kaynakları: htop
└── Log analizi: journalctl -u rendezvous

SORUN: Elektrik kesintisi
├── UPS otomatik devreye girer
├── Servis otomatik başlar (systemd)
└── Manuel kontrol: systemctl start rendezvous
```

---

## 10. ÖLÇEKLENDİRME ŞEMASI (Gelecek)

```
┌─────────────────────────────────────────────────────────┐
│              ÖLÇEKLENDİRME PLANI                        │
├─────────────────────────────────────────────────────────┤
│ BAŞLANGIÇ (1 sunucu):                                    │
│    ├── 1 fiziksel sunucu                                │
│    ├── 1 Docker container                               │
│    └── ~1000 eşzamanlı kullanıcı                        │
│                                                         │
│ ORTA (2-3 sunucu):                                       │
│    ├── Load balancer (Nginx)                            │
│    ├── 2-3 fiziksel sunucu                              │
│    └── ~5000 eşzamanlı kullanıcı                        │
│                                                         │
│ İLERİ (5+ sunucu):                                       │
│    ├── Kubernetes cluster (opsiyonel)                   │
│    ├── 5+ fiziksel sunucu                               │
│    └── ~20000+ eşzamanlı kullanıcı                      │
└─────────────────────────────────────────────────────────┘
```

---

## SONUÇ

Bu şema, fiziksel sunucu kurulumu için gerekli tüm adımları ve yapıyı gösterir. 
Her adımı sırayla takip ederek rendezvous servisinizi kurabilirsiniz.

**Önemli Notlar:**
- İlk kurulum 1-2 gün sürebilir
- Güvenlik ayarları kritik
- Düzenli bakım gerekli
- Yedekleme stratejisi önemli

