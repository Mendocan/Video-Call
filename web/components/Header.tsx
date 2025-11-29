'use client'

import { useState } from 'react'
import Link from 'next/link'
import Image from 'next/image'
import { useAuth } from '@/contexts/AuthContext'
import LanguageSwitcher from './LanguageSwitcher'
import { useLanguage } from '@/contexts/LanguageContext'

export default function Header() {
  const { user, isAuthenticated, logout } = useAuth()
  const { t } = useLanguage()

  return (
    <header className="bg-gray-800 border-b border-gray-700 w-full">
      <div className="w-full px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          <div className="flex items-center gap-6">
            <Link href="/" className="flex items-center">
              <Image 
                src="/logo.png" 
                alt="Video Call Logo" 
                width={40} 
                height={40}
                className="rounded-lg object-contain"
              />
            </Link>
            {isAuthenticated && (
              <nav className="hidden md:flex gap-4">
                <Link 
                  href="/call" 
                  className="text-xs font-medium text-gray-300 hover:text-white transition px-3 py-2 rounded hover:bg-gray-700"
                >
                  {t('nav.calls')}
                </Link>
                <Link 
                  href="/contacts" 
                  className="text-xs font-medium text-gray-300 hover:text-white transition px-3 py-2 rounded hover:bg-gray-700"
                >
                  {t('nav.contacts')}
                </Link>
              </nav>
            )}
          </div>
          <div className="flex items-center gap-4">
            <LanguageSwitcher />
            {isAuthenticated && user ? (
              <>
                <span className="text-xs font-medium text-gray-300 hidden sm:block">
                  {user.name}
                </span>
                <button
                  onClick={logout}
                  className="text-xs font-medium text-gray-300 hover:text-white transition px-3 py-2 rounded hover:bg-gray-700"
                >
                  {t('settings.logout')}
                </button>
              </>
            ) : (
              <>
                <Link
                  href="/login"
                  className="text-xs font-medium text-gray-300 hover:text-white transition px-3 py-2 rounded hover:bg-gray-700"
                >
                  {t('welcome.login')}
                </Link>
                <Link
                  href="/register"
                  className="bg-cyan-500 text-white px-4 py-2 rounded hover:bg-cyan-600 transition font-medium text-xs"
                >
                  {t('welcome.register')}
                </Link>
              </>
            )}
          </div>
        </div>
      </div>
    </header>
  )
}
