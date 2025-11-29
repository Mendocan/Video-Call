/**
 * Signaling Server IP Discovery
 * 
 * Otomatik IP bulma mekanizması:
 * 1. Backend API'den signaling server IP'sini al
 * 2. Fallback: Environment variable veya default URL'i kullan
 */

// Backend URL: Environment variable yoksa localhost kullan (development için)
// Production'da NEXT_PUBLIC_API_URL environment variable'ı set edilmeli
const BACKEND_URL = process.env.NEXT_PUBLIC_API_URL 
  ? (process.env.NEXT_PUBLIC_API_URL.startsWith('http') 
      ? process.env.NEXT_PUBLIC_API_URL 
      : `http://${process.env.NEXT_PUBLIC_API_URL}`)
  : 'http://localhost:3000';

const DEFAULT_SIGNALING_URL = process.env.NEXT_PUBLIC_SIGNALING_URL || 'ws://192.168.1.20:8080/ws';

interface ServerInfo {
  wsUrl: string;
  httpUrl: string;
  localIP?: string;
  publicIP?: string;
  lastUpdated: string;
}

interface ServerInfoResponse {
  success: boolean;
  serverInfo?: ServerInfo;
  error?: string;
}

/**
 * Backend API'den signaling server IP'sini al
 * @returns Signaling server WebSocket URL (ws://IP:PORT/ws) veya null
 */
export async function discoverSignalingServerUrl(): Promise<string | null> {
  try {
    const response = await fetch(`${BACKEND_URL}/api/signaling/server-info`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (response.ok) {
      const data: ServerInfoResponse = await response.json();
      if (data.success && data.serverInfo) {
        console.log('[SignalingServerDiscovery] Signaling server bulundu:', data.serverInfo.wsUrl);
        return data.serverInfo.wsUrl;
      }
    }

    console.warn('[SignalingServerDiscovery] Backend\'den signaling server bilgisi alınamadı:', response.status);
    return null;
  } catch (error: any) {
    console.error('[SignalingServerDiscovery] Signaling server discovery hatası:', error.message);
    return null;
  }
}

/**
 * Signaling server URL'ini al (discovery + fallback)
 * @param defaultUrl Fallback URL (environment variable'dan gelebilir)
 * @returns Signaling server WebSocket URL
 */
export async function getSignalingServerUrl(defaultUrl: string = DEFAULT_SIGNALING_URL): Promise<string> {
  const discoveredUrl = await discoverSignalingServerUrl();
  return discoveredUrl || defaultUrl;
}

