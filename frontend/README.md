# FocusTask — frontend

React, JavaScript, Vite e React Router. Cadastro e login usam a API real; o painel
mostra saudação, resumo do dia, cronômetro de foco e tarefas do dia. As sessões de foco têm histórico e criação
com a API real. Os detalhes de uma tarefa (`/tasks/:taskId`) e o perfil já funcionam. Projetos e lista de tarefas estão protegidos, mas seus CRUDs ainda não
foram implementados.

## Ambiente local

Use Node compatível com o Vite instalado (verificado com Node 22.14.0) e Java 21
no backend. Na raiz do projeto, mantenha o `.env` com suas configurações MySQL e
JWT, seguindo o `.env.example` da raiz. Esse arquivo pertence exclusivamente ao
backend; não copie suas credenciais para o frontend.

No PowerShell, na raiz:

```powershell
$env:JAVA_HOME = 'C:\Users\Carol\.jdks\ms-21.0.9'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev'
```

Em outro terminal, na pasta `frontend`:

```powershell
if (!(Test-Path .env)) { Copy-Item .env.example .env }
npm ci
npm run dev
```

O `frontend/.env` deve conter:

```dotenv
VITE_API_URL=http://localhost:8080/api/v1
```

Abra `http://localhost:5173`. As chamadas são diretas à API, sem proxy do Vite.
A URL base precisa ser HTTP(S), terminar em `/api/v1` e não conter parâmetros ou
credenciais. O cliente remove barras finais. Configuração ausente ou inválida
produz uma mensagem na tela quando houver tentativa de acesso à API.

No backend, `FOCUSTASK_CORS_ALLOWED_ORIGINS` aceita origens explícitas separadas
por vírgula, por exemplo:

```dotenv
FOCUSTASK_CORS_ALLOWED_ORIGINS=http://localhost:5173,https://seu-frontend.example.com
```

Origens não têm caminho ou barra final. O padrão do perfil `dev` é
`http://localhost:5173`; `127.0.0.1:5173` ou outra porta exige adicionar a origem
correspondente. No perfil `prod`, configure a lista no ambiente do serviço.
CORS está integrado ao Spring Security antes do filtro JWT e aceita
`Content-Type`, `Authorization`, GET, POST, PUT, PATCH, DELETE e OPTIONS.
Não usa cookies nem credenciais CORS. Docker Compose encaminha essa configuração.

## Backend hospedado e build

Configure no ambiente do frontend:

```dotenv
VITE_API_URL=https://seu-back.onrender.com/api/v1
```

Configure a origem pública do frontend em `FOCUSTASK_CORS_ALLOWED_ORIGINS` no
backend. Mantenha credenciais do banco e `FOCUSTASK_JWT_SECRET` apenas no backend.
Todas as variáveis `VITE_` são públicas e entram no código enviado ao navegador.

Reinicie o servidor Vite após alterar `.env`. Para publicação, configure a URL
**antes** de `npm run build`: o valor é incorporado ao build e não muda ao alterar
variáveis apenas no servidor que hospeda os arquivos estáticos. Com BrowserRouter,
configure o host estático para retornar `index.html` nas rotas do aplicativo.

Os arquivos existentes `.env.development`, `.env.production` e `.env.remoto`
foram preservados. Arquivos específicos de modo têm prioridade sobre `.env`;
`npm run build` usa o modo `production`. Atualmente `.env.production` aponta para
o backend local: ajuste-o para publicar, ou use uma variável de ambiente com
prioridade maior. Exemplo PowerShell na pasta `frontend`:

```powershell
$env:VITE_API_URL = 'https://seu-back.onrender.com/api/v1'
npm run build
Remove-Item Env:VITE_API_URL
```

Para usar o arquivo `.env.remoto` existente durante desenvolvimento:

```powershell
node node_modules/vite/bin/vite.js --mode remoto
```

Esse comando usa o endereço já presente nesse arquivo; ele não é alterado por
este trabalho. Não houve deploy ou teste do serviço remoto.

## Sessão e organização

| Arquivo | Responsabilidade |
|---|---|
| `src/api/client.js` | URL, fetch, JSON, Bearer, respostas vazias, ProblemDetail e eventos 401 |
| `src/features/auth/authService.js` | Cadastro, login e consulta a `/me` |
| `src/features/auth/AuthContext.jsx` | Estado da sessão, login, recuperação, logout e tela de nova tentativa |
| `src/features/auth/useAuth.js` | Acesso ao contexto |
| `src/features/auth/pages/LoginPage.jsx`, `src/features/auth/pages/RegisterPage.jsx` | Formulários controlados e mensagens |
| `src/features/auth/components/AuthField.jsx` | Campo de formulário com rótulo e mensagem de erro |
| `src/features/auth/auth.css` | Estilos específicos dos formulários de autenticação |
| `src/features/auth/validation.js` | Limites dos DTOs, incluindo 72 bytes UTF-8 |
| `src/routes/AppRoutes.jsx` | Rotas públicas e grupo de páginas que exigem usuário autenticado |
| `src/features/dashboard/pages/DashboardPage.jsx` | Painel: resumo, cronômetro (25 min) e tarefas do dia |
| `src/features/dashboard/components/Modal.jsx` | Diálogo base (Esc e clique fora fecham) |
| `src/features/dashboard/components/NewFocusModal.jsx` | Modal "Nova Sessão de Foco": projeto, tarefa e duração |
| `src/features/dashboard/components/CompletedSessionModal.jsx` | Modal "Sessão concluída" ao zerar o cronômetro |
| `src/features/dashboard/dashboardService.js` | Concluir tarefa (`PATCH /tasks/{id}`) e duração preferida (localStorage) |
| `src/features/dashboard/summary.js` | Lógica pura: saudação, resumo, cronômetro e tarefas do dia |
| `src/features/dashboard/dashboard.css` | Estilos específicos do painel |
| `src/features/focusSessions/pages/FocusSessionsPage.jsx` | Histórico em `/focus-sessions`: filtros Todas/Avulsa/Tarefa, pausar, retomar, concluir e excluir |
| `src/features/focusSessions/pages/NewFocusSessionPage.jsx` | Criação em `/focus-sessions/new`: projeto e tarefa opcionais; sem tarefa a sessão é avulsa |
| `src/features/tasks/pages/TaskDetailPage.jsx` | Detalhes da tarefa em `/tasks/:taskId`: dados, histórico de sessões, iniciar sessão, editar e excluir |
| `src/features/tasks/components/EditTaskModal.jsx` | Modal "Editar Tarefa" (título, descrição, status, prioridade, prazo) |
| `src/features/tasks/taskService.js` | `GET`, `PATCH` e `DELETE` de `/tasks/{id}` |
| `src/features/tasks/format.js` | Rótulos de status e prioridade e formatação do prazo |
| `src/features/tasks/tasks.css` | Estilos específicos dos detalhes da tarefa |
| `src/features/profile/pages/ProfilePage.jsx` | Perfil em `/profile`: edita nome e e-mail, exclui a conta; senha desabilitada (a API não tem troca de senha) |
| `src/features/profile/profileService.js` | `PATCH` e `DELETE` de `/me` |
| `src/features/profile/validation.js` | Validação do formulário e mensagens de erro do perfil |
| `src/features/profile/profile.css` | Estilos específicos do perfil |
| `src/layouts/initials.js` | Iniciais do nome, usadas no menu lateral e no perfil |
| `src/features/focusSessions/focusSessionService.js`, `format.js` | Chamadas à API e cálculo/formatação da duração (descontando pausas) |
| `src/features/focusSessions/focus-sessions.css` | Estilos das duas páginas de sessões |
| `src/layouts/AppLayout.jsx`, `src/layouts/app-layout.css` | Estrutura das rotas autenticadas: menu lateral à esquerda e a página (`Outlet`) ao lado |
| `src/layouts/components/SideBar.jsx`, `src/layouts/components/sidebar.css` | Menu lateral do Figma: rotas com destaque da atual (`NavLink`), nome e e-mail do `useAuth` |

O token fica no `sessionStorage` da aba, na chave `focustask.token`. O usuário
fica em memória. Ao iniciar, `useEffect` verifica `/me` antes de liberar as rotas.
O login só armazena o token depois dessa verificação. O JWT dura 15 minutos;
qualquer 401 autenticado limpa a sessão e pede novo login. Não há refresh token
ou logout no servidor; Sair limpa a sessão local. Uma requisição que ultrapassar
15 segundos é tratada como indisponibilidade, sem apagar o token salvo.

Falhas de conexão e respostas diferentes de 401 durante a recuperação preservam
o token e exibem Tentar novamente. Os efeitos cancelam requisições e invalidam
respostas antigas no logout/desmonte, inclusive no StrictMode. Um 401 atrasado
com token de sessão anterior não remove um token novo.

## Verificação

```powershell
npm test
npm run lint
npm run build
```

Os testes usam `node:test`. Os de sessão montam o provedor e as rotas com React DOM e jsdom.
O [guia de estudo](../estudar-depois.md) compara essa organização com a anterior.
Os testes cobrem respostas vazias,
ProblemDetail, headers, conexão, validação, recuperação, logout, 401 e concorrência.
Na raiz, execute `.\mvnw.cmd test` com Java 21. Testes de integração com
Testcontainers exigem Docker e usam bancos isolados. Não direcione esses testes
ao banco local existente. Os testes do frontend não substituem os testes no
navegador; veja os resultados em [AUTH_INTEGRACAO.md](../docs/AUTH_INTEGRACAO.md).
