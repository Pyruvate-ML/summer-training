const SESSION_KEY = 'xinqiao-session';

export function loadSession() {
  return JSON.parse(localStorage.getItem(SESSION_KEY) || 'null');
}

export function saveSession(session) {
  localStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

export function clearSession() {
  localStorage.removeItem(SESSION_KEY);
}

export function createAuthHeader(username, password) {
  return `Basic ${btoa(`${username}:${password}`)}`;
}

export async function login(credentials) {
  const response = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(credentials)
  });
  if (!response.ok) {
    throw new Error('账号或密码错误');
  }
  const session = await response.json();
  return {
    ...session,
    authHeader: createAuthHeader(credentials.username, credentials.password)
  };
}

export async function apiGet(path, session) {
  const response = await fetch(path, {
    headers: {
      Authorization: session.authHeader
    }
  });
  if (!response.ok) {
    throw new Error(`接口请求失败：${path}`);
  }
  return response.json();
}
