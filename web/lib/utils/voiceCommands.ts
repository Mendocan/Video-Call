// Voice Commands Utility
export class VoiceCommands {
  private recognition: any = null;
  private isListening: boolean = false;
  private onCommandCallback: ((command: string) => void) | null = null;

  constructor() {
    if (typeof window !== 'undefined' && ('webkitSpeechRecognition' in window || 'SpeechRecognition' in window)) {
      const SpeechRecognition = (window as any).webkitSpeechRecognition || (window as any).SpeechRecognition;
      this.recognition = new SpeechRecognition();
      this.recognition.continuous = true;
      this.recognition.interimResults = false;
      this.recognition.lang = 'tr-TR';

      this.recognition.onresult = (event: any) => {
        const last = event.results.length - 1;
        const command = event.results[last][0].transcript.toLowerCase().trim();
        
        if (this.onCommandCallback) {
          this.onCommandCallback(command);
        }
      };

      this.recognition.onerror = (event: any) => {
        console.error('Speech recognition error:', event.error);
      };

      this.recognition.onend = () => {
        if (this.isListening) {
          // Restart if still listening
          try {
            this.recognition.start();
          } catch (error) {
            console.error('Error restarting recognition:', error);
          }
        }
      };
    }
  }

  start(onCommand: (command: string) => void): boolean {
    if (!this.recognition) {
      console.warn('Speech recognition not supported');
      return false;
    }

    if (this.isListening) {
      return false;
    }

    this.onCommandCallback = onCommand;
    this.isListening = true;

    try {
      this.recognition.start();
      return true;
    } catch (error) {
      console.error('Error starting recognition:', error);
      this.isListening = false;
      return false;
    }
  }

  stop(): void {
    if (!this.recognition || !this.isListening) {
      return;
    }

    this.isListening = false;
    this.onCommandCallback = null;

    try {
      this.recognition.stop();
    } catch (error) {
      console.error('Error stopping recognition:', error);
    }
  }

  isSupported(): boolean {
    return this.recognition !== null;
  }

  getIsListening(): boolean {
    return this.isListening;
  }

  // Command handlers
  static parseCommand(command: string): {
    action: string;
    params: any;
  } {
    const lower = command.toLowerCase().trim();

    // Mute/Unmute
    if (lower.includes('sessiz') || lower.includes('mute')) {
      return { action: 'mute', params: {} };
    }
    if (lower.includes('sesi aç') || lower.includes('unmute')) {
      return { action: 'unmute', params: {} };
    }

    // Video on/off
    if (lower.includes('kamera kapat') || lower.includes('video off')) {
      return { action: 'videoOff', params: {} };
    }
    if (lower.includes('kamera aç') || lower.includes('video on')) {
      return { action: 'videoOn', params: {} };
    }

    // End call
    if (lower.includes('görüşmeyi bitir') || lower.includes('end call') || lower.includes('kapat')) {
      return { action: 'endCall', params: {} };
    }

    // Screen share
    if (lower.includes('ekran paylaş') || lower.includes('screen share')) {
      return { action: 'screenShare', params: {} };
    }

    return { action: 'unknown', params: { command } };
  }
}

