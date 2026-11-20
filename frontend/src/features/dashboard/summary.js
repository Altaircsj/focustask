import { activeSeconds } from '../focusSessions/format.js'

export const FOCUS_SECONDS = 25 * 60
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

// Sessão em andamento ou pausada mais recente; é a que o cronômetro acompanha.
export function currentSession(sessions) {
  return sessions
    .filter((session) => session.status !== 'COMPLETED')
    .sort((a, b) => Date.parse(b.startedAt) - Date.parse(a.startedAt))[0] ?? null
}

export function focusNumber(sessions, current, now = Date.now()) {
  const today = sessions.filter((session) => startedToday(session, now)).length
  return Math.max(1, today + (current ? 0 : 1))
}

export function remainingSeconds(session, now = Date.now()) {
  return Math.max(0, FOCUS_SECONDS - activeSeconds(session, now))
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
