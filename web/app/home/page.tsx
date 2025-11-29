'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Image from 'next/image';
import Link from 'next/link';
import { useAuth } from '@/contexts/AuthContext';
import { useLanguage } from '@/contexts/LanguageContext';
import { apiClient } from '@/lib/api/client';

export default function HomePage() {
  const router = useRouter();
  const { isAuthenticated, user } = useAuth();
  const { t, locale } = useLanguage();
  const [statusMessage, setStatusMessage] = useState<string>('');
  const [isConnected, setIsConnected] = useState(false);
  const [networkType, setNetworkType] = useState<'wifi' | 'mobile' | 'none'>('wifi');
  const [showDesktopOffer, setShowDesktopOffer] = useState(false);
  const [subscriptionCode, setSubscriptionCode] = useState('');
  const [isActivating, setIsActivating] = useState(false);
  const [hasDesktopSubscription, setHasDesktopSubscription] = useState(false);

  useEffect(() => {
    if (!isAuthenticated) {
      router.push('/');
      return;
    }

    // Ağ durumunu kontrol et
    if (typeof navigator !== 'undefined' && 'connection' in navigator) {
      const connection = (navigator as any).connection || (navigator as any).mozConnection || (navigator as any).webkitConnection;
      if (connection) {
        const updateNetworkType = () => {
          if (connection.effectiveType === '4g' || connection.type === 'cellular') {
            setNetworkType('mobile');
          } else if (connection.type === 'wifi') {
            setNetworkType('wifi');
          } else {
            setNetworkType('none');
          }
        };
        updateNetworkType();
        connection.addEventListener('change', updateNetworkType);
        return () => connection.removeEventListener('change', updateNetworkType);
      }
    }

    // Signaling durumunu kontrol et (basit ping)
    const checkConnection = async () => {
      try {
        // Otomatik IP bulma ile signaling URL'i al
        const { getSignalingServerUrl } = await import('@/lib/utils/signalingServerDiscovery');
        const defaultUrl = process.env.NEXT_PUBLIC_SIGNALING_URL || 'ws://192.168.1.20:8080/ws';
        const signalingUrl = await getSignalingServerUrl(defaultUrl);
        // Health check için HTTP endpoint kullan (WebSocket yerine)
        const healthUrl = signalingUrl.replace('ws://', 'http://').replace('wss://', 'https://').replace('/ws', '/health');
        
        const response = await fetch(healthUrl, { method: 'GET' });
        if (response.ok) {
          setIsConnected(true);
          setStatusMessage(locale === 'tr' ? 'Sunucuya bağlı' : 'Connected to server');
        } else {
          setIsConnected(false);
          setStatusMessage(locale === 'tr' ? 'Sunucuya bağlanılamadı' : 'Cannot connect to server');
        }
      } catch (error) {
        setIsConnected(false);
        setStatusMessage(locale === 'tr' ? 'Sunucuya bağlanılamadı' : 'Cannot connect to server');
      }
    };

    checkConnection();
    
    // Desktop abonelik kontrolü (isteğe bağlı teklif için)
    checkDesktopSubscription();
  }, [isAuthenticated, router, locale]);

  const checkDesktopSubscription = async () => {
    try {
      const response = await apiClient.getSubscription();
      if (response.success && response.data) {
        // Desktop için aktif abonelik var mı kontrol et
        const hasDesktop = response.data.deviceType === 'desktop' || 
                          (response.data.status === 'active' && response.data.expiresAt && new Date(response.data.expiresAt) > new Date());
        setHasDesktopSubscription(hasDesktop);
        
        // Eğer desktop aboneliği yoksa ve daha önce teklif gösterilmemişse, teklif göster
        if (!hasDesktop && !localStorage.getItem('desktopOfferShown')) {
          // İlk açılışta veya belirli koşullarda göster
          setTimeout(() => {
            setShowDesktopOffer(true);
          }, 2000); // 2 saniye sonra göster
        }
      }
    } catch (error) {
      console.error('Desktop subscription check error:', error);
    }
  };

  const handleActivateCode = async () => {
    if (!subscriptionCode.trim() || !user?.phone) return;
    
    setIsActivating(true);
    try {
      const response = await apiClient.activateSubscriptionCode(
        subscriptionCode.trim().toUpperCase(),
        user.phone,
        'desktop'
      );
      
      if (response.success) {
        setHasDesktopSubscription(true);
        setShowDesktopOffer(false);
        localStorage.setItem('desktopOfferShown', 'true');
        alert(locale === 'tr' ? 'Abonelik başarıyla aktifleştirildi!' : 'Subscription activated successfully!');
        setSubscriptionCode('');
      } else {
        alert(response.error || (locale === 'tr' ? 'Abonelik kodu geçersiz veya kullanılmış.' : 'Invalid or already used subscription code.'));
      }
    } catch (error: any) {
      console.error('Activation error:', error);
      alert(error.message || (locale === 'tr' ? 'Aktivasyon sırasında bir hata oluştu.' : 'An error occurred during activation.'));
    } finally {
      setIsActivating(false);
    }
  };

  if (!isAuthenticated) {
    return null;
  }

  return (
    <div className="min-h-screen bg-gray-900 text-white w-full">
      {/* Header */}
      <header className="bg-gray-800 border-b border-gray-700 px-6 py-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <Image
              src="/logo.png"
              alt="Logo"
              width={40}
              height={40}
              className="rounded"
            />
            <h1 className="text-xs font-medium">
              {locale === 'tr' ? 'Ana Sayfa' : 'Home'}
            </h1>
          </div>
          <Link
            href="/settings"
            className="text-gray-300 hover:text-white transition"
          >
            ⚙️
          </Link>
        </div>
      </header>

      <main className="p-6 space-y-6 w-full">
        {/* Durum Bilgisi */}
        <div className="bg-gray-800 rounded p-4">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-medium text-gray-400">
              {locale === 'tr' ? 'Durum' : 'Status'}
            </span>
            <div className="flex items-center gap-2">
              <span className={`text-xs font-medium ${isConnected ? 'text-green-400' : 'text-red-400'}`}>
                {networkType === 'wifi' ? '📶' : networkType === 'mobile' ? '📱' : '❌'}
              </span>
              <span className="text-xs font-medium text-gray-300">
                {networkType === 'wifi' 
                  ? (locale === 'tr' ? 'Wi-Fi' : 'Wi-Fi')
                  : networkType === 'mobile'
                  ? (locale === 'tr' ? 'Mobil' : 'Mobile')
                  : (locale === 'tr' ? 'Bağlantı Yok' : 'No Connection')
                }
              </span>
            </div>
          </div>
          <p className="text-cyan-400 text-xs font-medium">{statusMessage || (locale === 'tr' ? 'Hazır' : 'Ready')}</p>
        </div>

        {/* Hızlı Erişim Kartları */}
        <div className="grid grid-cols-2 gap-4 max-w-md">
          <Link
            href="/call"
            className="bg-cyan-500 hover:bg-cyan-600 rounded p-3 flex items-center justify-center gap-3 transition"
          >
            <div className="text-xl">📞</div>
            <div className="font-medium text-xs">
              {locale === 'tr' ? 'Arama Başlat' : 'Start Call'}
            </div>
          </Link>
          <Link
            href="/contacts"
            className="bg-green-600 hover:bg-green-700 rounded p-3 flex items-center justify-center gap-3 transition"
          >
            <div className="text-xl">👥</div>
            <div className="font-medium text-xs">
              {locale === 'tr' ? 'Kişiler' : 'Contacts'}
            </div>
          </Link>
        </div>

        {/* Güvenlik Bilgileri */}
        <div className="bg-gray-800 rounded p-4">
          <h3 className="text-xs font-medium mb-3 text-cyan-400">
            {locale === 'tr' ? 'Güvenlik Özellikleri' : 'Security Features'}
          </h3>
          <ul className="space-y-2">
            <li className="text-xs font-medium text-gray-300">🔒 {locale === 'tr' 
              ? 'Tüm medya ve sinyalleşme trafiği TLS 1.3 üzerinden uçtan uca şifrelenir.'
              : 'All media and signaling traffic is end-to-end encrypted over TLS 1.3.'}
            </li>
            <li className="text-xs font-medium text-gray-300">📱 {locale === 'tr'
              ? 'Rehber verileri yalnızca davet sürecinde okunur, sunucuya kaydedilmez.'
              : 'Contact data is only read during invitation, not stored on server.'}
            </li>
            <li className="text-xs font-medium text-gray-300">🛡️ {locale === 'tr'
              ? 'KVKK ve GDPR kapsamında veri sahibinin silme ve bilgi alma hakları desteklenir.'
              : 'Data deletion and access rights under KVKK and GDPR are supported.'}
            </li>
            <li className="text-xs font-medium text-gray-300">💾 {locale === 'tr'
              ? 'Görüntülü görüşmeler cihaz üzerinde işlenir; bulutta kalıcı kayıt tutulmaz.'
              : 'Video calls are processed on device; no permanent records in cloud.'}
            </li>
          </ul>
        </div>
      </main>

      {/* Desktop Uygulaması Teklif Modal */}
      {showDesktopOffer && !hasDesktopSubscription && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-gray-800 rounded-lg p-6 w-full max-w-md mx-4">
            <h2 className="text-xs font-medium mb-4 text-cyan-400">
              {t('desktopSubscription.offerTitle')}
            </h2>
            <p className="text-xs font-medium text-gray-300 mb-6">
              {t('desktopSubscription.offerDescription')}
            </p>

            {/* Avantajlar */}
            <div className="mb-6">
              <h3 className="text-xs font-medium mb-3 text-cyan-400">
                {t('desktopSubscription.benefits')}
              </h3>
              <ul className="space-y-2 text-xs font-medium text-gray-300">
                <li>✓ {t('desktopSubscription.benefit1')}</li>
                <li>✓ {t('desktopSubscription.benefit2')}</li>
                <li>✓ {t('desktopSubscription.benefit3')}</li>
                <li>✓ {t('desktopSubscription.benefit4')}</li>
              </ul>
            </div>

            {/* Abonelik Kodu Girişi */}
            <div className="mb-6">
              <label className="block text-xs font-medium mb-2 text-left">
                {t('desktopSubscription.enterCode')}
              </label>
              <input
                type="text"
                value={subscriptionCode}
                onChange={(e) => setSubscriptionCode(e.target.value.toUpperCase())}
                placeholder={t('desktopSubscription.codePlaceholder')}
                className="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded text-xs font-medium text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-cyan-500"
                maxLength={17}
              />
            </div>

            {/* Butonlar */}
            <div className="flex gap-2">
              <button
                onClick={() => {
                  setShowDesktopOffer(false);
                  localStorage.setItem('desktopOfferShown', 'true');
                }}
                className="flex-1 px-4 py-2 bg-gray-700 hover:bg-gray-600 rounded text-xs font-medium transition-colors"
              >
                {t('desktopSubscription.skip')}
              </button>
              <button
                onClick={handleActivateCode}
                disabled={!subscriptionCode.trim() || isActivating}
                className="flex-1 px-4 py-2 bg-cyan-500 hover:bg-cyan-600 disabled:bg-gray-600 disabled:cursor-not-allowed rounded text-xs font-medium transition-colors"
              >
                {isActivating ? t('desktopSubscription.checking') : t('desktopSubscription.activate')}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

