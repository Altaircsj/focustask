import { test } from 'node:test'
import assert from 'node:assert/strict'
import { createAuthSession } from '../src/features/auth/authSession.js'
import { ApiError } from '../src/api/client.js'

const user = { id: 1, name: 'Conta de teste', email: 'test@example.com' }
function storageWith(token = null) {
  return { get: () => token, set: (value) => { token = value }, remove: () => { token = null } }
}
function deferred() {
  let resolve, reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}

test('starts checking, then becomes anonymous without requesting /me when there is no token', async () => {
  const session = createAuthSession({ storage: storageWith(), service: { me: () => assert.fail('Unexpected /me') } })
  assert.equal(session.getSnapshot().status, 'checking')
  await session.restore()
  assert.equal(session.getSnapshot().status, 'anonymous')
})

test('validates /me before storing token and exposing user', async () => {
  const result = deferred()
  const storage = storageWith()
  const session = createAuthSession({ storage, service: { login: async () => 'test-token', me: () => result.promise } })
  const login = session.login({ email: user.email, password: 'test-only' })
  await Promise.resolve()
  assert.equal(storage.get(), null)
  assert.equal(session.getSnapshot().user, null)
  result.resolve(user)
  assert.equal(await login, true)
  assert.equal(storage.get(), 'test-token')
  assert.deepEqual(session.getSnapshot().user, user)
})

test('rejects login if profile fails, without persisting candidate token', async () => {
  const storage = storageWith()
  const session = createAuthSession({ storage, service: { login: async () => 'candidate', me: async () => { throw new ApiError('invalid', { status: 401 }) } } })
  await assert.rejects(session.login({}), { status: 401 })
  assert.equal(storage.get(), null)
  assert.equal(session.getSnapshot().user, null)
})

test('restores persisted session after refresh', async () => {
  const session = createAuthSession({ storage: storageWith('test-token'), service: { me: async (token) => { assert.equal(token, 'test-token'); return user } } })
  await session.restore()
  assert.equal(session.getSnapshot().status, 'authenticated')
  assert.deepEqual(session.getSnapshot().user, user)
})

test('clears invalid or expired tokens and requests a new login', async () => {
  const storage = storageWith('expired')
  const session = createAuthSession({ storage, service: { me: async () => { throw new ApiError('Unauthorized', { status: 401 }) } } })
  await session.restore()
  assert.equal(storage.get(), null)
  assert.equal(session.getSnapshot().status, 'anonymous')
  assert.match(session.getSnapshot().notice, /Entre novamente/)
})

test('keeps token on network/server failure and retries successfully', async () => {
  for (const status of [0, 503]) {
    let unavailable = true
    const storage = storageWith('valid')
    const session = createAuthSession({ storage, service: { me: async () => {
      if (unavailable) throw new ApiError('API indisponível', { status })
      return user
    } } })
    await session.restore()
    assert.equal(session.getSnapshot().status, 'error')
    assert.equal(storage.get(), 'valid')
    unavailable = false
    await session.restore()
    assert.deepEqual(session.getSnapshot().user, user)
  }
})

test('logout prevents a late restore response from restoring the session', async () => {
  const response = deferred()
  const storage = storageWith('valid')
  const session = createAuthSession({ storage, service: { me: () => response.promise } })
  const restore = session.restore()
  session.logout()
  response.resolve(user)
  await restore
  assert.equal(storage.get(), null)
  assert.equal(session.getSnapshot().user, null)
})

test('logout cancels a login even when /me ignores abort', async () => {
  const response = deferred()
  const storage = storageWith()
  const session = createAuthSession({ storage, service: { login: async () => 'candidate', me: () => response.promise } })
  const login = session.login({})
  await Promise.resolve()
  session.logout()
  response.resolve(user)
  assert.equal(await login, false)
  assert.equal(storage.get(), null)
})

test('effect cleanup and restart ignore stale success and failure in StrictMode', async () => {
  for (const fails of [true, false]) {
    const first = deferred()
    const storage = storageWith('valid')
    let count = 0
    const session = createAuthSession({ storage, service: { me: async () => ++count === 1 ? first.promise : user } })
    const old = session.restore()
    session.cancelPending()
    await session.restore()
    if (fails) first.reject(new ApiError('old failure', { status: 401 }))
    else first.resolve({ ...user, name: 'Stale' })
    await old
    assert.deepEqual(session.getSnapshot().user, user)
    assert.equal(storage.get(), 'valid')
  }
})

test('authenticated 401 clears current session but delayed 401 for an old token does not', async () => {
  const storage = storageWith('new-token')
  const session = createAuthSession({ storage, service: { me: async () => user } })
  await session.restore()
  session.invalidate('old-token')
  assert.equal(storage.get(), 'new-token')
  session.invalidate('new-token')
  assert.equal(storage.get(), null)
  assert.equal(session.getSnapshot().user, null)
})
