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

const focusSession = { userId: 1, pausedAt: null, totalPausedSeconds: 0 }
const history = [
  { ...focusSession, id: 1, taskId: 7, status: 'COMPLETED', startedAt: '2026-02-26T09:30:00Z', endedAt: '2026-02-26T09:55:00Z' },
  { ...focusSession, id: 2, taskId: null, status: 'COMPLETED', startedAt: '2026-02-25T16:00:00Z', endedAt: '2026-02-25T17:00:00Z' },
]

function mockFocusApi(sessions, onOther, tasks = [{ id: 7, projectId: 3, title: 'Revisar protótipo', status: 'TODO' }]) {
  sessionStorage.setItem(tokenKey, 'valido')
  fetch.mock.mockImplementation((url, options = {}) => {
    const path = url.replace('http://api.test/api/v1', '')
    if (path === '/me') return Response.json(user)
    if (path === '/focus-sessions' && options.method === 'GET') return Response.json(sessions)
    if (path === '/tasks' && options.method === 'GET') return Response.json(tasks)
    if (path === '/projects') return Response.json([{ id: 3, name: 'Redesign App' }])
    return onOther?.(path, options) ?? new Response(null, { status: 404 })
  })
}

const settle = () => act(() => new Promise((resolve) => setTimeout(resolve, 0)))
const rowsText = () => [...document.querySelectorAll('.sessions-table tbody tr')].map((row) => row.textContent)

test('o histórico mostra duração, tarefa, projeto e tipo, e filtra por tipo', async () => {
  mockFocusApi(history)
  window.history.pushState({}, '', '/focus-sessions')
  await openApp()
  await settle()
  const [recent, standalone] = rowsText()
  assert.match(recent, /25m 00s.*Revisar protótipo.*Redesign App.*Tarefa/)
  assert.match(standalone, /60m 00s.*—.*—.*Avulsa/)
  await act(() => [...document.querySelectorAll('.filter-chip')].find((chip) => chip.textContent === 'Avulsa').click())
  assert.equal(rowsText().length, 1)
  assert.match(rowsText()[0], /Avulsa/)
})

test('concluir uma sessão em andamento envia PATCH e atualiza a linha', async () => {
  const running = { ...focusSession, id: 5, taskId: null, status: 'RUNNING', startedAt: new Date(Date.now() - 60000).toISOString(), endedAt: null }
  const calls = []
  mockFocusApi([running], (path, options) => {
    calls.push([options.method, path, options.body])
    return Response.json({ ...running, status: 'COMPLETED', endedAt: new Date().toISOString() })
  })
  window.history.pushState({}, '', '/focus-sessions')
  await openApp()
  await settle()
  assert.match(rowsText()[0], /Em andamento/)
  await act(() => document.querySelector('.row-menu-trigger').click())
  await act(() => [...document.querySelectorAll('.row-menu-list button')].find((button) => button.textContent === 'Concluir').click())
  await settle()
  assert.deepEqual(calls, [['PATCH', '/focus-sessions/5', JSON.stringify({ status: 'COMPLETED' })]])
  assert.doesNotMatch(rowsText()[0], /Em andamento/)
})

test('a nova sessão vincula a tarefa do projeto escolhido e volta ao histórico', async () => {
  const calls = []
  mockFocusApi(history, (path, options) => {
    calls.push([options.method, path, options.body])
    return Response.json({ ...focusSession, id: 9, taskId: 7, status: 'RUNNING', startedAt: new Date().toISOString(), endedAt: null }, { status: 201 })
  })
  window.history.pushState({}, '', '/focus-sessions/new')
  await openApp()
  await settle()
  const choose = (id, value) => act(() => {
    const select = document.querySelector(id)
    select.value = value
    select.dispatchEvent(new window.Event('change', { bubbles: true }))
  })
  assert.equal(document.querySelector('#task').disabled, true)
  await choose('#project', '3')
  assert.equal(document.querySelector('#task').disabled, false)
  await choose('#task', '7')
  await act(() => document.querySelector('button[type="submit"]').click())
  await settle()
  assert.deepEqual(calls, [['POST', '/focus-sessions', JSON.stringify({ taskId: 7 })]])
  assert.equal(window.location.pathname, '/focus-sessions')
})

const dashboardTasks = [
  { id: 1, projectId: 3, title: 'Revisar protótipo', status: 'TODO', priority: 'HIGH', dueDate: null },
  { id: 2, projectId: 3, title: 'Escrever relatório', status: 'DONE', priority: 'LOW', dueDate: null },
  { id: 3, projectId: 3, title: 'Planejar o próximo mês', status: 'TODO', priority: 'LOW', dueDate: '2999-01-01' },
]

function mockDashboardApi(onOther) {
  const minutesAgo = (minutes) => new Date(Date.now() - minutes * 60000).toISOString()
  const sessions = [{ ...focusSession, id: 1, taskId: null, status: 'COMPLETED', startedAt: minutesAgo(3), endedAt: minutesAgo(2) }]
  mockFocusApi(sessions, onOther, dashboardTasks)
}

test('o painel mostra saudação, resumo, cronômetro e as tarefas do dia', async () => {
  mockDashboardApi()
  await openApp()
  await settle()
  assert.match(document.querySelector('h1').textContent, /^(Bom dia|Boa tarde|Boa noite), Conta$/)
  const stats = [...document.querySelectorAll('.stat-card')].map((card) => card.textContent)
  assert.deepEqual(stats.map((text) => text.replace(/^.*?(Tempo Focado|Sessões Concluídas|Tarefas Concluídas|Tarefas Pendentes)/, '$1')), ['Tempo Focado1m', 'Sessões Concluídas1', 'Tarefas Concluídas1', 'Tarefas Pendentes2'])
  assert.equal(document.querySelector('[role="timer"]').textContent, '25:00')
  assert.match(document.querySelector('.timer-badge').textContent, /Foco #2/)
  const rows = [...document.querySelectorAll('.task-row')].map((row) => row.textContent)
  assert.equal(rows.length, 1)
  assert.match(rows[0], /Revisar protótipo.*Redesign App.*Alta/)
})

test('iniciar um foco avulso cria a sessão e troca o botão para pausar', async () => {
  const calls = []
  mockDashboardApi((path, options) => {
    calls.push([options.method, path, options.body])
    return Response.json({ ...focusSession, id: 8, taskId: null, status: 'RUNNING', startedAt: new Date().toISOString(), endedAt: null }, { status: 201 })
  })
  await openApp()
  await settle()
  assert.equal(document.querySelector('button[aria-label="Pausar"]'), null)
  await act(() => [...document.querySelectorAll('button')].find((button) => button.textContent === 'Foco avulso').click())
  await settle()
  assert.deepEqual(calls, [['POST', '/focus-sessions', JSON.stringify({ taskId: null })]])
  assert.ok(document.querySelector('button[aria-label="Pausar"]'))
  assert.equal([...document.querySelectorAll('button')].find((button) => button.textContent === 'Foco avulso').disabled, true)
})

test('"Foco com tarefa" abre o modal e inicia a sessão com a tarefa e a duração escolhidas', async () => {
  const calls = []
  mockDashboardApi((path, options) => {
    calls.push([options.method, path, options.body])
    return Response.json({ ...focusSession, id: 9, taskId: 1, status: 'RUNNING', startedAt: new Date().toISOString(), endedAt: null }, { status: 201 })
  })
  await openApp()
  await settle()
  const byText = (text) => [...document.querySelectorAll('button')].find((button) => button.textContent === text)
  await act(() => byText('Foco com tarefa').click())
  assert.equal(document.querySelector('[role="dialog"] h2').textContent, 'Nova Sessão de Foco')
  assert.equal(document.querySelector('#focus-task').disabled, true)
  await act(() => {
    const select = document.querySelector('#focus-project')
    select.value = '3'
    select.dispatchEvent(new window.Event('change', { bubbles: true }))
  })
  assert.deepEqual([...document.querySelectorAll('#focus-task option')].map((option) => option.textContent), ['Nenhuma', 'Revisar protótipo', 'Planejar o próximo mês'])
  await act(() => {
    const select = document.querySelector('#focus-task')
    select.value = '1'
    select.dispatchEvent(new window.Event('change', { bubbles: true }))
  })
  await act(() => byText('45m').click())
  await act(() => byText('Iniciar Foco').click())
  await settle()
  assert.deepEqual(calls, [['POST', '/focus-sessions', JSON.stringify({ taskId: 1 })]])
  assert.equal(document.querySelector('[role="dialog"]'), null)
  assert.equal(document.querySelector('[role="timer"]').textContent, '45:00')
  assert.ok(document.querySelector('button[aria-label="Pausar"]'))
})

test('o modal de nova sessão marca a duração escolhida e fecha com Esc', async () => {
  mockDashboardApi()
  await openApp()
  await settle()
  const byText = (text) => [...document.querySelectorAll('button')].find((button) => button.textContent === text)
  await act(() => byText('Foco com tarefa').click())
  assert.equal(byText('25m').getAttribute('aria-pressed'), 'true')
  await act(() => byText('15m').click())
  assert.equal(byText('15m').getAttribute('aria-pressed'), 'true')
  assert.equal(byText('25m').getAttribute('aria-pressed'), 'false')
  await act(() => byText('Personalizado…').click())
  assert.ok(document.querySelector('input[aria-label="Minutos"]'))
  await act(() => document.dispatchEvent(new window.KeyboardEvent('keydown', { key: 'Escape', bubbles: true })))
  assert.equal(document.querySelector('[role="dialog"]'), null)
})

test('ao zerar o cronômetro a sessão é concluída e o modal de sessão concluída aparece', async () => {
  const calls = []
  const minutesAgo = (minutes) => new Date(Date.now() - minutes * 60000).toISOString()
  const running = { ...focusSession, id: 5, taskId: 1, status: 'RUNNING', startedAt: minutesAgo(30), endedAt: null }
  mockFocusApi([running], (path, options) => {
    calls.push([options.method, path, options.body])
    return Response.json({ ...running, status: 'COMPLETED', endedAt: new Date().toISOString() })
  }, dashboardTasks)
  await openApp()
  await settle()
  await act(() => new Promise((resolve) => setTimeout(resolve, 1200)))
  await settle()
  assert.deepEqual(calls, [['PATCH', '/focus-sessions/5', JSON.stringify({ status: 'COMPLETED' })]])
  assert.equal(document.querySelector('[role="dialog"] h2').textContent, 'Sessão concluída')
  assert.match(document.querySelector('.modal-summary').textContent, /Revisar protótipo/)
})

test('marcar uma tarefa do dia como concluída envia PATCH e atualiza o resumo', async () => {
  const calls = []
  mockDashboardApi((path, options) => {
    calls.push([options.method, path, options.body])
    return Response.json({ ...dashboardTasks[0], status: 'DONE' })
  })
  await openApp()
  await settle()
  await act(() => document.querySelector('.task-check').click())
  await settle()
  assert.deepEqual(calls, [['PATCH', '/tasks/1', JSON.stringify({ status: 'DONE' })]])
  assert.equal(document.querySelectorAll('.task-row').length, 0)
  assert.match(document.querySelector('.tasks-empty').textContent, /Nenhuma tarefa pendente/)
  assert.match([...document.querySelectorAll('.stat-card')].at(-1).textContent, /Tarefas Pendentes1/)
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
