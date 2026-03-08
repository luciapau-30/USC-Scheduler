const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

export type TokenSetter = (token: string | null) => void;

let accessToken: string | null = null;
let onTokenSet: TokenSetter | null = null;

export function setAccessToken(token: string | null) {
  accessToken = token;
  onTokenSet?.(token);
}

export function getAccessToken(): string | null {
  return accessToken;
}

export function onAccessTokenChange(fn: TokenSetter) {
  onTokenSet = fn;
}

export async function api(
  path: string,
  options: RequestInit & { skipAuth?: boolean; _retried?: boolean } = {}
): Promise<Response> {
  const { skipAuth, _retried, ...init } = options;
  const url = path.startsWith('http') ? path : `${API_URL}${path}`;
  const headers = new Headers(init.headers);
  if (!headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
  if (!skipAuth && accessToken) headers.set('Authorization', `Bearer ${accessToken}`);
  const res = await fetch(url, { ...init, headers, credentials: 'include' });
  if (res.status === 401 && !skipAuth && !_retried) {
    const refreshed = await refreshToken();
    if (refreshed) return api(path, { ...options, _retried: true });
  }
  return res;
}

async function refreshToken(): Promise<boolean> {
  const res = await fetch(`${API_URL}/api/auth/refresh`, { method: 'POST', credentials: 'include' });
  if (!res.ok) {
    setAccessToken(null);
    return false;
  }
  const data = await res.json();
  if (data.accessToken) setAccessToken(data.accessToken);
  return !!data.accessToken;
}

export { API_URL };
