export function validateProfile({ name, email }) {
  const errors = {}
  if (!name.trim() || name.length > 255) errors.name = 'Informe um nome com até 255 caracteres.'
  if (!email.trim() || email.length > 254 || !/^[^\s@]+@[^\s@]+$/.test(email.trim())) errors.email = 'Informe um e-mail válido com até 254 caracteres.'
  return errors
}

export function profileErrorMessage(error) {
  if (error.status === 409) return 'Este e-mail já está em uso por outra conta.'
  if (error.status === 400) return 'Confira os campos informados.'
  if (error.status >= 500) return 'O servidor está indisponível no momento. Tente novamente.'
  return error.message || 'Não foi possível concluir a solicitação. Tente novamente.'
}
