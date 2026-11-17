import { test } from 'node:test'
import assert from 'node:assert/strict'
import { ApiError, createApiClient, normalizeApiUrl } from '../src/api/client.js'

const apiUrl = 'http://localhost:8080/api/v1///'
function clientWith(response) {
  return createApiClient({ apiUrl, getToken: () => 'test-token', fetchImpl: async () => response })
}

test('normalizes trailing slashes and rejects absent or invalid API configuration', () => {
  assert.equal(normalizeApiUrl(apiUrl), 'http://localhost:8080/api/v1')
  for (const url of [undefined, '', '/api/v1', 'https://example.com', 'ftp://example.com/api/v1', 'https://user:password@example.com/api/v1', 'https://example.com/api/v1?q=1']) {
    assert.throws(() => normalizeApiUrl(url), ApiError)
  }
})

test('sends JSON to configured API without credentials or Bearer on public calls', async () => {
  let called
  const client = createApiClient({ apiUrl, getToken: () => 'stale', fetchImpl: async (...args) => {
    called = args
    return new Response(null, { status: 201 })
  } })
  assert.equal(await client.request('/auth/register', { method: 'POST', authenticated: false, body: { password: 'test-only' } }), null)
  assert.equal(called[0], 'http://localhost:8080/api/v1/auth/register')
  assert.equal(called[1].headers.Authorization, undefined)
  assert.equal(called[1].credentials, 'omit')
  assert.equal(called[1].headers['Content-Type'], 'application/json')
  assert.deepEqual(JSON.parse(called[1].body), { password: 'test-only' })
})

test('attaches stored or explicit Bearer and parses JSON', async () => {
  const seen = []
  const client = createApiClient({ apiUrl, getToken: () => 'stored', fetchImpl: async (_url, options) => {
    seen.push(options.headers)
    return Response.json({ id: 1 })
  } })
  assert.deepEqual(await client.request('/me'), { id: 1 })
  await client.request('/me', { token: 'new' })
  assert.equal(seen[0].Authorization, 'Bearer stored')
  assert.equal(seen[1].Authorization, 'Bearer new')
  assert.equal(seen[0]['Content-Type'], undefined)
})

test('accepts empty 201, 204 and 200 without parsing JSON', async () => {
  for (const status of [201, 204, 200]) assert.equal(await clientWith(new Response(null, { status })).request('/me'), null)
})

test('preserves ProblemDetail detail, status and field errors', async () => {
  const errors = { password: ['must have at least 6 characters'] }
  const client = clientWith(Response.json({ detail: 'Invalid fields', errors }, { status: 400 }))
  await assert.rejects(client.request('/me'), (error) => error.status === 400 && error.message === 'Invalid fields' && error.errors.password[0] === errors.password[0])
})

test('handles HTML errors, malformed JSON successes and connection failures', async () => {
  await assert.rejects(clientWith(new Response('<html>gateway</html>', { status: 503 })).request('/me'), /servidor está indisponível/)
  await assert.rejects(clientWith(new Response('{oops')).request('/me'), /resposta inválida/)
  const client = createApiClient({ apiUrl, getToken: () => null, fetchImpl: async () => { throw new TypeError('Failed to fetch') } })
  await assert.rejects(client.request('/me'), (error) => error.status === 0 && /conectar/.test(error.message))
})

test('notifies authenticated 401 only and unsubscribes listeners', async () => {
  const client = createApiClient({ apiUrl, getToken: () => 'test-token', fetchImpl: async () => new Response('', { status: 401 }) })
  const seen = []
  const unsubscribe = client.onUnauthorized((token) => seen.push(token))
  await assert.rejects(client.request('/me'))
  await assert.rejects(client.request('/auth/login', { authenticated: false }))
  unsubscribe()
  await assert.rejects(client.request('/me'))
  assert.deepEqual(seen, ['test-token'])
})

test('forwards cancellation and does not report it as a connection failure', async () => {
  const controller = new AbortController()
  controller.abort()
  const client = createApiClient({ apiUrl, getToken: () => null, fetchImpl: async (_url, options) => {
    options.signal.throwIfAborted()
  } })
  await assert.rejects(client.request('/me', { signal: controller.signal }), { name: 'AbortError' })
})

test('ignores a late 401 when the transport completes after cancellation', async () => {
  let finish
  const controller = new AbortController()
  const client = createApiClient({ apiUrl, getToken: () => 'current', fetchImpl: () => new Promise((resolve) => { finish = resolve }) })
  const notifications = []
  client.onUnauthorized((token) => notifications.push(token))
  const request = client.request('/me', { signal: controller.signal })
  controller.abort()
  finish(new Response(null, { status: 401 }))
  await assert.rejects(request, { name: 'AbortError' })
  assert.deepEqual(notifications, [])
})
