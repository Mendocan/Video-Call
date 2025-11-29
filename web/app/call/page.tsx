'use client';

import { useState, useEffect, useRef } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { useLanguage } from '@/contexts/LanguageContext';
import { useAuth } from '@/contexts/AuthContext';
import { SignalingClient } from '@/lib/signaling/client';
import { WebRTCClient } from '@/lib/webrtc/client';
import { getSignalingServerUrl } from '@/lib/utils/signalingServerDiscovery';
import ChatPanel from '@/components/ChatPanel';
import FileSharePanel from '@/components/FileSharePanel';
import { CallRecorder } from '@/lib/utils/callRecorder';
import { VideoEffects } from '@/lib/utils/videoEffects';
import { VoiceCommands } from '@/lib/utils/voiceCommands';
import { 
  HiVideoCamera, 
  HiMicrophone,
  HiPaperClip,
  HiPhone
} from 'react-icons/hi';
import { FaRecordVinyl, FaMicrophoneAlt, FaVideo, FaVideoSlash, FaMicrophoneSlash } from 'react-icons/fa';
import { MdScreenShare, MdStopScreenShare } from 'react-icons/md';
import { IoCall } from 'react-icons/io5';

const DEFAULT_SIGNALING_URL = process.env.NEXT_PUBLIC_SIGNALING_URL || 'ws://192.168.1.20:8080/ws';

interface Contact {
  id: string;
  name: string;
  phoneNumber: string;
  email?: string;
  isFavorite: boolean;
  note?: string;
}

export default function CallPage() {
  const { t, locale } = useLanguage();
  const { user, isAuthenticated, loading } = useAuth();
  const router = useRouter();
  const searchParams = useSearchParams();
  const [targetContact, setTargetContact] = useState<Contact | null>(null);
  const [groupId, setGroupId] = useState<string | null>(null);
  const [activeRoomCode, setActiveRoomCode] = useState<string | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [isCallActive, setIsCallActive] = useState(false);
  const [status, setStatus] = useState<string>('');
  const [isVideoEnabled, setIsVideoEnabled] = useState(true);
  const [isAudioEnabled, setIsAudioEnabled] = useState(true);
  const [isChatOpen, setIsChatOpen] = useState(false);
  const [isScreenSharing, setIsScreenSharing] = useState(false);
  const [isFileShareOpen, setIsFileShareOpen] = useState(false);
  const [isRecording, setIsRecording] = useState(false);
  const [videoEffect, setVideoEffect] = useState<'none' | 'blur' | 'grayscale' | 'sepia' | 'invert'>('none');
  const [isVoiceCommandsEnabled, setIsVoiceCommandsEnabled] = useState(false);

  const signalingClientRef = useRef<SignalingClient | null>(null);
  const webrtcClientRef = useRef<WebRTCClient | null>(null);
  const localVideoRef = useRef<HTMLVideoElement>(null);
  const remoteVideoRef = useRef<HTMLVideoElement>(null);
  const callRecorderRef = useRef<CallRecorder | null>(null);
  const videoEffectsRef = useRef<VideoEffects | null>(null);
  const voiceCommandsRef = useRef<VoiceCommands | null>(null);

  // Load contact from URL or localStorage
  useEffect(() => {
    if (!isAuthenticated || loading) return;

    const contactId = searchParams.get('contact');
    const joinParam = searchParams.get('join'); // For incoming calls

    if (contactId) {
      // Load contact from localStorage
      const stored = localStorage.getItem('contacts');
      if (stored) {
        const contacts: Contact[] = JSON.parse(stored);
        const contact = contacts.find(c => c.id === contactId);
        if (contact) {
          setTargetContact(contact);
          if (!joinParam) {
            // Start outgoing call
            startCall(contact);
          }
        } else {
          setStatus(locale === 'tr' ? 'Kişi bulunamadı' : 'Contact not found');
        }
      }
    } else if (joinParam) {
      // Incoming call - wait for incoming-call message
      initializeSignaling();
    } else {
      // No contact specified - redirect to contacts
      router.push('/contacts');
    }
  }, [isAuthenticated, loading, searchParams, router, locale]);

  useEffect(() => {
    // Cleanup on unmount
    return () => {
      if (signalingClientRef.current) {
        signalingClientRef.current.disconnect();
      }
      if (webrtcClientRef.current) {
        webrtcClientRef.current.disconnect();
      }
    };
  }, []);

  const initializeSignaling = async () => {
    if (!user?.phone) return;

    try {
      // Otomatik IP bulma ile signaling URL'i al
      const signalingUrl = await getSignalingServerUrl(DEFAULT_SIGNALING_URL);
      const signaling = new SignalingClient(signalingUrl);
      signalingClientRef.current = signaling;

      // Connect and register
      await signaling.connect();
      signaling.register(user.phone, user.name || undefined);

      // Listen for incoming calls
      signaling.onMessage('incoming-call', async (message) => {
        if (message.type === 'incoming-call') {
          setGroupId(message.groupId);
          setActiveRoomCode(message.roomCode);
          // Load contact from phone number
          const stored = localStorage.getItem('contacts');
          if (stored) {
            const contacts: Contact[] = JSON.parse(stored);
            const contact = contacts.find(c => c.phoneNumber === message.callerPhoneNumber);
            if (contact) {
              setTargetContact(contact);
            }
          }
        }
      });

      // Listen for call accepted
      signaling.onMessage('call-accepted', async (message) => {
        if (message.type === 'call-accepted' && message.roomCode) {
          setActiveRoomCode(message.roomCode);
          await joinRoom(message.roomCode);
        }
      });

      // Listen for call errors
      signaling.onMessage('call-error', (message) => {
        if (message.type === 'call-error') {
          setStatus(message.reason || (locale === 'tr' ? 'Arama hatası' : 'Call error'));
        }
      });
    } catch (error) {
      console.error('Signaling initialization error:', error);
      setStatus(t('errors.connectionFailed'));
    }
  };

  const startCall = async (contact: Contact) => {
    if (!user?.phone) {
      setStatus(locale === 'tr' ? 'Kullanıcı bilgisi bulunamadı' : 'User information not found');
      return;
    }

    try {
      setStatus(locale === 'tr' ? 'Arama başlatılıyor...' : 'Starting call...');

      // Initialize signaling client (otomatik IP bulma ile)
      const signalingUrl = await getSignalingServerUrl(DEFAULT_SIGNALING_URL);
      const signaling = new SignalingClient(signalingUrl);
      signalingClientRef.current = signaling;

      // Connect and register
      await signaling.connect();
      signaling.register(user.phone, user.name || undefined);

      // Listen for call-request-sent
      signaling.onMessage('call-request-sent', (message) => {
        if (message.type === 'call-request-sent') {
          setGroupId(message.groupId);
          setActiveRoomCode(message.roomCode);
          setStatus(locale === 'tr' ? 'Arama gönderildi, yanıt bekleniyor...' : 'Call sent, waiting for response...');
        }
      });

      // Listen for call-accepted
      signaling.onMessage('call-accepted', async (message) => {
        if (message.type === 'call-accepted' && message.roomCode) {
          setActiveRoomCode(message.roomCode);
          await joinRoom(message.roomCode);
        }
      });

      // Listen for call errors
      signaling.onMessage('call-error', (message) => {
        if (message.type === 'call-error') {
          setStatus(message.reason || (locale === 'tr' ? 'Arama hatası' : 'Call error'));
        }
      });

      // Send call request
      signaling.startCall(contact.phoneNumber, user.phone, user.name || undefined);
    } catch (error) {
      console.error('Start call error:', error);
      setStatus(t('errors.connectionFailed'));
    }
  };

  const joinRoom = async (roomCode: string) => {
    try {
      setStatus(locale === 'tr' ? 'Bağlanılıyor...' : 'Connecting...');

      // Initialize WebRTC client
      const webrtc = new WebRTCClient({ audio: true, video: true });
      webrtcClientRef.current = webrtc;

      // Get user media
      await webrtc.getUserMedia();
      
      // Initialize utilities
      if (localVideoRef.current) {
        videoEffectsRef.current = new VideoEffects(localVideoRef.current);
      }
      voiceCommandsRef.current = new VoiceCommands();
      callRecorderRef.current = new CallRecorder();
      
      // Set up local video
      if (localVideoRef.current && webrtc.getLocalStream()) {
        localVideoRef.current.srcObject = webrtc.getLocalStream();
      }

      const signaling = signalingClientRef.current;
      if (!signaling) return;

      // Connect to room
      await signaling.connect(roomCode);
      setIsConnected(true);

      // Set up signaling handlers
      signaling.onMessage('offer', async (message) => {
        if (message.type === 'offer') {
          const answer = await webrtc.createAnswer({ type: 'offer', sdp: message.sdp });
          signaling.sendAnswer(answer.sdp || '');
          setStatus(t('call.connected'));
          setIsCallActive(true);
        }
      });

      signaling.onMessage('answer', async (message) => {
        if (message.type === 'answer') {
          await webrtc.setRemoteDescription({ type: 'answer', sdp: message.sdp });
          setStatus(t('call.connected'));
          setIsCallActive(true);
        }
      });

      signaling.onMessage('ice-candidate', async (message) => {
        if (message.type === 'ice-candidate') {
          await webrtc.addIceCandidate({
            candidate: message.candidate,
            sdpMid: message.sdpMid || undefined,
            sdpMLineIndex: message.sdpMLineIndex,
          });
        }
      });

      // Set up WebRTC handlers
      webrtc.onIceCandidate = (candidate) => {
        signaling.sendIceCandidate(
          candidate.candidate || '',
          candidate.sdpMid,
          candidate.sdpMLineIndex || 0
        );
      };

      webrtc.onRemoteStream = (stream) => {
        if (remoteVideoRef.current) {
          remoteVideoRef.current.srcObject = stream;
        }
      };

      // Create and send offer
      const offer = await webrtc.createOffer();
      signaling.sendOffer(offer.sdp || '');
      setStatus(t('call.waiting'));
    } catch (error) {
      console.error('Join room error:', error);
      setStatus(t('errors.connectionFailed'));
    }
  };

  const acceptCall = async () => {
    if (!groupId || !signalingClientRef.current) return;

    try {
      signalingClientRef.current.acceptCall(groupId);
      if (activeRoomCode) {
        await joinRoom(activeRoomCode);
      }
    } catch (error) {
      console.error('Accept call error:', error);
      setStatus(t('errors.connectionFailed'));
    }
  };

  const rejectCall = () => {
    if (!groupId || !signalingClientRef.current) return;
    signalingClientRef.current.rejectCall(groupId);
    router.push('/home');
  };

  const endCall = async () => {
    // Stop recording if active
    if (callRecorderRef.current && isRecording) {
      const blob = await callRecorderRef.current.stopRecording();
      if (blob) {
        const callId = `call-${Date.now()}`;
        await callRecorderRef.current.saveToLocalStorage(blob, callId);
        setIsRecording(false);
      }
    }

    // Stop video effects
    if (videoEffectsRef.current) {
      videoEffectsRef.current.stop();
    }

    // Stop voice commands
    if (voiceCommandsRef.current) {
      voiceCommandsRef.current.stop();
    }

    if (signalingClientRef.current) {
      signalingClientRef.current.leave();
      signalingClientRef.current.disconnect();
    }
    if (webrtcClientRef.current) {
      webrtcClientRef.current.disconnect();
    }
    setIsConnected(false);
    setIsCallActive(false);
    setStatus(t('call.callEnded'));
    setGroupId(null);
    setActiveRoomCode(null);
    
    // Clear video elements
    if (localVideoRef.current) {
      localVideoRef.current.srcObject = null;
    }
    if (remoteVideoRef.current) {
      remoteVideoRef.current.srcObject = null;
    }

    // Navigate back to contacts
    router.push('/contacts');
  };

  const toggleVideo = () => {
    if (webrtcClientRef.current) {
      const newState = !isVideoEnabled;
      webrtcClientRef.current.setVideoEnabled(newState);
      setIsVideoEnabled(newState);
    }
  };

  const toggleAudio = () => {
    if (webrtcClientRef.current) {
      const newState = !isAudioEnabled;
      webrtcClientRef.current.setAudioEnabled(newState);
      setIsAudioEnabled(newState);
    }
  };

  const toggleRecording = async () => {
    if (!callRecorderRef.current || !webrtcClientRef.current) return;

    try {
      if (!isRecording) {
        const localStream = webrtcClientRef.current.getLocalStream();
        const remoteStream = webrtcClientRef.current.getRemoteStream();
        
        if (localStream) {
          const success = await callRecorderRef.current.startRecording(
            localStream,
            remoteStream || undefined
          );
          if (success) {
            setIsRecording(true);
          }
        }
      } else {
        const blob = await callRecorderRef.current.stopRecording();
        if (blob) {
          const callId = `call-${Date.now()}`;
          await callRecorderRef.current.saveToLocalStorage(blob, callId);
          setIsRecording(false);
        }
      }
    } catch (error) {
      console.error('Recording error:', error);
    }
  };

  const applyVideoEffect = async (effect: 'none' | 'blur' | 'grayscale' | 'sepia' | 'invert') => {
    if (!videoEffectsRef.current || !webrtcClientRef.current) return;

    try {
      setVideoEffect(effect);
      if (effect === 'none') {
        videoEffectsRef.current.stop();
        if (localVideoRef.current && webrtcClientRef.current.getLocalStream()) {
          localVideoRef.current.srcObject = webrtcClientRef.current.getLocalStream();
        }
      } else {
        const processedStream = await videoEffectsRef.current.applyEffect(effect, 5);
        if (localVideoRef.current) {
          localVideoRef.current.srcObject = processedStream;
        }
      }
    } catch (error) {
      console.error('Video effect error:', error);
    }
  };

  const toggleVoiceCommands = () => {
    if (!voiceCommandsRef.current) return;

    if (!isVoiceCommandsEnabled) {
      const success = voiceCommandsRef.current.start((command) => {
        const parsed = VoiceCommands.parseCommand(command);
        switch (parsed.action) {
          case 'mute':
            if (isAudioEnabled) toggleAudio();
            break;
          case 'unmute':
            if (!isAudioEnabled) toggleAudio();
            break;
          case 'videoOff':
            if (isVideoEnabled) toggleVideo();
            break;
          case 'videoOn':
            if (!isVideoEnabled) toggleVideo();
            break;
          case 'endCall':
            endCall();
            break;
          case 'screenShare':
            if (!isScreenSharing) toggleScreenShare();
            break;
        }
      });
      setIsVoiceCommandsEnabled(success);
    } else {
      voiceCommandsRef.current.stop();
      setIsVoiceCommandsEnabled(false);
    }
  };

  const toggleScreenShare = async () => {
    if (!webrtcClientRef.current) return;

    try {
      if (!isScreenSharing) {
        // Start screen share
        const screenStream = await webrtcClientRef.current.getDisplayMedia();
        const videoTrack = screenStream.getVideoTracks()[0];
        
        // Get current peer connection
        const pc = (webrtcClientRef.current as any).pc;
        if (pc && webrtcClientRef.current.getLocalStream()) {
          // Replace video track in peer connection
          const sender = pc.getSenders().find((s: RTCRtpSender) => 
            s.track && s.track.kind === 'video'
          );
          if (sender) {
            await sender.replaceTrack(videoTrack);
          }
          
          // Update local stream
          const oldVideoTrack = webrtcClientRef.current.getLocalStream()!.getVideoTracks()[0];
          if (oldVideoTrack) {
            oldVideoTrack.stop();
            webrtcClientRef.current.getLocalStream()!.removeTrack(oldVideoTrack);
          }
          webrtcClientRef.current.getLocalStream()!.addTrack(videoTrack);
        }

        // Update local video
        if (localVideoRef.current) {
          localVideoRef.current.srcObject = webrtcClientRef.current.getLocalStream();
        }

        // Handle screen share end
        videoTrack.onended = () => {
          setIsScreenSharing(false);
        };

        setIsScreenSharing(true);
      } else {
        // Stop screen share - restore camera
        const cameraStream = await webrtcClientRef.current.getUserMedia({ video: true });
        const videoTrack = cameraStream.getVideoTracks()[0];
        
        // Get current peer connection
        const pc = (webrtcClientRef.current as any).pc;
        if (pc && webrtcClientRef.current.getLocalStream()) {
          // Replace video track in peer connection
          const sender = pc.getSenders().find((s: RTCRtpSender) => 
            s.track && s.track.kind === 'video'
          );
          if (sender) {
            await sender.replaceTrack(videoTrack);
          }
          
          // Update local stream
          const oldVideoTrack = webrtcClientRef.current.getLocalStream()!.getVideoTracks()[0];
          if (oldVideoTrack) {
            oldVideoTrack.stop();
            webrtcClientRef.current.getLocalStream()!.removeTrack(oldVideoTrack);
          }
          webrtcClientRef.current.getLocalStream()!.addTrack(videoTrack);
        }

        if (localVideoRef.current) {
          localVideoRef.current.srcObject = webrtcClientRef.current.getLocalStream();
        }

        setIsScreenSharing(false);
      }
    } catch (error) {
      console.error('Screen share error:', error);
      setStatus(t('errors.permissionDenied'));
    }
  };

  return (
    <div className="h-full bg-gray-900 text-white flex flex-col w-full">
      {!isCallActive ? (
        /* Call Setup / Incoming Call */
        <div className="flex-1 flex items-center justify-center p-6">
          <div className="w-full max-w-md space-y-4 text-center">
            {targetContact && (
              <div className="mb-6">
                <h2 className="text-xs font-medium font-semibold mb-2">
                  {locale === 'tr' ? 'Arama' : 'Call'}
                </h2>
                <p className="text-xs font-medium text-gray-300">
                  {targetContact.name}
                </p>
                <p className="text-xs font-medium text-gray-400">
                  {targetContact.phoneNumber}
                </p>
              </div>
            )}
            {groupId && !isConnected && (
              <div className="flex gap-4">
                <button
                  onClick={acceptCall}
                  className="flex-1 px-4 py-2 bg-green-600 hover:bg-green-700 rounded text-xs font-medium transition-colors"
                >
                  {locale === 'tr' ? 'Kabul Et' : 'Accept'}
                </button>
                <button
                  onClick={rejectCall}
                  className="flex-1 px-4 py-2 bg-red-600 hover:bg-red-700 rounded text-xs font-medium transition-colors"
                >
                  {locale === 'tr' ? 'Reddet' : 'Reject'}
                </button>
              </div>
            )}
            {status && (
              <div className="text-center text-xs font-medium text-gray-400">{status}</div>
            )}
          </div>
        </div>
      ) : (
        /* Active Call - Split Layout: Video Area + Chat Panel */
        <div className="flex-1 flex gap-4 p-4 overflow-hidden">
          {/* Video Area (Left) */}
          <div className="flex-1 flex flex-col gap-4 overflow-hidden">
            {/* Video Grid */}
            <div className="flex-1 grid grid-cols-1 md:grid-cols-2 gap-4 min-h-0">
              {/* Remote Video */}
              <div className="relative bg-black rounded overflow-hidden">
                <video
                  ref={remoteVideoRef}
                  autoPlay
                  playsInline
                  className="w-full h-full object-cover"
                />
                {!remoteVideoRef.current?.srcObject && (
                  <div className="absolute inset-0 flex items-center justify-center text-gray-400">
                    {t('call.waiting')}
                  </div>
                )}
              </div>

              {/* Local Video */}
              <div className="relative bg-black rounded overflow-hidden">
                <video
                  ref={localVideoRef}
                  autoPlay
                  playsInline
                  muted
                  className="w-full h-full object-cover"
                />
                {!isVideoEnabled && (
                  <div className="absolute inset-0 flex items-center justify-center bg-gray-800 text-gray-400">
                    {t('call.videoOff')}
                  </div>
                )}
              </div>
            </div>

            {/* Controls */}
            <div className="space-y-3">
              {/* Main Controls */}
              <div className="flex items-center justify-center gap-2 flex-wrap">
                <button
                  onClick={toggleRecording}
                  className={`p-2 rounded transition-colors ${
                    isRecording
                      ? 'bg-red-600 hover:bg-red-700 animate-pulse'
                      : 'bg-gray-600 hover:bg-gray-700'
                  }`}
                  title={isRecording ? (locale === 'tr' ? 'Kaydı Durdur' : 'Stop Recording') : (locale === 'tr' ? 'Kaydı Başlat' : 'Start Recording')}
                >
                  <FaRecordVinyl className={`text-lg ${isRecording ? 'text-white' : 'text-gray-300'}`} />
                </button>
                <button
                  onClick={toggleAudio}
                  className={`p-2 rounded transition-colors ${
                    isAudioEnabled
                      ? 'bg-cyan-500 hover:bg-cyan-600'
                      : 'bg-red-600 hover:bg-red-700'
                  }`}
                  title={isAudioEnabled ? t('call.mute') : t('call.unmute')}
                >
                  {isAudioEnabled ? (
                    <HiMicrophone className="text-lg text-white" />
                  ) : (
                    <FaMicrophoneSlash className="text-lg text-white" />
                  )}
                </button>
                <button
                  onClick={toggleVideo}
                  className={`p-2 rounded transition-colors ${
                    isVideoEnabled
                      ? 'bg-cyan-500 hover:bg-cyan-600'
                      : 'bg-gray-600 hover:bg-gray-700'
                  }`}
                  title={isVideoEnabled ? t('call.videoOff') : t('call.videoOn')}
                >
                  {isVideoEnabled ? (
                    <HiVideoCamera className="text-lg text-white" />
                  ) : (
                    <FaVideoSlash className="text-lg text-gray-300" />
                  )}
                </button>
                <button
                  onClick={toggleScreenShare}
                  className={`p-2 rounded transition-colors ${
                    isScreenSharing
                      ? 'bg-green-600 hover:bg-green-700'
                      : 'bg-gray-600 hover:bg-gray-700'
                  }`}
                  title={isScreenSharing ? t('call.stopScreenShare') : t('call.screenShare')}
                >
                  {isScreenSharing ? (
                    <MdStopScreenShare className="text-lg text-white" />
                  ) : (
                    <MdScreenShare className="text-lg text-gray-300" />
                  )}
                </button>
                <button
                  onClick={() => setIsFileShareOpen(!isFileShareOpen)}
                  className={`p-2 rounded transition-colors ${
                    isFileShareOpen
                      ? 'bg-cyan-500 hover:bg-cyan-600'
                      : 'bg-gray-600 hover:bg-gray-700'
                  }`}
                  title={t('call.files')}
                >
                  <HiPaperClip className={`text-lg ${isFileShareOpen ? 'text-white' : 'text-gray-300'}`} />
                </button>
                <button
                  onClick={toggleVoiceCommands}
                  className={`p-2 rounded transition-colors ${
                    isVoiceCommandsEnabled
                      ? 'bg-purple-600 hover:bg-purple-700'
                      : 'bg-gray-600 hover:bg-gray-700'
                  }`}
                  title={isVoiceCommandsEnabled ? (locale === 'tr' ? 'Sesli Komutları Kapat' : 'Disable Voice Commands') : (locale === 'tr' ? 'Sesli Komutları Aç' : 'Enable Voice Commands')}
                >
                  <FaMicrophoneAlt className={`text-lg ${isVoiceCommandsEnabled ? 'text-white' : 'text-gray-300'}`} />
                </button>
                <button
                  onClick={endCall}
                  className="p-2 bg-red-600 hover:bg-red-700 rounded transition-colors"
                  title={t('call.endCall')}
                >
                  <IoCall className="text-lg text-white rotate-135" />
                </button>
              </div>

              {/* Video Effects */}
              <div className="flex items-center justify-center gap-2 flex-wrap">
                <span className="text-xs text-gray-400 px-2">
                  {locale === 'tr' ? 'Efektler:' : 'Effects:'}
                </span>
                {(['none', 'blur', 'grayscale', 'sepia', 'invert'] as const).map((effect) => (
                  <button
                    key={effect}
                    onClick={() => applyVideoEffect(effect)}
                    className={`px-2 py-1 rounded text-xs transition ${
                      videoEffect === effect
                        ? 'bg-cyan-500 text-white'
                        : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
                    }`}
                  >
                    {locale === 'tr' 
                      ? (effect === 'none' ? 'Yok' : effect === 'blur' ? 'Bulanık' : effect === 'grayscale' ? 'Gri' : effect === 'sepia' ? 'Sepya' : 'Ters')
                      : effect.charAt(0).toUpperCase() + effect.slice(1)
                    }
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* Chat Panel (Right) */}
          <div className="w-80 flex flex-col border-l border-gray-700">
            <ChatPanel
              signalingClient={signalingClientRef.current}
              currentUserPhoneNumber={user?.phone || 'unknown'}
              currentUserName={user?.name}
              isOpen={true}
              onClose={() => {}}
            />
          </div>
        </div>
      )}

      {/* File Share Panel (Modal) */}
      {isCallActive && isFileShareOpen && (
        <FileSharePanel
          signalingClient={signalingClientRef.current}
          currentUserPhoneNumber={user?.phone || 'unknown'}
          currentUserName={user?.name}
          isOpen={isFileShareOpen}
          onClose={() => setIsFileShareOpen(false)}
        />
      )}
    </div>
  );
}

