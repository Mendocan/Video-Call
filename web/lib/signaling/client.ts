/**
 * WebRTC Signaling Client
 * 
 * WebSocket üzerinden signaling sunucusuna bağlanır.
 * SDP (offer/answer) ve ICE candidate mesajlarını iletir.
 */

export type SignalingMessage = 
  | { type: 'register'; phoneNumber: string; name?: string }
  | { type: 'registered'; phoneNumber: string; name?: string; timestamp: string }
  | { type: 'offer'; sdp: string }
  | { type: 'answer'; sdp: string }
  | { type: 'ice-candidate'; candidate: string; sdpMid: string | null; sdpMLineIndex: number }
  | { type: 'join'; roomCode: string }
  | { type: 'leave'; roomCode?: string }
  | { type: 'chat'; message: string; senderPhoneNumber: string; senderName?: string; targetPhoneNumber?: string }
  | { type: 'file-share'; fileId: string; fileName: string; fileSize: number; mimeType: string; senderPhoneNumber: string; senderName?: string }
  | { type: 'call-request'; targetPhoneNumber?: string; groupId?: string; callerPhoneNumber: string; callerName?: string }
  | { type: 'call-request-sent'; groupId: string; roomCode: string; targetPhoneNumber: string; timestamp: string }
  | { type: 'incoming-call'; groupId: string; roomCode: string; callerPhoneNumber: string; callerName?: string; isGroupCall: boolean; timestamp: string }
  | { type: 'call-accept'; groupId: string }
  | { type: 'call-reject'; groupId: string }
  | { type: 'call-accepted'; groupId: string; roomCode: string }
  | { type: 'call-rejected'; groupId: string }
  | { type: 'call-error'; reason: string; targetPhoneNumber?: string }
  | { type: 'error'; reason: string }
  | { type: 'presence'; participants: number };

export type SignalingStatus = 'idle' | 'connecting' | 'connected' | 'disconnected' | 'error';

export class SignalingClient {
  private ws: WebSocket | null = null;
  private url: string;
  private roomCode: string | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 1000;
  private messageHandlers: Map<string, Set<(message: SignalingMessage) => void>> = new Map();
  private statusHandlers: Set<(status: SignalingStatus) => void> = new Set();
  private status: SignalingStatus = 'idle';

  constructor(url: string) {
    this.url = url;
  }

  connect(roomCode?: string): Promise<void> {
    return new Promise((resolve, reject) => {
      // Eğer roomCode boş string veya undefined ise, room code olmadan bağlan
      const actualRoomCode = roomCode || '';
      if (this.ws?.readyState === WebSocket.OPEN && this.roomCode === actualRoomCode) {
        resolve();
        return;
      }

      this.roomCode = actualRoomCode;
      this.setStatus('connecting');

      try {
        // Room code yoksa direkt bağlan, varsa query parameter ile
        const wsUrl = actualRoomCode ? `${this.url}?room=${actualRoomCode}` : this.url;
        this.ws = new WebSocket(wsUrl);

        this.ws.onopen = () => {
          this.setStatus('connected');
          this.reconnectAttempts = 0;
          resolve();
        };

        this.ws.onmessage = (event) => {
          try {
            const message = JSON.parse(event.data) as SignalingMessage;
            this.handleMessage(message);
          } catch (error) {
            console.error('Signaling message parse error:', error);
          }
        };

        this.ws.onerror = (error) => {
          // WebSocket error event doesn't provide detailed error info
          // The actual error will be in onclose with a close code
          console.warn('Signaling WebSocket error event triggered. Connection may have failed.');
        };

        this.ws.onclose = (event) => {
          this.setStatus('disconnected');
          this.ws = null;
          
          // Eğer bağlantı hiç açılmadıysa (code 1006 = abnormal closure)
          if (event.code === 1006 && this.reconnectAttempts === 0) {
            const errorMessage = `WebSocket connection failed to ${this.url}. Please check if the server is running and the URL is correct.`;
            console.error('Signaling WebSocket connection failed:', errorMessage);
            this.setStatus('error');
            reject(new Error(errorMessage));
            return;
          }
          
          // Otomatik yeniden bağlanma
          if (this.reconnectAttempts < this.maxReconnectAttempts && this.roomCode) {
            this.reconnectAttempts++;
            setTimeout(() => {
              this.connect(this.roomCode!).catch(console.error);
            }, this.reconnectDelay * this.reconnectAttempts);
          }
        };
      } catch (error) {
        this.setStatus('error');
        reject(error);
      }
    });
  }

  disconnect() {
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
    this.roomCode = null;
    this.setStatus('idle');
  }

  send(message: SignalingMessage): boolean {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message));
      return true;
    }
    return false;
  }

  sendOffer(sdp: string): boolean {
    return this.send({ type: 'offer', sdp });
  }

  sendAnswer(sdp: string): boolean {
    return this.send({ type: 'answer', sdp });
  }

  sendIceCandidate(candidate: string, sdpMid: string | null, sdpMLineIndex: number): boolean {
    return this.send({ type: 'ice-candidate', candidate, sdpMid, sdpMLineIndex });
  }

  sendChat(message: string, senderPhoneNumber: string, senderName?: string, targetPhoneNumber?: string): boolean {
    return this.send({ type: 'chat', message, senderPhoneNumber, senderName, targetPhoneNumber });
  }

  register(phoneNumber: string, name?: string): boolean {
    return this.send({ type: 'register', phoneNumber, name });
  }

  startCall(targetPhoneNumber: string, callerPhoneNumber: string, callerName?: string, groupId?: string): boolean {
    return this.send({ type: 'call-request', targetPhoneNumber, groupId, callerPhoneNumber, callerName });
  }

  acceptCall(groupId: string): boolean {
    return this.send({ type: 'call-accept', groupId });
  }

  rejectCall(groupId: string): boolean {
    return this.send({ type: 'call-reject', groupId });
  }

  leave(): boolean {
    return this.send({ type: 'leave', roomCode: this.roomCode || undefined });
  }

  onMessage(type: string, handler: (message: SignalingMessage) => void): () => void {
    if (!this.messageHandlers.has(type)) {
      this.messageHandlers.set(type, new Set());
    }
    this.messageHandlers.get(type)!.add(handler);

    // Cleanup function
    return () => {
      this.messageHandlers.get(type)?.delete(handler);
    };
  }

  onStatusChange(handler: (status: SignalingStatus) => void): () => void {
    this.statusHandlers.add(handler);
    // İlk durumu bildir
    handler(this.status);

    // Cleanup function
    return () => {
      this.statusHandlers.delete(handler);
    };
  }

  getStatus(): SignalingStatus {
    return this.status;
  }

  isConnected(): boolean {
    return this.ws?.readyState === WebSocket.OPEN && this.status === 'connected';
  }

  private setStatus(status: SignalingStatus) {
    if (this.status !== status) {
      this.status = status;
      this.statusHandlers.forEach(handler => handler(status));
    }
  }

  private handleMessage(message: SignalingMessage) {
    // Type-specific handlers
    const handlers = this.messageHandlers.get(message.type);
    if (handlers) {
      handlers.forEach(handler => handler(message));
    }

    // General handler
    const generalHandlers = this.messageHandlers.get('*');
    if (generalHandlers) {
      generalHandlers.forEach(handler => handler(message));
    }
  }
}

