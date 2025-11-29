'use client';

import { useState, useEffect, useRef } from 'react';
import { useLanguage } from '@/contexts/LanguageContext';
import { SignalingClient } from '@/lib/signaling/client';

interface ChatMessage {
  id: string;
  message: string;
  senderPhoneNumber: string;
  senderName?: string;
  timestamp: number;
  isOwn: boolean;
}

interface ChatPanelProps {
  signalingClient: SignalingClient | null;
  currentUserPhoneNumber: string;
  currentUserName?: string;
  isOpen: boolean;
  onClose: () => void;
}

export default function ChatPanel({
  signalingClient,
  currentUserPhoneNumber,
  currentUserName,
  isOpen,
  onClose,
}: ChatPanelProps) {
  const { t } = useLanguage();
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputMessage, setInputMessage] = useState('');
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!signalingClient) return;

    const unsubscribe = signalingClient.onMessage('chat', (message) => {
      if (message.type === 'chat') {
        const chatMessage: ChatMessage = {
          id: Date.now().toString() + Math.random(),
          message: message.message,
          senderPhoneNumber: message.senderPhoneNumber,
          senderName: message.senderName,
          timestamp: Date.now(),
          isOwn: message.senderPhoneNumber === currentUserPhoneNumber,
        };
        setMessages((prev) => [...prev, chatMessage]);
      }
    });

    return unsubscribe;
  }, [signalingClient, currentUserPhoneNumber]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const sendMessage = () => {
    if (!inputMessage.trim() || !signalingClient) return;

    signalingClient.sendChat(inputMessage, currentUserPhoneNumber, currentUserName);

    const chatMessage: ChatMessage = {
      id: Date.now().toString(),
      message: inputMessage,
      senderPhoneNumber: currentUserPhoneNumber,
      senderName: currentUserName,
      timestamp: Date.now(),
      isOwn: true,
    };
    setMessages((prev) => [...prev, chatMessage]);
    setInputMessage('');
  };

  return (
    <div className="h-full bg-gray-800 flex flex-col">
      {/* Header */}
      <div className="bg-gray-900 p-3 flex items-center justify-between border-b border-gray-700">
        <h3 className="text-xs font-medium">{t('call.chat')}</h3>
        {onClose && (
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-white transition-colors text-xs font-medium"
          >
            ✕
          </button>
        )}
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {messages.length === 0 ? (
          <div className="text-center text-gray-400 text-xs font-medium py-8">
            {t('call.noMessages') || 'Henüz mesaj yok'}
          </div>
        ) : (
          messages.map((msg) => (
            <div
              key={msg.id}
              className={`flex ${msg.isOwn ? 'justify-end' : 'justify-start'}`}
            >
              <div
                className={`max-w-[70%] rounded-lg p-3 ${
                  msg.isOwn
                    ? 'bg-blue-600 text-white'
                    : 'bg-gray-700 text-white'
                }`}
              >
                {!msg.isOwn && msg.senderName && (
                  <div className="text-xs text-gray-300 mb-1">{msg.senderName}</div>
                )}
                <div className="text-xs font-medium">{msg.message}</div>
                <div className="text-xs opacity-70 mt-1">
                  {new Date(msg.timestamp).toLocaleTimeString('tr-TR', {
                    hour: '2-digit',
                    minute: '2-digit',
                  })}
                </div>
              </div>
            </div>
          ))
        )}
        <div ref={messagesEndRef} />
      </div>

      {/* Input */}
      <div className="p-3 border-t border-gray-700">
        <div className="flex gap-2">
          <input
            type="text"
            value={inputMessage}
            onChange={(e) => setInputMessage(e.target.value)}
            onKeyPress={(e) => {
              if (e.key === 'Enter') {
                sendMessage();
              }
            }}
            placeholder={t('call.chat') + '...'}
            className="flex-1 px-3 py-2 bg-gray-700 border border-gray-600 rounded text-xs font-medium text-white placeholder-gray-500"
          />
          <button
            onClick={sendMessage}
            disabled={!inputMessage.trim()}
            className="px-3 py-2 bg-cyan-500 hover:bg-cyan-600 disabled:bg-gray-600 disabled:cursor-not-allowed rounded text-xs font-medium transition-colors"
          >
            {t('call.send') || 'Gönder'}
          </button>
        </div>
      </div>
    </div>
  );
}

