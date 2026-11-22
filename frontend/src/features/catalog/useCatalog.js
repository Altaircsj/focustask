import { useCallback, useEffect, useRef, useState } from 'react'
import { catalogService, focusSessionService } from '../focusSessions/focusSessionService'

// Carrega projetos, tarefas e sessões (e, opcionalmente, um dado extra) para as telas de catálogo.
export function useCatalog(extra, extraKey) {
  const [data, setData] = useState({ status: 'loading' })
  const [attempt, setAttempt] = useState(0)
  const extraRef = useRef(extra)

  useEffect(() => { extraRef.current = extra })
  useEffect(() => {
    const controller = new AbortController()
    const { signal } = controller
    const loaders = [catalogService.projects(signal), catalogService.tasks(signal), focusSessionService.list(signal)]
    if (extraRef.current) loaders.push(extraRef.current(signal))
    Promise.all(loaders).then(([projects, tasks, sessions, other]) => {
      if (signal.aborted) return
      if (![projects, tasks, sessions].every(Array.isArray) || (extraRef.current && !other)) throw new Error('A API retornou uma resposta inválida.')
      setData({ status: 'ready', projects, tasks, sessions, extra: other })
    }).catch((error) => { if (!signal.aborted) setData({ status: 'error', code: error.status, message: error.message }) })
    return () => controller.abort()
  }, [attempt, extraKey])

  const retry = useCallback(() => {
    setData({ status: 'loading' })
    setAttempt((value) => value + 1)
  }, [])

  return { data, setData, retry }
}

export function useAction() {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const working = useRef(false)

  const run = useCallback(async (request, onDone) => {
    if (working.current) return
    working.current = true
    setBusy(true)
    setError('')
    try {
      onDone(await request())
    } catch (failure) {
      setError(failure.message)
    } finally {
      working.current = false
      setBusy(false)
    }
  }, [])

  return { busy, error, run, reset: useCallback(() => setError(''), []) }
}
