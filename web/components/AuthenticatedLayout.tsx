'use client';

import { useAuth } from '@/contexts/AuthContext';
import SidebarNavigation from '@/components/SidebarNavigation';
import ContactsList from '@/components/ContactsList';
import { ReactNode } from 'react';

interface AuthenticatedLayoutProps {
  children: ReactNode;
}

export default function AuthenticatedLayout({ children }: AuthenticatedLayoutProps) {
  const { isAuthenticated, loading } = useAuth();

  // Loading durumunda loading ekranı göster
  if (loading) {
    return (
      <div className="h-full flex items-center justify-center bg-gray-900">
        <div className="text-center text-gray-400">
          <p className="text-xs font-medium">Yükleniyor...</p>
        </div>
      </div>
    );
  }

  // Giriş yapılmamışsa sadece içeriği göster (karşılama ekranı için)
  if (!isAuthenticated) {
    return <div className="h-full">{children}</div>;
  }

  // Giriş yapılmışsa sidebar ve contacts list ile göster
  return (
    <div className="h-full flex">
      <SidebarNavigation />
      <ContactsList />
      <div className="flex-1 overflow-auto">
        {children}
      </div>
    </div>
  );
}

