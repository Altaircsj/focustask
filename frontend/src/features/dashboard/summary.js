import { activeSeconds } from '../focusSessions/format.js'
import { defaultSession } from '../focusSessions/sessionInfo.js'

export const DEFAULT_MINUTES = 25
export const MINUTE_OPTIONS = [15, 25, 45, 60, 90, 120, 240]
export const MAX_MINUTES = 240
export const FOCUS_SECONDS = DEFAULT_MINUTES * 60
const PRIORITY_RANK = { HIGH: 0, MEDIUM: 1, LOW: 2 }
const pad = (value) => String(value).padStart(2, '0')

export function greeting(date = new Date()) {
  const hour = date.getHours()
  if (hour >= 5 && hour < 12) return 'Bom dia'
  return hour >= 12 && hour < 18 ? 'Boa tarde' : 'Boa noite'
}

export function firstName(name) {
  return name.trim().split(/\s+/)[0]
}

export function formatClock(totalSeconds) {
  return `${pad(Math.floor(totalSeconds / 60))}:${pad(totalSeconds % 60)}`
}

export function formatFocusedTime(totalSeconds) {
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  return hours > 0 ? `${hours}h ${minutes}m` : `${minutes}m`
}

function isSameDay(a, b) {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate()
}

function startedToday(session, now) {
  return isSameDay(new Date(session.startedAt), new Date(now))
}

// Tempo e sessões são do dia; tarefas não guardam data de conclusão, então contam no total.
export function summarize({ sessions, tasks }, now = Date.now()) {
  const today = sessions.filter((session) => startedToday(session, now))
  return {
    focusedSeconds: today.reduce((total, session) => total + activeSeconds(session, now), 0),
    completedSessions: today.filter((session) => session.status === 'COMPLETED').length,
    doneTasks: tasks.filter((task) => task.status === 'DONE').length,
    pendingTasks: tasks.filter((task) => task.status !== 'DONE').length,
  }
}

// Sessão que o Painel mostra quando nenhuma foi escolhida: a que está rodando ou, sem nenhuma rodando,
// a última que recebeu Play (pausada ou concluída).
export function currentSession(sessions) {
  return defaultSession(sessions)
}

// Posição da sessão exibida entre as do mesmo dia, pela ordem de início; sem sessão, a próxima do dia.
export function focusNumber(sessions, viewed, now = Date.now()) {
  const day = new Date(viewed ? viewed.startedAt : now)
  const sameDay = sessions.filter((session) => isSameDay(new Date(session.startedAt), day))
  if (!viewed) return sameDay.length + 1
  const started = Date.parse(viewed.startedAt)
  const before = sameDay.filter((session) => Date.parse(session.startedAt) < started
    || (Date.parse(session.startedAt) === started && session.id < viewed.id))
  return before.length + 1
}

export function remainingSeconds(session, now = Date.now(), total = FOCUS_SECONDS) {
  return Math.max(0, total - activeSeconds(session, now))
}

// Pendentes com prazo até hoje ou sem prazo, do prazo mais antigo e maior prioridade ao mais recente.
export function dailyTasks(tasks, now = Date.now(), limit = 5) {
  const date = new Date(now)
  const today = `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
  const rank = (task) => PRIORITY_RANK[task.priority] ?? 3
  return tasks
    .filter((task) => task.status !== 'DONE' && (!task.dueDate || task.dueDate <= today))
    .sort((a, b) => (a.dueDate ?? '9999').localeCompare(b.dueDate ?? '9999') || rank(a) - rank(b) || a.id - b.id)
    .slice(0, limit)
}
