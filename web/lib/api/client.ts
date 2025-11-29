// Backend API Client
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:3000/api';

export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: string;
  message?: string;
}

export class ApiClient {
  private baseUrl: string;
  private token: string | null = null;

  constructor(baseUrl: string = API_BASE_URL) {
    this.baseUrl = baseUrl;
    if (typeof window !== 'undefined') {
      this.token = localStorage.getItem('authToken');
    }
  }

  setToken(token: string | null) {
    this.token = token;
    if (token && typeof window !== 'undefined') {
      localStorage.setItem('authToken', token);
    } else if (typeof window !== 'undefined') {
      localStorage.removeItem('authToken');
    }
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<ApiResponse<T>> {
    const url = `${this.baseUrl}${endpoint}`;
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...(options.headers as Record<string, string> || {}),
    };

    if (this.token) {
      headers['Authorization'] = `Bearer ${this.token}`;
    }

    try {
      const response = await fetch(url, {
        ...options,
        headers,
      });

      const data = await response.json();

      if (!response.ok) {
        return {
          success: false,
          error: data.error || data.message || 'Request failed',
        };
      }

      return {
        success: true,
        data: data.data || data,
      };
    } catch (error: any) {
      return {
        success: false,
        error: error.message || 'Network error',
      };
    }
  }

  // Auth
  async login(phoneNumber: string, verificationCode?: string) {
    return this.request<{ token: string; user: any }>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ phoneNumber, verificationCode }),
    });
  }

  async register(phoneNumber: string, email?: string) {
    return this.request<{ token: string; user: any }>('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ phoneNumber, email }),
    });
  }

  async verifyPhone(phoneNumber: string) {
    return this.request<{ message: string }>('/auth/verify-phone', {
      method: 'POST',
      body: JSON.stringify({ phoneNumber }),
    });
  }

  // Call History
  async getCallHistory() {
    return this.request<any[]>('/calls/history');
  }

  async saveCallHistory(callData: any) {
    return this.request<any>('/calls/history', {
      method: 'POST',
      body: JSON.stringify(callData),
    });
  }

  // Contacts
  async getContacts() {
    return this.request<any[]>('/contacts');
  }

  async addContact(contact: any) {
    return this.request<any>('/contacts', {
      method: 'POST',
      body: JSON.stringify(contact),
    });
  }

  async updateContact(id: string, contact: any) {
    return this.request<any>(`/contacts/${id}`, {
      method: 'PUT',
      body: JSON.stringify(contact),
    });
  }

  async deleteContact(id: string) {
    return this.request<void>(`/contacts/${id}`, {
      method: 'DELETE',
    });
  }

  // Calendar
  async getScheduledCalls() {
    return this.request<any[]>('/calendar/scheduled');
  }

  async scheduleCall(callData: any) {
    return this.request<any>('/calendar/schedule', {
      method: 'POST',
      body: JSON.stringify(callData),
    });
  }

  async updateScheduledCall(id: string, callData: any) {
    return this.request<any>(`/calendar/scheduled/${id}`, {
      method: 'PUT',
      body: JSON.stringify(callData),
    });
  }

  async deleteScheduledCall(id: string) {
    return this.request<void>(`/calendar/scheduled/${id}`, {
      method: 'DELETE',
    });
  }

  // Statistics
  async getCallStatistics() {
    return this.request<any>('/statistics/calls');
  }

  // Subscription
  async getSubscription() {
    return this.request<any>('/subscription');
  }

  async verifyDevice(deviceId: string) {
    return this.request<any>('/subscription/verify-device', {
      method: 'POST',
      body: JSON.stringify({ deviceId }),
    });
  }

  async activateSubscriptionCode(code: string, phoneNumber: string, deviceType: 'mobile' | 'desktop' = 'desktop') {
    return this.request<any>('/subscription/activate-code', {
      method: 'POST',
      body: JSON.stringify({ code, phoneNumber, deviceType }),
    });
  }

  // Presence
  async updatePresence(status: string, customMessage?: string) {
    return this.request<any>('/presence', {
      method: 'PUT',
      body: JSON.stringify({ status, customMessage }),
    });
  }

  async getPresence(phoneNumber: string) {
    return this.request<any>(`/presence/${phoneNumber}`);
  }
}

export const apiClient = new ApiClient();

