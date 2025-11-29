'use client';

import { useState, useEffect, useRef, useMemo } from 'react';
import { useRouter } from 'next/navigation';
import { useLanguage } from '@/contexts/LanguageContext';
import { useAuth } from '@/contexts/AuthContext';
import { SignalingClient } from '@/lib/signaling/client';
import { getSignalingServerUrl } from '@/lib/utils/signalingServerDiscovery';
import { HiArrowLeft, HiPaperAirplane } from 'react-icons/hi';

interface Contact {
  id: string;
  name: string;
  phoneNumber: string;
  email?: string;
  isFavorite: boolean;
}

interface ChatMessage {
  id: string;
  message: string;
  senderPhoneNumber: string;
  senderName?: string;
  timestamp: number;
  isFromMe: boolean;
}

const DEFAULT_SIGNALING_URL = process.env.NEXT_PUBLIC_SIGNALING_URL || 'ws://192.168.1.20:8080/ws';

export default function ChatPage() {
  const router = useRouter();
  const { t, locale } = useLanguage();
  const { user, isAuthenticated, loading } = useAuth();
  const [contacts, setContacts] = useState<Contact[]>([]);
  const [selectedContact, setSelectedContact] = useState<Contact | null>(null);
  const [messageText, setMessageText] = useState('');
  const [chatMessagesByContact, setChatMessagesByContact] = useState<Map<string, ChatMessage[]>>(new Map());
  const [signalingClient, setSignalingClient] = useState<SignalingClient | null>(null);
  const [signalingUrl, setSignalingUrl] = useState<string>(DEFAULT_SIGNALING_URL);
  const [connectionStatus, setConnectionStatus] = useState<'idle' | 'connecting' | 'connected' | 'disconnected' | 'error'>('connecting');
  const [connectionError, setConnectionError] = useState<string | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const clientRef = useRef<SignalingClient | null>(null);
  const isConnectingRef = useRef(false);
  const phoneNumberRef = useRef<string | null>(null);
  
  // Otomatik IP bulma
  useEffect(() => {
    if (isAuthenticated) {
      getSignalingServerUrl(DEFAULT_SIGNALING_URL).then(url => {
        if (url !== signalingUrl) {
          console.log('[ChatPage] Signaling server URL güncellendi:', url);
          setSignalingUrl(url);
        }
      }).catch(error => {
        console.error('[ChatPage] Signaling server discovery hatası:', error);
        // Hata olsa bile devam et, default URL kullanılacak
      });
    }
  }, [isAuthenticated]);

  // Load contacts from localStorage
  useEffect(() => {
    if (isAuthenticated) {
      const stored = localStorage.getItem('contacts');
      if (stored) {
        setContacts(JSON.parse(stored));
      }
    }
  }, [isAuthenticated]);

  // Reconnect function
  const reconnectToSignaling = async () => {
    if (!isAuthenticated || !user?.phone) return;
    
    if (signalingClient) {
      signalingClient.disconnect();
    }
    
    setConnectionStatus('connecting');
    setConnectionError(null);
    
    // Güncel URL'i kullan
    const currentUrl = await getSignalingServerUrl(signalingUrl);
    const client = new SignalingClient(currentUrl);
    
    // Listen for status changes
    const statusUnsubscribe = client.onStatusChange((status) => {
      setConnectionStatus(status);
      if (status === 'error') {
        setConnectionError(locale === 'tr' ? 'Bağlantı hatası' : 'Connection error');
      } else if (status === 'connected') {
        setConnectionError(null);
      }
    });

    try {
      await client.connect();
      await new Promise(resolve => setTimeout(resolve, 100));
      client.register(user.phone, user.name || undefined);
      setSignalingClient(client);
      setConnectionStatus('connected');
    } catch (error: any) {
      console.error('Signaling connection error:', error?.message || error);
      setConnectionStatus('error');
      setConnectionError(error?.message || (locale === 'tr' ? 'Sunucuya bağlanılamadı' : 'Cannot connect to server'));
    }
  };

  // Initialize signaling client and register
  useEffect(() => {
    if (!isAuthenticated || !user?.phone) {
      // Eğer authenticated değilse, mevcut bağlantıyı kapat
      if (clientRef.current) {
        clientRef.current.disconnect();
        clientRef.current = null;
        isConnectingRef.current = false;
        phoneNumberRef.current = null;
      }
      return;
    }
    
    const currentPhone = user.phone;
    
    // Telefon numarası değişmediyse ve zaten bağlıysa tekrar bağlanma
    if (phoneNumberRef.current === currentPhone && clientRef.current && clientRef.current.isConnected()) {
      return;
    }
    
    // Eğer telefon numarası değiştiyse eski bağlantıyı kapat
    if (phoneNumberRef.current && phoneNumberRef.current !== currentPhone && clientRef.current) {
      clientRef.current.disconnect();
      clientRef.current = null;
      isConnectingRef.current = false;
    }
    
    // Zaten bağlanıyorsa bekle
    if (isConnectingRef.current) {
      return;
    }

    isConnectingRef.current = true;
    phoneNumberRef.current = currentPhone;
    
    // Güncel URL'i kullan (async olarak al)
    const initializeClient = async () => {
      try {
        const currentUrl = await getSignalingServerUrl(signalingUrl);
        const client = new SignalingClient(currentUrl);
        clientRef.current = client;
        
        // Register user
        try {
          setConnectionStatus('connecting');
          setConnectionError(null);
          
          // Connect without room code first
          await client.connect();
          
          // Wait a bit for connection to be ready
          await new Promise(resolve => setTimeout(resolve, 100));
          
          // Send register message
          client.register(currentPhone, user.name || undefined);
          
          setSignalingClient(client);
          setConnectionStatus('connected');
          isConnectingRef.current = false;
        } catch (error: any) {
          console.error('Signaling connection error:', error?.message || error);
          setConnectionStatus('error');
          setConnectionError(error?.message || (locale === 'tr' ? 'Sunucuya bağlanılamadı' : 'Cannot connect to server'));
          isConnectingRef.current = false;
        }
        
        // Listen for status changes
        const statusUnsubscribe = client.onStatusChange((status) => {
          setConnectionStatus(status);
          if (status === 'error') {
            setConnectionError(locale === 'tr' ? 'Bağlantı hatası' : 'Connection error');
          } else if (status === 'connected') {
            setConnectionError(null);
          }
        });
        
        // Listen for incoming chat messages
        const unsubscribe = client.onMessage('chat', (message: any) => {
          if (message.type === 'chat' && message.targetPhoneNumber === currentPhone) {
            const chatMessage: ChatMessage = {
              id: Date.now().toString() + Math.random(),
              message: message.message,
              senderPhoneNumber: message.senderPhoneNumber,
              senderName: message.senderName,
              timestamp: Date.now(),
              isFromMe: false
            };

            setChatMessagesByContact(prev => {
              const newMap = new Map(prev);
              const contactMessages = newMap.get(message.senderPhoneNumber) || [];
              newMap.set(message.senderPhoneNumber, [...contactMessages, chatMessage]);
              return newMap;
            });
          }
        });
        
        // Cleanup function
        return () => {
          unsubscribe();
          statusUnsubscribe();
          if (clientRef.current === client) {
            clientRef.current.disconnect();
            clientRef.current = null;
          }
          isConnectingRef.current = false;
        };
      } catch (error: any) {
        console.error('Signaling server discovery error:', error);
        isConnectingRef.current = false;
        return null;
      }
    };
    
    let cleanup: (() => void) | null = null;
    initializeClient().then(result => {
      cleanup = result || null;
    });

    // Cleanup - sadece telefon numarası değiştiğinde veya component unmount olduğunda
    return () => {
      // Sadece telefon numarası değiştiyse veya component unmount oluyorsa cleanup yap
      if (phoneNumberRef.current !== currentPhone || !isAuthenticated) {
        cleanup?.();
        if (phoneNumberRef.current !== currentPhone) {
          phoneNumberRef.current = null;
        }
      }
    };
  }, [isAuthenticated, user?.phone, locale]);

  // Scroll to bottom when messages change
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [chatMessagesByContact, selectedContact]);

  useEffect(() => {
    if (!loading && !isAuthenticated) {
      router.push('/');
      return;
    }
  }, [isAuthenticated, loading, router]);

  const filteredMessages = selectedContact
    ? chatMessagesByContact.get(selectedContact.phoneNumber) || []
    : [];

  const sendMessage = () => {
    if (!messageText.trim() || !selectedContact || !signalingClient || !user?.phone) return;

    const chatMessage: ChatMessage = {
      id: Date.now().toString(),
      message: messageText,
      senderPhoneNumber: user.phone,
      senderName: user.name || undefined,
      timestamp: Date.now(),
      isFromMe: true
    };

    // Add to local state
    setChatMessagesByContact(prev => {
      const newMap = new Map(prev);
      const contactMessages = newMap.get(selectedContact.phoneNumber) || [];
      newMap.set(selectedContact.phoneNumber, [...contactMessages, chatMessage]);
      return newMap;
    });

    // Send via signaling (kişiye özel)
    signalingClient.sendChat(
      messageText,
      user.phone,
      user.name || undefined,
      selectedContact.phoneNumber
    );

    setMessageText('');
  };

  const formatTime = (timestamp: number): string => {
    const date = new Date(timestamp);
    const now = new Date();
    const diff = now.getTime() - timestamp;

    if (diff < 60000) {
      return t('chat.now') || 'Şimdi';
    } else if (diff < 3600000) {
      const minutes = Math.floor(diff / 60000);
      return `${minutes} ${t('chat.minutesAgo') || 'dk önce'}`;
    } else if (diff < 86400000) {
      return date.toLocaleTimeString(locale === 'tr' ? 'tr-TR' : 'en-US', {
        hour: '2-digit',
        minute: '2-digit'
      });
    } else {
      return date.toLocaleString(locale === 'tr' ? 'tr-TR' : 'en-US', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    }
  };

  const getLastMessage = (phoneNumber: string): ChatMessage | null => {
    const messages = chatMessagesByContact.get(phoneNumber);
    return messages && messages.length > 0 ? messages[messages.length - 1] : null;
  };

  if (!isAuthenticated) {
    return null;
  }

  return (
    <div className="min-h-screen bg-gray-900 text-white w-full flex">
      {!selectedContact ? (
        // Contact list
        <div className="w-full p-6">
          <h1 className="text-xs font-medium mb-6">{t('chat.title')}</h1>
          
          {/* Connection status */}
          {connectionStatus === 'error' && connectionError && (
            <div className="mb-4 p-3 bg-red-900/50 border border-red-700 rounded text-xs font-medium">
              <p className="text-red-300 font-semibold">
                {locale === 'tr' ? '⚠️ Bağlantı Hatası' : '⚠️ Connection Error'}
              </p>
              <p className="text-red-300 mt-1">{connectionError}</p>
              <p className="text-red-400 mt-2 text-[10px]">
                {locale === 'tr' 
                  ? 'Sunucuya bağlanılamıyor. Lütfen:\n• İnternet bağlantınızı kontrol edin\n• Sunucunun çalıştığından emin olun\n• Firewall ayarlarını kontrol edin' 
                  : 'Cannot connect to server. Please:\n• Check your internet connection\n• Ensure the server is running\n• Check firewall settings'}
              </p>
              <button
                onClick={reconnectToSignaling}
                className="mt-2 px-3 py-1.5 bg-red-700 hover:bg-red-600 rounded text-[10px] font-medium transition-colors"
              >
                {locale === 'tr' ? '🔄 Yeniden Dene' : '🔄 Retry'}
              </button>
            </div>
          )}
          
          {connectionStatus === 'connecting' && (
            <div className="mb-4 p-3 bg-yellow-900/50 border border-yellow-700 rounded text-xs font-medium">
              <p className="text-yellow-300">
                {locale === 'tr' ? 'Sunucuya bağlanılıyor...' : 'Connecting to server...'}
              </p>
            </div>
          )}
          
          {contacts.length === 0 ? (
            <div className="text-center py-12 text-gray-400">
              <p className="text-xs font-medium">{t('chat.noContacts')}</p>
            </div>
          ) : (
            <div className="space-y-2">
              {contacts.map((contact) => {
                const lastMessage = getLastMessage(contact.phoneNumber);
                return (
                  <div
                    key={contact.id}
                    onClick={() => setSelectedContact(contact)}
                    className="bg-gray-800 rounded p-4 cursor-pointer hover:bg-gray-700 transition"
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex-1">
                        <h3 className="text-xs font-medium font-semibold">{contact.name}</h3>
                        {lastMessage && (
                          <>
                            <p className="text-xs font-medium text-gray-400 mt-1 truncate">
                              {lastMessage.message}
                            </p>
                          </>
                        )}
                      </div>
                      {lastMessage && (
                        <span className="text-xs font-medium text-gray-500 ml-4">
                          {formatTime(lastMessage.timestamp)}
                        </span>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      ) : (
        // Chat detail
        <div className="w-full flex flex-col">
          {/* Header */}
          <div className="bg-gray-800 border-b border-gray-700 px-6 py-4 flex items-center gap-4">
            <button
              onClick={() => setSelectedContact(null)}
              className="text-gray-400 hover:text-white transition"
            >
              <HiArrowLeft className="text-xl" />
            </button>
            <h2 className="text-xs font-medium font-semibold">{selectedContact.name}</h2>
          </div>

          {/* Messages */}
          <div className="flex-1 overflow-y-auto p-6 space-y-4">
            {filteredMessages.length === 0 ? (
              <div className="text-center text-gray-400 py-12">
                <p className="text-xs font-medium">{t('chat.noMessages')}</p>
              </div>
            ) : (
              filteredMessages.map((message) => (
                <div
                  key={message.id}
                  className={`flex ${message.isFromMe ? 'justify-end' : 'justify-start'}`}
                >
                  <div
                    className={`max-w-[70%] rounded-lg p-3 ${
                      message.isFromMe
                        ? 'bg-cyan-500 text-white'
                        : 'bg-gray-700 text-white'
                    }`}
                  >
                    {!message.isFromMe && message.senderName && (
                      <div className="text-xs text-gray-300 mb-1">{message.senderName}</div>
                    )}
                    <div className="text-xs font-medium">{message.message}</div>
                    <div className="text-xs opacity-70 mt-1">
                      {formatTime(message.timestamp)}
                    </div>
                  </div>
                </div>
              ))
            )}
            <div ref={messagesEndRef} />
          </div>

          {/* Input */}
          <div className="bg-gray-800 border-t border-gray-700 p-4">
            <div className="flex gap-2">
              <input
                type="text"
                value={messageText}
                onChange={(e) => setMessageText(e.target.value)}
                onKeyPress={(e) => {
                  if (e.key === 'Enter') {
                    sendMessage();
                  }
                }}
                placeholder={t('chat.typeMessage')}
                className="flex-1 px-3 py-2 bg-gray-700 border border-gray-600 rounded text-xs font-medium text-white placeholder-gray-500"
              />
              <button
                onClick={sendMessage}
                disabled={!messageText.trim()}
                className="px-4 py-2 bg-cyan-500 hover:bg-cyan-600 disabled:bg-gray-600 disabled:cursor-not-allowed rounded text-xs font-medium transition-colors flex items-center gap-2"
              >
                <HiPaperAirplane className="text-sm" />
                {t('chat.send')}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

