const API_URL = import.meta.env?.VITE_API_URL

export const SESSION_TOKEN_KEY = 'focustask.token'
export const tokenStorage = {
  get: () => sessionStorage.getItem(SESSION_TOKEN_KEY),
  set: (token) => sessionStorage.setItem(SESSION_TOKEN_KEY, token),
  remove: () => sessionStorage.removeItem(SESSION_TOKEN_KEY),
}

export class ApiError extends Error {
  constructor(message, { status = 0, errors = {}, cause } = {}) {
    super(message, { cause })
    this.name = 'ApiError'
    this.status = status
    this.errors = errors
  }
}

export function normalizeApiUrl(value) {
  if (!value?.trim()) {
    throw new ApiError('A URL da API não foi configurada. Defina VITE_API_URL no ambiente do frontend e reinicie o Vite.')
  }
  const base = value.trim().replace(/\/+$/, '')
  let url
  try { url = new URL(base) } catch { /* Report configuration without echoing its value. */ }
  if (!url || !['http:', 'https:'].includes(url.protocol) || url.username || url.password
    || url.search || url.hash || url.pathname !== '/api/v1') {
    throw new ApiError('VITE_API_URL deve ser uma URL HTTP ou HTTPS terminada em /api/v1.')
  }
  return base
}

export function createApiClient({ apiUrl = API_URL, fetchImpl = (...args) => fetch(...args), getToken = tokenStorage.get } = {}) {
  const unauthorizedListeners = new Set()
  async function request(path, { method = 'GET', body, authenticated = true, token, signal } = {}) {
    const base = normalizeApiUrl(apiUrl)
    if (!path.startsWith('/') || path.startsWith('//') || path.includes('..')) throw new ApiError('Caminho da API inválido.')
    const bearer = authenticated ? (token ?? getToken()) : null
    const headers = { Accept: 'application/json, application/problem+json' }
    if (body !== undefined) headers['Content-Type'] = 'application/json'
    if (bearer) headers.Authorization = `Bearer ${bearer}`
    let response, text
    try {
      response = await fetchImpl(`${base}${path}`, {
        method, headers, body: body === undefined ? undefined : JSON.stringify(body), credentials: 'omit',
        signal: AbortSignal.any([AbortSignal.timeout(15000), ...(signal ? [signal] : [])]),
      })
      // 201/204 and other empty successes have no JSON to parse.
      text = await response.text()
    } catch (cause) {
      if (signal?.aborted) throw cause
      throw new ApiError('Não foi possível conectar à API. Verifique sua conexão e tente novamente.', { cause })
    }
    // Some transports can finish despite abort; cancelled requests cannot expire a newer session.
    signal?.throwIfAborted()
    if (response.status === 401 && bearer) unauthorizedListeners.forEach((listener) => listener(bearer))
    let data = null
    if (text.trim()) {
      try { data = JSON.parse(text) } catch {
        if (response.ok) throw new ApiError('A API retornou uma resposta inválida.', { status: response.status })
      }
    }
    if (!response.ok) {
      const fallback = response.status >= 500 ? 'O servidor está indisponível no momento. Tente novamente.' : 'Não foi possível concluir a solicitação.'
      throw new ApiError(data?.detail || fallback, { status: response.status, errors: data?.errors || {} })
    }
    return data
  }
  return {
    request,
    onUnauthorized(listener) { unauthorizedListeners.add(listener); return () => unauthorizedListeners.delete(listener) },
  }
}

export const api = createApiClient()
