import { test } from 'node:test'
import assert from 'node:assert/strict'
import { authErrorMessage, validateAuth } from '../src/features/auth/validation.js'

const values = { name: 'Conta de teste', email: 'test@example.com', password: 'secret12' }

test('register validates required fields, DTO limits and password minimum', () => {
  assert.deepEqual(validateAuth(values, true), {})
  assert.deepEqual(Object.keys(validateAuth({ name: '', email: '', password: '' }, true)), ['name', 'email', 'password'])
  assert.ok(validateAuth({ ...values, name: 'x'.repeat(256) }, true).name)
  assert.ok(validateAuth({ ...values, email: `${'x'.repeat(250)}@example.com` }, true).email)
  assert.ok(validateAuth({ ...values, password: '12345' }, true).password)
  assert.equal(validateAuth({ ...values, password: '12345' }).password, undefined)
})

test('password limit measures UTF-8 bytes including accents and emoji', () => {
  for (const password of ['a'.repeat(72), 'é'.repeat(36), '😀'.repeat(18)]) assert.equal(validateAuth({ ...values, password }, true).password, undefined)
  for (const password of ['a'.repeat(73), 'é'.repeat(37), '😀'.repeat(19)]) assert.match(validateAuth({ ...values, password }, true).password, /72 bytes/)
})

test('explains duplicate email, bad credentials, validation and unavailable server', () => {
  assert.match(authErrorMessage({ status: 409 }, true), /já está cadastrado/)
  assert.match(authErrorMessage({ status: 401 }), /incorretos/)
  assert.match(authErrorMessage({ status: 400 }), /campos/)
  assert.match(authErrorMessage({ status: 503 }), /indisponível/)
})
