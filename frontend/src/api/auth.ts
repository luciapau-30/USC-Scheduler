import { api, setAccessToken } from './client';

export interface TokenResponse {
  accessToken: string;
  expiresInSeconds: number;
}

export async function login(email: string, password: string): Promise<TokenResponse> {
  const res = await api('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
    skipAuth: true,
  });
  if (!res.ok) throw new Error(await res.text().then((t) => t || res.statusText));
  const data: TokenResponse = await res.json();
  setAccessToken(data.accessToken);
  return data;
}

export async function register(email: string, password: string): Promise<TokenResponse> {
  const res = await api('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
    skipAuth: true,
  });
  if (!res.ok) throw new Error(await res.text().then((t) => t || res.statusText));
  const data: TokenResponse = await res.json();
  setAccessToken(data.accessToken);
  return data;
}

export async function logout(): Promise<void> {
  await api('/api/auth/logout', { method: 'POST' }).catch(() => {});
  setAccessToken(null);
}
