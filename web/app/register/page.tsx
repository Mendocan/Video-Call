'use client'

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import Link from 'next/link'
import Header from '@/components/Header'
import { useAuth } from '@/contexts/AuthContext'

export default function RegisterPage() {
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    phone: '',
    password: '',
    confirmPassword: '',
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState(false)
  const { register, isAuthenticated } = useAuth()
  const router = useRouter()

  useEffect(() => {
    // Zaten giriş yapmışsa call sayfasına yönlendir
    if (isAuthenticated) {
      router.push('/call')
    }
  }, [isAuthenticated, router])

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    })
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError('')

    // Şifre kontrolü
    if (formData.password !== formData.confirmPassword) {
      setError('Şifreler eşleşmiyor.')
      setLoading(false)
      return
    }

    if (formData.password.length < 6) {
      setError('Şifre en az 6 karakter olmalıdır.')
      setLoading(false)
      return
    }

    try {
      await register({
        name: formData.name,
        email: formData.email,
        phone: formData.phone,
        password: formData.password,
      })
      setSuccess(true)
      // 2 saniye sonra call sayfasına yönlendir
      setTimeout(() => {
        router.push('/call')
      }, 2000)
    } catch (err: any) {
      setError(err.message || 'Kayıt başarısız. Lütfen bilgilerinizi kontrol edin.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="min-h-screen bg-navy w-full flex flex-col items-center">
      <Header />
      <div className="flex-1 w-full max-w-sm mx-auto px-4 py-16" style={{ marginTop: '200px' }}>
        <div className="bg-slate/50 rounded-lg p-8" style={{ border: '2px solid #00B8D4' }}>
          <h1 className="text-lg font-bold text-teal mb-6 text-center">Hesap Oluştur</h1>
          
          {error && (
            <div className="bg-red-500/20 border border-red-500 text-red-200 px-4 py-3 rounded-lg mb-4 text-sm">
              {error}
            </div>
          )}

          {success && (
            <div className="bg-green-500/20 border border-green-500 text-green-200 px-4 py-3 rounded-lg mb-4 text-sm">
              Hesabınız başarıyla oluşturuldu! Giriş sayfasına yönlendiriliyorsunuz...
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4 flex flex-col items-center">
            <div className="flex flex-col items-center" style={{ width: '85%' }}>
              <label htmlFor="name" className="block text-sm font-medium text-teal mb-2 w-full text-left">
                Ad Soyad
              </label>
              <input
                type="text"
                id="name"
                name="name"
                value={formData.name}
                onChange={handleChange}
                required
                className="bg-slate px-4 py-3 text-teal placeholder-teal/50 focus:outline-none transition w-full"
                style={{ borderRadius: '4px', border: '1px solid #00B8D4', fontSize: '14px' }}
                placeholder="Adınız Soyadınız"
              />
            </div>

            <div className="flex flex-col items-center" style={{ width: '85%' }}>
              <label htmlFor="email" className="block text-sm font-medium text-teal mb-2 w-full text-left">
                E-posta
              </label>
              <input
                type="email"
                id="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                required
                className="bg-slate px-4 py-3 text-teal placeholder-teal/50 focus:outline-none transition w-full"
                style={{ borderRadius: '4px', border: '1px solid #00B8D4', fontSize: '14px' }}
                placeholder="ornek@email.com"
              />
            </div>

            <div className="flex flex-col items-center" style={{ width: '85%' }}>
              <label htmlFor="phone" className="block text-sm font-medium text-teal mb-2 w-full text-left">
                Telefon Numarası
              </label>
              <input
                type="tel"
                id="phone"
                name="phone"
                value={formData.phone}
                onChange={handleChange}
                required
                className="bg-slate px-4 py-3 text-teal placeholder-teal/50 focus:outline-none transition w-full"
                style={{ borderRadius: '4px', border: '1px solid #00B8D4', fontSize: '14px' }}
                placeholder="05XX XXX XX XX"
              />
            </div>

            <div className="flex flex-col items-center" style={{ width: '85%' }}>
              <label htmlFor="password" className="block text-sm font-medium text-teal mb-2 w-full text-left">
                Şifre
              </label>
              <input
                type="password"
                id="password"
                name="password"
                value={formData.password}
                onChange={handleChange}
                required
                className="bg-slate px-4 py-3 text-teal placeholder-teal/50 focus:outline-none transition w-full"
                style={{ borderRadius: '4px', border: '1px solid #00B8D4', fontSize: '14px' }}
                placeholder="En az 6 karakter"
              />
            </div>

            <div className="flex flex-col items-center" style={{ width: '85%' }}>
              <label htmlFor="confirmPassword" className="block text-sm font-medium text-teal mb-2 w-full text-left">
                Şifre Tekrar
              </label>
              <input
                type="password"
                id="confirmPassword"
                name="confirmPassword"
                value={formData.confirmPassword}
                onChange={handleChange}
                required
                className="bg-slate px-4 py-3 text-teal placeholder-teal/50 focus:outline-none transition w-full"
                style={{ borderRadius: '4px', border: '1px solid #00B8D4', fontSize: '14px' }}
                placeholder="Şifrenizi tekrar girin"
              />
            </div>

            <button
              type="submit"
              disabled={loading || success}
              className="w-full bg-teal text-navy px-6 py-3 rounded-lg hover:bg-accent transition font-semibold text-base disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? 'Kayıt yapılıyor...' : success ? 'Kayıt Başarılı!' : 'Kayıt Ol'}
            </button>
          </form>

          <div className="mt-6 text-center">
            <p className="text-sm text-teal/70">
              Zaten hesabınız var mı?{' '}
              <Link href="/login" className="text-orange-400 hover:text-orange-300 transition font-medium">
                Giriş Yap
              </Link>
            </p>
          </div>
        </div>
      </div>
    </main>
  )
}

