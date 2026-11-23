import test from 'node:test'
import assert from 'node:assert/strict'
import {
  applyActivation, byLastActivation, defaultSession, describeSession, lastActivatedSession, listTitle, runningSession, sessionLink,
} from '../src/features/focusSessions/sessionInfo.js'
import { projectSessions } from '../src/features/projects/summary.js'

const at = (minute) => new Date(Date.UTC(2026, 8, 17, 12, minute)).toISOString()
const session = (overrides) => ({ id: 1, taskId: null, projectId: null, title: null, status: 'PAUSED', startedAt: at(0), lastActivatedAt: at(0), endedAt: null, pausedAt: at(5), totalPausedSeconds: 0, ...overrides })

test('a sessão que conta tempo é a que roda; havendo várias, a ativada por último', () => {
  assert.equal(runningSession([session({ id: 1 }), session({ id: 2 })]), null)
  const legacy = [
    session({ id: 1, status: 'RUNNING', lastActivatedAt: at(1) }),
    session({ id: 2, status: 'RUNNING', lastActivatedAt: at(9) }),
    session({ id: 3, status: 'PAUSED', lastActivatedAt: at(20) }),
  ]
  assert.equal(runningSession(legacy).id, 2)
})

test('a última ativada considera retomadas, não só a criação, e desempata por início e id', () => {
  const sessions = [
    session({ id: 1, startedAt: at(0), lastActivatedAt: at(30) }),
    session({ id: 2, startedAt: at(10), lastActivatedAt: at(10) }),
  ]
  assert.equal(lastActivatedSession(sessions).id, 1)
  assert.equal(lastActivatedSession([]), null)
  const tied = [session({ id: 4, lastActivatedAt: at(7) }), session({ id: 5, lastActivatedAt: at(7) })]
  assert.equal(lastActivatedSession(tied).id, 5)
  assert.ok(byLastActivation(tied[0], tied[1]) > 0)
  assert.equal(lastActivatedSession([{ ...session({ id: 6 }), lastActivatedAt: undefined, startedAt: at(40) }, session({ id: 7 })]).id, 6, 'sem o campo vale a criação')
})

test('sem sessão rodando o Painel cai na última ativada, mesmo concluída', () => {
  const done = session({ id: 9, status: 'COMPLETED', pausedAt: null, endedAt: at(50), lastActivatedAt: at(40) })
  assert.equal(defaultSession([session({ id: 1, lastActivatedAt: at(5) }), done]).id, 9)
  assert.equal(defaultSession([done, session({ id: 2, status: 'RUNNING', pausedAt: null, lastActivatedAt: at(1) })]).id, 2)
  assert.equal(defaultSession([]), null)
})

test('o nome do player segue título, tarefa, projeto e sessão avulsa, sem repetir nomes', () => {
  const tasks = new Map([[7, { id: 7, projectId: 3, title: 'Revisar autenticação' }]])
  const projects = new Map([[3, { id: 3, name: 'Programação Web' }]])
  assert.deepEqual(describeSession(session({ title: ' Revisão para a prova ', taskId: 7 }), tasks, projects).details, ['Projeto: Programação Web', 'Tarefa: Revisar autenticação'])
  assert.equal(describeSession(session({ title: ' Revisão para a prova ', taskId: 7 }), tasks, projects).name, 'Revisão para a prova')
  const byTask = describeSession(session({ taskId: 7 }), tasks, projects)
  assert.equal(byTask.name, 'Revisar autenticação')
  assert.deepEqual(byTask.details, ['Projeto: Programação Web'])
  const byProject = describeSession(session({ projectId: 3 }), tasks, projects)
  assert.equal(byProject.name, 'Programação Web')
  assert.deepEqual(byProject.details, [])
  const loose = describeSession(session({}), tasks, projects)
  assert.equal(loose.name, 'Sessão avulsa')
  assert.deepEqual(loose.details, [])
  assert.deepEqual(describeSession(session({ title: 'Programação Web', projectId: 3 }), tasks, projects).details, [])
})

test('os títulos das listas têm um nome para cada tipo de sessão', () => {
  const task = { title: 'Revisar autenticação' }
  const project = { name: 'Programação Web' }
  assert.equal(listTitle(session({ title: 'Prova' }), task, project), 'Prova')
  assert.equal(listTitle(session({ taskId: 7 }), task, project), 'Foco em Revisar autenticação')
  assert.equal(listTitle(session({ projectId: 3 }), null, project), 'Foco no projeto Programação Web')
  assert.equal(listTitle(session({ title: '   ' }), null, null), 'Sessão avulsa')
  assert.equal(sessionLink(42), '/dashboard?sessao=42')
})

test('ativar uma sessão pausa localmente a que rodava no mesmo instante e não mexe em outras', () => {
  const sessions = [
    session({ id: 1, status: 'RUNNING', pausedAt: null }),
    session({ id: 2, status: 'COMPLETED', pausedAt: null, endedAt: at(3) }),
    session({ id: 3 }),
  ]
  const activated = session({ id: 3, status: 'RUNNING', pausedAt: null, lastActivatedAt: at(15) })
  const next = applyActivation(sessions, activated)
  assert.deepEqual(next.map((item) => [item.id, item.status]), [[1, 'PAUSED'], [2, 'COMPLETED'], [3, 'RUNNING']])
  assert.equal(next[0].pausedAt, at(15))
  assert.equal(sessions[0].status, 'RUNNING', 'não altera o estado anterior')
  const created = applyActivation(next, session({ id: 8, status: 'RUNNING', pausedAt: null, lastActivatedAt: at(20) }))
  assert.equal(created.length, 4)
  assert.deepEqual(created.filter((item) => item.status === 'RUNNING').map((item) => item.id), [8])
})

test('pausar ou concluir apenas substitui a sessão devolvida', () => {
  const sessions = [session({ id: 1, status: 'RUNNING', pausedAt: null }), session({ id: 2 })]
  const paused = applyActivation(sessions, session({ id: 1, pausedAt: at(30) }))
  assert.deepEqual(paused.map((item) => item.status), ['PAUSED', 'PAUSED'])
  const completed = applyActivation(paused, session({ id: 2, status: 'COMPLETED', pausedAt: null, endedAt: at(31) }))
  assert.deepEqual(completed.map((item) => item.status), ['PAUSED', 'COMPLETED'])
})

test('as sessões do projeto reúnem as diretas e as das tarefas, sem duplicatas', () => {
  const project = { id: 3 }
  const tasks = [{ id: 1, projectId: 3 }, { id: 2, projectId: 4 }]
  const sessions = [
    session({ id: 1, taskId: 1, lastActivatedAt: at(1) }),
    session({ id: 2, projectId: 3, lastActivatedAt: at(9) }),
    session({ id: 3, taskId: 2 }),
    session({ id: 4 }),
    session({ id: 5, projectId: 4 }),
    session({ id: 1, taskId: 1, lastActivatedAt: at(1) }),
  ]
  assert.deepEqual(projectSessions(project, tasks, sessions).map((item) => item.id), [2, 1])
  assert.deepEqual(projectSessions(project, [], []), [])
})
