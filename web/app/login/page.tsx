'use client'

import { useState, useEffect, Suspense } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import Link from 'next/link'
import Header from '@/components/Header'
import { useAuth } from '@/contexts/AuthContext'

function LoginContent() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const { login, isAuthenticated } = useAuth()
  const router = useRouter()
  const searchParams = useSearchParams()
  const redirect = searchParams.get('redirect') || '/call'

  useEffect(() => {
    // Zaten giriş yapmışsa yönlendir
    if (isAuthenticated) {
      router.push(redirect)
    }
  }, [isAuthenticated, redirect, router])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError('')

    try {
      await login(email, password)
      router.push(redirect)
    } catch (err: any) {
      setError(err.message || 'Giriş başarısız. Lütfen bilgilerinizi kontrol edin.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="min-h-screen bg-navy w-full flex flex-col items-center">
      <Header />
      <div className="flex-1 w-full max-w-sm mx-auto px-4 py-16" style={{ marginTop: '350px' }}>
        <div className="bg-slate/50 rounded-lg p-8" style={{ border: '2px solid #00B8D4' }}>
          <h1 className="text-lg font-bold text-teal mb-6 text-center">Giriş Yap</h1>
          
          {error && (
            <div className="bg-red-500/20 border border-red-500 text-red-200 px-4 py-3 rounded-lg mb-4 text-sm">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4 flex flex-col items-center">
            <div className="flex flex-col items-center" style={{ width: '85%' }}>
              <label htmlFor="email" className="block text-sm font-medium text-teal mb-2 w-full text-left">
                E-posta
              </label>
              <input
                type="email"
                id="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                className="bg-slate px-4 py-3 text-teal placeholder-teal/50 focus:outline-none transition w-full"
                style={{ borderRadius: '4px', border: '1px solid #00B8D4', fontSize: '14px' }}
                placeholder="ornek@email.com"
              />
            </div>

            <div className="flex flex-col items-center" style={{ width: '85%' }}>
              <label htmlFor="password" className="block text-sm font-medium text-teal mb-2 w-full text-left">
                Şifre
              </label>
              <input
                type="password"
                id="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                className="bg-slate px-4 py-3 text-teal placeholder-teal/50 focus:outline-none transition w-full"
                style={{ borderRadius: '4px', border: '1px solid #00B8D4', fontSize: '14px' }}
                placeholder="••••••••"
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-teal text-navy px-6 py-3 rounded-lg hover:bg-accent transition font-semibold text-base disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? 'Giriş yapılıyor...' : 'Giriş Yap'}
            </button>
          </form>

          <div className="mt-6 text-center">
            <p className="text-sm text-teal/70">
              Hesabınız yok mu?{' '}
              <Link href="/register" className="text-orange-400 hover:text-orange-300 transition font-medium">
                Hesap Oluştur
              </Link>
            </p>
          </div>
        </div>
      </div>
    </main>
  )
}

export default function LoginPage() {
  return (
    <Suspense fallback={
      <main className="min-h-screen bg-navy w-full flex flex-col items-center">
        <Header />
        <div className="flex-1 flex items-center justify-center">
          <div className="text-teal text-lg">Yükleniyor...</div>
        </div>
      </main>
    }>
      <LoginContent />
    </Suspense>
  )
}

