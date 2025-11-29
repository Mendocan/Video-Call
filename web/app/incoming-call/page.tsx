'use client';

import { useState, useEffect, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';
import { useLanguage } from '@/contexts/LanguageContext';
import { SignalingClient } from '@/lib/signaling/client';
import { WebRTCClient } from '@/lib/webrtc/client';
import { getSignalingServerUrl } from '@/lib/utils/signalingServerDiscovery';

const DEFAULT_SIGNALING_URL = process.env.NEXT_PUBLIC_SIGNALING_URL || 'wss://signaling.videocall.com/ws';

interface IncomingCallData {
  groupId: string;
  roomCode: string;
  callerName: string;
  callerPhone: string;
  isVideo: boolean;
}

function IncomingCallContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { isAuthenticated, user } = useAuth();
  const { t, locale } = useLanguage();
  const [callData, setCallData] = useState<IncomingCallData | null>(null);
  const [isAnswering, setIsAnswering] = useState(false);

  useEffect(() => {
    if (!isAuthenticated || !user?.phone) {
      router.push('/');
      return;
    }

    // Initialize signaling and listen for incoming calls
    const initializeSignaling = async () => {
      try {
        // Otomatik IP bulma ile signaling URL'i al
        const signalingUrl = await getSignalingServerUrl(DEFAULT_SIGNALING_URL);
        const signaling = new SignalingClient(signalingUrl);
        await signaling.connect();
        signaling.register(user.phone, user.name || undefined);

        // Listen for incoming calls
        signaling.onMessage('incoming-call', (message) => {
          if (message.type === 'incoming-call') {
            setCallData({
              groupId: message.groupId,
              roomCode: message.roomCode,
              callerName: message.callerName || message.callerPhoneNumber,
              callerPhone: message.callerPhoneNumber,
              isVideo: true, // Default to video call
            });
          }
        });

        // Check localStorage for incoming call notification (from notification click)
        const incomingCall = localStorage.getItem('incomingCall');
        if (incomingCall) {
          const data = JSON.parse(incomingCall);
          setCallData(data);
          localStorage.removeItem('incomingCall');
        }
      } catch (error) {
        console.error('Signaling initialization error:', error);
        router.push('/home');
      }
    };

    initializeSignaling();
  }, [isAuthenticated, user, router]);

  const handleAnswer = async () => {
    if (!callData || !user?.phone) return;

    setIsAnswering(true);
    
    try {
      // Otomatik IP bulma ile signaling URL'i al
      const signalingUrl = await getSignalingServerUrl(DEFAULT_SIGNALING_URL);
      const signaling = new SignalingClient(signalingUrl);
      await signaling.connect();
      signaling.register(user.phone, user.name || undefined);
      
      // Accept the call
      signaling.acceptCall(callData.groupId);
      
      // Navigate to call page and join
      router.push(`/call?join=true&groupId=${callData.groupId}&roomCode=${callData.roomCode}`);
    } catch (error) {
      console.error('Error accepting call:', error);
      setIsAnswering(false);
    }
  };

  const handleReject = async () => {
    if (!callData || !user?.phone) return;

    // Send reject signal if signaling is connected
    try {
      // Otomatik IP bulma ile signaling URL'i al
      const signalingUrl = await getSignalingServerUrl(DEFAULT_SIGNALING_URL);
      const signaling = new SignalingClient(signalingUrl);
      await signaling.connect();
      signaling.register(user.phone, user.name || undefined);
      signaling.rejectCall(callData.groupId);
      signaling.disconnect();
    } catch (error) {
      console.error('Error rejecting call:', error);
    }

    // Save as missed call
    const missedCall = {
      id: Date.now().toString(),
      contactName: callData.callerName,
      contactPhone: callData.callerPhone,
      callType: 'missed',
      duration: 0,
      timestamp: Date.now(),
      isVideo: callData.isVideo,
    };

    const existing = localStorage.getItem('callHistory');
    const history = existing ? JSON.parse(existing) : [];
    history.unshift(missedCall);
    localStorage.setItem('callHistory', JSON.stringify(history.slice(0, 100))); // Keep last 100

    router.push('/home');
  };

  if (!isAuthenticated || !callData) {
    return null;
  }

  return (
    <div className="min-h-screen bg-gray-900 text-white flex items-center justify-center">
      <div className="bg-gray-800 rounded p-8 max-w-md w-full mx-4 text-center">
        <div className="mb-6">
          <div className="text-6xl mb-4 animate-pulse">📞</div>
          <h2 className="text-xs font-medium mb-2">
            {locale === 'tr' ? 'Gelen Arama' : 'Incoming Call'}
          </h2>
          <p className="text-xs font-medium text-gray-300 mb-1">
            {callData.callerName || callData.callerPhone}
          </p>
          {callData.isVideo && (
            <p className="text-xs font-medium text-cyan-400">
              {locale === 'tr' ? 'Görüntülü Arama' : 'Video Call'}
            </p>
          )}
        </div>

        <div className="flex gap-4 justify-center">
          <button
            onClick={handleReject}
            disabled={isAnswering}
            className="bg-red-600 hover:bg-red-700 disabled:bg-gray-600 disabled:cursor-not-allowed rounded w-14 h-14 flex items-center justify-center transition text-xl"
          >
            ✕
          </button>
          <button
            onClick={handleAnswer}
            disabled={isAnswering}
            className="bg-green-600 hover:bg-green-700 disabled:bg-gray-600 disabled:cursor-not-allowed rounded w-14 h-14 flex items-center justify-center transition text-xl"
          >
            ✓
          </button>
        </div>

        {isAnswering && (
          <p className="mt-4 text-gray-400 text-xs font-medium">
            {locale === 'tr' ? 'Bağlanılıyor...' : 'Connecting...'}
          </p>
        )}
      </div>
    </div>
  );
}

export default function IncomingCallPage() {
  return (
    <Suspense fallback={
      <div className="min-h-screen bg-gray-900 text-white flex items-center justify-center">
        <div className="text-center">
          <p className="text-xs font-medium text-gray-400">Yükleniyor...</p>
        </div>
      </div>
    }>
      <IncomingCallContent />
    </Suspense>
  );
}

