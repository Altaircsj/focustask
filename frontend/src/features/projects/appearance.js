export const PROJECT_COLORS = [
  ['Índigo', '#4D49FC'],
  ['Âmbar', '#F59E0B'],
  ['Verde', '#10B981'],
  ['Vermelho', '#EF4444'],
  ['Roxo', '#A855F7'],
  ['Ciano', '#06B6D4'],
  ['Cinza-claro', '#E2E8F0'],
]

export const DEFAULT_COLOR = PROJECT_COLORS[0][1]
export const TAG_SUGGESTIONS = ['Sprint Ativa', 'UI/UX', 'Development']

export function tagError(tags, tag) {
  if (!tag || [...tag].length > 30) return 'Use de 1 a 30 caracteres para a tag.'
  if (tags.some((item) => item.toLowerCase() === tag.toLowerCase())) return 'Esta tag já foi adicionada.'
  if (tags.length >= 10) return 'Cada projeto pode ter até 10 tags.'
  return ''
}
