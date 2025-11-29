'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';
import { useLanguage } from '@/contexts/LanguageContext';

interface ScheduledCall {
  id: string;
  contactName: string;
  contactPhone: string;
  scheduledTime: number;
  note?: string;
  isCompleted: boolean;
  isCancelled: boolean;
}

export default function CalendarPage() {
  const router = useRouter();
  const { isAuthenticated } = useAuth();
  const { t, locale } = useLanguage();
  const [scheduledCalls, setScheduledCalls] = useState<ScheduledCall[]>([]);
  const [showScheduleDialog, setShowScheduleDialog] = useState(false);
  const [formData, setFormData] = useState({
    contactName: '',
    contactPhone: '',
    scheduledDate: '',
    scheduledTime: '',
    note: '',
  });

  useEffect(() => {
    if (!isAuthenticated) {
      router.push('/');
      return;
    }

    // localStorage'dan randevuları yükle
    const saved = localStorage.getItem('scheduledCalls');
    if (saved) {
      setScheduledCalls(JSON.parse(saved));
    }
  }, [isAuthenticated, router]);

  const saveScheduledCalls = (calls: ScheduledCall[]) => {
    setScheduledCalls(calls);
    localStorage.setItem('scheduledCalls', JSON.stringify(calls));
  };

  const handleSchedule = () => {
    if (!formData.contactName || !formData.contactPhone || !formData.scheduledDate || !formData.scheduledTime) {
      return;
    }

    const dateTime = new Date(`${formData.scheduledDate}T${formData.scheduledTime}`);
    const timestamp = dateTime.getTime();

    const newCall: ScheduledCall = {
      id: Date.now().toString(),
      contactName: formData.contactName,
      contactPhone: formData.contactPhone,
      scheduledTime: timestamp,
      note: formData.note || undefined,
      isCompleted: false,
      isCancelled: false,
    };

    saveScheduledCalls([...scheduledCalls, newCall]);
    setFormData({ contactName: '', contactPhone: '', scheduledDate: '', scheduledTime: '', note: '' });
    setShowScheduleDialog(false);
  };

  const handleComplete = (id: string) => {
    const updated = scheduledCalls.map(call =>
      call.id === id ? { ...call, isCompleted: true } : call
    );
    saveScheduledCalls(updated);
  };

  const handleCancel = (id: string) => {
    const updated = scheduledCalls.map(call =>
      call.id === id ? { ...call, isCancelled: true } : call
    );
    saveScheduledCalls(updated);
  };

  const handleDelete = (id: string) => {
    if (confirm(locale === 'tr' ? 'Randevuyu silmek istediğinize emin misiniz?' : 'Are you sure you want to delete this appointment?')) {
      saveScheduledCalls(scheduledCalls.filter(call => call.id !== id));
    }
  };

  const formatDateTime = (timestamp: number): string => {
    const date = new Date(timestamp);
    return date.toLocaleString(locale === 'tr' ? 'tr-TR' : 'en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const upcomingCalls = scheduledCalls
    .filter(call => !call.isCompleted && !call.isCancelled && call.scheduledTime > Date.now())
    .sort((a, b) => a.scheduledTime - b.scheduledTime);

  const pastCalls = scheduledCalls
    .filter(call => call.isCompleted || call.isCancelled || call.scheduledTime <= Date.now())
    .sort((a, b) => b.scheduledTime - a.scheduledTime);

  if (!isAuthenticated) {
    return null;
  }

  return (
    <div className="min-h-screen bg-gray-900 text-white w-full">
      {/* Header */}
      <header className="bg-gray-800 border-b border-gray-700 px-6 py-4">
        <div className="flex items-center justify-between">
          <h1 className="text-xs font-medium">
            {locale === 'tr' ? 'Randevular' : 'Calendar'}
          </h1>
          <button
            onClick={() => setShowScheduleDialog(true)}
            className="bg-cyan-500 hover:bg-cyan-600 px-4 py-2 rounded text-xs font-medium transition"
          >
            + {locale === 'tr' ? 'Yeni Randevu' : 'New Appointment'}
          </button>
        </div>
      </header>

      <main className="p-6 space-y-6 w-full">
        {scheduledCalls.length === 0 ? (
          <div className="text-center py-12 text-gray-400">
            <div className="text-3xl mb-4">📅</div>
            <p>{locale === 'tr' ? 'Henüz randevu yok' : 'No appointments yet'}</p>
          </div>
        ) : (
          <>
            {/* Yaklaşan Randevular */}
            {upcomingCalls.length > 0 && (
              <div>
                <h2 className="text-xs font-medium mb-4">
                  {locale === 'tr' ? 'Yaklaşan Randevular' : 'Upcoming Appointments'}
                </h2>
                <div className="space-y-3">
                  {upcomingCalls.map((call) => (
                    <div key={call.id} className="bg-gray-800 rounded p-4">
                      <div className="flex items-start justify-between">
                        <div className="flex-1">
                          <h3 className="font-medium text-xs">{call.contactName}</h3>
                          <p className="text-xs font-medium text-gray-400">{call.contactPhone}</p>
                          <p className="text-xs font-medium text-cyan-400 mt-1">
                            {formatDateTime(call.scheduledTime)}
                          </p>
                          {call.note && (
                            <p className="text-xs font-medium text-gray-300 mt-2">{call.note}</p>
                          )}
                        </div>
                        <div className="flex gap-2">
                          <button
                            onClick={() => router.push(`/call?contact=${call.contactPhone}`)}
                            className="px-3 py-1 bg-cyan-500 hover:bg-cyan-600 rounded text-xs font-medium transition"
                          >
                            {locale === 'tr' ? 'Ara' : 'Call'}
                          </button>
                          <button
                            onClick={() => handleComplete(call.id)}
                            className="px-3 py-1 bg-green-600 hover:bg-green-700 rounded text-xs font-medium transition"
                          >
                            ✓
                          </button>
                          <button
                            onClick={() => handleCancel(call.id)}
                            className="px-3 py-1 bg-yellow-600 hover:bg-yellow-700 rounded text-xs font-medium transition"
                          >
                            ✕
                          </button>
                          <button
                            onClick={() => handleDelete(call.id)}
                            className="px-3 py-1 bg-red-600 hover:bg-red-700 rounded text-xs font-medium transition"
                          >
                            🗑️
                          </button>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Geçmiş Randevular */}
            {pastCalls.length > 0 && (
              <div>
                <h2 className="text-xs font-medium mb-4">
                  {locale === 'tr' ? 'Geçmiş Randevular' : 'Past Appointments'}
                </h2>
                <div className="space-y-3">
                  {pastCalls.map((call) => (
                    <div key={call.id} className="bg-gray-800 rounded p-4 opacity-60">
                      <div className="flex items-start justify-between">
                        <div className="flex-1">
                          <h3 className="font-medium text-xs">{call.contactName}</h3>
                          <p className="text-xs font-medium text-gray-400">{call.contactPhone}</p>
                          <p className="text-xs font-medium text-gray-500 mt-1">
                            {formatDateTime(call.scheduledTime)}
                          </p>
                          {call.isCompleted && (
                            <span className="text-xs text-green-400 mt-1 block">
                              {locale === 'tr' ? 'Tamamlandı' : 'Completed'}
                            </span>
                          )}
                          {call.isCancelled && (
                            <span className="text-xs text-yellow-400 mt-1 block">
                              {locale === 'tr' ? 'İptal Edildi' : 'Cancelled'}
                            </span>
                          )}
                        </div>
                        <button
                          onClick={() => handleDelete(call.id)}
                          className="px-3 py-1 bg-red-600 hover:bg-red-700 rounded text-sm transition"
                        >
                          🗑️
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </>
        )}
      </main>

      {/* Yeni Randevu Dialog */}
      {showScheduleDialog && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-gray-800 rounded p-6 w-full max-w-lg mx-auto">
            <h2 className="text-xs font-medium mb-4">
              {locale === 'tr' ? 'Yeni Randevu' : 'New Appointment'}
            </h2>
            <div className="space-y-4">
              <div>
                <label className="block text-xs font-medium mb-1">
                  {locale === 'tr' ? 'Kişi Adı' : 'Contact Name'}
                </label>
                <input
                  type="text"
                  value={formData.contactName}
                  onChange={(e) => setFormData({ ...formData, contactName: e.target.value })}
                  className="w-full px-3 py-2 bg-gray-700 border border-gray-600 rounded text-xs font-medium text-white"
                  required
                />
              </div>
              <div>
                <label className="block text-xs font-medium mb-1">
                  {locale === 'tr' ? 'Telefon Numarası' : 'Phone Number'}
                </label>
                <input
                  type="tel"
                  value={formData.contactPhone}
                  onChange={(e) => setFormData({ ...formData, contactPhone: e.target.value })}
                  className="w-full px-3 py-2 bg-gray-700 border border-gray-600 rounded text-xs font-medium text-white"
                  required
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-medium mb-1">
                    {locale === 'tr' ? 'Tarih' : 'Date'}
                  </label>
                  <input
                    type="date"
                    value={formData.scheduledDate}
                    onChange={(e) => setFormData({ ...formData, scheduledDate: e.target.value })}
                    className="w-full px-3 py-2 bg-gray-700 border border-gray-600 rounded text-xs font-medium text-white"
                    required
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium mb-1">
                    {locale === 'tr' ? 'Saat' : 'Time'}
                  </label>
                  <input
                    type="time"
                    value={formData.scheduledTime}
                    onChange={(e) => setFormData({ ...formData, scheduledTime: e.target.value })}
                    className="w-full px-3 py-2 bg-gray-700 border border-gray-600 rounded text-xs font-medium text-white"
                    required
                  />
                </div>
              </div>
              <div>
                <label className="block text-xs font-medium mb-1">
                  {locale === 'tr' ? 'Not (İsteğe Bağlı)' : 'Note (Optional)'}
                </label>
                <textarea
                  value={formData.note}
                  onChange={(e) => setFormData({ ...formData, note: e.target.value })}
                  className="w-full px-3 py-2 bg-gray-700 border border-gray-600 rounded text-xs font-medium text-white"
                  rows={3}
                />
              </div>
              <div className="flex gap-2 mt-6">
                <button
                  onClick={() => {
                    setShowScheduleDialog(false);
                    setFormData({ contactName: '', contactPhone: '', scheduledDate: '', scheduledTime: '', note: '' });
                  }}
                  className="flex-1 px-4 py-2 bg-gray-700 hover:bg-gray-600 rounded text-xs font-medium transition-colors"
                >
                  {t('common.cancel')}
                </button>
                <button
                  onClick={handleSchedule}
                  className="flex-1 px-4 py-2 bg-cyan-500 hover:bg-cyan-600 rounded text-xs font-medium transition-colors"
                >
                  {t('common.save')}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

