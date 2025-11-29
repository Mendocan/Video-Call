'use client';

import { useState, useEffect } from 'react';
import { useLanguage } from '@/contexts/LanguageContext';
import { useAuth } from '@/contexts/AuthContext';
import Link from 'next/link';

interface Contact {
  id: string;
  name: string;
  phoneNumber: string;
  email?: string;
  isFavorite: boolean;
  note?: string;
}

export default function ContactsPage() {
  const { t, locale } = useLanguage();
  const { isAuthenticated } = useAuth();
  const [contacts, setContacts] = useState<Contact[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [showAddModal, setShowAddModal] = useState(false);
  const [showQRScanner, setShowQRScanner] = useState(false);
  const [editingContact, setEditingContact] = useState<Contact | null>(null);
  const [formData, setFormData] = useState({
    name: '',
    phoneNumber: '',
    email: '',
    note: '',
  });

  // Load contacts from localStorage (temporary, will be replaced with API)
  useEffect(() => {
    if (isAuthenticated) {
      const stored = localStorage.getItem('contacts');
      if (stored) {
        setContacts(JSON.parse(stored));
      }
    }
  }, [isAuthenticated]);

  // Save contacts to localStorage (temporary)
  const saveContacts = (newContacts: Contact[]) => {
    setContacts(newContacts);
    localStorage.setItem('contacts', JSON.stringify(newContacts));
  };

  const filteredContacts = contacts.filter(contact =>
    contact.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    contact.phoneNumber.includes(searchQuery) ||
    contact.email?.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const handleAdd = () => {
    const newContact: Contact = {
      id: Date.now().toString(),
      name: formData.name,
      phoneNumber: formData.phoneNumber,
      email: formData.email || undefined,
      isFavorite: false,
      note: formData.note || undefined,
    };
    saveContacts([...contacts, newContact]);
    setFormData({ name: '', phoneNumber: '', email: '', note: '' });
    setShowAddModal(false);
  };

  const handleEdit = (contact: Contact) => {
    setEditingContact(contact);
    setFormData({
      name: contact.name,
      phoneNumber: contact.phoneNumber,
      email: contact.email || '',
      note: contact.note || '',
    });
    setShowAddModal(true);
  };

  const handleUpdate = () => {
    if (!editingContact) return;
    const updated = contacts.map(c =>
      c.id === editingContact.id
        ? { ...c, ...formData, email: formData.email || undefined, note: formData.note || undefined }
        : c
    );
    saveContacts(updated);
    setFormData({ name: '', phoneNumber: '', email: '', note: '' });
    setEditingContact(null);
    setShowAddModal(false);
  };

  const handleDelete = (id: string) => {
    if (confirm(t('common.delete') + '?')) {
      saveContacts(contacts.filter(c => c.id !== id));
    }
  };

  const toggleFavorite = (id: string) => {
    const updated = contacts.map(c =>
      c.id === id ? { ...c, isFavorite: !c.isFavorite } : c
    );
    saveContacts(updated);
  };

  const startCall = (contact: Contact) => {
    // Navigate to call page with contact ID
    window.location.href = `/call?contact=${contact.id}`;
  };

  if (!isAuthenticated) {
    return (
      <div className="min-h-screen bg-gray-900 text-white flex items-center justify-center">
        <div className="text-center">
          <h1 className="text-xs font-medium mb-4">{t('contacts.title')}</h1>
          <p className="mb-4">{t('common.loading')}</p>
          <Link href="/login" className="text-blue-400 hover:underline">
            {t('welcome.login')}
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-900 text-white w-full">
      {/* Header */}
      <header className="bg-gray-800 p-4">
        <div className="w-full flex items-center justify-between px-4">
          <h1 className="text-xs font-medium">{t('contacts.title')}</h1>
          <div className="flex gap-2">
            <button
              onClick={() => {
                // Export contacts as JSON
                const dataStr = JSON.stringify(contacts, null, 2);
                const dataBlob = new Blob([dataStr], { type: 'application/json' });
                const url = URL.createObjectURL(dataBlob);
                const link = document.createElement('a');
                link.href = url;
                link.download = 'contacts.json';
                link.click();
                URL.revokeObjectURL(url);
              }}
              className="px-3 py-2 bg-green-600 hover:bg-green-700 rounded text-xs font-medium transition-colors"
              title={locale === 'tr' ? 'Kişileri Dışa Aktar' : 'Export Contacts'}
            >
              📥
            </button>
            <label className="px-3 py-2 bg-blue-600 hover:bg-blue-700 rounded text-xs font-medium transition-colors cursor-pointer">
              📤
              <input
                type="file"
                accept=".json"
                className="hidden"
                onChange={(e) => {
                  const file = e.target.files?.[0];
                  if (file) {
                    const reader = new FileReader();
                    reader.onload = (event) => {
                      try {
                        const imported = JSON.parse(event.target?.result as string);
                        if (Array.isArray(imported)) {
                          // Merge with existing contacts (avoid duplicates by phone number)
                          const merged = [...contacts];
                          imported.forEach((contact: Contact) => {
                            if (!merged.find(c => c.phoneNumber === contact.phoneNumber)) {
                              merged.push(contact);
                            }
                          });
                          saveContacts(merged);
                          alert(locale === 'tr' ? 'Kişiler başarıyla içe aktarıldı' : 'Contacts imported successfully');
                        }
                      } catch (error) {
                        alert(locale === 'tr' ? 'Dosya okunamadı' : 'Failed to read file');
                      }
                    };
                    reader.readAsText(file);
                  }
                }}
              />
            </label>
            <button
              onClick={() => setShowQRScanner(true)}
              className="px-3 py-2 bg-purple-600 hover:bg-purple-700 rounded text-xs font-medium transition-colors"
              title={locale === 'tr' ? 'QR Kod ile İçe Aktar' : 'Import via QR Code'}
            >
              📷
            </button>
            <button
              onClick={() => {
                setEditingContact(null);
                setFormData({ name: '', phoneNumber: '', email: '', note: '' });
                setShowAddModal(true);
              }}
              className="px-4 py-2 bg-cyan-500 hover:bg-cyan-600 rounded text-xs font-medium transition-colors"
            >
              {t('contacts.addContact')}
            </button>
          </div>
        </div>
      </header>

      {/* Search */}
      <div className="w-full p-4">
        <input
          type="text"
          placeholder={t('contacts.search')}
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded text-xs font-medium text-white placeholder-gray-500"
        />
      </div>

      {/* Contacts List */}
      <main className="w-full max-w-4xl mx-auto p-4">
        {filteredContacts.length === 0 ? (
          <div className="text-center py-12 text-gray-400">
            {searchQuery ? t('contacts.noContacts') : t('contacts.noContacts')}
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {filteredContacts
              .sort((a, b) => {
                if (a.isFavorite !== b.isFavorite) {
                  return a.isFavorite ? -1 : 1;
                }
                return a.name.localeCompare(b.name);
              })
              .map((contact) => (
                <div
                  key={contact.id}
                  className="bg-gray-800 rounded p-4 hover:bg-gray-700 transition-colors"
                >
                  <div className="flex items-start justify-between mb-2">
                    <div className="flex-1">
                      <div className="flex items-center gap-2">
                        <h3 className="text-xs font-medium">{contact.name}</h3>
                        {contact.isFavorite && <span>⭐</span>}
                      </div>
                      <p className="text-gray-400 text-xs font-medium">{contact.phoneNumber}</p>
                      {contact.email && (
                        <p className="text-gray-500 text-xs">{contact.email}</p>
                      )}
                    </div>
                  </div>
                  {contact.note && (
                    <p className="text-gray-400 text-xs font-medium mb-3">{contact.note}</p>
                  )}
                  <div className="flex gap-2 mt-4">
                    <button
                      onClick={() => startCall(contact)}
                      className="flex-1 px-3 py-2 bg-blue-600 hover:bg-blue-700 rounded text-xs transition-colors"
                    >
                      {t('contacts.videoCall')}
                    </button>
                    <button
                      onClick={() => toggleFavorite(contact.id)}
                      className="px-3 py-2 bg-gray-700 hover:bg-gray-600 rounded text-xs transition-colors"
                      title={contact.isFavorite ? t('contacts.favorite') : ''}
                    >
                      {contact.isFavorite ? '⭐' : '☆'}
                    </button>
                    <button
                      onClick={() => handleEdit(contact)}
                      className="px-3 py-2 bg-gray-700 hover:bg-gray-600 rounded text-xs transition-colors"
                    >
                      {t('contacts.edit')}
                    </button>
                    <button
                      onClick={() => handleDelete(contact.id)}
                      className="px-3 py-2 bg-red-600 hover:bg-red-700 rounded text-xs font-medium transition-colors"
                    >
                      {t('contacts.remove')}
                    </button>
                  </div>
                </div>
              ))}
          </div>
        )}
      </main>

      {/* Add/Edit Modal */}
      {showAddModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-gray-800 rounded p-6 w-full max-w-lg mx-auto">
            <h2 className="text-xs font-medium mb-4">
              {editingContact ? t('contacts.edit') : t('contacts.addContact')}
            </h2>
            <div className="space-y-4">
              <div>
                <label className="block text-xs font-medium mb-1">
                  {t('contacts.name')}
                </label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  className="w-full px-3 py-2 bg-gray-700 border border-gray-600 rounded text-xs font-medium text-white"
                  required
                />
              </div>
              <div>
                <label className="block text-xs font-medium mb-1">
                  {t('contacts.phoneNumber')}
                </label>
                <input
                  type="tel"
                  value={formData.phoneNumber}
                  onChange={(e) => setFormData({ ...formData, phoneNumber: e.target.value })}
                  className="w-full px-3 py-2 bg-gray-700 border border-gray-600 rounded text-xs font-medium text-white"
                  required
                />
              </div>
              <div>
                <label className="block text-xs font-medium mb-1">
                  {t('contacts.email')}
                </label>
                <input
                  type="email"
                  value={formData.email}
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                  className="w-full px-3 py-2 bg-gray-700 border border-gray-600 rounded text-xs font-medium text-white"
                />
              </div>
              <div>
                <label className="block text-xs font-medium mb-1">
                  {t('common.note') || 'Not'}
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
                    setShowAddModal(false);
                    setEditingContact(null);
                    setFormData({ name: '', phoneNumber: '', email: '', note: '' });
                  }}
                  className="flex-1 px-4 py-2 bg-gray-700 hover:bg-gray-600 rounded text-xs font-medium transition-colors"
                >
                  {t('common.cancel')}
                </button>
                <button
                  onClick={editingContact ? handleUpdate : handleAdd}
                  className="flex-1 px-4 py-2 bg-cyan-500 hover:bg-cyan-600 rounded text-xs font-medium transition-colors"
                >
                  {t('common.save')}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* QR Code Scanner Modal */}
      {showQRScanner && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-gray-800 rounded p-6 w-full max-w-lg mx-auto">
            <h2 className="text-xs font-medium mb-4">
              {locale === 'tr' ? 'QR Kod Okut' : 'Scan QR Code'}
            </h2>
            <div className="mb-4">
              <p className="text-xs font-medium text-gray-400 mb-4">
                {locale === 'tr' 
                  ? 'Mobil uygulamadan oluşturulan QR kodu okutun veya içeriğini buraya yapıştırın'
                  : 'Scan the QR code generated from the mobile app or paste the content here'}
              </p>
              <input
                type="text"
                placeholder={locale === 'tr' ? 'QR kod içeriğini buraya yapıştırın (CONTACTS: ile başlamalı)' : 'Paste QR code content here (must start with CONTACTS:)'}
                className="w-full px-3 py-2 bg-gray-700 border border-gray-600 rounded text-xs font-medium text-white placeholder-gray-500 mb-4"
                id="qr-input"
              />
              <div className="flex gap-2">
                <button
                  onClick={() => {
                    const input = document.getElementById('qr-input') as HTMLInputElement;
                    const qrData = input?.value || '';
                    if (qrData.startsWith('CONTACTS:')) {
                      try {
                        const base64Data = qrData.substring(9); // Remove "CONTACTS:" prefix
                        const jsonString = atob(base64Data);
                        const imported = JSON.parse(jsonString);
                        if (Array.isArray(imported)) {
                          const merged = [...contacts];
                          imported.forEach((contact: Contact) => {
                            if (!merged.find(c => c.phoneNumber === contact.phoneNumber)) {
                              merged.push(contact);
                            }
                          });
                          saveContacts(merged);
                          alert(locale === 'tr' ? 'Kişiler başarıyla içe aktarıldı' : 'Contacts imported successfully');
                          setShowQRScanner(false);
                        }
                      } catch (error) {
                        alert(locale === 'tr' ? 'QR kod okunamadı' : 'Failed to read QR code');
                      }
                    } else {
                      alert(locale === 'tr' ? 'Geçersiz QR kod formatı' : 'Invalid QR code format');
                    }
                  }}
                  className="flex-1 px-4 py-2 bg-cyan-500 hover:bg-cyan-600 rounded text-xs font-medium transition-colors"
                >
                  {locale === 'tr' ? 'İçe Aktar' : 'Import'}
                </button>
                <button
                  onClick={() => setShowQRScanner(false)}
                  className="flex-1 px-4 py-2 bg-gray-700 hover:bg-gray-600 rounded text-xs font-medium transition-colors"
                >
                  {t('common.cancel')}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

