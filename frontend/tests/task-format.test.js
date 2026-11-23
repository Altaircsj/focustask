import { test } from 'node:test'
import assert from 'node:assert/strict'
import { formatDueDate, formatMinutes, formatFocusedSeconds } from '../src/features/tasks/format.js'

const now = new Date(2026, 1, 26, 15, 0).getTime()

test('formata estimativas em minutos e mantém a ausência distinta de zero', () => {
  assert.equal(formatMinutes(25), '25 min')
  assert.equal(formatMinutes(65), '1h05')
  assert.equal(formatMinutes(90), '1h30')
  assert.equal(formatMinutes(360), '6h')
  assert.equal(formatMinutes(null), '—')
  assert.equal(formatMinutes(undefined, 'Sem estimativa'), 'Sem estimativa')
  assert.equal(formatFocusedSeconds(0), '0 min')
  assert.equal(formatFocusedSeconds(90 * 60 + 59), '1h30')
})

test('o prazo vira Hoje, Amanhã ou Ontem perto da data atual', () => {
  assert.equal(formatDueDate('2026-02-26', now), 'Hoje')
  assert.equal(formatDueDate('2026-02-27', now), 'Amanhã')
  assert.equal(formatDueDate('2026-02-25', now), 'Ontem')
})

test('o prazo distante mostra dia e mês, com o ano só se for outro', () => {
  assert.equal(formatDueDate('2026-03-05', now), '05 Mar')
  assert.equal(formatDueDate('2027-01-09', now), '09 Jan 2027')
  assert.equal(formatDueDate(null, now), 'Sem prazo')
})
