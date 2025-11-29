'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Image from 'next/image';
import { useAuth } from '@/contexts/AuthContext';
import { useLanguage } from '@/contexts/LanguageContext';
import { FaUserPlus, FaSignInAlt } from 'react-icons/fa';

export default function Home() {
  const router = useRouter();
  const { isAuthenticated, loading, loginWithPhone, register } = useAuth();
  const { t, locale } = useLanguage();
  const [activeTab, setActiveTab] = useState<'login' | 'register'>('login');
  
  // Login form state
  const [phoneNumber, setPhoneNumber] = useState('');
  const [termsAccepted, setTermsAccepted] = useState(false);
  const [rememberMe, setRememberMe] = useState(true);
  const [phoneError, setPhoneError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Register form state
  const [registerData, setRegisterData] = useState({
    name: '',
    email: '',
    phone: '',
    password: '',
    confirmPassword: '',
  });
  const [registerError, setRegisterError] = useState<string | null>(null);

  useEffect(() => {
    // Giriş yapılmışsa /home'a yönlendir
    if (!loading && isAuthenticated) {
      router.push('/home');
    }
  }, [isAuthenticated, loading, router]);

  useEffect(() => {
    const savedPhone = localStorage.getItem('phoneNumber');
    if (savedPhone) {
      setPhoneNumber(savedPhone);
    }
  }, []);

  const validatePhoneNumber = (phone: string): boolean => {
    const cleaned = phone.replace(/\D/g, '');
    if (cleaned.length === 0) {
      setPhoneError(locale === 'tr' ? 'Telefon numarası gereklidir' : 'Phone number is required');
      return false;
    }
    if (cleaned.startsWith('90') && cleaned.length === 12) {
      setPhoneError(null);
      return true;
    }
    if (cleaned.startsWith('0') && cleaned.length === 11) {
      setPhoneError(null);
      return true;
    }
    if (cleaned.startsWith('+90') && cleaned.length === 13) {
      setPhoneError(null);
      return true;
    }
    setPhoneError(locale === 'tr' 
      ? 'Geçerli bir telefon numarası giriniz (örn: +905551234567 veya 05551234567)'
      : 'Enter a valid phone number (e.g., +905551234567 or 05551234567)');
    return false;
  };

  const handleLogin = async () => {
    if (!validatePhoneNumber(phoneNumber) || !termsAccepted) {
      return;
    }

    setIsSubmitting(true);
    try {
      const cleanedPhone = phoneNumber.replace(/\D/g, '');
      const formattedPhone = cleanedPhone.startsWith('90') 
        ? `+${cleanedPhone}` 
        : cleanedPhone.startsWith('0')
        ? `+9${cleanedPhone}`
        : `+${cleanedPhone}`;

      await loginWithPhone(formattedPhone);
      
      if (!rememberMe) {
        localStorage.removeItem('phoneNumber');
      }
      
      router.push('/home');
    } catch (error) {
      console.error('Login error:', error);
      setPhoneError(locale === 'tr' 
        ? 'Giriş başarısız. Lütfen tekrar deneyin.'
        : 'Login failed. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleRegister = async () => {
    setRegisterError(null);

    if (!registerData.name || !registerData.email || !registerData.phone || !registerData.password) {
      setRegisterError(locale === 'tr' ? 'Tüm alanlar gereklidir' : 'All fields are required');
      return;
    }

    if (registerData.password !== registerData.confirmPassword) {
      setRegisterError(locale === 'tr' ? 'Şifreler eşleşmiyor' : 'Passwords do not match');
      return;
    }

    if (registerData.password.length < 6) {
      setRegisterError(locale === 'tr' ? 'Şifre en az 6 karakter olmalıdır' : 'Password must be at least 6 characters');
      return;
    }

    if (!validatePhoneNumber(registerData.phone)) {
      setRegisterError(phoneError || (locale === 'tr' ? 'Geçerli bir telefon numarası giriniz' : 'Enter a valid phone number'));
      return;
    }

    setIsSubmitting(true);
    try {
      const cleanedPhone = registerData.phone.replace(/\D/g, '');
      const formattedPhone = cleanedPhone.startsWith('90') 
        ? `+${cleanedPhone}` 
        : cleanedPhone.startsWith('0')
        ? `+9${cleanedPhone}`
        : `+${cleanedPhone}`;

      await register({
        name: registerData.name,
        email: registerData.email,
        phone: formattedPhone,
        password: registerData.password,
      });
      
      router.push('/home');
    } catch (error: any) {
      console.error('Register error:', error);
      setRegisterError(error.message || (locale === 'tr' ? 'Kayıt başarısız. Lütfen tekrar deneyin.' : 'Registration failed. Please try again.'));
    } finally {
      setIsSubmitting(false);
    }
  };

  if (loading || isAuthenticated) {
    return (
      <div className="h-screen bg-gray-900 text-white flex items-center justify-center">
        <div className="text-center">
          <h1 className="text-xs font-medium mb-4">{t('common.loading')}</h1>
        </div>
      </div>
    );
  }

  return (
    <div className="h-screen bg-gray-900 text-white flex items-center justify-center">
      {/* Ana İçerik */}
      <div className="w-full max-w-md p-6">
        <div className="w-full max-w-md">
          {/* Logo */}
          <div className="flex justify-center mb-6">
            <Image
              src="/logo.png"
              alt="Video Call Logo"
              width={120}
              height={120}
              className="rounded object-contain"
            />
          </div>

          {/* Hoşgeldiniz Mesajı */}
          <div className="text-center mb-8">
            <h1 className="text-xs font-medium mb-2">
              {locale === 'tr' ? 'Hoş Geldiniz' : 'Welcome'}
            </h1>
            <p className="text-xs font-medium text-gray-400">
              {locale === 'tr' 
                ? 'Uygulamaya erişmek için lütfen giriş yapın veya hesap oluşturun'
                : 'Please login or create an account to access the application'}
            </p>
          </div>

          {/* Tab Butonları */}
          <div className="flex gap-2 mb-6">
            <button
              onClick={() => setActiveTab('login')}
              className={`flex-1 py-3 px-4 rounded transition-colors flex items-center justify-center gap-2 ${
                activeTab === 'login'
                  ? 'bg-cyan-500 text-white'
                  : 'bg-gray-800 text-gray-400 hover:bg-gray-700'
              }`}
            >
              <FaSignInAlt className="text-lg" />
              <span className="text-xs font-medium">{locale === 'tr' ? 'Giriş Yap' : 'Login'}</span>
            </button>
            <button
              onClick={() => setActiveTab('register')}
              className={`flex-1 py-3 px-4 rounded transition-colors flex items-center justify-center gap-2 ${
                activeTab === 'register'
                  ? 'bg-cyan-500 text-white'
                  : 'bg-gray-800 text-gray-400 hover:bg-gray-700'
              }`}
            >
              <FaUserPlus className="text-lg" />
              <span className="text-xs font-medium">{locale === 'tr' ? 'Hesap Oluştur' : 'Register'}</span>
            </button>
          </div>

          {/* Giriş Kartı */}
          <div className="bg-gray-800 rounded p-6 shadow-lg">
            {activeTab === 'login' ? (
              <div className="space-y-4">
                {/* Telefon Numarası Input */}
                <div>
                  <label className="block text-xs font-medium mb-2 text-left">
                    {locale === 'tr' ? 'Telefon Numarası' : 'Phone Number'}
                  </label>
                  <input
                    type="tel"
                    value={phoneNumber}
                    onChange={(e) => {
                      setPhoneNumber(e.target.value);
                      setPhoneError(null);
                    }}
                    placeholder={locale === 'tr' ? '+905551234567' : '+905551234567'}
                    className={`w-64 px-4 py-3 bg-gray-700 border rounded text-xs font-medium text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-cyan-500 ${
                      phoneError ? 'border-red-500' : 'border-gray-600'
                    }`}
                  />
                  {phoneError && (
                    <p className="mt-1 text-xs font-medium text-red-400">{phoneError}</p>
                  )}
                </div>

                {/* Kullanıcı Sözleşmesi Checkbox */}
                <div className="flex items-start gap-2">
                  <input
                    type="checkbox"
                    id="terms"
                    checked={termsAccepted}
                    onChange={(e) => setTermsAccepted(e.target.checked)}
                    className="mt-1 w-4 h-4 text-cyan-500 bg-gray-700 border-gray-600 rounded focus:ring-cyan-500"
                  />
                  <label htmlFor="terms" className="text-xs font-medium text-gray-300 cursor-pointer">
                    {locale === 'tr' 
                      ? 'Kullanıcı sözleşmesini ve gizlilik politikasını kabul ediyorum'
                      : 'I accept the user agreement and privacy policy'}
                  </label>
                </div>

                {/* Beni Hatırla Checkbox */}
                <div className="flex items-start gap-2">
                  <input
                    type="checkbox"
                    id="remember"
                    checked={rememberMe}
                    onChange={(e) => setRememberMe(e.target.checked)}
                    className="mt-1 w-4 h-4 text-cyan-500 bg-gray-700 border-gray-600 rounded focus:ring-cyan-500"
                  />
                  <label htmlFor="remember" className="text-xs font-medium text-gray-300 cursor-pointer">
                    {locale === 'tr' ? 'Beni Hatırla' : 'Remember Me'}
                  </label>
                </div>

                {/* Sözleşme Linki */}
                <div className="text-center">
                  <a
                    href="/legal/terms"
                    className="text-xs text-cyan-400 hover:text-cyan-300 transition"
                  >
                    {locale === 'tr' 
                      ? 'Kullanıcı Sözleşmesi ve Gizlilik Politikası'
                      : 'User Agreement and Privacy Policy'}
                  </a>
                </div>

                {/* Giriş Butonu */}
                <button
                  onClick={handleLogin}
                  disabled={!phoneNumber.trim() || !termsAccepted || isSubmitting}
                  className="w-64 bg-cyan-500 hover:bg-cyan-600 disabled:bg-gray-600 disabled:cursor-not-allowed text-white font-medium py-3 px-4 rounded transition text-xs"
                >
                  {isSubmitting 
                    ? (locale === 'tr' ? 'Giriş yapılıyor...' : 'Logging in...')
                    : (locale === 'tr' ? 'Giriş Yap' : 'Login')
                  }
                </button>
              </div>
            ) : (
              <div className="space-y-4">
                {/* Ad Soyad */}
                <div>
                  <label className="block text-xs font-medium mb-2 text-left">
                    {locale === 'tr' ? 'Ad Soyad' : 'Full Name'}
                  </label>
                  <input
                    type="text"
                    value={registerData.name}
                    onChange={(e) => setRegisterData({ ...registerData, name: e.target.value })}
                    placeholder={locale === 'tr' ? 'Adınız Soyadınız' : 'Your Full Name'}
                    className="w-64 px-4 py-3 bg-gray-700 border border-gray-600 rounded text-xs font-medium text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-cyan-500"
                  />
                </div>

                {/* E-posta */}
                <div>
                  <label className="block text-xs font-medium mb-2 text-left">
                    {locale === 'tr' ? 'E-posta' : 'Email'}
                  </label>
                  <input
                    type="email"
                    value={registerData.email}
                    onChange={(e) => setRegisterData({ ...registerData, email: e.target.value })}
                    placeholder={locale === 'tr' ? 'ornek@email.com' : 'example@email.com'}
                    className="w-64 px-4 py-3 bg-gray-700 border border-gray-600 rounded text-xs font-medium text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-cyan-500"
                  />
                </div>

                {/* Telefon Numarası */}
                <div>
                  <label className="block text-xs font-medium mb-2 text-left">
                    {locale === 'tr' ? 'Telefon Numarası' : 'Phone Number'}
                  </label>
                  <input
                    type="tel"
                    value={registerData.phone}
                    onChange={(e) => {
                      setRegisterData({ ...registerData, phone: e.target.value });
                      setRegisterError(null);
                    }}
                    placeholder={locale === 'tr' ? '+905551234567' : '+905551234567'}
                    className="w-64 px-4 py-3 bg-gray-700 border border-gray-600 rounded text-xs font-medium text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-cyan-500"
                  />
                </div>

                {/* Şifre */}
                <div>
                  <label className="block text-xs font-medium mb-2 text-left">
                    {locale === 'tr' ? 'Şifre' : 'Password'}
                  </label>
                  <input
                    type="password"
                    value={registerData.password}
                    onChange={(e) => setRegisterData({ ...registerData, password: e.target.value })}
                    placeholder={locale === 'tr' ? 'En az 6 karakter' : 'At least 6 characters'}
                    className="w-64 px-4 py-3 bg-gray-700 border border-gray-600 rounded text-xs font-medium text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-cyan-500"
                  />
                </div>

                {/* Şifre Tekrar */}
                <div>
                  <label className="block text-xs font-medium mb-2 text-left">
                    {locale === 'tr' ? 'Şifre Tekrar' : 'Confirm Password'}
                  </label>
                  <input
                    type="password"
                    value={registerData.confirmPassword}
                    onChange={(e) => setRegisterData({ ...registerData, confirmPassword: e.target.value })}
                    placeholder={locale === 'tr' ? 'Şifrenizi tekrar girin' : 'Re-enter your password'}
                    className="w-64 px-4 py-3 bg-gray-700 border border-gray-600 rounded text-xs font-medium text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-cyan-500"
                  />
                </div>

                {registerError && (
                  <p className="text-xs font-medium text-red-400">{registerError}</p>
                )}

                {/* Kayıt Butonu */}
                <button
                  onClick={handleRegister}
                  disabled={isSubmitting}
                  className="w-64 bg-cyan-500 hover:bg-cyan-600 disabled:bg-gray-600 disabled:cursor-not-allowed text-white font-medium py-3 px-4 rounded transition text-xs"
                >
                  {isSubmitting 
                    ? (locale === 'tr' ? 'Kayıt yapılıyor...' : 'Registering...')
                    : (locale === 'tr' ? 'Kayıt Ol' : 'Register')
                  }
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
