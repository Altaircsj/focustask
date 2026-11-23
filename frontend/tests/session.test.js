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
    if (path === '/projects') return Response.json([{ id: 3, name: 'Redesign App', color: '#E2E8F0' }])
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

test('o histórico ordena os filtros como no Figma e permite editar uma sessão em andamento', async () => {
  const running = { ...focusSession, id: 5, taskId: null, projectId: null, title: null, plannedMinutes: 25, status: 'RUNNING', startedAt: new Date(Date.now() - 60000).toISOString(), endedAt: null }
  const calls = []
  mockFocusApi([running], (path, options) => {
    calls.push([options.method, path, options.body])
    return Response.json({ ...running, title: 'Estudar' })
  })
  window.history.pushState({}, '', '/focus-sessions')
  await openApp()
  await settle()
  assert.deepEqual([...document.querySelectorAll('.filter-chip')].map((chip) => chip.textContent), ['Todas', 'Avulsa', 'Projeto', 'Tarefa'])
  await act(() => document.querySelector('.row-menu-trigger').click())
  const items = [...document.querySelectorAll('.row-menu-list button')].map((button) => button.textContent)
  assert.deepEqual(items, ['Editar', 'Excluir'])
  await act(() => document.querySelector('.row-menu-list button').click())
  assert.equal(document.querySelector('[role="dialog"] h2').textContent, 'Editar Sessão de Foco')
  await act(() => [...document.querySelectorAll('button')].find((button) => button.textContent === 'Salvar alterações').click())
  await settle()
  assert.deepEqual(calls.map(([method, path]) => [method, path]), [['PATCH', '/focus-sessions/5']])
  assert.equal(document.querySelector('[role="dialog"]'), null)
  assert.match(rowsText()[0], /Estudar/)
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
  await act(() => document.querySelector('button[aria-label="Concluir sessão: Sessão avulsa"]').click())
  await settle()
  assert.deepEqual(calls, [['PATCH', '/focus-sessions/5', JSON.stringify({ status: 'COMPLETED' })]])
  assert.doesNotMatch(rowsText()[0], /Em andamento/)
})

test('a rota de nova sessão abre o formulário do painel e inicia o relógio', async () => {
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
  assert.equal(document.querySelector('#focus-task').disabled, true)
  await choose('#focus-project', '3')
  assert.equal(document.querySelector('#focus-task').disabled, false)
  await choose('#focus-task', '7')
  await act(() => document.querySelector('button[type="submit"]').click())
  await settle()
  assert.deepEqual(calls, [['POST', '/focus-sessions', JSON.stringify({ title: '', plannedMinutes: 25, taskId: 7, projectId: null })]])
  assert.equal(window.location.pathname, '/dashboard')
  assert.ok(document.querySelector('button[aria-label="Pausar"]'))
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
  // Sem sessão rodando, o Painel mostra a última que recebeu Play, mesmo concluída, com a ação Novo foco.
  assert.equal(document.querySelector('[role="timer"]').textContent, '01:00')
  assert.equal(document.querySelector('.timer-badge').textContent, 'Foco #1 · Concluído')
  assert.ok([...document.querySelectorAll('button')].some((button) => button.textContent === 'Novo foco'))
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
  assert.deepEqual(calls, [['POST', '/focus-sessions', JSON.stringify({ taskId: null, plannedMinutes: 25 })]])
  assert.ok(document.querySelector('button[aria-label="Pausar"]'))
  assert.equal([...document.querySelectorAll('button')].find((button) => button.textContent === 'Foco avulso').disabled, false)
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
  assert.deepEqual(calls, [['POST', '/focus-sessions', JSON.stringify({ title: '', plannedMinutes: 45, taskId: 1, projectId: null })]])
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
  for (const label of ['15m', '90m', '120m', '240m']) {
    await act(() => byText(label).click())
    assert.equal(byText(label).getAttribute('aria-pressed'), 'true')
  }
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
  assert.equal(document.querySelector('#review-task').selectedOptions[0].textContent, 'Revisar protótipo')
})

test('"Finalizar sessão" conclui antes do tempo e abre o modal de sessão concluída', async () => {
  const calls = []
  const running = { ...focusSession, id: 6, taskId: null, status: 'RUNNING', startedAt: new Date(Date.now() - 5 * 60000).toISOString(), endedAt: null }
  mockFocusApi([running], (path, options) => {
    calls.push([options.method, path, options.body])
    return Response.json({ ...running, status: 'COMPLETED', endedAt: new Date().toISOString() })
  }, dashboardTasks)
  await openApp()
  await settle()
  await act(() => [...document.querySelectorAll('button')].find((button) => button.textContent === 'Finalizar sessão').click())
  await settle()
  assert.deepEqual(calls, [['PATCH', '/focus-sessions/6', JSON.stringify({ status: 'COMPLETED' })]])
  assert.equal(document.querySelector('[role="dialog"] h2').textContent, 'Sessão concluída')
  assert.match(document.querySelector('.modal-summary').textContent, /5m 0\ds/)
  assert.equal([...document.querySelectorAll('button')].some((button) => button.textContent === 'Finalizar sessão'), false)
})

const buttonNamed = (label) => [...document.querySelectorAll('button')].find((button) => button.textContent === label)
const click = (button) => act(() => button.click())

for (const status of ['RUNNING', 'PAUSED']) {
  test(`edição e cancelamento do descarte preservam o relógio em ${status}`, async (t) => {
    let now = Date.now()
    t.mock.method(Date, 'now', () => now)
    const session = {
      ...focusSession, id: 14, title: 'Revisar', taskId: 7, status, plannedMinutes: 45,
      startedAt: new Date(now - 60000).toISOString(),
      pausedAt: status === 'PAUSED' ? new Date(now).toISOString() : null,
    }
    const writes = []
    mockFocusApi([session], (path, options) => {
      const body = JSON.parse(options.body)
      writes.push([path, options.method, body])
      return Response.json({ ...session, ...body })
    })
    await openApp()
    await settle()
    const discard = () => click(document.querySelector('[aria-label="Descartar sessão"]'))
    const clock = () => document.querySelector('[role="timer"]').textContent
    await discard()
    now += 10000
    await act(() => new Promise((resolve) => setTimeout(resolve, 1100)))
    assert.equal(clock(), status === 'PAUSED' ? '44:00' : '43:50')
    await click(buttonNamed('Manter sessão'))
    await discard()
    await act(() => document.dispatchEvent(new window.KeyboardEvent('keydown', { key: 'Escape' })))
    await discard()
    await act(() => document.querySelector('.modal-overlay').dispatchEvent(new window.MouseEvent('mousedown', { bubbles: true })))
    assert.equal(document.querySelector('[role="dialog"]'), null)
    assert.deepEqual(writes, [])
    await click(document.querySelector('[aria-label="Opções da sessão"]'))
    await click(buttonNamed('Editar sessão atual'))
    assert.equal(document.querySelector('#focus-title').value, 'Revisar')
    assert.equal(document.querySelector('#focus-task').disabled, true)
    await click(buttonNamed('60m'))
    await click(buttonNamed('Salvar alterações'))
    assert.deepEqual(writes, [['/focus-sessions/14', 'PATCH', { title: 'Revisar', plannedMinutes: 60 }]])
    assert.equal(clock(), status === 'PAUSED' ? '59:00' : '58:50')
    assert.ok(document.querySelector(status === 'PAUSED' ? '[aria-label="Retomar"]' : '[aria-label="Pausar"]'))
  })
}

test('descarte mantém a sessão na falha e remove do histórico após nova tentativa', async () => {
  const sessions = [{ ...focusSession, id: 8, status: 'PAUSED', plannedMinutes: 45,
    startedAt: '2026-02-26T09:30:00Z', pausedAt: '2026-02-26T09:33:00Z' }]
  let attempts = 0
  mockFocusApi(sessions, (path, options) => {
    assert.equal(path, '/focus-sessions/8')
    assert.equal(options.method, 'DELETE')
    if (++attempts === 1) return new Response(null, { status: 503 })
    sessions.pop()
    return new Response(null, { status: 204 })
  })
  await openApp()
  await settle()
  await click(document.querySelector('[aria-label="Descartar sessão"]'))
  await click(buttonNamed('Descartar sessão'))
  assert.ok(document.querySelector('[role="dialog"] [role="alert"]'))
  assert.equal(document.querySelector('[role="timer"]').textContent, '42:00')
  await click(buttonNamed('Descartar sessão'))
  assert.equal(attempts, 2)
  assert.equal(document.querySelector('[role="timer"]').textContent, '45:00')
  assert.equal(document.querySelector('[aria-label="Descartar sessão"]').disabled, true)
  await click(document.querySelector('[aria-label="Opções da sessão"]'))
  assert.equal(document.querySelector('.timer-menu-list').textContent, 'Ver histórico')
  await click(document.querySelector('.timer-menu-list a'))
  await settle()
  assert.equal(window.location.pathname, '/focus-sessions')
  assert.match(document.querySelector('.sessions-empty').textContent, /ainda não tem sessões/)
})

test('falha na revisão permite repetir só a atualização do mesmo registro concluído', async () => {
  const session = { ...focusSession, id: 15, title: 'Estudar', taskId: 7, status: 'RUNNING',
    startedAt: new Date(Date.now() - 60000).toISOString() }
  const writes = []
  mockFocusApi([session], (path, options) => {
    assert.equal(path, '/focus-sessions/15')
    assert.equal(options.method, 'PATCH')
    const body = JSON.parse(options.body)
    writes.push(body)
    if (writes.length === 2) return new Response(null, { status: 503 })
    if (body.status) Object.assign(session, { status: 'COMPLETED', endedAt: new Date().toISOString() })
    else session.taskId = null
    return Response.json(session)
  })
  await openApp()
  await settle()
  await click(buttonNamed('Finalizar sessão'))
  const summary = document.querySelector('.modal-summary').textContent
  await click(buttonNamed('Manter avulsa'))
  assert.ok(document.querySelector('[role="dialog"] [role="alert"]'))
  assert.equal(document.querySelector('.modal-summary').textContent, summary)
  await click(buttonNamed('Manter avulsa'))
  assert.deepEqual(writes, [{ status: 'COMPLETED' }, { title: 'Estudar', association: {} }, { title: 'Estudar', association: {} }])
  assert.equal(document.querySelector('[role="dialog"]'), null)
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

const detailTask = { id: 7, projectId: 3, title: 'Revisar protótipo', description: 'Alta fidelidade', status: 'TODO', priority: 'HIGH', dueDate: null, estimatedMinutes: 90 }

function mockTaskApi(sessions, onOther) {
  mockFocusApi(sessions, (path, options) => {
    if (path === '/tasks/7' && options.method === 'GET') return Response.json(detailTask)
    return onOther?.(path, options)
  })
}

test('os detalhes da tarefa mostram os dados, o histórico e iniciam uma sessão para ela', async () => {
  const calls = []
  mockTaskApi(history, (path, options) => {
    calls.push([options.method, path, options.body])
    return Response.json({ ...focusSession, id: 20, taskId: 7, status: 'RUNNING', startedAt: new Date().toISOString(), endedAt: null }, { status: 201 })
  })
  window.history.pushState({}, '', '/tasks/7')
  await openApp()
  await settle()
  assert.equal(document.querySelector('h1').textContent, 'Revisar protótipo')
  const facts = [...document.querySelectorAll('.task-facts .stat-card')].map((card) => card.textContent)
  assert.deepEqual(facts, ['ProjetoRedesign App', 'PrioridadeAlta', 'Prazo de entregaSem prazo', 'Tempo estimado1h30', 'Tempo focado25 min'])
  assert.equal(document.querySelectorAll('.sessions-table tbody tr').length, 1)
  await act(() => [...document.querySelectorAll('button')].find((button) => button.textContent === 'Iniciar sessão para esta tarefa').click())
  await settle()
  assert.deepEqual(calls, [['POST', '/focus-sessions', JSON.stringify({ taskId: 7, plannedMinutes: 25 })]])
  assert.equal(window.location.pathname + window.location.search, '/dashboard?sessao=20')
})

test('com outra sessão aberta ainda é possível iniciar uma pela tarefa e abrir o Painel nela', async () => {
  const running = { ...focusSession, id: 5, taskId: null, status: 'RUNNING', startedAt: new Date(Date.now() - 60000).toISOString(), endedAt: null }
  const calls = []
  mockTaskApi([running], (path, options) => {
    calls.push([options.method, path, options.body])
    return Response.json({ ...focusSession, id: 21, taskId: 7, status: 'RUNNING', startedAt: new Date().toISOString(), lastActivatedAt: new Date().toISOString(), endedAt: null }, { status: 201 })
  })
  window.history.pushState({}, '', '/tasks/7')
  await openApp()
  await settle()
  assert.equal(document.querySelector('button[aria-label="Iniciar sessão para esta tarefa"]').disabled, false)
  assert.doesNotMatch(document.querySelector('.task-timer').textContent, /Já existe uma sessão/)
  await act(() => document.querySelector('button[aria-label="Iniciar sessão para esta tarefa"]').click())
  await settle()
  assert.deepEqual(calls, [['POST', '/focus-sessions', JSON.stringify({ taskId: 7, plannedMinutes: 25 })]])
  assert.equal(window.location.pathname + window.location.search, '/dashboard?sessao=21')
})

test('editar a tarefa envia PATCH e excluir remove e volta para a lista', async () => {
  const calls = []
  mockTaskApi([], (path, options) => {
    calls.push([options.method, path, options.body])
    return options.method === 'DELETE' ? new Response(null, { status: 204 }) : Response.json({ ...detailTask, title: 'Novo título' })
  })
  window.history.pushState({}, '', '/tasks/7')
  await openApp()
  await settle()
  const byText = (text) => [...document.querySelectorAll('button')].find((button) => button.textContent === text)
  await act(() => byText('Editar').click())
  assert.equal(document.querySelector('[role="dialog"] h2').textContent, 'Editar Tarefa')
  assert.equal(document.querySelector('#task-estimate').value, '90')
  await typeInto('#task-title', 'Novo título')
  await act(() => byText('Salvar Tarefa').click())
  await settle()
  assert.equal(calls[0][0], 'PATCH')
  assert.equal(calls[0][1], '/tasks/7')
  assert.equal(JSON.parse(calls[0][2]).priority, 'HIGH')
  assert.equal(Object.hasOwn(JSON.parse(calls[0][2]), 'estimatedMinutes'), false)
  assert.equal(document.querySelector('h1').textContent, 'Novo título')
  await act(() => byText('Excluir').click())
  await act(() => byText('Excluir tarefa').click())
  await settle()
  assert.deepEqual(calls[1], ['DELETE', '/tasks/7', undefined])
  assert.equal(window.location.pathname, '/tasks')
})

test('uma tarefa inexistente mostra o aviso e o caminho de volta', async () => {
  mockFocusApi([], (path) => (path === '/tasks/99' ? Response.json({ message: 'Not found' }, { status: 404 }) : undefined))
  window.history.pushState({}, '', '/tasks/99')
  await openApp()
  await settle()
  assert.match(document.querySelector('[role="alert"]').textContent, /Tarefa não encontrada/)
  assert.equal(document.querySelector('a[href="/tasks"].dash-btn').textContent, 'Voltar às tarefas')
})

function mockProfileApi(onOther) {
  sessionStorage.setItem(tokenKey, 'valido')
  fetch.mock.mockImplementation((url, options = {}) => {
    const path = url.replace('http://api.test/api/v1', '')
    if (path === '/me' && options.method === 'GET') return Response.json(user)
    return onOther(path, options)
  })
}

// O React deste ambiente só observa digitação pelo caminho de foco + tecla.
async function typeInto(selector, value) {
  // O caminho de compatibilidade do React chama attachEvent/detachEvent, que o jsdom não tem.
  window.HTMLElement.prototype.attachEvent ??= () => {}
  window.HTMLElement.prototype.detachEvent ??= () => {}
  await act(() => {
    const input = document.querySelector(selector)
    input.dispatchEvent(new window.FocusEvent('focusin', { bubbles: true }))
    Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set.call(input, value)
    input.dispatchEvent(new window.KeyboardEvent('keyup', { bubbles: true }))
    input.dispatchEvent(new window.FocusEvent('focusout', { bubbles: true }))
  })
}

test('o perfil só habilita salvar após mudar, envia só o campo alterado e atualiza o menu', async () => {
  const calls = []
  mockProfileApi((path, options) => {
    calls.push([options.method, path, options.body])
    return Response.json({ ...user, name: 'Novo Nome' })
  })
  window.history.pushState({}, '', '/profile')
  await openApp()
  await settle()
  const save = () => [...document.querySelectorAll('button')].find((button) => button.textContent === 'Salvar Alterações')
  assert.equal(document.querySelector('h1').textContent, 'Perfil')
  assert.equal(document.querySelector('#profile-name').value, 'Conta de teste')
  assert.equal(save().disabled, true)
  await typeInto('#new-password', 'rascunho')
  assert.equal(save().disabled, true)
  await typeInto('#profile-name', 'Novo Nome')
  assert.equal(save().disabled, false)
  await act(() => save().click())
  await settle()
  assert.deepEqual(calls, [['PATCH', '/me', JSON.stringify({ name: 'Novo Nome' })]])
  assert.match(document.querySelector('[role="status"]').textContent, /Alterações salvas/)
  assert.match(document.querySelector('.sidebar').textContent, /Novo Nome/)
  assert.equal(save().disabled, true)
})

test('o perfil recusa nome em branco e mostra o conflito de e-mail da API', async () => {
  const calls = []
  mockProfileApi((path, options) => {
    calls.push([options.method, path])
    return Response.json({ message: 'conflict' }, { status: 409 })
  })
  window.history.pushState({}, '', '/profile')
  await openApp()
  await settle()
  const save = () => [...document.querySelectorAll('button')].find((button) => button.textContent === 'Salvar Alterações')
  await typeInto('#profile-name', '   ')
  await act(() => save().click())
  assert.match(document.querySelector('#profile-name-error').textContent, /nome com até 255/)
  assert.equal(calls.length, 0)
  await typeInto('#profile-name', 'Outro Nome')
  await act(() => save().click())
  await settle()
  assert.match(document.querySelector('[role="alert"]').textContent, /já está em uso/)
})

test('troca de senha valida os campos e mantém o login quando a senha atual é recusada', async () => {
  const calls = []
  mockProfileApi((path, options) => {
    calls.push([path, options.method, JSON.parse(options.body)])
    return Response.json({ errors: { currentPassword: ['Senha atual incorreta.'] } }, { status: 400 })
  })
  window.history.pushState({}, '', '/profile')
  await openApp()
  await settle()
  const submit = () => click(document.querySelector('#password-form button[type="submit"]'))
  await typeInto('#current-password', ' anterior ')
  await typeInto('#new-password', ' nova senha ')
  await typeInto('#confirm-password', 'outra')
  await submit()
  assert.match(document.querySelector('#confirm-password-error').textContent, /confirmação/)
  await typeInto('#new-password', 'é'.repeat(37))
  await typeInto('#confirm-password', 'é'.repeat(37))
  await submit()
  assert.match(document.querySelector('#new-password-error').textContent, /72 bytes/)
  assert.equal(calls.length, 0)
  await typeInto('#new-password', ' nova senha ')
  await typeInto('#confirm-password', ' nova senha ')
  await submit()
  assert.deepEqual(calls, [['/me/password', 'PUT', { currentPassword: ' anterior ', newPassword: ' nova senha ' }]])
  assert.equal(document.querySelector('#current-password-error').textContent, 'Senha atual incorreta.')
  assert.equal(sessionStorage.getItem(tokenKey), 'valido')
  assert.equal(window.location.pathname, '/profile')
})

test('troca confirmada envia apenas uma vez e encerra o login com aviso', async () => {
  let calls = 0
  const pending = deferred()
  mockProfileApi(() => { calls++; return pending.promise })
  window.history.pushState({}, '', '/profile')
  await openApp()
  await settle()
  await typeInto('#current-password', 'anterior')
  await typeInto('#new-password', 'é'.repeat(36))
  await typeInto('#confirm-password', 'é'.repeat(36))
  await act(() => {
    const form = document.querySelector('#password-form')
    for (let i = 0; i < 2; i++) form.dispatchEvent(new window.Event('submit', { bubbles: true, cancelable: true }))
  })
  assert.equal(calls, 1)
  assert.equal(document.querySelector('#password-form button').disabled, true)
  await act(() => pending.resolve(new Response(null, { status: 204 })))
  assert.equal(sessionStorage.getItem(tokenKey), null)
  assert.equal(window.location.pathname, '/login')
  assert.equal(document.querySelector('[role="status"]').textContent, 'Senha alterada. Entre com a nova senha.')
})

test('excluir a conta pede confirmação, envia DELETE /me e volta ao login', async () => {
  const calls = []
  mockProfileApi((path, options) => {
    calls.push([options.method, path])
    return new Response(null, { status: 204 })
  })
  window.history.pushState({}, '', '/profile')
  await openApp()
  await settle()
  const byText = (text) => [...document.querySelectorAll('button')].find((button) => button.textContent === text)
  await act(() => byText('Excluir Conta').click())
  assert.equal(document.querySelector('[role="dialog"] h2').textContent, 'Excluir sua conta?')
  assert.equal(calls.length, 0)
  await act(() => byText('Excluir conta').click())
  await settle()
  assert.deepEqual(calls, [['DELETE', '/me']])
  assert.equal(sessionStorage.getItem(tokenKey), null)
  assert.equal(window.location.pathname, '/login')
})

const catalogProject = { id: 3, userId: 1, name: 'Redesign App', description: 'App de produtividade', color: '#E2E8F0', tags: ['Development'] }
const catalogSessions = [{ ...focusSession, id: 1, taskId: 1, projectId: null, status: 'COMPLETED', startedAt: '2026-02-26T09:30:00Z', endedAt: '2026-02-26T09:55:00Z' }]

function mockCatalogApi(onOther, tasks = dashboardTasks, sessions = catalogSessions) {
  sessionStorage.setItem(tokenKey, 'valido')
  fetch.mock.mockImplementation((url, options = {}) => {
    const path = url.replace('http://api.test/api/v1', '')
    if (path === '/me') return Response.json(user)
    if (options.method === 'GET') {
      if (path === '/focus-sessions') return Response.json(sessions)
      if (path === '/tasks') return Response.json(tasks)
      if (path === '/projects') return Response.json([catalogProject])
      if (path === '/projects/3') return Response.json(catalogProject)
      if (path === '/projects/99') return Response.json({ message: 'Not found' }, { status: 404 })
    }
    return onOther?.(path, options) ?? new Response(null, { status: 404 })
  })
}

const buttonByText = (text) => [...document.querySelectorAll('button')].find((button) => button.textContent === text)

for (const route of ['/dashboard', '/tasks', '/tasks/7']) {
  test(`o indicador de projeto em ${route} usa a cor da API`, async () => {
    mockTaskApi([])
    window.history.pushState({}, '', route)
    await openApp()
    await settle()
    const badge = document.querySelector('.project-tag')
    assert.equal(badge.textContent, 'Redesign App')
    assert.equal(badge.style.getPropertyValue('--project-color'), '#E2E8F0')
  })
}

test('a lista de projetos mostra progresso, tarefas e tempo focado, e cria um projeto', async () => {
  const calls = []
  mockCatalogApi((path, options) => {
    calls.push([options.method, path, options.body])
    return Response.json({ id: 8, userId: 1, ...JSON.parse(options.body) }, { status: 201 })
  })
  window.history.pushState({}, '', '/projects')
  await openApp()
  await settle()
  const card = document.querySelector('.project-card')
  assert.equal(card.style.getPropertyValue('--project-color'), '#E2E8F0')
  assert.match(card.textContent, /Redesign App/)
  assert.match(card.textContent, /33%/)
  assert.match(card.textContent, /1\/3 tarefas/)
  assert.match(card.textContent, /25m/)
  assert.equal(card.querySelector('a').getAttribute('href'), '/projects/3')
  await act(() => buttonByText('Novo Projeto').click())
  assert.equal(document.querySelector('[role="dialog"] h2').textContent, 'Criar Novo Projeto')
  assert.equal(buttonByText('Salvar Projeto').disabled, true)
  assert.equal(document.querySelector('input[aria-label="Índigo"]').checked, true)
  assert.equal(document.querySelectorAll('[name="project-color"]').length, 7)
  await typeInto('#project-name', 'Novo app')
  await click(document.querySelector('input[aria-label="Roxo"]'))
  await click(buttonByText('UI/UX'))
  await click(buttonByText('Nova Tag'))
  await typeInto('#project-tag-input', ' ui/ux ')
  await click(buttonByText('Adicionar'))
  assert.match(document.querySelector('#project-tag-error').textContent, /já foi adicionada/)
  await typeInto('#project-tag-input', ' Férias  2026 ')
  await act(() => {
    const input = document.querySelector('#project-tag-input')
    input.dispatchEvent(new window.FocusEvent('focusin', { bubbles: true }))
    input.dispatchEvent(new window.KeyboardEvent('keydown', { key: 'Enter', bubbles: true, cancelable: true }))
  })
  assert.equal(calls.length, 0)
  await act(() => buttonByText('Salvar Projeto').click())
  await settle()
  assert.deepEqual(calls, [['POST', '/projects', JSON.stringify({ name: 'Novo app', description: '', color: '#A855F7', tags: ['UI/UX', 'Férias  2026'] })]])
  assert.equal(document.querySelector('[role="dialog"]'), null)
  assert.equal(document.querySelectorAll('.projects-grid .project-card:not(.project-card-empty)').length, 2)
})

for (const route of ['/projects', '/projects/3']) {
  test(`edição por ${route} conserva ou descarta o rascunho conforme o resultado`, async () => {
    const calls = []
    mockCatalogApi((path, options) => {
      calls.push([path, options.method, JSON.parse(options.body)])
      if (calls.length === 1) return new Response(null, { status: 503 })
      return Response.json({ ...catalogProject, ...JSON.parse(options.body) })
    })
    window.history.pushState({}, '', route)
    await openApp()
    await settle()
    const edit = async () => {
      if (route === '/projects') {
        await click(document.querySelector('.project-card .row-menu-trigger'))
        await click(buttonByText('Editar'))
      } else await click(buttonByText('Editar Projeto'))
    }
    await edit()
    assert.equal(document.querySelector('[aria-label="Cinza-claro"]').checked, true)
    await click(document.querySelector('[aria-label="Remover Development"]'))
    await click(buttonByText('Cancelar'))
    assert.equal(calls.length, 0)
    await edit()
    assert.ok(document.querySelector('[aria-label="Remover Development"]'))
    await click(document.querySelector('[aria-label="Remover Development"]'))
    await click(document.querySelector('[aria-label="Roxo"]'))
    await click(buttonByText('Salvar Projeto'))
    assert.ok(document.querySelector('[role="dialog"] [role="alert"]'))
    assert.equal(document.querySelector('[aria-label="Roxo"]').checked, true)
    await click(buttonByText('Salvar Projeto'))
    assert.deepEqual(calls[1], calls[0])
    assert.equal(calls[1][1], 'PATCH')
    assert.deepEqual(calls[1][2].tags, [])
    assert.equal(document.querySelector('[role="dialog"]'), null)
    await edit()
    assert.equal(document.querySelector('[aria-label="Roxo"]').checked, true)
    assert.equal(document.querySelector('[aria-label="Remover Development"]'), null)
  })
}

test('excluir um projeto pede confirmação e remove o cartão', async () => {
  const calls = []
  mockCatalogApi((path, options) => {
    calls.push([options.method, path])
    return new Response(null, { status: 204 })
  })
  window.history.pushState({}, '', '/projects')
  await openApp()
  await settle()
  await act(() => document.querySelector('.project-card .row-menu-trigger').click())
  assert.deepEqual([...document.querySelectorAll('.row-menu-list button')].map((button) => button.textContent), ['Editar', 'Excluir'])
  await act(() => [...document.querySelectorAll('.row-menu-list button')].find((button) => button.textContent === 'Excluir').click())
  assert.equal(calls.length, 0)
  await act(() => buttonByText('Excluir projeto').click())
  await settle()
  assert.deepEqual(calls, [['DELETE', '/projects/3']])
  assert.equal(document.querySelectorAll('.projects-grid .project-card:not(.project-card-empty)').length, 0)
})

test('os detalhes do projeto listam as tarefas e criam uma nova no projeto', async () => {
  const calls = []
  mockCatalogApi((path, options) => {
    calls.push([options.method, path, options.body])
    if (options.method === 'POST') return Response.json({ id: 40, projectId: 3, title: 'Nova', description: '', status: 'TODO', priority: 'MEDIUM', dueDate: null }, { status: 201 })
    return Response.json({ id: 40, projectId: 3, title: 'Nova', description: '', status: 'IN_PROGRESS', priority: 'HIGH', dueDate: null })
  })
  window.history.pushState({}, '', '/projects/3')
  await openApp()
  await settle()
  assert.equal(document.querySelector('h1').textContent, 'Detalhes do Projeto - Redesign App')
  assert.equal(document.querySelectorAll('.projects-grid .project-card:not(.project-card-empty)').length, 3)
  assert.match(document.querySelector('.project-card').textContent, /Pendente/)
  assert.match(document.querySelector('.project-summary').textContent, /Sem estimativas/)
  await act(() => buttonByText('Nova Tarefa').click())
  assert.equal(document.querySelector('#task-project').value, '3')
  await typeInto('#task-title', 'Nova')
  assert.equal(document.querySelector('#task-estimate').value, '')
  await typeInto('#task-estimate', '90')
  await act(() => document.querySelectorAll('#task-priority + .chip-row .chip')[2].click())
  await act(() => {
    const select = document.querySelector('#task-status')
    select.value = 'IN_PROGRESS'
    select.dispatchEvent(new window.Event('change', { bubbles: true }))
  })
  await act(() => buttonByText('Salvar Tarefa').click())
  await settle()
  assert.deepEqual(calls, [
    ['POST', '/projects/3/tasks', JSON.stringify({ title: 'Nova', description: '', estimatedMinutes: 90 })],
    ['PATCH', '/tasks/40', JSON.stringify({ status: 'IN_PROGRESS', priority: 'HIGH' })],
  ])
  assert.equal(document.querySelectorAll('.projects-grid .project-card:not(.project-card-empty)').length, 4)
})

for (const route of ['/tasks', '/projects/3']) {
  test(`estimativa em ${route} sobrevive à falha do PATCH complementar e permite cancelar e limpar`, async () => {
    let saved
    let attempts = 0
    mockCatalogApi((path, options) => {
      const body = JSON.parse(options.body)
      if (options.method === 'POST') {
        saved = { ...body, id: 51, projectId: 3, status: 'TODO', priority: 'MEDIUM' }
        return Response.json(saved, { status: 201 })
      }
      assert.equal(path, '/tasks/51')
      attempts++
      if (attempts <= 2) return new Response(null, { status: 500 })
      saved = { ...saved, ...body }
      return Response.json(saved)
    })
    window.history.pushState({}, '', route)
    await openApp()
    await settle()
    const clickButton = (label) => act(() => buttonByText(label).click())
    async function edit() {
      await act(() => document.querySelector('[aria-label="Ações da tarefa Estimada"]').click())
      await clickButton('Editar')
    }
    await clickButton('Nova Tarefa')
    await typeInto('#task-title', 'Estimada')
    for (const invalid of ['0', '-1', '1.5']) {
      await typeInto('#task-estimate', invalid)
      assert.equal(buttonByText('Salvar Tarefa').disabled, true)
    }
    await typeInto('#task-estimate', '90')
    await clickButton('Salvar Tarefa')
    await settle()
    assert.equal(saved.estimatedMinutes, 90)
    assert.match(document.body.textContent, /Tarefa criada, mas/)
    await edit()
    assert.equal(document.querySelector('#task-estimate').value, '90')
    await typeInto('#task-estimate', '')
    await clickButton('Cancelar')
    await edit()
    assert.equal(document.querySelector('#task-estimate').value, '90')
    await typeInto('#task-estimate', '360')
    await clickButton('Salvar Tarefa')
    await settle()
    assert.equal(document.querySelector('#task-estimate').value, '360')
    assert.equal(saved.estimatedMinutes, 90)
    await clickButton('Salvar Tarefa')
    await settle()
    assert.equal(saved.estimatedMinutes, 360)
    await edit()
    await typeInto('#task-estimate', '')
    await clickButton('Salvar Tarefa')
    await settle()
    assert.equal(saved.estimatedMinutes, null)
    await edit()
    assert.equal(document.querySelector('#task-estimate').value, '')
  })
}

test('um projeto inexistente mostra o aviso e o caminho de volta', async () => {
  mockCatalogApi()
  window.history.pushState({}, '', '/projects/99')
  await openApp()
  await settle()
  assert.match(document.querySelector('[role="alert"]').textContent, /Projeto não encontrado/)
  assert.equal(document.querySelector('a[href="/projects"].dash-btn').textContent, 'Voltar aos projetos')
})

test('o detalhe do projeto recalcula conclusão e estimativas ao editar as tarefas', async () => {
  const tasks = [
    { ...dashboardTasks[0], estimatedMinutes: 90 },
    { ...dashboardTasks[1], estimatedMinutes: 30 },
    { ...dashboardTasks[2], estimatedMinutes: null },
    { ...dashboardTasks[1], id: 4, title: 'Publicar', estimatedMinutes: null },
    { ...dashboardTasks[1], id: 5, projectId: 8, estimatedMinutes: 360 },
  ]
  mockCatalogApi((path, options) => {
    assert.equal(path, '/tasks/1')
    assert.equal(options.method, 'PATCH')
    Object.assign(tasks[0], JSON.parse(options.body))
    return Response.json(tasks[0])
  }, tasks, [...catalogSessions, { ...catalogSessions[0], id: 2, projectId: 3, taskId: null }])
  window.history.pushState({}, '', '/projects/3')
  await openApp()
  await settle()
  const card = document.querySelector('.project-task-grid .project-card')
  function check(done, percent, estimate, status, label) {
    assert.equal(document.querySelector('#project-completion').textContent, `${done} de 4 tarefas concluídas · ${percent}%`)
    assert.equal(document.querySelector('[role="progressbar"]').getAttribute('aria-valuenow'), String(percent))
    assert.equal(document.querySelectorAll('[role="progressbar"]').length, 1)
    assert.deepEqual([...document.querySelectorAll('.project-time-totals dd')].map(el => el.textContent), [estimate, '50 min'])
    assert.equal(card.querySelector('.task-state-label').textContent, label)
    const track = card.querySelector('.task-state-track')
    assert.equal(track.dataset.status, status)
    assert.equal(track.getAttribute('aria-hidden'), 'true')
    assert.equal(track.hasAttribute('aria-valuenow'), false)
  }
  async function edit(changes) {
    await act(() => card.querySelector('.row-menu-trigger').click())
    await act(() => buttonByText('Editar').click())
    if (changes.status) await act(() => {
      const select = document.querySelector('#task-status')
      select.value = changes.status
      select.dispatchEvent(new window.Event('change', { bubbles: true }))
    })
    if ('estimate' in changes) await typeInto('#task-estimate', changes.estimate)
    await act(() => buttonByText('Salvar Tarefa').click())
    await settle()
  }
  check(2, 50, '2h estimadas em 2 de 4 tarefas', 'TODO', 'Pendente')
  assert.match(card.textContent, /Estimativa: 1h30/)
  assert.match(card.textContent, /Focado: 25 min/)
  await edit({ status: 'IN_PROGRESS' })
  check(2, 50, '2h estimadas em 2 de 4 tarefas', 'IN_PROGRESS', 'Em andamento')
  await edit({ status: 'DONE' })
  check(3, 75, '2h estimadas em 2 de 4 tarefas', 'DONE', '✓ Concluída')
  await edit({ estimate: '120' })
  check(3, 75, '2h30 estimadas em 2 de 4 tarefas', 'DONE', '✓ Concluída')
  await edit({ estimate: '' })
  check(3, 75, '30 min estimados em 1 de 4 tarefas', 'DONE', '✓ Concluída')
  assert.doesNotMatch(card.textContent, /Estimativa:/)
})

test('um projeto vazio mostra o progresso zero e a ausência de estimativas', async () => {
  mockCatalogApi(undefined, [], [])
  window.history.pushState({}, '', '/projects/3')
  await openApp()
  await settle()
  assert.equal(document.querySelector('#project-completion').textContent, '0 de 0 tarefas concluídas · 0%')
  assert.equal(document.querySelector('.project-time-totals dd').textContent, 'Sem estimativas')
})

test('a tabela compara estimativa e foco sem concluir tarefas que ultrapassaram a previsão', async () => {
  const tasks = dashboardTasks.map((task, i) => ({ ...task, estimatedMinutes: [10, 360, null][i] }))
  mockCatalogApi(undefined, tasks)
  window.history.pushState({}, '', '/tasks')
  await openApp()
  await settle()
  const table = document.querySelector('.tasks-table')
  const columns = [...table.tHead.rows[0].cells].map(cell => cell.textContent)
  const estimated = columns.indexOf('Estimado')
  assert.ok(estimated >= 0)
  assert.equal(columns[estimated + 1], 'Focado')
  const rows = [...table.tBodies[0].rows]
  assert.deepEqual(rows.map(row => [row.cells[estimated].textContent, row.cells[estimated + 1].textContent]),
    [['10 min', '25 min'], ['6h', '0 min'], ['—', '0 min']])
  assert.equal(rows[0].querySelector('.task-check').checked, false)
  assert.equal(rows[0].querySelector('.status-badge').dataset.status, 'TODO')
})

for (const [estimate, expected] of [[90, '1h30'], [null, 'Sem estimativa']]) {
  test(`o detalhe exibe ${expected} e zero focado quando a tarefa não tem sessões`, async () => {
    mockFocusApi([], () => Response.json({ ...detailTask, estimatedMinutes: estimate }))
    window.history.pushState({}, '', '/tasks/7')
    await openApp()
    await settle()
    const values = [...document.querySelectorAll('.task-facts dd')].map(el => el.textContent)
    assert.deepEqual(values.slice(-2), [expected, '0 min'])
  })
}

test('a lista de tarefas filtra por status, conclui pelo checkbox e exclui', async () => {
  const calls = []
  mockCatalogApi((path, options) => {
    calls.push([options.method, path, options.body])
    return options.method === 'DELETE' ? new Response(null, { status: 204 }) : Response.json({ ...dashboardTasks[0], status: 'DONE' })
  })
  window.history.pushState({}, '', '/tasks')
  await openApp()
  await settle()
  const titles = () => [...document.querySelectorAll('.tasks-table tbody tr')].map((row) => row.querySelector('a').textContent)
  assert.deepEqual([...document.querySelectorAll('.filter-chip')].map((chip) => chip.textContent), ['Todas', 'Pendentes', 'Em Andamento', 'Concluídas'])
  assert.deepEqual(titles(), ['Revisar protótipo', 'Escrever relatório', 'Planejar o próximo mês'])
  await act(() => buttonByText('Pendentes').click())
  assert.deepEqual(titles(), ['Revisar protótipo', 'Planejar o próximo mês'])
  await act(() => buttonByText('Concluídas').click())
  assert.deepEqual(titles(), ['Escrever relatório'])
  await act(() => buttonByText('Todas').click())
  await act(() => document.querySelector('input[aria-label="Concluir tarefa: Revisar protótipo"]').click())
  await settle()
  assert.deepEqual(calls[0], ['PATCH', '/tasks/1', JSON.stringify({ status: 'DONE' })])
  assert.equal(document.querySelector('input[aria-label="Reabrir tarefa: Revisar protótipo"]').checked, true)
  await act(() => document.querySelector('.tasks-table .row-menu-trigger').click())
  await act(() => [...document.querySelectorAll('.row-menu-list button')].find((button) => button.textContent === 'Excluir').click())
  await act(() => buttonByText('Excluir tarefa').click())
  await settle()
  assert.deepEqual(calls[1], ['DELETE', '/tasks/1', undefined])
  assert.equal(titles().includes('Revisar protótipo'), false)
})

// --- Plano A: várias sessões abertas, uma só contando tempo ---

const ago = (minutes) => new Date(Date.now() - minutes * 60000).toISOString()
const flowA = () => ({ ...focusSession, id: 11, title: 'Foco A', taskId: null, projectId: null, plannedMinutes: 60, status: 'RUNNING', startedAt: ago(20), lastActivatedAt: ago(10), endedAt: null })
const flowB = () => ({ ...focusSession, id: 12, title: 'Foco B', taskId: 7, projectId: null, plannedMinutes: 25, status: 'PAUSED', startedAt: ago(40), lastActivatedAt: ago(40), pausedAt: ago(30), endedAt: null })
const timerName = () => document.querySelector('.timer-session-name')?.textContent
const goTo = (path) => act(() => {
  window.history.pushState({}, '', path)
  window.dispatchEvent(new window.PopStateEvent('popstate'))
})

test('abrir uma sessão pelo link só consulta: nada é enviado e o aviso aponta para a que roda', async () => {
  const calls = []
  mockFocusApi([flowA(), flowB()], (path, options) => { calls.push([options.method, path]); return new Response(null, { status: 500 }) })
  window.history.pushState({}, '', '/dashboard?sessao=12')
  await openApp()
  await settle()
  assert.equal(timerName(), 'Foco B')
  assert.equal(document.querySelector('.timer-session-details').textContent, 'Projeto: Redesign App · Tarefa: Revisar protótipo')
  assert.ok(document.querySelector('button[aria-label="Retomar"]'))
  assert.equal(document.querySelector('.timer-badge').textContent, 'Foco #1 · Pausado')
  const notice = document.querySelector('.timer-notice')
  assert.equal(notice.textContent, 'Em andamento: Foco A')
  assert.equal(notice.querySelector('a').getAttribute('href'), '/dashboard?sessao=11')
  await act(() => window.dispatchEvent(new window.PopStateEvent('popstate')))
  assert.deepEqual(calls, [], 'abrir, atualizar ou voltar não envia Play, Pause nem Concluir')
})

test('pelo menu Painel aparece a sessão que roda, mesmo havendo uma criada depois', async () => {
  const newer = { ...flowB(), id: 13, title: 'Mais nova', startedAt: ago(2), lastActivatedAt: ago(2), pausedAt: ago(1) }
  mockFocusApi([flowA(), newer], undefined)
  await openApp()
  await settle()
  assert.equal(timerName(), 'Foco A')
  assert.ok(document.querySelector('button[aria-label="Pausar"]'))
  assert.equal(document.querySelector('.timer-notice'), null)
})

test('sem sessão rodando o Painel mostra a última que recebeu Play, mesmo uma antiga retomada depois', async () => {
  const resumedOld = { ...flowB(), id: 21, title: 'Antiga retomada', startedAt: ago(90), lastActivatedAt: ago(5), pausedAt: ago(3), totalPausedSeconds: 600 }
  const newer = { ...flowB(), id: 22, title: 'Mais nova', startedAt: ago(20), lastActivatedAt: ago(20), pausedAt: ago(15) }
  mockFocusApi([newer, resumedOld], undefined)
  await openApp()
  await settle()
  assert.equal(timerName(), 'Antiga retomada')
  assert.ok(document.querySelector('button[aria-label="Retomar"]'))
})

test('retomar a sessão exibida pausa a que rodava e as duas ficam corretas no Painel', async () => {
  const calls = []
  const b = flowB()
  mockFocusApi([flowA(), b], (path, options) => {
    calls.push([options.method, path, options.body])
    return Response.json({ ...b, status: 'RUNNING', pausedAt: null, totalPausedSeconds: 600, lastActivatedAt: new Date().toISOString() })
  })
  window.history.pushState({}, '', '/dashboard?sessao=12')
  await openApp()
  await settle()
  await act(() => document.querySelector('button[aria-label="Retomar"]').click())
  await settle()
  assert.deepEqual(calls, [['PATCH', '/focus-sessions/12', JSON.stringify({ status: 'RUNNING' })]])
  assert.ok(document.querySelector('button[aria-label="Pausar"]'))
  assert.equal(document.querySelector('.timer-notice'), null)
  await goTo('/dashboard?sessao=11')
  assert.equal(timerName(), 'Foco A')
  assert.ok(document.querySelector('button[aria-label="Retomar"]'), 'A ficou pausada')
  assert.equal(document.querySelector('.timer-notice').textContent, 'Em andamento: Foco B')
})

test('"Foco avulso" cria outro foco, não alterna o relógio: a sessão que rodava fica pausada', async () => {
  const calls = []
  mockFocusApi([flowA()], (path, options) => {
    calls.push([options.method, path, options.body])
    return Response.json({ ...focusSession, id: 30, title: null, taskId: null, projectId: null, plannedMinutes: 25, status: 'RUNNING', startedAt: new Date().toISOString(), lastActivatedAt: new Date().toISOString(), endedAt: null }, { status: 201 })
  })
  window.history.pushState({}, '', '/dashboard?sessao=11')
  await openApp()
  await settle()
  assert.equal(buttonByText('Foco avulso').disabled, false)
  await act(() => buttonByText('Foco avulso').click())
  await settle()
  assert.deepEqual(calls, [['POST', '/focus-sessions', JSON.stringify({ taskId: null, plannedMinutes: 25 })]])
  assert.equal(window.location.search, '?sessao=30', 'o Painel passa a exibir a sessão criada')
  assert.equal(timerName(), 'Sessão avulsa')
  assert.ok(document.querySelector('button[aria-label="Pausar"]'))
  assert.equal(document.querySelector('.timer-notice'), null)
  await goTo('/dashboard?sessao=11')
  assert.ok(document.querySelector('button[aria-label="Retomar"]'))
  assert.equal(document.querySelector('.timer-notice').textContent, 'Em andamento: Sessão avulsa')
})

test('o Play central age só na sessão exibida e "Finalizar sessão" conclui o ID identificado', async () => {
  const calls = []
  const b = flowB()
  mockFocusApi([flowA(), b], (path, options) => {
    calls.push([options.method, path, options.body])
    return Response.json({ ...b, status: 'COMPLETED', pausedAt: null, endedAt: new Date().toISOString() })
  })
  window.history.pushState({}, '', '/dashboard?sessao=12')
  await openApp()
  await settle()
  await act(() => buttonByText('Finalizar sessão').click())
  await settle()
  assert.deepEqual(calls, [['PATCH', '/focus-sessions/12', JSON.stringify({ status: 'COMPLETED' })]])
  assert.equal(document.querySelector('[role="dialog"] h2').textContent, 'Sessão concluída')
})

test('uma sessão concluída mostra o resumo e "Novo foco", sem Play nem Pause', async () => {
  const done = { ...flowB(), id: 40, title: 'Concluída', status: 'COMPLETED', pausedAt: null, startedAt: ago(30), lastActivatedAt: ago(30), endedAt: ago(5) }
  const calls = []
  mockFocusApi([flowA(), done], (path, options) => { calls.push([options.method, path]); return new Response(null, { status: 500 }) })
  window.history.pushState({}, '', '/dashboard?sessao=40')
  await openApp()
  await settle()
  assert.equal(timerName(), 'Concluída')
  assert.match(document.querySelector('.timer-summary').textContent, /Tempo focado: 25m 00s de 25 min planejados/)
  assert.equal(document.querySelector('.timer-badge').textContent, 'Foco #1 · Concluído')
  for (const label of ['Pausar', 'Retomar']) assert.equal(document.querySelector(`button[aria-label="${label}"]`), null)
  assert.equal(document.querySelector('.timer-notice').textContent, 'Em andamento: Foco A')
  await act(() => buttonByText('Novo foco').click())
  assert.equal(document.querySelector('[role="dialog"] h2').textContent, 'Nova Sessão de Foco')
  assert.deepEqual(calls, [])
})

for (const link of ['999', 'abc']) {
  test(`um link para uma sessão indisponível (${link}) avisa e não deixa os controles agirem em outra`, async () => {
    const calls = []
    mockFocusApi([flowA()], (path, options) => { calls.push([options.method, path]); return new Response(null, { status: 500 }) })
    window.history.pushState({}, '', `/dashboard?sessao=${link}`)
    await openApp()
    await settle()
    assert.match(document.querySelector('[role="alert"]').textContent, /Esta sessão não foi encontrada/)
    assert.equal(document.querySelector('[role="timer"]'), null)
    assert.equal(document.querySelector('button[aria-label="Pausar"]'), null)
    const back = document.querySelector('a[href="/dashboard"].dash-btn')
    assert.equal(back.textContent, 'Voltar ao Painel')
    await act(() => back.click())
    await settle()
    assert.equal(window.location.search, '')
    assert.equal(timerName(), 'Foco A')
    assert.deepEqual(calls, [])
  })
}

test('consultar outra sessão não impede o Painel de concluir a que venceu', async () => {
  const calls = []
  const expired = { ...flowA(), startedAt: ago(30), lastActivatedAt: ago(30), plannedMinutes: 25 }
  mockFocusApi([expired, flowB()], (path, options) => {
    calls.push([options.method, path, options.body])
    return Response.json({ ...expired, status: 'COMPLETED', endedAt: new Date().toISOString() })
  })
  window.history.pushState({}, '', '/dashboard?sessao=12')
  await openApp()
  await settle()
  assert.deepEqual(calls, [], 'a sessão consultada não é concluída')
  await act(() => new Promise((resolve) => setTimeout(resolve, 1200)))
  await settle()
  assert.deepEqual(calls, [['PATCH', '/focus-sessions/11', JSON.stringify({ status: 'COMPLETED' })]])
  assert.equal(document.querySelector('[role="dialog"] h2').textContent, 'Sessão concluída')
  assert.equal(timerName(), 'Foco B')
})

test('a rota de nova sessão abre o formulário mesmo com outra sessão em andamento', async () => {
  mockFocusApi([flowA()], undefined)
  window.history.pushState({}, '', '/focus-sessions/new')
  await openApp()
  await settle()
  assert.equal(document.querySelector('[role="dialog"] h2').textContent, 'Nova Sessão de Foco')
  assert.equal(document.querySelector('[role="alert"]'), null)
})

test('descartar a sessão consultada volta ao Painel sem tratá-la como indisponível', async () => {
  const b = flowB()
  mockFocusApi([flowA(), b], (path, options) => (options.method === 'DELETE' ? new Response(null, { status: 204 }) : new Response(null, { status: 500 })))
  window.history.pushState({}, '', '/dashboard?sessao=12')
  await openApp()
  await settle()
  await click(document.querySelector('[aria-label="Descartar sessão"]'))
  await click(buttonByText('Descartar sessão'))
  await settle()
  assert.equal(window.location.search, '')
  assert.equal(document.querySelector('[role="alert"]'), null)
  assert.equal(timerName(), 'Foco A')
})

const listA = () => ({ ...flowA(), startedAt: ago(20) })
const listSessions = () => [
  listA(),
  flowB(),
  { ...focusSession, id: 13, title: null, taskId: 7, projectId: null, status: 'COMPLETED', startedAt: ago(100), lastActivatedAt: ago(100), endedAt: ago(90) },
  { ...focusSession, id: 14, title: null, taskId: null, projectId: 3, status: 'COMPLETED', startedAt: ago(200), lastActivatedAt: ago(200), endedAt: ago(190) },
  { ...focusSession, id: 15, title: null, taskId: null, projectId: null, status: 'COMPLETED', startedAt: ago(300), lastActivatedAt: ago(300), endedAt: ago(290) },
]
const rowOf = (id) => document.querySelector(`.session-link[href="/dashboard?sessao=${id}"]`).closest('tr')

test('a lista mostra controles por linha e o título como link para o Painel', async () => {
  mockFocusApi(listSessions(), undefined)
  window.history.pushState({}, '', '/focus-sessions')
  await openApp()
  await settle()
  const titles = [...document.querySelectorAll('.session-link')].map((link) => [link.textContent, link.getAttribute('href')])
  assert.deepEqual(titles, [
    ['Foco A', '/dashboard?sessao=11'], ['Foco B', '/dashboard?sessao=12'], ['Foco em Revisar protótipo', '/dashboard?sessao=13'],
    ['Foco no projeto Redesign App', '/dashboard?sessao=14'], ['Sessão avulsa', '/dashboard?sessao=15'],
  ])
  const labels = (id) => [...rowOf(id).querySelectorAll('.row-control')].map((button) => button.getAttribute('aria-label'))
  assert.deepEqual(labels(11), ['Pausar sessão: Foco A', 'Concluir sessão: Foco A'])
  assert.deepEqual(labels(12), ['Retomar sessão: Foco B', 'Concluir sessão: Foco B'])
  for (const id of [13, 14, 15]) {
    assert.deepEqual(labels(id), [])
    assert.equal(rowOf(id).querySelector('.row-done').textContent, 'Concluída')
  }
  await act(() => rowOf(11).querySelector('.row-menu-trigger').click())
  assert.deepEqual([...rowOf(11).querySelectorAll('.row-menu-list button')].map((button) => button.textContent), ['Editar', 'Excluir'])
  await act(() => rowOf(12).querySelector('.session-link').click())
  assert.equal(window.location.pathname + window.location.search, '/dashboard?sessao=12')
  assert.equal(timerName(), 'Foco B')
})

test('retomar na lista deixa A pausada e B rodando, sem sair da página, e ignora cliques repetidos', async () => {
  const calls = []
  const b = flowB()
  mockFocusApi(listSessions(), (path, options) => {
    calls.push([options.method, path, options.body])
    return Response.json({ ...b, status: 'RUNNING', pausedAt: null, lastActivatedAt: new Date().toISOString() })
  })
  window.history.pushState({}, '', '/focus-sessions')
  await openApp()
  await settle()
  const resume = rowOf(12).querySelector('button[aria-label="Retomar sessão: Foco B"]')
  await act(() => { resume.click(); resume.click() })
  await settle()
  assert.deepEqual(calls, [['PATCH', '/focus-sessions/12', JSON.stringify({ status: 'RUNNING' })]])
  assert.equal(window.location.pathname, '/focus-sessions')
  assert.match(rowOf(11).textContent, /Pausada/)
  assert.match(rowOf(12).textContent, /Em andamento/)
  assert.ok(rowOf(11).querySelector('button[aria-label="Retomar sessão: Foco A"]'))
  assert.ok(rowOf(12).querySelector('button[aria-label="Pausar sessão: Foco B"]'))
})

test('concluir uma sessão pausada na lista não mexe na que está rodando', async () => {
  const calls = []
  const b = flowB()
  mockFocusApi(listSessions(), (path, options) => {
    calls.push([options.method, path, options.body])
    return Response.json({ ...b, status: 'COMPLETED', pausedAt: null, endedAt: new Date().toISOString() })
  })
  window.history.pushState({}, '', '/focus-sessions')
  await openApp()
  await settle()
  await act(() => rowOf(12).querySelector('button[aria-label="Concluir sessão: Foco B"]').click())
  await settle()
  assert.deepEqual(calls, [['PATCH', '/focus-sessions/12', JSON.stringify({ status: 'COMPLETED' })]])
  assert.match(rowOf(11).textContent, /Em andamento/)
  assert.equal(rowOf(12).querySelector('.row-done').textContent, 'Concluída')
})

test('uma falha ao retomar mostra o erro e mantém as linhas como o servidor as devolve', async () => {
  mockFocusApi(listSessions(), () => Response.json({ detail: 'Falha no servidor' }, { status: 503 }))
  window.history.pushState({}, '', '/focus-sessions')
  await openApp()
  await settle()
  await act(() => rowOf(12).querySelector('button[aria-label="Retomar sessão: Foco B"]').click())
  await settle()
  assert.ok(document.querySelector('[role="alert"]'))
  assert.match(rowOf(11).textContent, /Em andamento/)
  assert.match(rowOf(12).textContent, /Pausada/)
  assert.equal(rowOf(12).querySelector('button[aria-label="Retomar sessão: Foco B"]').disabled, false)
})

test('os detalhes do projeto listam as sessões do projeto e abrem cada uma no Painel', async () => {
  const sessions = [
    { ...focusSession, id: 51, title: 'Estudo', taskId: 1, projectId: null, status: 'COMPLETED', startedAt: '2026-02-26T09:30:00Z', lastActivatedAt: '2026-02-26T09:30:00Z', endedAt: '2026-02-26T09:55:00Z' },
    { ...focusSession, id: 52, title: null, taskId: null, projectId: 3, status: 'PAUSED', startedAt: '2026-02-27T09:30:00Z', lastActivatedAt: '2026-02-27T09:30:00Z', pausedAt: '2026-02-27T09:40:00Z', endedAt: null },
    { ...focusSession, id: 53, title: 'Outro projeto', taskId: 99, projectId: null, status: 'COMPLETED', startedAt: '2026-02-26T09:30:00Z', endedAt: '2026-02-26T09:55:00Z' },
    { ...focusSession, id: 54, title: 'Avulsa', taskId: null, projectId: null, status: 'COMPLETED', startedAt: '2026-02-26T09:30:00Z', endedAt: '2026-02-26T09:55:00Z' },
  ]
  mockCatalogApi(undefined, dashboardTasks, sessions)
  window.history.pushState({}, '', '/projects/3')
  await openApp()
  await settle()
  assert.equal(document.querySelector('#project-sessions').textContent, 'Sessões do projeto')
  const rows = [...document.querySelectorAll('.project-session-row')]
  assert.deepEqual(rows.map((row) => row.querySelector('strong').textContent), ['Foco no projeto Redesign App', 'Estudo'])
  assert.match(rows[0].textContent, /Pausada · 10m 00s/)
  const links = rows.map((row) => row.querySelector('a'))
  assert.deepEqual(links.map((link) => link.getAttribute('href')), ['/dashboard?sessao=52', '/dashboard?sessao=51'])
  assert.equal(links[0].textContent, 'Abrir no Painel')
  assert.equal(links[0].getAttribute('aria-label'), 'Abrir no Painel: Foco no projeto Redesign App')
  await act(() => links[1].click())
  await settle()
  assert.equal(window.location.pathname + window.location.search, '/dashboard?sessao=51')
})

test('um projeto sem sessões mostra o estado vazio', async () => {
  mockCatalogApi(undefined, dashboardTasks, [])
  window.history.pushState({}, '', '/projects/3')
  await openApp()
  await settle()
  assert.equal(document.querySelector('.project-sessions-section .tasks-empty').textContent, 'Nenhuma sessão de foco neste projeto ainda.')
})
