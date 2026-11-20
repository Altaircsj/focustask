const MONTHS = ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez']
const pad = (value) => String(value).padStart(2, '0')
const startOfDay = (date) => new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime()

// Tempo ativo: sem as pausas. Pausada conta até a pausa; concluída, até o fim.
export function activeSeconds(session, now = Date.now()) {
  const start = Date.parse(session.startedAt)
  const end = session.endedAt ? Date.parse(session.endedAt) : session.pausedAt ? Date.parse(session.pausedAt) : now
  return Math.max(0, Math.floor((end - start) / 1000) - session.totalPausedSeconds)
}

export function formatDuration(totalSeconds) {
  return `${Math.floor(totalSeconds / 60)}m ${pad(totalSeconds % 60)}s`
}

export function formatStartedAt(iso, now = new Date()) {
  const date = new Date(iso)
  const time = `${pad(date.getHours())}:${pad(date.getMinutes())}`
  const days = Math.round((startOfDay(now) - startOfDay(date)) / 86400000)
  if (days === 0) return `Hoje, ${time}`
  if (days === 1) return `Ontem, ${time}`
  const year = date.getFullYear() === now.getFullYear() ? '' : ` ${date.getFullYear()}`
  return `${pad(date.getDate())} ${MONTHS[date.getMonth()]}${year}, ${time}`
}
