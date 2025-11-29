// Video Effects Utility
export class VideoEffects {
  private canvas: HTMLCanvasElement | null = null;
  private ctx: CanvasRenderingContext2D | null = null;
  private videoElement: HTMLVideoElement | null = null;
  private stream: MediaStream | null = null;
  private animationFrame: number | null = null;
  private effect: 'none' | 'blur' | 'grayscale' | 'sepia' | 'invert' = 'none';
  private blurIntensity: number = 5;

  constructor(videoElement: HTMLVideoElement) {
    this.videoElement = videoElement;
    this.canvas = document.createElement('canvas');
    this.ctx = this.canvas.getContext('2d');
  }

  async applyEffect(
    effect: 'none' | 'blur' | 'grayscale' | 'sepia' | 'invert',
    intensity: number = 5
  ): Promise<MediaStream> {
    this.effect = effect;
    this.blurIntensity = intensity;

    if (effect === 'none') {
      // Return original stream
      if (this.videoElement && this.videoElement.srcObject) {
        return this.videoElement.srcObject as MediaStream;
      }
      throw new Error('No video stream available');
    }

    if (!this.videoElement || !this.canvas || !this.ctx) {
      throw new Error('Video element or canvas not initialized');
    }

    const videoTrack = (this.videoElement.srcObject as MediaStream)?.getVideoTracks()[0];
    if (!videoTrack) {
      throw new Error('No video track available');
    }

    // Set canvas size
    this.canvas.width = this.videoElement.videoWidth || 640;
    this.canvas.height = this.videoElement.videoHeight || 480;

    // Create new stream with processed video
    const processedStream = this.canvas.captureStream(30); // 30 FPS

    // Process frames
    const processFrame = () => {
      if (!this.ctx || !this.videoElement || !this.canvas) return;

      this.ctx.drawImage(this.videoElement, 0, 0, this.canvas.width, this.canvas.height);

      // Apply effects
      switch (this.effect) {
        case 'blur':
          this.ctx.filter = `blur(${this.blurIntensity}px)`;
          this.ctx.drawImage(this.canvas, 0, 0);
          this.ctx.filter = 'none';
          break;
        case 'grayscale':
          const imageData = this.ctx.getImageData(0, 0, this.canvas.width, this.canvas.height);
          const data = imageData.data;
          for (let i = 0; i < data.length; i += 4) {
            const gray = data[i] * 0.299 + data[i + 1] * 0.587 + data[i + 2] * 0.114;
            data[i] = gray;
            data[i + 1] = gray;
            data[i + 2] = gray;
          }
          this.ctx.putImageData(imageData, 0, 0);
          break;
        case 'sepia':
          this.ctx.filter = 'sepia(100%)';
          this.ctx.drawImage(this.canvas, 0, 0);
          this.ctx.filter = 'none';
          break;
        case 'invert':
          this.ctx.filter = 'invert(100%)';
          this.ctx.drawImage(this.canvas, 0, 0);
          this.ctx.filter = 'none';
          break;
      }

      this.animationFrame = requestAnimationFrame(processFrame);
    };

    processFrame();
    this.stream = processedStream;

    return processedStream;
  }

  stop() {
    if (this.animationFrame) {
      cancelAnimationFrame(this.animationFrame);
      this.animationFrame = null;
    }
    if (this.stream) {
      this.stream.getTracks().forEach((track) => track.stop());
      this.stream = null;
    }
  }
}

