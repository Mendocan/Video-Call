// Notifications Utility for Electron
export class NotificationManager {
  private static instance: NotificationManager | null = null;

  static getInstance(): NotificationManager {
    if (!NotificationManager.instance) {
      NotificationManager.instance = new NotificationManager();
    }
    return NotificationManager.instance;
  }

  async showNotification(
    title: string,
    body: string,
    options?: NotificationOptions
  ): Promise<void> {
    // Check if running in Electron
    if (typeof window !== 'undefined' && (window as any).electronAPI) {
      try {
        await (window as any).electronAPI.showNotification(title, body, options);
        return;
      } catch (error) {
        console.error('Electron notification error:', error);
      }
    }

    // Fallback to browser notifications
    if ('Notification' in window && Notification.permission === 'granted') {
      new Notification(title, {
        body,
        icon: '/logo.png',
        ...options,
      });
    } else if ('Notification' in window && Notification.permission !== 'denied') {
      // Request permission
      const permission = await Notification.requestPermission();
      if (permission === 'granted') {
        new Notification(title, {
          body,
          icon: '/logo.png',
          ...options,
        });
      }
    }
  }

  async requestPermission(): Promise<NotificationPermission> {
    if (typeof window === 'undefined') return 'denied';
    
    if ('Notification' in window) {
      return await Notification.requestPermission();
    }
    return 'denied';
  }

  showIncomingCall(
    callerName: string,
    callerPhone: string,
    roomCode: string,
    isVideo: boolean = true
  ): void {
    this.showNotification(
      'Gelen Arama',
      `${callerName || callerPhone} ${isVideo ? 'görüntülü' : 'sesli'} arama yapıyor`,
      {
        tag: `call-${roomCode}`,
        requireInteraction: true,
        data: {
          type: 'incoming-call',
          roomCode,
          callerName,
          callerPhone,
          isVideo,
        },
      }
    );
  }

  showMessage(senderName: string, message: string): void {
    this.showNotification(
      `Mesaj: ${senderName}`,
      message,
      {
        tag: `message-${Date.now()}`,
      }
    );
  }

  showCalendarReminder(eventTitle: string, eventTime: Date): void {
    const timeStr = eventTime.toLocaleTimeString('tr-TR', {
      hour: '2-digit',
      minute: '2-digit',
    });
    this.showNotification(
      'Randevu Hatırlatıcısı',
      `${eventTitle} - ${timeStr}`,
      {
        tag: `calendar-${eventTime.getTime()}`,
      }
    );
  }
}

export const notificationManager = NotificationManager.getInstance();

