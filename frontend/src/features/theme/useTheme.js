import { useSyncExternalStore } from 'react'
import { loadTheme, saveTheme, subscribeTheme } from './theme'

// [escolha atual, função para escolher]: 'system', 'light' ou 'dark'.
export function useTheme() {
  const choice = useSyncExternalStore(subscribeTheme, loadTheme, () => 'system')
  return [choice, saveTheme]
}
