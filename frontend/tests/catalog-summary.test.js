import { test } from 'node:test'
import assert from 'node:assert/strict'
import { focusedByTask, projectStats } from '../src/features/projects/summary.js'

const now = new Date(2026, 1, 26, 15, 0).getTime()
const at = (hour, minute = 0) => new Date(2026, 1, 26, hour, minute).toISOString()
const session = (overrides) => ({ id: 1, taskId: null, projectId: null, status: 'COMPLETED', startedAt: at(9), endedAt: at(9, 25), pausedAt: null, totalPausedSeconds: 0, ...overrides })

test('o resumo do projeto conta tarefas concluídas, percentual e tempo focado', () => {
  const tasks = [
    { id: 1, projectId: 5, status: 'DONE', estimatedMinutes: 90 },
    { id: 2, projectId: 5, status: 'TODO', estimatedMinutes: 30 },
    { id: 3, projectId: 5, status: 'IN_PROGRESS', estimatedMinutes: null },
    { id: 4, projectId: 5, status: 'DONE' },
    { id: 9, projectId: 6, status: 'DONE', estimatedMinutes: 360 },
  ]
  const sessions = [
    session({ id: 1, taskId: 1, totalPausedSeconds: 90 }),
    session({ id: 2, taskId: 2, projectId: 5, startedAt: at(10), endedAt: at(10, 15) }),
    session({ id: 3, taskId: null, projectId: 5, startedAt: at(11), endedAt: at(11, 10) }),
    session({ id: 4, taskId: 9, startedAt: at(12), endedAt: at(12, 45) }),
  ]
  assert.deepEqual(projectStats({ id: 5 }, tasks, sessions, now), {
    total: 4, done: 2, percent: 50, estimatedMinutes: 120, estimatedCount: 2, focusedSeconds: (25 + 15 + 10) * 60 - 90,
  })
})

test('um projeto sem tarefas fica em 0% e sem tempo', () => {
  assert.deepEqual(projectStats({ id: 1 }, [], [], now), {
    total: 0, done: 0, percent: 0, estimatedMinutes: 0, estimatedCount: 0, focusedSeconds: 0,
  })
})

test('o tempo focado por tarefa soma só sessões associadas a tarefas', () => {
  const totals = focusedByTask([session({ id: 1, taskId: 1 }), session({ id: 2, taskId: 1 }), session({ id: 3, taskId: null })], now)
  assert.equal(totals.get(1), 50 * 60)
  assert.equal(totals.size, 1)
})
