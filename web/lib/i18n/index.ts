import tr from './locales/tr.json';
import en from './locales/en.json';

export type Locale = 'tr' | 'en';

export const locales: Record<Locale, typeof tr> = {
  tr,
  en,
};

export const defaultLocale: Locale = 'tr';

export function getTranslations(locale: Locale = defaultLocale) {
  return locales[locale] || locales[defaultLocale];
}

export function t(key: string, locale: Locale = defaultLocale): string {
  const translations = getTranslations(locale);
  const keys = key.split('.');
  let value: any = translations;
  
  for (const k of keys) {
    if (value && typeof value === 'object' && k in value) {
      value = value[k];
    } else {
      return key; // Key bulunamazsa key'i döndür
    }
  }
  
  return typeof value === 'string' ? value : key;
}

