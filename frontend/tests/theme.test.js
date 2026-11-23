import { afterEach, beforeEach, test } from 'node:test'
import assert from 'node:assert/strict'
import { JSDOM } from 'jsdom'
import { THEME_KEY, applyTheme, initTheme, loadTheme, resolveTheme, saveTheme, subscribeTheme } from '../src/features/theme/theme.js'

let dom
let system

// Simula o sistema operacional: `matches` e o evento "change" de prefers-color-scheme.
function setup(prefersDark = false) {
  dom = new JSDOM('', { url: 'http://frontend.test/' })
  globalThis.window = dom.window
  globalThis.document = dom.window.document
  const listeners = new Set()
  const query = { matches: prefersDark, addEventListener: (_, listener) => listeners.add(listener), removeEventListener: (_, listener) => listeners.delete(listener) }
  dom.window.matchMedia = () => query
  system = { listeners, set(value) { query.matches = value; listeners.forEach((listener) => listener({ matches: value })) } }
}

beforeEach(() => setup())
afterEach(() => {
  dom.window.close()
  delete globalThis.window
  delete globalThis.document
})

const theme = () => document.documentElement.dataset.theme

test('o tema resolvido segue a escolha e, no automático, o sistema', () => {
  assert.equal(resolveTheme('light', true), 'light')
  assert.equal(resolveTheme('dark', false), 'dark')
  assert.equal(resolveTheme('system', true), 'dark')
  assert.equal(resolveTheme('system', false), 'light')
  assert.equal(resolveTheme('qualquer', true), 'dark', 'valor desconhecido se comporta como automático')
})

test('sem escolha salva, ou com valor inválido ou armazenamento indisponível, vale o automático', () => {
  assert.equal(loadTheme(), 'system')
  dom.window.localStorage.setItem(THEME_KEY, 'roxo')
  assert.equal(loadTheme(), 'system')
  dom.window.localStorage.setItem(THEME_KEY, 'dark')
  assert.equal(loadTheme(), 'dark')
  Object.defineProperty(dom.window, 'localStorage', { get() { throw new Error('bloqueado') } })
  assert.equal(loadTheme(), 'system')
  assert.doesNotThrow(() => saveTheme('light'))
  assert.equal(theme(), 'light', 'sem armazenamento a escolha ainda vale nesta página')
})

test('escolher um tema o salva, aplica em <html> e avisa quem observa', () => {
  let notified = 0
  const unsubscribe = subscribeTheme(() => { notified += 1 })
  saveTheme('dark')
  assert.equal(theme(), 'dark')
  assert.equal(dom.window.localStorage.getItem(THEME_KEY), 'dark')
  assert.equal(loadTheme(), 'dark')
  saveTheme('light')
  assert.equal(theme(), 'light')
  assert.equal(notified, 2)
  saveTheme('invalido')
  assert.equal(theme(), 'light')
  assert.equal(notified, 2)
  unsubscribe()
  saveTheme('system')
  assert.equal(notified, 2)
})

test('ao iniciar, aplica o tema salvo; no automático acompanha o sistema ao vivo', () => {
  setup(true)
  const stop = initTheme()
  assert.equal(theme(), 'dark', 'automático com sistema escuro')
  system.set(false)
  assert.equal(theme(), 'light')
  system.set(true)
  assert.equal(theme(), 'dark')
  stop()
  assert.equal(system.listeners.size, 0, 'remove o ouvinte ao encerrar')
})

test('uma escolha fixa ignora as mudanças do sistema', () => {
  dom.window.localStorage.setItem(THEME_KEY, 'light')
  const stop = initTheme()
  assert.equal(theme(), 'light')
  system.set(true)
  assert.equal(theme(), 'light')
  saveTheme('system')
  assert.equal(theme(), 'dark', 'voltar ao automático passa a seguir o sistema')
  stop()
})

test('uma mudança feita em outra aba é aplicada', () => {
  const stop = initTheme()
  dom.window.localStorage.setItem(THEME_KEY, 'dark')
  dom.window.dispatchEvent(new dom.window.StorageEvent('storage', { key: THEME_KEY, newValue: 'dark' }))
  assert.equal(theme(), 'dark')
  dom.window.dispatchEvent(new dom.window.StorageEvent('storage', { key: 'outra-chave' }))
  assert.equal(theme(), 'dark')
  stop()
})

test('sem matchMedia (navegador antigo) o automático cai no claro', () => {
  delete dom.window.matchMedia
  applyTheme('system')
  assert.equal(theme(), 'light')
  assert.doesNotThrow(() => initTheme()())
})
