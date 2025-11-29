'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';
import { useLanguage } from '@/contexts/LanguageContext';

export default function SettingsPage() {
  const router = useRouter();
  const { isAuthenticated, logout } = useAuth();
  const { locale, setLocale, t } = useLanguage();
  const [currentLanguage, setCurrentLanguage] = useState<'tr' | 'en'>(locale);

  useEffect(() => {
    if (!isAuthenticated) {
      router.push('/');
      return;
    }
  }, [isAuthenticated, router]);

  const handleLanguageChange = (lang: 'tr' | 'en') => {
    setCurrentLanguage(lang);
    setLocale(lang);
  };

  if (!isAuthenticated) {
    return null;
  }

  return (
    <div className="min-h-screen bg-gray-900 text-white w-full pb-20">
      {/* Header */}
      <header className="bg-gray-800 border-b border-gray-700 px-6 py-4">
        <h1 className="text-xs font-medium">{t('settings.title')}</h1>
      </header>

      <main className="p-6 space-y-6 w-full">
        {/* Dil Ayarları */}
        <div className="bg-gray-800 rounded p-4">
          <h3 className="text-xs font-medium mb-4">{t('settings.language')}</h3>
          <div className="space-y-2">
            <button
              onClick={() => handleLanguageChange('tr')}
              className={`w-full px-4 py-2 rounded text-xs font-medium transition ${
                currentLanguage === 'tr'
                  ? 'bg-blue-600 text-white'
                  : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
              }`}
            >
              🇹🇷 {t('settings.turkish')}
            </button>
            <button
              onClick={() => handleLanguageChange('en')}
              className={`w-full px-4 py-2 rounded text-xs font-medium transition ${
                currentLanguage === 'en'
                  ? 'bg-blue-600 text-white'
                  : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
              }`}
            >
              🇬🇧 {t('settings.english')}
            </button>
          </div>
        </div>

        {/* Video Ayarları */}
        <div className="bg-gray-800 rounded p-4">
          <h3 className="font-semibold mb-4">{t('settings.video')}</h3>
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-gray-300">
                {locale === 'tr' ? 'Kamera Kalitesi' : 'Camera Quality'}
              </span>
              <select className="px-3 py-1 bg-gray-700 border border-gray-600 rounded text-white text-xs font-medium">
                <option>720p</option>
                <option>1080p</option>
              </select>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-gray-300">
                {locale === 'tr' ? 'Varsayılan Kamera' : 'Default Camera'}
              </span>
              <select className="px-3 py-1 bg-gray-700 border border-gray-600 rounded text-white text-xs font-medium">
                <option>{locale === 'tr' ? 'Ön Kamera' : 'Front Camera'}</option>
                <option>{locale === 'tr' ? 'Arka Kamera' : 'Back Camera'}</option>
              </select>
            </div>
          </div>
        </div>

        {/* Ses Ayarları */}
        <div className="bg-gray-800 rounded p-4">
          <h3 className="font-semibold mb-4">{t('settings.audio')}</h3>
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-gray-300">
                {locale === 'tr' ? 'Mikrofon Hassasiyeti' : 'Microphone Sensitivity'}
              </span>
              <input
                type="range"
                min="0"
                max="100"
                defaultValue="50"
                className="w-32"
              />
            </div>
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-gray-300">
                {locale === 'tr' ? 'Ses Çıkışı' : 'Audio Output'}
              </span>
              <select className="px-3 py-1 bg-gray-700 border border-gray-600 rounded text-white text-xs font-medium">
                <option>{locale === 'tr' ? 'Hoparlör' : 'Speaker'}</option>
                <option>{locale === 'tr' ? 'Kulaklık' : 'Headphones'}</option>
              </select>
            </div>
          </div>
        </div>

        {/* Bildirimler */}
        <div className="bg-gray-800 rounded p-4">
          <h3 className="font-semibold mb-4">{t('settings.notifications')}</h3>
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-gray-300">
                {locale === 'tr' ? 'Gelen Arama Bildirimleri' : 'Incoming Call Notifications'}
              </span>
              <input type="checkbox" defaultChecked className="w-4 h-4" />
            </div>
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-gray-300">
                {locale === 'tr' ? 'Randevu Hatırlatıcıları' : 'Appointment Reminders'}
              </span>
              <input type="checkbox" defaultChecked className="w-4 h-4" />
            </div>
          </div>
        </div>

        {/* Çıkış */}
        <div className="bg-gray-800 rounded p-4">
          <button
            onClick={logout}
            className="w-full px-4 py-2 bg-red-600 hover:bg-red-700 rounded text-xs font-medium transition text-white"
          >
            {t('settings.logout')}
          </button>
        </div>
      </main>
    </div>
  );
}

