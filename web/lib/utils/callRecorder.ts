// Call Recorder Utility
export class CallRecorder {
  private mediaRecorder: MediaRecorder | null = null;
  private recordedChunks: Blob[] = [];
  private stream: MediaStream | null = null;
  private isRecording: boolean = false;
  private startTime: number = 0;

  async startRecording(
    audioStream: MediaStream,
    videoStream?: MediaStream
  ): Promise<boolean> {
    try {
      const tracks: MediaStreamTrack[] = [...audioStream.getTracks()];
      if (videoStream) {
        tracks.push(...videoStream.getTracks());
      }

      this.stream = new MediaStream(tracks);
      this.recordedChunks = [];
      this.startTime = Date.now();

      const options: MediaRecorderOptions = {
        mimeType: 'video/webm;codecs=vp9,opus',
      };

      // Fallback options
      if (!MediaRecorder.isTypeSupported(options.mimeType!)) {
        options.mimeType = 'video/webm;codecs=vp8,opus';
      }
      if (!MediaRecorder.isTypeSupported(options.mimeType!)) {
        options.mimeType = 'video/webm';
      }
      if (!MediaRecorder.isTypeSupported(options.mimeType!)) {
        options.mimeType = '';
      }

      this.mediaRecorder = new MediaRecorder(this.stream, options);
      this.mediaRecorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          this.recordedChunks.push(event.data);
        }
      };

      this.mediaRecorder.start(1000); // Collect data every second
      this.isRecording = true;

      return true;
    } catch (error) {
      console.error('Error starting recording:', error);
      return false;
    }
  }

  async stopRecording(): Promise<Blob | null> {
    return new Promise((resolve) => {
      if (!this.mediaRecorder || !this.isRecording) {
        resolve(null);
        return;
      }

      this.mediaRecorder.onstop = () => {
        const blob = new Blob(this.recordedChunks, { type: 'video/webm' });
        this.cleanup();
        resolve(blob);
      };

      this.mediaRecorder.stop();
      this.isRecording = false;
    });
  }

  getDuration(): number {
    if (!this.isRecording) return 0;
    return Math.floor((Date.now() - this.startTime) / 1000);
  }

  isCurrentlyRecording(): boolean {
    return this.isRecording;
  }

  private cleanup() {
    if (this.stream) {
      this.stream.getTracks().forEach((track) => track.stop());
      this.stream = null;
    }
    this.mediaRecorder = null;
    this.recordedChunks = [];
  }

  async downloadRecording(blob: Blob, filename: string = 'call-recording.webm') {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }

  async saveToLocalStorage(blob: Blob, callId: string): Promise<void> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => {
        const base64 = reader.result as string;
        const recordings = JSON.parse(
          localStorage.getItem('callRecordings') || '[]'
        );
        recordings.push({
          id: callId,
          data: base64,
          timestamp: Date.now(),
        });
        localStorage.setItem('callRecordings', JSON.stringify(recordings));
        resolve();
      };
      reader.onerror = reject;
      reader.readAsDataURL(blob);
    });
  }

  static getRecordings(): any[] {
    const recordings = localStorage.getItem('callRecordings');
    return recordings ? JSON.parse(recordings) : [];
  }

  static deleteRecording(id: string): void {
    const recordings = this.getRecordings();
    const filtered = recordings.filter((r: any) => r.id !== id);
    localStorage.setItem('callRecordings', JSON.stringify(filtered));
  }
}

