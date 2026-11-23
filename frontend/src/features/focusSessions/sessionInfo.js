// Regras puras sobre sessões de foco: qual roda, qual recebeu o último Play, como nomeá-las
// e como refletir no estado local o que o servidor fez ao ativar uma sessão.

const time = (iso) => Date.parse(iso)

// Instante do último Play; registros sem o campo caem na criação.
export function activatedAt(session) {
  return session.lastActivatedAt ?? session.startedAt
}

// Mais recente primeiro: último Play, depois criação e, por fim, o maior id (desempate determinístico).
export function byLastActivation(a, b) {
  return time(activatedAt(b)) - time(activatedAt(a)) || time(b.startedAt) - time(a.startedAt) || b.id - a.id
}

// A sessão que está contando tempo. Contas antigas podem ter mais de uma; vale a ativada por último.
export function runningSession(sessions) {
  return sessions.filter((session) => session.status === 'RUNNING').sort(byLastActivation)[0] ?? null
}

// A que recebeu o último Play, esteja rodando, pausada ou concluída.
export function lastActivatedSession(sessions) {
  return [...sessions].sort(byLastActivation)[0] ?? null
}

// O que o Painel mostra sem uma sessão escolhida: a que roda ou, se nenhuma roda, a última ativada.
export function defaultSession(sessions) {
  return runningSession(sessions) ?? lastActivatedSession(sessions)
}

// Nome para o player: título, depois tarefa, depois projeto e, por fim, "Sessão avulsa".
// As linhas de detalhe não repetem o nome já exibido.
export function describeSession(session, tasks, projects) {
  const task = session.taskId == null ? null : tasks.get(session.taskId) ?? null
  const project = projects.get(task ? task.projectId : session.projectId) ?? null
  const name = session.title?.trim() || task?.title || project?.name || 'Sessão avulsa'
  const details = []
  if (project && project.name !== name) details.push(`Projeto: ${project.name}`)
  if (task && task.title !== name) details.push(`Tarefa: ${task.title}`)
  return { task, project, name, details }
}

// Título nas listas: o da sessão ou "Foco em [tarefa]", "Foco no projeto [projeto]", "Sessão avulsa".
export function listTitle(session, task, project) {
  const title = session.title?.trim()
  if (title) return title
  if (task) return `Foco em ${task.title}`
  if (project) return `Foco no projeto ${project.name}`
  return 'Sessão avulsa'
}

export const sessionLink = (id) => `/dashboard?sessao=${id}`

export function replaceSession(sessions, session) {
  return sessions.some((item) => item.id === session.id)
    ? sessions.map((item) => (item.id === session.id ? session : item))
    : [...sessions, session]
}

// Aplica a resposta de uma criação, Play, pausa ou conclusão. Quando a sessão devolvida está rodando,
// o servidor pausou, no mesmo instante da ativação, as outras que rodavam; o estado local faz o mesmo.
export function applyActivation(sessions, activated) {
  const next = replaceSession(sessions, activated)
  if (activated.status !== 'RUNNING') return next
  const instant = activated.lastActivatedAt ?? new Date().toISOString()
  return next.map((session) => (session.id !== activated.id && session.status === 'RUNNING'
    ? { ...session, status: 'PAUSED', pausedAt: instant }
    : session))
}
