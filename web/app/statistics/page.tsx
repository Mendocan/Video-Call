'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';
import { useLanguage } from '@/contexts/LanguageContext';
import { apiClient } from '@/lib/api/client';

interface CallStatistics {
  totalCalls: number;
  totalDuration: number; // seconds
  incomingCalls: number;
  outgoingCalls: number;
  missedCalls: number;
  averageDuration: number; // seconds
}

export default function StatisticsPage() {
  const router = useRouter();
  const { isAuthenticated } = useAuth();
  const { t, locale } = useLanguage();
  const [stats, setStats] = useState<CallStatistics | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!isAuthenticated) {
      router.push('/');
      return;
    }

    loadStatistics();
  }, [isAuthenticated, router]);

  const loadStatistics = async () => {
    try {
      setLoading(true);
      // Try API first, fallback to localStorage
      const response = await apiClient.getCallStatistics();
      if (response.success && response.data) {
        setStats(response.data);
      } else {
        // Calculate from localStorage
        const history = localStorage.getItem('callHistory');
        if (history) {
          const calls = JSON.parse(history);
          calculateStats(calls);
        }
      }
    } catch (error) {
      // Fallback to localStorage
      const history = localStorage.getItem('callHistory');
      if (history) {
        const calls = JSON.parse(history);
        calculateStats(calls);
      }
    } finally {
      setLoading(false);
    }
  };

  const calculateStats = (calls: any[]) => {
    const stats: CallStatistics = {
      totalCalls: calls.length,
      totalDuration: 0,
      incomingCalls: 0,
      outgoingCalls: 0,
      missedCalls: 0,
      averageDuration: 0,
    };

    calls.forEach((call) => {
      stats.totalDuration += call.duration || 0;
      switch (call.callType) {
        case 'incoming':
          stats.incomingCalls++;
          break;
        case 'outgoing':
          stats.outgoingCalls++;
          break;
        case 'missed':
          stats.missedCalls++;
          break;
      }
    });

    stats.averageDuration = stats.totalCalls > 0 
      ? Math.floor(stats.totalDuration / stats.totalCalls) 
      : 0;

    setStats(stats);
  };

  const formatDuration = (seconds: number): string => {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    
    if (hours > 0) {
      return `${hours}${locale === 'tr' ? ' saat' : 'h'} ${minutes}${locale === 'tr' ? ' dakika' : 'm'}`;
    }
    return `${minutes}${locale === 'tr' ? ' dakika' : 'm'}`;
  };

  if (!isAuthenticated) {
    return null;
  }

  return (
    <div className="min-h-screen bg-gray-900 text-white w-full pb-20">
      {/* Header */}
      <header className="bg-gray-800 border-b border-gray-700 px-6 py-4">
        <div className="flex items-center justify-between">
          <h1 className="text-xs font-medium">
            {locale === 'tr' ? 'Görüşme İstatistikleri' : 'Call Statistics'}
          </h1>
          <button
            onClick={() => router.back()}
            className="text-gray-300 hover:text-white transition"
          >
            ✕
          </button>
        </div>
      </header>

      {/* Statistics */}
      <main className="p-6 space-y-6 w-full">
        {loading ? (
          <div className="text-center py-8 text-gray-400">
            {locale === 'tr' ? 'Yükleniyor...' : 'Loading...'}
          </div>
        ) : stats ? (
          <>
            {/* Total Stats */}
            <div className="bg-gray-800 rounded p-6">
              <h2 className="text-xs font-medium mb-4">
                {locale === 'tr' ? 'Genel İstatistikler' : 'General Statistics'}
              </h2>
              <div className="grid grid-cols-2 gap-4">
                <div className="bg-gray-700 rounded p-4">
                  <div className="text-xs font-medium text-cyan-400">{stats.totalCalls}</div>
                  <div className="text-xs font-medium text-gray-400">
                    {locale === 'tr' ? 'Toplam Arama' : 'Total Calls'}
                  </div>
                </div>
                <div className="bg-gray-700 rounded p-4">
                  <div className="text-lg font-bold text-green-400">
                    {formatDuration(stats.totalDuration)}
                  </div>
                  <div className="text-xs font-medium text-gray-400">
                    {locale === 'tr' ? 'Toplam Süre' : 'Total Duration'}
                  </div>
                </div>
              </div>
            </div>

            {/* Call Types */}
            <div className="bg-gray-800 rounded p-6">
              <h2 className="text-xs font-medium mb-4">
                {locale === 'tr' ? 'Arama Türleri' : 'Call Types'}
              </h2>
              <div className="grid grid-cols-3 gap-4">
                <div className="bg-gray-700 rounded-lg p-4 text-center">
                  <div className="text-lg font-bold text-green-400">{stats.incomingCalls}</div>
                  <div className="text-xs font-medium text-gray-400">
                    {locale === 'tr' ? 'Gelen' : 'Incoming'}
                  </div>
                </div>
                <div className="bg-gray-700 rounded-lg p-4 text-center">
                  <div className="text-xs font-medium text-cyan-400">{stats.outgoingCalls}</div>
                  <div className="text-xs font-medium text-gray-400">
                    {locale === 'tr' ? 'Giden' : 'Outgoing'}
                  </div>
                </div>
                <div className="bg-gray-700 rounded-lg p-4 text-center">
                  <div className="text-lg font-bold text-red-400">{stats.missedCalls}</div>
                  <div className="text-xs font-medium text-gray-400">
                    {locale === 'tr' ? 'Kaçırılan' : 'Missed'}
                  </div>
                </div>
              </div>
            </div>

            {/* Average */}
            <div className="bg-gray-800 rounded p-6">
              <h2 className="text-xs font-medium mb-4">
                {locale === 'tr' ? 'Ortalama' : 'Average'}
              </h2>
              <div className="bg-gray-700 rounded-lg p-4">
                <div className="text-xs font-medium text-purple-400">
                  {formatDuration(stats.averageDuration)}
                </div>
                <div className="text-sm text-gray-400">
                  {locale === 'tr' ? 'Ortalama Görüşme Süresi' : 'Average Call Duration'}
                </div>
              </div>
            </div>
          </>
        ) : (
          <div className="text-center py-8 text-gray-400">
            {locale === 'tr' ? 'İstatistik bulunamadı' : 'No statistics found'}
          </div>
        )}
      </main>
    </div>
  );
}

