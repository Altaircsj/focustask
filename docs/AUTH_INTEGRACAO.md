# Verificação da integração de autenticação

Verificado em 16/11/2026, no ambiente local, com Java 21.0.9, Maven 3.9.16,
Node 22.14.0, Vite 8.3.8 e MySQL 8.4.11. Backend em
`http://localhost:8080` e frontend em `http://localhost:5173`.

## Implementação

- Cliente HTTP central com `VITE_API_URL`, normalização da URL base `/api/v1`,
  JSON, Bearer, respostas vazias 201/204, ProblemDetail e cancelamento.
- Cadastro com nome, email e senha, validação de 6 caracteres e 72 bytes UTF-8,
  erros por campo, email duplicado e redirecionamento com confirmação.
- Login real, validação em `/me` antes de persistir o token no `sessionStorage`,
  recuperação ao iniciar e painel com nome e email reais.
- Rotas internas protegidas, redirecionamento inicial e logout local.
- 401 autenticado encerra a sessão; conexão/5xx preservam o token durante
  recuperação e permitem tentar novamente. Respostas atrasadas são descartadas.
- CORS no Spring Security com lista explícita configurável. Autenticação JWT,
  permissões e ownership foram preservados.
- Wrapper Maven corrigido para não indexar `Target` quando nulo.
- Exemplos de ambiente e guias atualizados para MySQL local, Docker/PostgreSQL
  e configuração futura de backend hospedado.

## Resultados

| Verificação | Resultado |
|---|---|
| `npm run build` | Passou |
| `npm run lint` | Passou, sem avisos |
| `npm test` | 22 testes passaram; sem novas dependências |
| `mvnw.cmd test` com Java 21 | 230 testes encontrados; 201 executados com sucesso, 29 ignorados; zero falhas/erros |
| CORS no MockMvc | Origens local/hospedada de teste, preflight antes do JWT, métodos, headers, origem desconhecida e respostas 200/401 passaram |
| Testes HTTP contra MySQL local | Login/me, email duplicado, senha incorreta, token inválido, validação, UTF-8 e CORS passaram |

Os 29 testes ignorados dependem de Testcontainers: não havia Docker disponível.
Os testes automatizados de banco não foram redirecionados ao banco local.

O terminal inicialmente usava Java 8. Para a verificação foi selecionado
`C:\Users\Carol\.jdks\ms-21.0.9`, sem alterar a configuração global da máquina.
O wrapper apresentou o erro `Target[0]` antes da correção e iniciou normalmente
depois dela. O executor também exigiu acesso ao cache Maven da usuária;
superada essa restrição de acesso, o build/testes passaram. As tentativas
anteriores de compilação não foram tratadas como falhas do código.

## Integração real no navegador

| Caso | Evidência observada |
|---|---|
| Cadastro persistido | Formulário encaminhou ao login com confirmação; mesma conta continuou acessível após reiniciar o backend |
| Email duplicado | Mensagem amigável no formulário e HTTP 409 |
| Senha incorreta | Mensagem de credenciais incorretas e HTTP 401 |
| Login correto | Painel exibiu nome/email reais; login e `/me` retornaram 200 |
| Atualização | Recarregar `/dashboard` preservou a sessão e recuperou `/me` |
| Rota protegida sem sessão | Acesso direto a `/dashboard` e `/profile` encaminhou para `/login` |
| Logout | Sair encaminhou ao login; acesso posterior ao perfil continuou bloqueado |
| Token inválido/expirado | Token inválido deu 401 na API real; expiração/adulteração passaram no MockMvc, e limpeza da sessão passou nos testes do frontend |
| Backend indisponível | API foi interrompida e a página recarregada; exibiu Tentar novamente; após reinício, recuperou o painel sem novo login |
| CORS | Cadastro, login e `/me` funcionaram entre as portas 5173/8080; preflight real passou para GET/POST/PUT/PATCH/DELETE e rejeitou origem desconhecida com 403 |
| Validação | Campos vazios e senha de 74 bytes (`é` repetido 37 vezes) foram rejeitados na interface; a API confirmou 400 com `errors` por campo |

Foi criada somente a conta identificada como teste
`teste.auth.20261116.2135@example.com`, com nome `Conta de teste integração 20261116`.
Ela foi mantida no banco para identificação e revisão. Nenhum dado existente foi
apagado, o banco não foi recriado e as migrations existentes foram preservadas.

## Limites e próximos ambientes

- Não se aguardaram 15 minutos para expiração natural no navegador. O cenário
  de expiração foi coberto pelo backend e pela lógica de sessão com respostas simuladas.
- Concorrência, limpeza de efeitos e respostas após logout foram verificadas
  por testes automatizados; o navegador foi usado com o StrictMode existente.
- Os CRUDs de projetos, tarefas, sessões e perfil seguem pendentes por escopo.
- Docker/PostgreSQL e serviço hospedado não foram testados nesta etapa.
- `.env` da raiz e arquivos de modo do frontend foram preservados. O novo
  `frontend/.env` contém somente a URL pública da API. Arquivos locais continuam
  ignorados pelo Git; os exemplos não contêm credenciais reais.
- `.env.production` existente ainda aponta para localhost: configure a URL
  hospedada antes de publicar, seguindo [o guia do frontend](../frontend/README.md).
- Não houve commit, push ou deploy.

O CORS segue a integração documentada pelo
[Spring Security](https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html).
A recuperação e sua limpeza respeitam o ciclo extra de desenvolvimento descrito
na documentação do [React useEffect](https://react.dev/reference/react/useEffect).
