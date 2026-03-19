import { create } from 'zustand';
import { User } from '@/types';

interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  login: (user: User, accessToken: string, refreshToken: string) => void;
  logout: () => void;
  hydrate: () => void;
}

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1';

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  accessToken: null,
  refreshToken: null,
  isAuthenticated: false,

  login: (user, accessToken, refreshToken) => {
    if (typeof window !== 'undefined') {
      // Only store refresh token and user in localStorage
      // Access token stays in memory only (XSS mitigation)
      localStorage.setItem('refreshToken', refreshToken);
      localStorage.setItem('user', JSON.stringify(user));
    }
    set({ user, accessToken, refreshToken, isAuthenticated: true });
  },

  logout: () => {
    // Call server to blacklist token
    const { accessToken } = get();
    if (accessToken) {
      fetch(`${API_BASE_URL}/auth/logout`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${accessToken}`,
          'Content-Type': 'application/json',
        },
      }).catch(() => { /* best effort */ });
    }

    if (typeof window !== 'undefined') {
      localStorage.removeItem('accessToken'); // cleanup legacy
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
    }
    set({ user: null, accessToken: null, refreshToken: null, isAuthenticated: false });
  },

  hydrate: () => {
    if (typeof window === 'undefined') return;
    const refreshToken = localStorage.getItem('refreshToken');
    const userStr = localStorage.getItem('user');

    // Legacy: clean up accessToken from localStorage if present
    localStorage.removeItem('accessToken');

    if (refreshToken && userStr) {
      try {
        const user = JSON.parse(userStr) as User;
        // Attempt to get fresh access token using refresh token
        fetch(`${API_BASE_URL}/auth/refresh`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken }),
        })
          .then((res) => res.ok ? res.json() : Promise.reject())
          .then((json) => {
            if (json.success && json.data) {
              set({
                user,
                accessToken: json.data.accessToken,
                refreshToken: json.data.refreshToken,
                isAuthenticated: true,
              });
              localStorage.setItem('refreshToken', json.data.refreshToken);
            } else {
              throw new Error();
            }
          })
          .catch(() => {
            // Refresh failed — clear everything
            localStorage.removeItem('refreshToken');
            localStorage.removeItem('user');
            set({ user: null, accessToken: null, refreshToken: null, isAuthenticated: false });
          });

        // Temporarily set authenticated with user info while refreshing
        set({ user, refreshToken, isAuthenticated: true });
      } catch {
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
      }
    }
  },
}));
