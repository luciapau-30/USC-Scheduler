import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { getAccessToken, onAccessTokenChange } from '../api/client';
import * as authApi from '../api/auth';

type AuthState = { ready: false } | { ready: true; token: string | null };

const AuthContext = createContext<{
  token: string | null;
  ready: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
} | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<AuthState>({ ready: false });

  useEffect(() => {
    setState({ ready: true, token: getAccessToken() });
    return onAccessTokenChange((token) => setState((s) => (s.ready ? { ...s, token } : s)));
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    await authApi.login(email, password);
    setState((s) => (s.ready ? { ...s, token: getAccessToken() } : s));
  }, []);

  const register = useCallback(async (email: string, password: string) => {
    await authApi.register(email, password);
    setState((s) => (s.ready ? { ...s, token: getAccessToken() } : s));
  }, []);

  const logout = useCallback(async () => {
    await authApi.logout();
    setState((s) => (s.ready ? { ...s, token: null } : s));
  }, []);

  return (
    <AuthContext.Provider
      value={{
        token: state.ready ? state.token : null,
        ready: state.ready,
        login,
        register,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
