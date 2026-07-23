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

async function readError(response, fallback) {
  try {
    const data = await response.json();
    return data.message || data.error || fallback;
  } catch {
    return fallback;
  }
}

export async function login(credentials) {
  const response = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(credentials)
  });
  if (!response.ok) {
    throw new Error(await readError(response, '账号或密码错误'));
  }
  const session = await response.json();
  return {
    ...session,
    authHeader: createAuthHeader(credentials.username, credentials.password)
  };
}

export async function register(payload) {
  const response = await fetch('/api/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  if (!response.ok) {
    throw new Error(await readError(response, '注册失败'));
  }
  const session = await response.json();
  return {
    ...session,
    authHeader: createAuthHeader(payload.username, payload.password)
  };
}

export async function apiGet(path, session) {
  const response = await fetch(path, {
    headers: {
      Authorization: session.authHeader
    }
  });
  if (!response.ok) {
    throw new Error(await readError(response, `接口请求失败：${path}`));
  }
  return response.json();
}

export async function apiPost(path, session, payload) {
  const response = await fetch(path, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: session.authHeader
    },
    body: JSON.stringify(payload)
  });
  if (!response.ok) {
    throw new Error(await readError(response, `接口请求失败：${path}`));
  }
  return response.json();
}

export async function apiPut(path, session, payload) {
  const response = await fetch(path, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      Authorization: session.authHeader
    },
    body: JSON.stringify(payload)
  });
  if (!response.ok) {
    throw new Error(await readError(response, `接口请求失败：${path}`));
  }
  return response.json();
}

export async function apiDelete(path, session, payload = {}) {
  const response = await fetch(path, {
    method: 'DELETE',
    headers: {
      'Content-Type': 'application/json',
      Authorization: session.authHeader
    },
    body: JSON.stringify(payload)
  });
  if (!response.ok) {
    throw new Error(await readError(response, `接口请求失败：${path}`));
  }
  return response.json();
}
