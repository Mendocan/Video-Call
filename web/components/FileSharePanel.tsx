'use client';

import { useState, useRef } from 'react';
import { useLanguage } from '@/contexts/LanguageContext';
import { SignalingClient } from '@/lib/signaling/client';

interface SharedFile {
  id: string;
  fileName: string;
  fileSize: number;
  mimeType: string;
  senderPhoneNumber: string;
  senderName?: string;
  timestamp: number;
  isOwn: boolean;
}

interface FileSharePanelProps {
  signalingClient: SignalingClient | null;
  currentUserPhoneNumber: string;
  currentUserName?: string;
  isOpen: boolean;
  onClose: () => void;
}

export default function FileSharePanel({
  signalingClient,
  currentUserPhoneNumber,
  currentUserName,
  isOpen,
  onClose,
}: FileSharePanelProps) {
  const { t } = useLanguage();
  const [sharedFiles, setSharedFiles] = useState<SharedFile[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileSelect = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file || !signalingClient) return;

    try {
      // Read file as base64
      const reader = new FileReader();
      reader.onload = () => {
        const base64Data = reader.result as string;
        const fileId = Date.now().toString() + Math.random().toString(36).substring(7);

        // Send file share message
        signalingClient.send({
          type: 'file-share',
          fileId,
          fileName: file.name,
          fileSize: file.size,
          mimeType: file.type,
          senderPhoneNumber: currentUserPhoneNumber,
          senderName: currentUserName,
        });

        // Add to local list
        const sharedFile: SharedFile = {
          id: fileId,
          fileName: file.name,
          fileSize: file.size,
          mimeType: file.type,
          senderPhoneNumber: currentUserPhoneNumber,
          senderName: currentUserName,
          timestamp: Date.now(),
          isOwn: true,
        };
        setSharedFiles((prev) => [...prev, sharedFile]);
      };
      reader.readAsDataURL(file);
    } catch (error) {
      console.error('File share error:', error);
    }
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  };

  if (!isOpen) return null;

  return (
    <div className="fixed right-0 top-0 h-full w-80 bg-gray-800 border-l border-gray-700 flex flex-col z-40">
      {/* Header */}
      <div className="bg-gray-900 p-4 flex items-center justify-between border-b border-gray-700">
        <h3 className="text-xs font-medium">{t('call.files')}</h3>
        <button
          onClick={onClose}
          className="text-gray-400 hover:text-white transition-colors"
        >
          ✕
        </button>
      </div>

      {/* Files List */}
      <div className="flex-1 overflow-y-auto p-4 space-y-3">
        {sharedFiles.length === 0 ? (
          <div className="text-center text-gray-400 text-xs font-medium py-8">
            {t('call.noFiles') || 'Henüz dosya paylaşılmadı'}
          </div>
        ) : (
          sharedFiles.map((file) => (
            <div
              key={file.id}
              className={`p-3 rounded-lg ${
                file.isOwn ? 'bg-blue-600/20' : 'bg-gray-700'
              }`}
            >
              <div className="flex items-start justify-between mb-1">
                <div className="flex-1 min-w-0">
                  <div className="text-xs font-medium truncate">{file.fileName}</div>
                  <div className="text-xs text-gray-400">
                    {formatFileSize(file.fileSize)} • {file.mimeType}
                  </div>
                </div>
              </div>
              {!file.isOwn && file.senderName && (
                <div className="text-xs text-gray-400 mt-1">
                  {t('call.from') || 'Gönderen'}: {file.senderName}
                </div>
              )}
              <div className="text-xs text-gray-500 mt-1">
                {new Date(file.timestamp).toLocaleTimeString('tr-TR', {
                  hour: '2-digit',
                  minute: '2-digit',
                })}
              </div>
            </div>
          ))
        )}
      </div>

      {/* Upload Button */}
      <div className="p-4 border-t border-gray-700">
        <input
          ref={fileInputRef}
          type="file"
          onChange={handleFileSelect}
          className="hidden"
        />
        <button
          onClick={() => fileInputRef.current?.click()}
          className="w-full px-4 py-2 bg-blue-600 hover:bg-blue-700 rounded-md transition-colors"
        >
          {t('call.shareFile') || 'Dosya Paylaş'}
        </button>
      </div>
    </div>
  );
}

