import { useState } from 'react'

const usuario = {
  nome: 'Usuário de Teste',
  email: 'asda@exemplo.com',
  senha: '123456',
}

export default function LoginPage() {
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [mensagem, setMensagem] = useState('')

  function handleSubmit(e) {
    e.preventDefault()

    //TODO : Implementar a lógica de autenticação aqui
    if (email === usuario.email && senha === usuario.senha) {
      setMensagem(`Bem-vindo, ${usuario.nome}!`)
    } else {
      setMensagem('E-mail ou senha incorretos.')
    }
    
  }

  return (
    <main>
      <h1>Entrar no FocusTask</h1>

      <form onSubmit={handleSubmit}>
        <div>
          <label htmlFor="email">E-mail</label>
          <input
            id="email"
            type="email"
            autoComplete="username"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </div>

        <div>
          <label htmlFor="senha">Senha</label>
          <input
            id="senha"
            type="password"
            autoComplete="current-password"
            value={senha}
            onChange={(e) => setSenha(e.target.value)}
            required
          />
        </div>

        <button type="submit">Entrar</button>

        <p role="status">{mensagem}</p>
      </form>
    </main>
  )
}