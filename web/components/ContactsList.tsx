'use client';

import { useState, useEffect } from 'react';
import { useLanguage } from '@/contexts/LanguageContext';
import { useAuth } from '@/contexts/AuthContext';
import { useRouter } from 'next/navigation';
import { HiStar, HiPhone } from 'react-icons/hi';

interface Contact {
  id: string;
  name: string;
  phoneNumber: string;
  email?: string;
  isFavorite: boolean;
  note?: string;
}

export default function ContactsList() {
  const { t } = useLanguage();
  const { isAuthenticated } = useAuth();
  const router = useRouter();
  const [contacts, setContacts] = useState<Contact[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedContact, setSelectedContact] = useState<string | null>(null);

  // Load contacts from localStorage (temporary, will be replaced with API)
  useEffect(() => {
    if (isAuthenticated) {
      const stored = localStorage.getItem('contacts');
      if (stored) {
        setContacts(JSON.parse(stored));
      }
    }
  }, [isAuthenticated]);

  const filteredContacts = contacts
    .filter(contact =>
      contact.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      contact.phoneNumber.includes(searchQuery) ||
      contact.email?.toLowerCase().includes(searchQuery.toLowerCase())
    )
    .sort((a, b) => {
      // Favoriler önce
      if (a.isFavorite && !b.isFavorite) return -1;
      if (!a.isFavorite && b.isFavorite) return 1;
      // Sonra alfabetik
      return a.name.localeCompare(b.name);
    });

  const startCall = (contact: Contact) => {
    router.push(`/call?contact=${contact.id}`);
  };

  if (!isAuthenticated) {
    return (
      <div className="w-64 bg-gray-800 border-r border-gray-700 flex items-center justify-center">
        <p className="text-xs font-medium text-gray-400">{t('common.loading')}</p>
      </div>
    );
  }

  return (
    <div className="w-64 bg-gray-800 border-r border-gray-700 flex flex-col h-full">
      {/* Header */}
      <div className="p-4 border-b border-gray-700">
        <h2 className="text-xs font-medium mb-2">{t('contacts.title')}</h2>
        <input
          type="text"
          placeholder={t('contacts.search')}
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="w-full px-3 py-2 bg-gray-700 border border-gray-600 rounded text-xs font-medium text-white placeholder-gray-500 focus:outline-none focus:border-cyan-500"
        />
      </div>

      {/* Contacts List */}
      <div className="flex-1 overflow-y-auto">
        {filteredContacts.length === 0 ? (
          <div className="p-4 text-center">
            <p className="text-xs font-medium text-gray-400">{t('contacts.noContacts')}</p>
          </div>
        ) : (
          <div className="divide-y divide-gray-700">
            {filteredContacts.map((contact) => (
              <div
                key={contact.id}
                onClick={() => {
                  setSelectedContact(contact.id);
                  startCall(contact);
                }}
                className={`p-3 cursor-pointer hover:bg-gray-700 transition-colors ${
                  selectedContact === contact.id ? 'bg-gray-700' : ''
                }`}
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2 flex-1 min-w-0">
                    {contact.isFavorite && (
                      <HiStar className="text-yellow-400 text-sm" />
                    )}
                    <div className="flex-1 min-w-0">
                      <p className="text-xs font-medium truncate">{contact.name}</p>
                      <p className="text-xs text-gray-400 truncate">{contact.phoneNumber}</p>
                    </div>
                  </div>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      startCall(contact);
                    }}
                    className="ml-2 p-1 text-blue-400 hover:text-blue-300 transition-colors"
                    title={t('call.startCall')}
                  >
                    <HiPhone className="text-lg" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

