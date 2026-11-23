import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  FOCUS_SECONDS, currentSession, dailyTasks, firstName, focusNumber, formatClock, formatFocusedTime, greeting, remainingSeconds, summarize,
} from '../src/features/dashboard/summary.js'

const now = new Date(2026, 1, 26, 15, 0).getTime()
const at = (hour, minute = 0, day = 26) => new Date(2026, 1, day, hour, minute).toISOString()
const session = (overrides) => ({ id: 1, taskId: null, status: 'COMPLETED', startedAt: at(9), endedAt: at(9, 25), pausedAt: null, totalPausedSeconds: 0, ...overrides })

test('a saudação segue o horário e usa só o primeiro nome', () => {
  assert.equal(greeting(new Date(2026, 1, 26, 4)), 'Boa noite')
  assert.equal(greeting(new Date(2026, 1, 26, 9)), 'Bom dia')
  assert.equal(greeting(new Date(2026, 1, 26, 15)), 'Boa tarde')
  assert.equal(greeting(new Date(2026, 1, 26, 18)), 'Boa noite')
  assert.equal(firstName('  João   John '), 'João')
})

test('formata o relógio e o tempo focado', () => {
  assert.equal(formatClock(FOCUS_SECONDS), '25:00')
  assert.equal(formatClock(61), '01:01')
  assert.equal(formatFocusedTime(4 * 3600 + 32 * 60 + 59), '4h 32m')
  assert.equal(formatFocusedTime(45 * 60), '45m')
  assert.equal(formatFocusedTime(0), '0m')
})

test('o resumo conta tempo e sessões só de hoje e tarefas no total', () => {
  const sessions = [
    session({ id: 1 }),
    session({ id: 2, startedAt: at(10), endedAt: at(10, 15), totalPausedSeconds: 300 }),
    session({ id: 3, startedAt: at(14, 50), endedAt: null, status: 'RUNNING' }),
    session({ id: 4, startedAt: at(9, 0, 25), endedAt: at(10, 0, 25) }),
  ]
  const tasks = [{ status: 'DONE' }, { status: 'DONE' }, { status: 'TODO' }, { status: 'IN_PROGRESS' }, { status: 'TODO' }]
  assert.deepEqual(summarize({ sessions, tasks }, now), {
    focusedSeconds: 25 * 60 + 10 * 60 + 10 * 60,
    completedSessions: 2,
    doneTasks: 2,
    pendingTasks: 3,
  })
})

test('o Painel acompanha a sessão que roda e, sem nenhuma rodando, a última que recebeu Play', () => {
  const sessions = [
    session({ id: 1 }),
    session({ id: 2, status: 'PAUSED', startedAt: at(8), endedAt: null, pausedAt: at(8, 30) }),
    session({ id: 3, status: 'RUNNING', startedAt: at(7), endedAt: null }),
  ]
  assert.equal(currentSession(sessions).id, 3)
  assert.equal(currentSession([session({ id: 1 })]).id, 1)
  assert.equal(currentSession([]), null)
  const paused = sessions.slice(0, 2)
  assert.equal(currentSession(paused).id, 1, 'concluída em seguida é a última ativada')
  const resumedLater = [paused[0], { ...paused[1], lastActivatedAt: at(14) }]
  assert.equal(currentSession(resumedLater).id, 2, 'a retomada mais recente vence a criação mais recente')
})

test('o tempo restante parte de 25 minutos e nunca fica negativo', () => {
  const running = session({ status: 'RUNNING', startedAt: at(14, 50), endedAt: null })
  assert.equal(remainingSeconds(running, now), 15 * 60)
  assert.equal(remainingSeconds(session({ status: 'RUNNING', startedAt: at(13), endedAt: null }), now), 0)
  assert.equal(remainingSeconds(running, now, 45 * 60), 35 * 60)
})

test('o número do foco é a posição da sessão exibida entre as do mesmo dia', () => {
  const sessions = [session({ id: 1 }), session({ id: 2, status: 'RUNNING', startedAt: at(14, 50), endedAt: null })]
  assert.equal(focusNumber(sessions, sessions[1], now), 2)
  assert.equal(focusNumber(sessions, sessions[0], now), 1, 'consultar uma sessão antiga mostra a posição dela')
  assert.equal(focusNumber(sessions.slice(0, 1), null, now), 2)
  assert.equal(focusNumber([], null, now), 1)
  const yesterday = session({ id: 9, status: 'RUNNING', startedAt: at(23, 0, 25), endedAt: null })
  assert.equal(focusNumber([yesterday], yesterday, now), 1)
})

test('as tarefas do dia são pendentes, sem prazo futuro e ordenadas por prazo e prioridade', () => {
  const tasks = [
    { id: 1, status: 'DONE', dueDate: '2026-02-26', priority: 'HIGH' },
    { id: 2, status: 'TODO', dueDate: '2026-03-10', priority: 'HIGH' },
    { id: 3, status: 'TODO', dueDate: null, priority: 'HIGH' },
    { id: 4, status: 'TODO', dueDate: '2026-02-26', priority: 'LOW' },
    { id: 5, status: 'IN_PROGRESS', dueDate: '2026-02-26', priority: 'HIGH' },
    { id: 6, status: 'TODO', dueDate: '2026-02-20', priority: null },
  ]
  assert.deepEqual(dailyTasks(tasks, now).map((task) => task.id), [6, 5, 4, 3])
  assert.equal(dailyTasks(tasks, now, 2).length, 2)
})
