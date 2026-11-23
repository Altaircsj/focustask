const MONTHS = ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez']
const pad = (value) => String(value).padStart(2, '0')

export const PRIORITY_LABEL = { HIGH: 'Alta', MEDIUM: 'Média', LOW: 'Baixa' }
export const STATUS_LABEL = { TODO: 'A fazer', IN_PROGRESS: 'Em andamento', DONE: 'Concluída' }

export function formatMinutes(minutes, empty = '—') {
  if (minutes == null) return empty
  if (minutes < 60) return `${minutes} min`
  const hours = Math.floor(minutes / 60)
  const remainder = minutes % 60
  return remainder ? `${hours}h${pad(remainder)}` : `${hours}h`
}

export function formatFocusedSeconds(seconds) {
  return formatMinutes(Math.floor(seconds / 60))
}

function localDate(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

// O prazo é só uma data ('YYYY-MM-DD'); o backend não guarda horário.
export function formatDueDate(dueDate, now = Date.now()) {
  if (!dueDate) return 'Sem prazo'
  const today = new Date(now)
  const offset = (days) => localDate(new Date(today.getFullYear(), today.getMonth(), today.getDate() + days))
  if (dueDate === localDate(today)) return 'Hoje'
  if (dueDate === offset(1)) return 'Amanhã'
  if (dueDate === offset(-1)) return 'Ontem'
  const [year, month, day] = dueDate.split('-').map(Number)
  return `${pad(day)} ${MONTHS[month - 1]}${year === today.getFullYear() ? '' : ` ${year}`}`
}
