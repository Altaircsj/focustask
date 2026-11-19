import { after, afterEach, before, beforeEach, test } from 'node:test'
import assert from 'node:assert/strict'
import { JSDOM } from 'jsdom'
import { createServer } from 'vite'
import { createElement as h, StrictMode, act, useEffect } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router'

const user = { id: 1, name: 'Conta de teste', email: 'teste@example.com' }
const tokenKey = 'focustask.token'
let vite, dom, root, AuthProvider, AppRoutes, useAuth, api, auth

before(async () => {
  vite = await createServer({
    envDir: false,
    configFile: false,
    define: { 'import.meta.env.VITE_API_URL': JSON.stringify('http://api.test/api/v1') },
    oxc: { jsx: { runtime: 'automatic' } },
    server: { middlewareMode: true, hmr: false, watch: null },
  })
  AuthProvider = (await vite.ssrLoadModule('/src/features/auth/AuthContext.jsx')).AuthProvider
  useAuth = (await vite.ssrLoadModule('/src/features/auth/useAuth.js')).useAuth
  api = (await vite.ssrLoadModule('/src/api/client.js')).api
  AppRoutes = (await vite.ssrLoadModule('/src/routes/AppRoutes.jsx')).default
})
after(async () => { await vite.close() })

beforeEach((t) => {
  dom = new JSDOM('<div id="root"></div>', { url: 'http://frontend.test/dashboard' })
  for (const key of ['window', 'document', 'sessionStorage']) {
    globalThis[key] = dom.window[key]
  }
  globalThis.IS_REACT_ACT_ENVIRONMENT = true
  root = createRoot(document.querySelector('#root'))
  t.mock.method(globalThis, 'fetch', () => { throw new Error('Requisição inesperada') })
})
afterEach(async () => {
  await act(() => root.unmount())
  dom.window.close()
  for (const key of ['window', 'document', 'sessionStorage', 'IS_REACT_ACT_ENVIRONMENT']) delete globalThis[key]
})

function Pages() {
  const session = useAuth()
  useEffect(() => { auth = session }, [session])
  return h(AppRoutes)
}

async function openApp() {
  await act(() => root.render(h(StrictMode, null, h(BrowserRouter, null, h(AuthProvider, null, h(Pages))))))
}

function deferred() {
  return Promise.withResolvers()
}

test('rota privada redireciona ao login, sem consultar /me quando não há token', async () => {
  await openApp()
  assert.equal(window.location.pathname, '/login')
  assert.equal(fetch.mock.callCount(), 0)
})

test('só salva o token depois de /me, mostra o painel e sai pela rota protegida', async () => {
  const profile = deferred()
  fetch.mock.mockImplementation((url) => url.endsWith('/auth/login') ? Response.json({ token: 'novo' }) : profile.promise)
  await openApp()
  const login = auth.login({ email: user.email, password: 'test-only' })
  assert.equal(sessionStorage.getItem(tokenKey), null)
  await act(async () => {
    profile.resolve(Response.json(user))
    await login
  })
  assert.equal(sessionStorage.getItem(tokenKey), 'novo')
  assert.equal(window.location.pathname, '/dashboard')
  assert.match(document.body.textContent, /teste@example.com/)
  await act(() => document.querySelector('button').click())
  assert.equal(sessionStorage.getItem(tokenKey), null)
  assert.equal(window.location.pathname, '/login')
})

test('falha ao validar o novo token impede o login', async () => {
  fetch.mock.mockImplementation((url) => url.endsWith('/auth/login') ? Response.json({ token: 'candidato' }) : new Response(null, { status: 401 }))
  await openApp()
  await assert.rejects(auth.login({}), { status: 401 })
  assert.equal(sessionStorage.getItem(tokenKey), null)
  assert.equal(window.location.pathname, '/login')
})

test('a recuperação confirma a conta e ignora um 401 tardio do primeiro efeito do StrictMode', async () => {
  const first = deferred()
  sessionStorage.setItem(tokenKey, 'valido')
  fetch.mock.mockImplementation(() => Response.json(user))
  fetch.mock.mockImplementationOnce(() => first.promise)
  await openApp()
  assert.equal(auth.user.email, user.email)
  await act(() => first.resolve(new Response(null, { status: 401 })))
  assert.equal(window.location.pathname, '/dashboard')
  assert.equal(sessionStorage.getItem(tokenKey), 'valido')
})

test('uma sessão expirada exige novo login', async () => {
  sessionStorage.setItem(tokenKey, 'expirado')
  fetch.mock.mockImplementation(() => new Response(null, { status: 401 }))
  await openApp()
  assert.equal(sessionStorage.getItem(tokenKey), null)
  assert.equal(window.location.pathname, '/login')
  assert.match(document.body.textContent, /Entre novamente/)
})

for (const failure of ['conexão', '503']) {
  test(`falha de ${failure} preserva o token e permite tentar novamente`, async () => {
    sessionStorage.setItem(tokenKey, 'valido')
    fetch.mock.mockImplementation(() => {
      if (failure === 'conexão') throw new TypeError('Offline')
      return new Response(null, { status: 503 })
    })
    await openApp()
    assert.ok(document.querySelector('[role="alert"]'))
    assert.equal(sessionStorage.getItem(tokenKey), 'valido')
    fetch.mock.mockImplementation(() => Response.json(user))
    await act(() => [...document.querySelectorAll('button')].find(button => button.textContent === 'Tentar novamente').click())
    assert.equal(auth.user.email, user.email)
  })
}

test('o menu lateral lista as rotas, destaca a atual e mostra a conta', async () => {
  sessionStorage.setItem(tokenKey, 'valido')
  fetch.mock.mockImplementation(() => Response.json(user))
  await openApp()
  const menu = document.querySelector('nav[aria-label="Menu lateral"]')
  assert.deepEqual([...menu.querySelectorAll('a')].map((link) => link.getAttribute('href')), ['/dashboard', '/projects', '/tasks', '/focus-sessions', '/profile'])
  assert.equal(menu.querySelector('[aria-current="page"]').textContent, 'Painel')
  assert.match(document.querySelector('.sidebar-user').textContent, /Conta de teste.*teste@example\.com/)
})

test('um 401 de outra chamada não encerra uma sessão mais recente', async () => {
  sessionStorage.setItem(tokenKey, 'novo')
  fetch.mock.mockImplementation(() => Response.json(user))
  await openApp()
  fetch.mock.mockImplementation(() => new Response(null, { status: 401 }))
  await assert.rejects(api.request('/tasks', { token: 'antigo' }), { status: 401 })
  assert.equal(sessionStorage.getItem(tokenKey), 'novo')
  await act(() => assert.rejects(api.request('/tasks'), { status: 401 }))
  assert.equal(window.location.pathname, '/login')
  assert.equal(sessionStorage.getItem(tokenKey), null)
})

test('logout descarta /me pendente mesmo se o transporte entregar a resposta', async () => {
  const profile = deferred()
  const requested = deferred()
  fetch.mock.mockImplementation((url) => {
    if (url.endsWith('/auth/login')) return Response.json({ token: 'candidato' })
    requested.resolve()
    return profile.promise
  })
  await openApp()
  const login = auth.login({})
  await requested.promise
  await act(() => auth.logout())
  profile.resolve(Response.json(user))
  await assert.rejects(login, { name: 'AbortError' })
  assert.equal(sessionStorage.getItem(tokenKey), null)
  assert.equal(auth.user, null)
})

test('desmontar o provedor cancela uma recuperação em andamento', async () => {
  const profile = deferred()
  sessionStorage.setItem(tokenKey, 'valido')
  let signal
  fetch.mock.mockImplementation((_url, options) => {
    signal = options.signal
    return profile.promise
  })
  await openApp()
  assert.match(document.body.textContent, /Verificando sua sessão/)
  await act(() => root.render(null))
  assert.ok(signal.aborted)
  await act(() => profile.resolve(new Response(null, { status: 401 })))
  assert.equal(sessionStorage.getItem(tokenKey), 'valido')
})
