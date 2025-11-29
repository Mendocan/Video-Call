/**
 * WebRTC Client
 * 
 * WebRTC bağlantısını yönetir.
 * Video, ses ve ekran paylaşımı desteği.
 */

export type WebRTCStatus = 'idle' | 'connecting' | 'connected' | 'disconnected' | 'failed';

export interface WebRTCOptions {
  iceServers?: RTCConfiguration['iceServers'];
  audio?: boolean;
  video?: boolean;
}

export class WebRTCClient {
  private pc: RTCPeerConnection | null = null;
  private localStream: MediaStream | null = null;
  private remoteStream: MediaStream | null = null;
  private options: WebRTCOptions;
  private statusHandlers: Set<(status: WebRTCStatus) => void> = new Set();
  private status: WebRTCStatus = 'idle';

  // Default STUN servers (Google'ın ücretsiz sunucuları)
  private defaultIceServers: RTCIceServer[] = [
    { urls: 'stun:stun.l.google.com:19302' },
    { urls: 'stun:global.stun.twilio.com:3478' },
  ];

  constructor(options: WebRTCOptions = {}) {
    this.options = {
      audio: true,
      video: true,
      iceServers: this.defaultIceServers,
      ...options,
    };
  }

  async initialize(): Promise<void> {
    if (this.pc) {
      return;
    }

    const config: RTCConfiguration = {
      iceServers: this.options.iceServers || this.defaultIceServers,
    };

    this.pc = new RTCPeerConnection(config);

    // ICE candidate event
    this.pc.onicecandidate = (event) => {
      if (event.candidate) {
        // Signaling client'a gönderilecek (dışarıdan handler ile)
        this.onIceCandidate?.(event.candidate);
      }
    };

    // Connection state change
    this.pc.onconnectionstatechange = () => {
      if (!this.pc) return;
      const state = this.pc.connectionState;
      switch (state) {
        case 'connected':
          this.setStatus('connected');
          break;
        case 'disconnected':
        case 'failed':
        case 'closed':
          this.setStatus('disconnected');
          break;
        case 'connecting':
          this.setStatus('connecting');
          break;
        case 'new':
          // Initial state, keep as connecting
          this.setStatus('connecting');
          break;
      }
    };

    // Track events (remote stream)
    this.pc.ontrack = (event) => {
      if (event.streams && event.streams[0]) {
        this.remoteStream = event.streams[0];
        this.onRemoteStream?.(event.streams[0]);
      }
    };
  }

  async getUserMedia(constraints: MediaStreamConstraints = {}): Promise<MediaStream> {
    const defaultConstraints: MediaStreamConstraints = {
      audio: this.options.audio !== false,
      video: this.options.video !== false ? {
        width: { ideal: 1280 },
        height: { ideal: 720 },
        frameRate: { ideal: 30 },
      } : false,
    };

    const finalConstraints = { ...defaultConstraints, ...constraints };
    this.localStream = await navigator.mediaDevices.getUserMedia(finalConstraints);
    return this.localStream;
  }

  async createOffer(): Promise<RTCSessionDescriptionInit> {
    if (!this.pc) {
      await this.initialize();
    }

    if (this.localStream) {
      // Local stream'i peer connection'a ekle
      this.localStream.getTracks().forEach(track => {
        if (this.pc && this.localStream) {
          this.pc.addTrack(track, this.localStream);
        }
      });
    }

    const offer = await this.pc!.createOffer();
    await this.pc!.setLocalDescription(offer);
    return offer;
  }

  async createAnswer(offer: RTCSessionDescriptionInit): Promise<RTCSessionDescriptionInit> {
    if (!this.pc) {
      await this.initialize();
    }

    // Önce remote description'ı set et
    await this.pc!.setRemoteDescription(new RTCSessionDescription(offer));

    if (this.localStream) {
      // Local stream'i peer connection'a ekle
      this.localStream.getTracks().forEach(track => {
        if (this.pc && this.localStream) {
          this.pc.addTrack(track, this.localStream);
        }
      });
    }

    const answer = await this.pc!.createAnswer();
    await this.pc!.setLocalDescription(answer);
    return answer;
  }

  async setRemoteDescription(description: RTCSessionDescriptionInit): Promise<void> {
    if (!this.pc) {
      await this.initialize();
    }
    await this.pc!.setRemoteDescription(new RTCSessionDescription(description));
  }

  async addIceCandidate(candidate: RTCIceCandidateInit): Promise<void> {
    if (!this.pc) {
      await this.initialize();
    }
    await this.pc!.addIceCandidate(new RTCIceCandidate(candidate));
  }

  async getDisplayMedia(): Promise<MediaStream> {
    const stream = await navigator.mediaDevices.getDisplayMedia({
      video: true,
      audio: true,
    });
    return stream;
  }

  setVideoEnabled(enabled: boolean) {
    if (this.localStream) {
      this.localStream.getVideoTracks().forEach(track => {
        track.enabled = enabled;
      });
    }
  }

  setAudioEnabled(enabled: boolean) {
    if (this.localStream) {
      this.localStream.getAudioTracks().forEach(track => {
        track.enabled = enabled;
      });
    }
  }

  getLocalStream(): MediaStream | null {
    return this.localStream;
  }

  getRemoteStream(): MediaStream | null {
    return this.remoteStream;
  }

  onStatusChange(handler: (status: WebRTCStatus) => void): () => void {
    this.statusHandlers.add(handler);
    handler(this.status);

    return () => {
      this.statusHandlers.delete(handler);
    };
  }

  getStatus(): WebRTCStatus {
    return this.status;
  }

  // Callbacks (dışarıdan set edilecek)
  onIceCandidate?: (candidate: RTCIceCandidate) => void;
  onRemoteStream?: (stream: MediaStream) => void;

  disconnect() {
    // Local stream'i durdur
    if (this.localStream) {
      this.localStream.getTracks().forEach(track => track.stop());
      this.localStream = null;
    }

    // Remote stream'i temizle
    this.remoteStream = null;

    // Peer connection'ı kapat
    if (this.pc) {
      this.pc.close();
      this.pc = null;
    }

    this.setStatus('idle');
  }

  private setStatus(status: WebRTCStatus) {
    if (this.status !== status) {
      this.status = status;
      this.statusHandlers.forEach(handler => handler(status));
    }
  }
}

