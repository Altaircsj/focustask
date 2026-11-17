export function validateAuth(values, register = false) {
  const errors = {}
  if (register && (!values.name.trim() || values.name.length > 255)) errors.name = 'Informe um nome com até 255 caracteres.'
  if (!values.email.trim() || values.email.length > 254 || !/^[^\s@]+@[^\s@]+$/.test(values.email)) errors.email = 'Informe um e-mail válido com até 254 caracteres.'
  if (!values.password.trim()) errors.password = 'Informe sua senha.'
  else if (register && values.password.length < 6) errors.password = 'A senha deve ter pelo menos 6 caracteres.'
  else if (new TextEncoder().encode(values.password).length > 72) errors.password = 'A senha deve ter no máximo 72 bytes UTF-8. Acentos e emojis podem ocupar mais de um byte.'
  return errors
}

export function authErrorMessage(error, register = false) {
  if (register && error.status === 409) return 'Este e-mail já está cadastrado. Entre ou use outro e-mail.'
  if (!register && error.status === 401) return 'E-mail ou senha incorretos.'
  if (error.status === 400) return 'Confira os campos informados.'
  if (error.status >= 500) return 'O servidor está indisponível no momento. Tente novamente.'
  return error.message || 'Não foi possível concluir a solicitação. Tente novamente.'
}
