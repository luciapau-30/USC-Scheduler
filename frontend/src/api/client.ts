const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

function isNetworkError(e: unknown): boolean {
  if (e instanceof TypeError) return e.message === 'Failed to fetch' || e.message.includes('fetch');
  if (e instanceof Error) return e.message === 'Failed to fetch' || e.message.includes('Load failed');
  return false;
}

function wrapNetworkError(e: unknown): never {
  if (isNetworkError(e)) {
    throw new Error(
      `Cannot reach the server at ${API_URL}. Is the backend running? Start it from the backend folder: ./mvnw spring-boot:run`
    );
  }
  throw e;
}

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
  let res: Response;
  try {
    res = await fetch(url, { ...init, headers, credentials: 'include' });
  } catch (e) {
    wrapNetworkError(e);
  }
  if (res!.status === 401 && !skipAuth && !_retried) {
    const refreshed = await refreshToken();
    if (refreshed) return api(path, { ...options, _retried: true });
  }
  return res!;
}

async function refreshToken(): Promise<boolean> {
  let res: Response;
  try {
    res = await fetch(`${API_URL}/api/auth/refresh`, { method: 'POST', credentials: 'include' });
  } catch (e) {
    wrapNetworkError(e);
  }
  if (!res!.ok) {
    setAccessToken(null);
    return false;
  }
  const data = await res!.json();
  if (data.accessToken) setAccessToken(data.accessToken);
  return !!data.accessToken;
}

export { API_URL };
