// Tema da interface: "system" (segue o sistema), "light" ou "dark". A escolha fica no localStorage e o tema
// resolvido vai para <html data-theme>, que o CSS (index.css) usa para trocar as variáveis de cor.

export const THEME_KEY = 'focustask.theme'
export const THEME_CHOICES = ['system', 'light', 'dark']
const CHANGE_EVENT = 'focustask:theme'
const DARK_QUERY = '(prefers-color-scheme: dark)'

export function loadTheme() {
  try {
    const value = window.localStorage.getItem(THEME_KEY)
    return THEME_CHOICES.includes(value) ? value : 'system'
  } catch {
    return 'system'
  }
}

export function systemPrefersDark() {
  return Boolean(window.matchMedia?.(DARK_QUERY).matches)
}

export function resolveTheme(choice, prefersDark = systemPrefersDark()) {
  return choice === 'dark' || (choice !== 'light' && prefersDark) ? 'dark' : 'light'
}

export function applyTheme(choice = loadTheme()) {
  document.documentElement.dataset.theme = resolveTheme(choice)
}

export function saveTheme(choice) {
  if (!THEME_CHOICES.includes(choice)) return
  try { window.localStorage.setItem(THEME_KEY, choice) } catch { /* armazenamento indisponível: vale só nesta página */ }
  applyTheme(choice)
  window.dispatchEvent(new window.Event(CHANGE_EVENT))
}

export function subscribeTheme(callback) {
  window.addEventListener(CHANGE_EVENT, callback)
  window.addEventListener('storage', callback)
  return () => {
    window.removeEventListener(CHANGE_EVENT, callback)
    window.removeEventListener('storage', callback)
  }
}

// Aplica o tema salvo e o mantém em dia: acompanha o sistema no modo automático e outras abas.
export function initTheme() {
  applyTheme()
  const query = window.matchMedia?.(DARK_QUERY)
  const follow = () => { if (loadTheme() === 'system') applyTheme('system') }
  const sync = (event) => { if (event.key === THEME_KEY || event.key === null) applyTheme() }
  query?.addEventListener('change', follow)
  window.addEventListener('storage', sync)
  return () => {
    query?.removeEventListener('change', follow)
    window.removeEventListener('storage', sync)
  }
}
