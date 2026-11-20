import { test } from 'node:test'
import assert from 'node:assert/strict'
import { activeSeconds, formatDuration, formatStartedAt } from '../src/features/focusSessions/format.js'

const base = { startedAt: '2026-02-26T09:30:00Z', endedAt: null, pausedAt: null, totalPausedSeconds: 0 }

test('a duração de uma sessão concluída desconta o tempo pausado', () => {
  const session = { ...base, endedAt: '2026-02-26T10:00:00Z', totalPausedSeconds: 300 }
  assert.equal(activeSeconds(session), 25 * 60)
})

test('uma sessão em andamento conta até agora e uma pausada, até a pausa', () => {
  const now = Date.parse('2026-02-26T09:45:00Z')
  assert.equal(activeSeconds(base, now), 15 * 60)
  assert.equal(activeSeconds({ ...base, pausedAt: '2026-02-26T09:40:00Z' }, now), 10 * 60)
})

test('a duração nunca fica negativa', () => {
  assert.equal(activeSeconds({ ...base, totalPausedSeconds: 9999 }, Date.parse('2026-02-26T09:31:00Z')), 0)
})

test('formata a duração como minutos e segundos', () => {
  assert.equal(formatDuration(25 * 60), '25m 00s')
  assert.equal(formatDuration(3605), '60m 05s')
})

test('formata a data como Hoje, Ontem ou dia e mês', () => {
  const now = new Date(2026, 1, 26, 18, 0)
  assert.equal(formatStartedAt(new Date(2026, 1, 26, 16, 30).toISOString(), now), 'Hoje, 16:30')
  assert.equal(formatStartedAt(new Date(2026, 1, 25, 17, 40).toISOString(), now), 'Ontem, 17:40')
  assert.equal(formatStartedAt(new Date(2026, 1, 20, 9, 5).toISOString(), now), '20 Fev, 09:05')
  assert.equal(formatStartedAt(new Date(2025, 11, 31, 23, 59).toISOString(), now), '31 Dez 2025, 23:59')
})
