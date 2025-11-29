'use client';

import { useLanguage } from '@/contexts/LanguageContext';
import { Locale } from '@/lib/i18n';

export default function LanguageSwitcher() {
  const { locale, setLocale, t } = useLanguage();

  const toggleLanguage = () => {
    setLocale(locale === 'tr' ? 'en' : 'tr');
  };

  return (
    <button
      onClick={toggleLanguage}
      className="px-3 py-1.5 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-md transition-colors"
      aria-label={t('settings.language')}
    >
      {locale === 'tr' ? '🇹🇷 TR' : '🇬🇧 EN'}
    </button>
  );
}

