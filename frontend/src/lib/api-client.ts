import { useAuthStore } from '@/stores/auth-store';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1';

interface ApiResponse<T> {
  success: boolean;
  data: T;
  meta?: {
    timestamp: string;
  };
  error?: {
    code: string;
    message: string;
  };
}

class ApiClient {
  private baseUrl: string;
  private refreshing: Promise<boolean> | null = null;

  constructor(baseUrl: string) {
    this.baseUrl = baseUrl;
  }

  private getHeaders(): HeadersInit {
    const headers: HeadersInit = {
      'Content-Type': 'application/json',
    };

    if (typeof window !== 'undefined') {
      // Get access token from zustand store (memory only, not localStorage)
      const token = useAuthStore.getState().accessToken;
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
    }

    return headers;
  }

  private async tryRefreshToken(): Promise<boolean> {
    if (this.refreshing) return this.refreshing;

    this.refreshing = (async () => {
      try {
        const refreshToken = typeof window !== 'undefined'
          ? localStorage.getItem('refreshToken')
          : null;
        if (!refreshToken) return false;

        const res = await fetch(`${this.baseUrl}/auth/refresh`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken }),
        });

        if (!res.ok) return false;

        const json: ApiResponse<{ accessToken: string; refreshToken: string }> = await res.json();
        if (!json.success || !json.data) return false;

        // Store new tokens: accessToken in memory, refreshToken in localStorage
        useAuthStore.getState().login(
          useAuthStore.getState().user!,
          json.data.accessToken,
          json.data.refreshToken,
        );
        return true;
      } catch {
        return false;
      } finally {
        this.refreshing = null;
      }
    })();

    return this.refreshing;
  }

  private async request<T>(path: string, options: RequestInit): Promise<T> {
    let res = await fetch(`${this.baseUrl}${path}`, {
      ...options,
      headers: this.getHeaders(),
    });

    if (res.status === 401 && typeof window !== 'undefined' && localStorage.getItem('refreshToken')) {
      const refreshed = await this.tryRefreshToken();
      if (refreshed) {
        res = await fetch(`${this.baseUrl}${path}`, {
          ...options,
          headers: this.getHeaders(),
        });
      }
    }

    if (res.status === 401 && typeof window !== 'undefined') {
      useAuthStore.getState().logout();
    }

    if (!res.ok) {
      const json = await res.json().catch(() => null);
      throw new Error(json?.error?.message || `API Error: ${res.status} ${res.statusText}`);
    }

    const json: ApiResponse<T> = await res.json();
    if (!json.success) {
      throw new Error(json.error?.message || 'Unknown API error');
    }

    return (json.data ?? null) as T;
  }

  async get<T>(path: string): Promise<T> {
    return this.request<T>(path, { method: 'GET' });
  }

  async post<T>(path: string, body?: unknown): Promise<T> {
    return this.request<T>(path, {
      method: 'POST',
      body: body ? JSON.stringify(body) : undefined,
    });
  }

  async delete<T>(path: string): Promise<T> {
    return this.request<T>(path, { method: 'DELETE' });
  }
}

export const apiClient = new ApiClient(API_BASE_URL);
