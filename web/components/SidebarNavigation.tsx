'use client';

import { useRouter, usePathname } from 'next/navigation';
import { useLanguage } from '@/contexts/LanguageContext';
import { useAuth } from '@/contexts/AuthContext';
import { HiHome, HiChatAlt2, HiUserGroup, HiCalendar } from 'react-icons/hi';

// Chat ikonu için ChatAlt2 kullanıyoruz
const HiChat = HiChatAlt2;

export default function SidebarNavigation() {
  const router = useRouter();
  const pathname = usePathname();
  const { t } = useLanguage();
  const { isAuthenticated } = useAuth();

  const navItems = [
    { href: '/home', label: t('nav.home'), icon: HiHome },
    { href: '/chat', label: t('nav.chat'), icon: HiChat },
    { href: '/contacts', label: t('nav.contacts'), icon: HiUserGroup },
    { href: '/calendar', label: t('nav.calendar'), icon: HiCalendar },
  ];

  const handleNavigation = (href: string) => {
    if (!isAuthenticated && href !== '/home') {
      router.push('/');
      return;
    }
    router.push(href);
  };

  return (
    <nav className="w-20 bg-gray-800 border-r border-gray-700 flex flex-col items-center py-4 space-y-2">
      {navItems.map((item) => {
        const isActive = pathname === item.href || (item.href === '/home' && (pathname === '/' || pathname === '/home'));
        const IconComponent = item.icon;
        return (
          <button
            key={item.href}
            onClick={() => handleNavigation(item.href)}
            title={item.label}
            className={`flex flex-col items-center justify-center w-16 h-16 rounded transition-colors ${
              isActive 
                ? 'bg-cyan-500 text-white' 
                : 'text-gray-400 hover:text-gray-300 hover:bg-gray-700'
            }`}
          >
            <IconComponent className="text-2xl mb-1" />
            <span className="text-xs font-medium">{item.label}</span>
          </button>
        );
      })}
    </nav>
  );
}

