'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';
import { useLanguage } from '@/contexts/LanguageContext';
import { apiClient } from '@/lib/api/client';

interface CallHistory {
  id: string;
  contactName: string;
  contactPhone: string;
  callType: 'incoming' | 'outgoing' | 'missed';
  duration: number; // seconds
  timestamp: number;
  isVideo: boolean;
}

export default function CallHistoryPage() {
  const router = useRouter();
  const { isAuthenticated } = useAuth();
  const { t, locale } = useLanguage();
  const [callHistory, setCallHistory] = useState<CallHistory[]>([]);
  const [filter, setFilter] = useState<'all' | 'incoming' | 'outgoing' | 'missed'>('all');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!isAuthenticated) {
      router.push('/');
      return;
    }

    loadCallHistory();
  }, [isAuthenticated, router]);

  const loadCallHistory = async () => {
    try {
      setLoading(true);
      // Try API first, fallback to localStorage
      const response = await apiClient.getCallHistory();
      if (response.success && response.data) {
        setCallHistory(response.data);
      } else {
        // Fallback to localStorage
        const stored = localStorage.getItem('callHistory');
        if (stored) {
          setCallHistory(JSON.parse(stored));
        }
      }
    } catch (error) {
      // Fallback to localStorage
      const stored = localStorage.getItem('callHistory');
      if (stored) {
        setCallHistory(JSON.parse(stored));
      }
    } finally {
      setLoading(false);
    }
  };

  const formatDuration = (seconds: number): string => {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    
    if (hours > 0) {
      return `${hours}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    }
    return `${minutes}:${secs.toString().padStart(2, '0')}`;
  };

  const formatDate = (timestamp: number): string => {
    const date = new Date(timestamp);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));

    if (days === 0) {
      return locale === 'tr' ? 'Bugün' : 'Today';
    } else if (days === 1) {
      return locale === 'tr' ? 'Dün' : 'Yesterday';
    } else if (days < 7) {
      return locale === 'tr' ? `${days} gün önce` : `${days} days ago`;
    }

    return date.toLocaleDateString(locale === 'tr' ? 'tr-TR' : 'en-US', {
      day: 'numeric',
      month: 'short',
      year: date.getFullYear() !== now.getFullYear() ? 'numeric' : undefined,
    });
  };

  const getCallTypeLabel = (type: string): string => {
    if (locale === 'tr') {
      switch (type) {
        case 'incoming': return 'Gelen';
        case 'outgoing': return 'Giden';
        case 'missed': return 'Kaçırılan';
        default: return type;
      }
    } else {
      switch (type) {
        case 'incoming': return 'Incoming';
        case 'outgoing': return 'Outgoing';
        case 'missed': return 'Missed';
        default: return type;
      }
    }
  };

  const getCallTypeIcon = (type: string): string => {
    switch (type) {
      case 'incoming': return '📥';
      case 'outgoing': return '📤';
      case 'missed': return '📞';
      default: return '📞';
    }
  };

  const filteredHistory = callHistory.filter(call => {
    if (filter === 'all') return true;
    return call.callType === filter;
  });

  const sortedHistory = [...filteredHistory].sort((a, b) => b.timestamp - a.timestamp);

  if (!isAuthenticated) {
    return null;
  }

  return (
    <div className="min-h-screen bg-gray-900 text-white w-full pb-20">
      {/* Header */}
      <header className="bg-gray-800 border-b border-gray-700 px-6 py-4">
        <div className="flex items-center justify-between">
          <h1 className="text-xs font-medium">
            {locale === 'tr' ? 'Görüşme Geçmişi' : 'Call History'}
          </h1>
          <button
            onClick={() => router.back()}
            className="text-gray-300 hover:text-white transition"
          >
            ✕
          </button>
        </div>
      </header>

      {/* Filters */}
      <div className="bg-gray-800 border-b border-gray-700 px-6 py-3">
        <div className="flex gap-2">
          {(['all', 'incoming', 'outgoing', 'missed'] as const).map((f) => (
            <button
              key={f}
              onClick={() => setFilter(f)}
              className={`px-4 py-2 rounded text-xs transition ${
                filter === f
                  ? 'bg-blue-600 text-white'
                  : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
              }`}
            >
              {locale === 'tr' 
                ? (f === 'all' ? 'Tümü' : f === 'incoming' ? 'Gelen' : f === 'outgoing' ? 'Giden' : 'Kaçırılan')
                : (f === 'all' ? 'All' : f === 'incoming' ? 'Incoming' : f === 'outgoing' ? 'Outgoing' : 'Missed')
              }
            </button>
          ))}
        </div>
      </div>

      {/* Call History List */}
      <main className="p-6 space-y-4 w-full">
        {loading ? (
          <div className="text-center py-8 text-gray-400">
            {locale === 'tr' ? 'Yükleniyor...' : 'Loading...'}
          </div>
        ) : sortedHistory.length === 0 ? (
          <div className="text-center py-8 text-gray-400">
            {locale === 'tr' ? 'Görüşme geçmişi bulunamadı' : 'No call history found'}
          </div>
        ) : (
          sortedHistory.map((call) => (
            <div
              key={call.id}
              className="bg-gray-800 rounded p-4 hover:bg-gray-750 transition cursor-pointer"
              onClick={() => {
                // Navigate to call with contact
                router.push(`/call?contact=${call.contactPhone}`);
              }}
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-4 flex-1">
                  <div className="text-xl">
                    {getCallTypeIcon(call.callType)}
                  </div>
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      <span className="font-medium text-xs">{call.contactName || call.contactPhone}</span>
                      {call.isVideo && (
                        <span className="text-xs bg-blue-600 px-2 py-1 rounded">
                          {locale === 'tr' ? 'Video' : 'Video'}
                        </span>
                      )}
                    </div>
                    <div className="flex items-center gap-3 text-xs font-medium text-gray-400">
                      <span>{getCallTypeLabel(call.callType)}</span>
                      <span>•</span>
                      <span>{formatDate(call.timestamp)}</span>
                      {call.duration > 0 && (
                        <>
                          <span>•</span>
                          <span>{formatDuration(call.duration)}</span>
                        </>
                      )}
                    </div>
                  </div>
                </div>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    router.push(`/call?contact=${call.contactPhone}`);
                  }}
                  className="text-blue-400 hover:text-blue-300 transition"
                >
                  📞
                </button>
              </div>
            </div>
          ))
        )}
      </main>
    </div>
  );
}

