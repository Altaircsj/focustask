import { activeSeconds } from '../focusSessions/format.js'

export function projectStats(project, tasks, sessions, now = Date.now()) {
  const own = tasks.filter((task) => task.projectId === project.id)
  const ids = new Set(own.map((task) => task.id))
  const done = own.filter((task) => task.status === 'DONE').length
  const focusedSeconds = sessions
    .filter((session) => ids.has(session.taskId) || (session.taskId == null && session.projectId === project.id))
    .reduce((total, session) => total + activeSeconds(session, now), 0)
  return { total: own.length, done, percent: own.length === 0 ? 0 : Math.round((done * 100) / own.length), focusedSeconds }
}

export function focusedByTask(sessions, now = Date.now()) {
  const totals = new Map()
  for (const session of sessions) {
    if (session.taskId == null) continue
    totals.set(session.taskId, (totals.get(session.taskId) ?? 0) + activeSeconds(session, now))
  }
  return totals
}
