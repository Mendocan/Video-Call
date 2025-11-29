'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';
import { useLanguage } from '@/contexts/LanguageContext';
import { apiClient } from '@/lib/api/client';

interface Subscription {
  id: string;
  plan: string;
  status: 'active' | 'expired' | 'cancelled';
  expiresAt: string | null;
  deviceCount: number;
  maxDevices: number;
}

export default function SubscriptionPage() {
  const router = useRouter();
  const { isAuthenticated, user } = useAuth();
  const { t, locale } = useLanguage();
  const [subscription, setSubscription] = useState<Subscription | null>(null);
  const [loading, setLoading] = useState(true);
  const [deviceId, setDeviceId] = useState<string>('');

  useEffect(() => {
    if (!isAuthenticated) {
      router.push('/');
      return;
    }

    loadSubscription();
    generateDeviceId();
  }, [isAuthenticated, router]);

  const generateDeviceId = () => {
    // Generate a unique device ID
    let stored = localStorage.getItem('deviceId');
    if (!stored) {
      stored = `device-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;
      localStorage.setItem('deviceId', stored);
    }
    setDeviceId(stored);
  };

  const loadSubscription = async () => {
    try {
      setLoading(true);
      const response = await apiClient.getSubscription();
      if (response.success && response.data) {
        setSubscription(response.data);
      } else {
        // Fallback: Check localStorage
        const stored = localStorage.getItem('subscription');
        if (stored) {
          setSubscription(JSON.parse(stored));
        }
      }
    } catch (error) {
      console.error('Error loading subscription:', error);
    } finally {
      setLoading(false);
    }
  };

  const verifyDevice = async () => {
    if (!deviceId) return;

    try {
      const response = await apiClient.verifyDevice(deviceId);
      if (response.success) {
        alert(locale === 'tr' ? 'Cihaz doğrulandı' : 'Device verified');
        loadSubscription();
      } else {
        alert(locale === 'tr' ? 'Cihaz doğrulanamadı' : 'Device verification failed');
      }
    } catch (error) {
      console.error('Error verifying device:', error);
      alert(locale === 'tr' ? 'Hata oluştu' : 'An error occurred');
    }
  };

  const formatDate = (dateString: string | null): string => {
    if (!dateString) return locale === 'tr' ? 'Sınırsız' : 'Unlimited';
    const date = new Date(dateString);
    return date.toLocaleDateString(locale === 'tr' ? 'tr-TR' : 'en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  };

  if (!isAuthenticated) {
    return null;
  }

  return (
    <div className="min-h-screen bg-gray-900 text-white w-full pb-20">
      {/* Header */}
      <header className="bg-gray-800 border-b border-gray-700 px-6 py-4">
        <div className="flex items-center justify-between">
          <h1 className="text-xs font-medium">
            {locale === 'tr' ? 'Abonelik Yönetimi' : 'Subscription Management'}
          </h1>
          <button
            onClick={() => router.back()}
            className="text-gray-300 hover:text-white transition"
          >
            ✕
          </button>
        </div>
      </header>

      {/* Content */}
      <main className="p-6 space-y-6 w-full">
        {loading ? (
          <div className="text-center py-8 text-gray-400">
            {locale === 'tr' ? 'Yükleniyor...' : 'Loading...'}
          </div>
        ) : subscription ? (
          <>
            {/* Subscription Status */}
            <div className="bg-gray-800 rounded p-6">
              <h2 className="text-xs font-medium mb-4">
                {locale === 'tr' ? 'Abonelik Durumu' : 'Subscription Status'}
              </h2>
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <span className="text-gray-400">
                    {locale === 'tr' ? 'Plan' : 'Plan'}
                  </span>
                  <span className="font-medium text-xs">{subscription.plan}</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-gray-400">
                    {locale === 'tr' ? 'Durum' : 'Status'}
                  </span>
                  <span
                    className={`px-3 py-1 rounded ${
                      subscription.status === 'active'
                        ? 'bg-green-600 text-white'
                        : subscription.status === 'expired'
                        ? 'bg-red-600 text-white'
                        : 'bg-gray-600 text-white'
                    }`}
                  >
                    {locale === 'tr'
                      ? subscription.status === 'active'
                        ? 'Aktif'
                        : subscription.status === 'expired'
                        ? 'Süresi Dolmuş'
                        : 'İptal Edilmiş'
                      : subscription.status === 'active'
                      ? 'Active'
                      : subscription.status === 'expired'
                      ? 'Expired'
                      : 'Cancelled'}
                  </span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-gray-400">
                    {locale === 'tr' ? 'Bitiş Tarihi' : 'Expires At'}
                  </span>
                  <span>{formatDate(subscription.expiresAt)}</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-gray-400">
                    {locale === 'tr' ? 'Cihaz Sayısı' : 'Device Count'}
                  </span>
                  <span>
                    {subscription.deviceCount} / {subscription.maxDevices}
                  </span>
                </div>
              </div>
            </div>

            {/* Device Verification */}
            <div className="bg-gray-800 rounded p-6">
              <h2 className="text-xs font-medium mb-4">
                {locale === 'tr' ? 'Cihaz Doğrulama' : 'Device Verification'}
              </h2>
              <div className="space-y-4">
                <div>
                  <label className="block text-xs font-medium text-gray-400 mb-2">
                    {locale === 'tr' ? 'Cihaz ID' : 'Device ID'}
                  </label>
                  <input
                    type="text"
                    value={deviceId}
                    readOnly
                    className="w-full px-4 py-2 bg-gray-700 border border-gray-600 rounded text-xs font-medium text-white"
                  />
                </div>
                <button
                  onClick={verifyDevice}
                  className="w-full px-4 py-2 bg-cyan-500 hover:bg-cyan-600 rounded text-xs font-medium transition"
                >
                  {locale === 'tr' ? 'Cihazı Doğrula' : 'Verify Device'}
                </button>
              </div>
            </div>

            {/* Abonelik Kodu Aktivasyonu */}
            <div className="bg-gray-800 rounded p-6">
              <h2 className="text-xs font-medium mb-4">
                {locale === 'tr' ? 'Abonelik Kodu Aktifleştir' : 'Activate Subscription Code'}
              </h2>
              <div className="space-y-4">
                <div>
                  <label className="block text-xs font-medium text-gray-400 mb-2">
                    {locale === 'tr' ? 'Abonelik Kodu' : 'Subscription Code'}
                  </label>
                  <input
                    type="text"
                    placeholder={locale === 'tr' ? 'VC-XXXX-XXXX-XXXX' : 'VC-XXXX-XXXX-XXXX'}
                    className="w-full px-4 py-2 bg-gray-700 border border-gray-600 rounded text-xs font-medium text-white placeholder-gray-500"
                    id="subscription-code-input"
                    maxLength={17}
                  />
                </div>
                <button
                  onClick={async () => {
                    const input = document.getElementById('subscription-code-input') as HTMLInputElement;
                    const code = input?.value?.trim().toUpperCase() || '';
                    if (!code || !user?.phone) {
                      alert(locale === 'tr' ? 'Lütfen abonelik kodunu girin' : 'Please enter subscription code');
                      return;
                    }
                    try {
                      const response = await apiClient.activateSubscriptionCode(code, user.phone, 'desktop');
                      if (response.success) {
                        alert(locale === 'tr' ? 'Abonelik başarıyla aktifleştirildi!' : 'Subscription activated successfully!');
                        loadSubscription();
                        if (input) input.value = '';
                      } else {
                        alert(response.error || (locale === 'tr' ? 'Abonelik kodu geçersiz' : 'Invalid subscription code'));
                      }
                    } catch (error: any) {
                      alert(error.message || (locale === 'tr' ? 'Aktivasyon hatası' : 'Activation error'));
                    }
                  }}
                  className="w-full px-4 py-2 bg-cyan-500 hover:bg-cyan-600 rounded text-xs font-medium transition"
                >
                  {locale === 'tr' ? 'Aktifleştir' : 'Activate'}
                </button>
                <p className="text-xs font-medium text-gray-400 text-center">
                  {locale === 'tr' 
                    ? 'Yıllık abonelik kodunuz desktop uygulaması için de geçerlidir.'
                    : 'Your yearly subscription code is valid for desktop application as well.'}
                </p>
              </div>
            </div>
          </>
        ) : (
          <div className="bg-gray-800 rounded-lg p-6 text-center">
            <p className="text-gray-400 mb-4">
              {locale === 'tr' ? 'Aktif abonelik bulunamadı' : 'No active subscription found'}
            </p>
            <button
              onClick={() => router.push('/pricing')}
              className="px-6 py-2 bg-blue-600 hover:bg-blue-700 rounded text-sm transition"
            >
              {locale === 'tr' ? 'Abonelik Satın Al' : 'Subscribe Now'}
            </button>
          </div>
        )}
      </main>
    </div>
  );
}

