'use client'

import { createContext, useContext, useState, useEffect, ReactNode } from 'react'

interface User {
  id: string
  name: string
  email: string
  phone: string
  createdAt: string
}

interface AuthContextType {
  user: User | null
  token: string | null
  loading: boolean
  login: (email: string, password: string) => Promise<void>
  loginWithPhone: (phoneNumber: string) => Promise<void>
  register: (userData: RegisterData) => Promise<void>
  logout: () => void
  isAuthenticated: boolean
}

interface RegisterData {
  name: string
  email: string
  phone: string
  password: string
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [token, setToken] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    // Sayfa yüklendiğinde token'ı veya telefon numarasını kontrol et
    const storedToken = localStorage.getItem('token')
    const storedPhone = localStorage.getItem('phoneNumber')
    const storedUser = localStorage.getItem('user')
    
    if (storedToken) {
      setToken(storedToken)
      fetchUser(storedToken)
    } else if (storedPhone && storedUser) {
      // Telefon numarası ile giriş yapılmışsa
      try {
        const user = JSON.parse(storedUser)
        setUser(user)
        setToken('phone-auth') // Telefon numarası ile giriş için özel token
      } catch (error) {
        console.error('Error parsing stored user:', error)
      } finally {
        setLoading(false)
      }
    } else {
      setLoading(false)
    }
  }, [])

  const fetchUser = async (authToken: string) => {
    try {
      const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:3000'
      const res = await fetch(`${apiUrl}/api/auth/me`, {
        headers: {
          'Authorization': `Bearer ${authToken}`
        }
      })

      if (res.ok) {
        const data = await res.json()
        setUser(data.user)
      } else {
        // Token geçersizse temizle
        localStorage.removeItem('token')
        setToken(null)
      }
    } catch (error) {
      console.error('Error fetching user:', error)
      localStorage.removeItem('token')
      setToken(null)
    } finally {
      setLoading(false)
    }
  }

  const login = async (email: string, password: string) => {
    const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:3000'
    const res = await fetch(`${apiUrl}/api/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ email, password }),
    })

    const data = await res.json()

    if (!res.ok) {
      throw new Error(data.error || 'Giriş başarısız')
    }

    setToken(data.token)
    setUser(data.user)
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(data.user))
  }

  const loginWithPhone = async (phoneNumber: string) => {
    try {
      // Backend API'yi dene
      const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:3000'
      const res = await fetch(`${apiUrl}/api/auth/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ phoneNumber }),
      })

      if (res.ok) {
        const data = await res.json()
        setToken(data.token)
        setUser(data.user)
        localStorage.setItem('token', data.token)
        localStorage.setItem('user', JSON.stringify(data.user))
        localStorage.setItem('phoneNumber', phoneNumber)
        return
      }
    } catch (error) {
      console.log('Backend API not available, using local auth')
    }

    // Backend yoksa local authentication (test için)
    const cleanedPhone = phoneNumber.replace(/\D/g, '')
    const formattedPhone = cleanedPhone.startsWith('90') 
      ? `+${cleanedPhone}` 
      : cleanedPhone.startsWith('0')
      ? `+9${cleanedPhone}`
      : `+${cleanedPhone}`

    const user: User = {
      id: `user-${Date.now()}`,
      name: formattedPhone,
      email: '',
      phone: formattedPhone,
      createdAt: new Date().toISOString(),
    }

    setUser(user)
    setToken('phone-auth')
    localStorage.setItem('user', JSON.stringify(user))
    localStorage.setItem('phoneNumber', formattedPhone)
    localStorage.setItem('token', 'phone-auth')
  }

  const register = async (userData: RegisterData) => {
    const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:3000'
    const res = await fetch(`${apiUrl}/api/auth/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(userData),
    })

    const data = await res.json()

    if (!res.ok) {
      throw new Error(data.error || 'Kayıt başarısız')
    }

    // Kayıt sonrası otomatik giriş yap
    await login(userData.email, userData.password)
  }

  const logout = () => {
    setUser(null)
    setToken(null)
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    localStorage.removeItem('phoneNumber')
  }

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        loading,
        login,
        loginWithPhone,
        register,
        logout,
        isAuthenticated: !!user && !!token,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (context === undefined) {
    // AuthProvider yoksa default değerler döndür (server component render sırasında)
      return {
        user: null,
        token: null,
        loading: false,
        login: async () => {},
        loginWithPhone: async () => {},
        register: async () => {},
        logout: () => {},
        isAuthenticated: false,
      }
  }
  return context
}

