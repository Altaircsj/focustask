# FocusTask

Backend Spring Boot 4.1.1 / Java 21, organizado em `features.user`,
`features.project`, `features.task` e `features.focusSession`.

## Estado desta etapa

Entities e repositories são package-private. Vínculos entre features usam IDs;
as APIs `*InternalApi` expõem apenas IDs, DTOs e operações necessárias.
Os quatro Application Services coordenam os casos de uso, os mappers, a
persistência e as transações. Controllers dependem somente da aplicação local.

A API possui 28 endpoints, autenticação JWT stateless e RBAC mínimo. GET, PUT e
PATCH retornam 200 com DTOs; criações de recursos retornam 201 com corpo e Location;
DELETE retorna 204 sem corpo. Registro retorna 201 vazio, e login retorna 200 com token.
As URLs antigas com `/users/{userId}` foram removidas, sem aliases. Não há frontend React.

### Responsabilidades

| Feature | Application Service | Domain Service / regras |
|---|---|---|
| User | CRUD, mapeamento e exclusão coordenada | `UserService`: unicidade do email normalizado |
| Project | CRUD, contexto do usuário e exclusão coordenada | Busca contextualizada na aplicação; sem Domain Service artificial |
| Task | CRUD, contexto e exclusão com desvinculação das sessões | `TaskService`: movimentação somente entre projetos do mesmo usuário |
| FocusSession | CRUD, bloqueio, vínculo e intenção de estado | `FocusSessionService`: tarefa do mesmo usuário; transições e tempos na entidade |

Controllers tratam HTTP e montam Location. Application Services não retornam
Entities nem conhecem HTTP. Domain Services não recebem DTOs HTTP, não salvam
Entities e participam da transação coordenada pela aplicação. Outras features
continuam acessíveis somente por suas APIs internas, sem dependências entre
Application Services.

### Criação de Task

```http
POST /api/v1/projects/42/tasks
Content-Type: application/json

{
  "title": "Revisar Spring Data JPA",
  "description": "Estudar os exemplos da disciplina",
  "dueDate": "2026-10-05"
}
```

O `projectId` da URL determina a associação. `TaskApplicationService` verifica
pela API interna se o projeto pertence ao usuário autenticado antes de criar a entidade.
Somente `title`, `description` e `dueDate` participam da criação; campos extras
como `id`, `projectId`, `status` e `priority` não alteram identidade, vínculo ou
defaults. Status e prioridade começam em `TODO` e `MEDIUM`.

A criação retorna `TaskResponseDTO` e Location para `/api/v1/tasks/{taskId}`.
Um projeto inexistente ou de outro usuário produz 404, sem inserção.
Os DTOs internos `UserDTO` e `ProjectDTO` continuam separados dos contratos HTTP.

## Autenticação e autorização

`User` é a única identidade persistida. `AuthenticatedUser` é um adapter de
Security, sem tabela. As entidades, repositories e Services comuns permanecem
encapsulados e não acessam SecurityContext. Controllers recebem o principal por
`@AuthenticationPrincipal` e passam seu ID aos Application Services.

Endpoints com prefixo `/api/v1`:

| Métodos | Caminho | Acesso |
|---|---|---|
| POST | `/auth/register` | Público; cria USER |
| POST | `/auth/login` | Público |
| GET | `/users` | ADMIN |
| GET, PUT, PATCH, DELETE | `/me` | Autenticado, própria conta |
| GET, POST | `/projects` | Autenticado |
| GET, PUT, PATCH, DELETE | `/projects/{projectId}` | Autenticado + ownership |
| GET | `/tasks` | Autenticado, próprias tarefas |
| GET, POST | `/projects/{projectId}/tasks` | Autenticado + ownership do projeto |
| GET, PUT, PATCH, DELETE | `/tasks/{taskId}` | Autenticado + ownership |
| GET, POST | `/focus-sessions` | Autenticado |
| GET, PUT, PATCH, DELETE | `/focus-sessions/{focusSessionId}` | Autenticado + ownership |
| GET, POST | `/tasks/{taskId}/focus-sessions` | Autenticado + ownership da tarefa |

Não existe GET público. ADMIN possui ROLE_ADMIN e ROLE_USER, mas não ignora
ownership. O acesso a recursos alheios produz 404. A única operação administrativa
é a listagem de usuários; não há gerenciamento de roles por HTTP.

Registro recebe `name`, `email` e `password`, retorna **201 sem corpo, Location ou
token**. Login recebe `email` e `password`, retorna `{"token":"<JWT>"}`. Os dois
DTOs rejeitam campos desconhecidos, inclusive role. Email duplicado retorna 409.
Use `Authorization: Bearer <token>` nas demais requisições.

Senha de cadastro: obrigatória, mínimo de 6 caracteres, máximo de 72 bytes UTF-8.
O limite é medido em bytes, sem truncar Unicode. BCrypt calcula o hash uma única
vez; apenas o hash é persistido. O login não impõe o mínimo de cadastro à senha
informada, mas rejeita valores vazios ou que excedam 72 bytes. DTOs e principal
não revelam credenciais em sua representação textual. Respostas de User contêm
somente id, name e email. PUT/PATCH de `/me` não alteram senha, hash, ID ou role.

JWT usa Auth0 java-jwt 4.6.1, HS256, issuer `focustask-api`, email normalizado
como subject, claim `userId`, `iat` e expiração de 15 minutos. A assinatura e os
claims obrigatórios são verificados; o filtro carrega a conta atual pelo email
e confere o ID. Authorities vêm do banco, não do payload do token.

Alterar email exige novo login; o token anterior é recusado enquanto seu subject
não corresponder à conta. Reatribuir esse email a outra conta não valida o token
antigo, graças à conferência do ID. Restaurar o email na mesma conta antes da
expiração pode revalidar esse token: não há blacklist ou versão de revogação.
Excluir a conta impede o uso posterior de seus tokens. Mudanças de role no banco
valem na próxima requisição. Não há sessão HTTP, refresh token ou endpoint de logout.

`SecurityFilter` é registrado somente na cadeia Spring Security, antes de
`UsernamePasswordAuthenticationFilter`. Registro/login não exigem Bearer e ignoram
um header de token antigo. HTTP Basic, form login, request cache e CSRF estão
desabilitados; a autenticação depende exclusivamente do Bearer enviado pelo cliente.

### Primeiro ADMIN em desenvolvimento

Registre uma conta normalmente e identifique seu ID. Em um cliente SQL conectado
ao banco de desenvolvimento correto, promova **somente essa conta**:

```sql
UPDATE users SET role = 'ADMIN' WHERE id = <ID_CONFERIDO> AND role = 'USER';
```

Confirme uma linha alterada e faça novo login para o teste manual. Não há admin
automático, senha conhecida, seed de credenciais ou endpoint público de promoção.

### Exceções e tratamento HTTP

As exceções de negócio não dependem de HTTP:

```text
NegocioException (abstrata, shared.exception)
├── EntidadeNaoEncontradaException (concreta, shared.exception)
├── OperacaoInvalidaException (concreta, shared.exception)
└── EmailAlreadyExistsException (features.user)
```

São quatro classes. As categorias compartilhadas ficam em `shared.exception`;
`EntidadeNaoEncontradaException` e `OperacaoInvalidaException` recebem mensagens
pelo construtor público. A única exception específica, `EmailAlreadyExistsException`,
permanece diretamente em `features.user`, permitindo distinguir a duplicidade no
handler centralizado. Não há necessidade de subpacotes de exceptions nas features.

Recurso inexistente ou indisponível no contexto informado gera
`EntidadeNaoEncontradaException` com a identificação correta: "User not found",
"Project not found", "Task not found" ou "Focus session not found". A validação
permanece nos mesmos pontos dos services e APIs internas. Na entidade FocusSession,
pausar ou retomar uma sessão concluída gera `OperacaoInvalidaException`, com
"A completed session cannot be paused" ou "A completed session cannot be resumed".
As demais invariantes preservam as exceções padrão do Java já utilizadas.

`GlobalExceptionHandler`, em `shared.exception`, concentra a tradução HTTP com
`@RestControllerAdvice` e estende `ResponseEntityExceptionHandler`. As quatro
exceptions permanecem independentes de HTTP. Controllers não possuem try/catch
para regras de negócio; services e APIs internas não constroem respostas HTTP.

| Falha | HTTP | Detail |
|---|---|---|
| `EntidadeNaoEncontradaException` | 404 | Mensagem que identifica o recurso |
| `MethodArgumentNotValidException` | 400 | `One or more fields are invalid.` e mapa `errors` |
| Corpo ausente, JSON malformado, tipo/enum/data inválido | 400 | Mensagem pública sobre corpo inválido |
| Parâmetro com tipo incompatível | 400 | Mensagem pública sobre parâmetro inválido |
| `EmailAlreadyExistsException` | 409 | `Email already registered` |
| `DataIntegrityViolationException` | 409 | `The operation conflicts with existing data.` |
| `OperacaoInvalidaException` | 422 | Mensagem da operação rejeitada |
| Outra `NegocioException` | 400 | Mensagem de negócio |
| Credenciais/token ausentes, inválidos ou expirados | 401 | `Authentication is required or credentials are invalid.` |
| Papel insuficiente | 403 | `Access is denied.` |
| Falha inesperada | 500 | `An unexpected error occurred. Please try again later.` |

O POST de Task em projeto inexistente volta a responder 404 com `Project not found`.
Recursos fora do contexto do usuário continuam retornando 404, sem revelar sua
existência em outro contexto. Pausar ou retomar uma sessão concluída responde 422.
Duplicidade e integridade usam 409; a mensagem técnica do banco não é enviada.
Erros próprios do MVC, como 405 e 415, preservam seus status e headers.

As respostas utilizam `ProblemDetail` compatível com RFC 9457 e
`application/problem+json`, com `type: about:blank`, título do status HTTP,
`status`, `detail` e `instance`. No MVC, o Spring preenche `instance` com o caminho da requisição, incluindo o
contexto e sem query string, e negocia o content type. Na cadeia de filtros,
`SecurityProblemHandler` implementa AuthenticationEntryPoint e AccessDeniedHandler,
serializando o mesmo ProblemDetail com o Jackson da aplicação. Respostas 401
incluem `WWW-Authenticate: Bearer`. Falhas técnicas de autenticação produzem 500
seguro, não 401; credenciais incorretas e email inexistente compartilham o mesmo erro.
Não há wrapper, timestamp ou DTO paralelo de erro.

Exemplo de validação (as mensagens seguem as constraints e o locale da requisição):

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "One or more fields are invalid.",
  "instance": "/api/v1/me",
  "errors": {
    "name": ["size must be between 1 and 255", "must not be blank"]
  }
}
```

`errors` é um mapa de listas de strings: todas as mensagens de cada campo são
preservadas, sem valores rejeitados, códigos internos ou objetos do BindingResult.
A ordem das mensagens não faz parte do contrato. As mensagens de domínio atuais
são controladas e seguras; novas mensagens de negócio devem preservar esse cuidado.

Falhas inesperadas são registradas com stack trace somente no log do servidor.
A resposta pública não expõe SQL, constraints, classes Java ou detalhes internos.
As propriedades `spring.web.error.include-stacktrace=never`,
`include-message=never`, `include-binding-errors=never` e `include-exception=false`
protegem também o fallback padrão do Spring Boot. Não é necessário ativar um
segundo advice por `spring.mvc.problemdetails.enabled`.

### Contratos de entrada e validação

Project, Task e FocusSession têm `CreateDTO`, `UpdateDTO`, `PatchDTO` e `ResponseDTO`.
User usa RegisterRequestDTO/LoginRequestDTO e os DTOs atuais de perfil. Os 13 corpos
de entrada usam `@Valid`; erros estruturais retornam HTTP 400 com ProblemDetail
e mensagens agrupadas por campo no handler centralizado. Nomes/títulos têm até 255 caracteres; email é validado
e tem até 254. IDs presentes nos DTOs devem ser positivos. `dueDate` é opcional
e pode estar no passado. Descrições continuam opcionais, sem limite adicional.

| Feature | CREATE | PUT | PATCH |
|---|---|---|---|
| User | Registro: `name`, `email`, `password` obrigatórios | Ambos obrigatórios | Ambos opcionais |
| Project | `name` obrigatório; `description` opcional | Mesmo conjunto | Ambos opcionais |
| Task | `title` obrigatório; `description`, `dueDate` opcionais | `projectId`, `title`, `status`, `priority` obrigatórios; descrição/prazo opcionais | Todos opcionais |
| FocusSession | `taskId` opcional | `status` obrigatório; `taskId` opcional | Ambos opcionais |

PATCH interpreta ausência e `null` como **não alterar**; `{}` é válido. Valores
presentes ainda devem ser válidos, inclusive nomes/títulos não vazios nem só espaços.
PUT representa substituição dos campos editáveis: `null` limpa os opcionais.
Essas diferenças são aplicadas pelos casos de uso e testadas nos mappers e na
aplicação. Nenhum DTO aplica novos defaults ou regras de negócio.

FocusSession pode nascer avulsa com `{}` em POST `/api/v1/focus-sessions`. O POST
`/api/v1/tasks/{taskId}/focus-sessions` não recebe corpo: taskId vem do path e
o usuário vem do principal. Datas, duração e estado inicial são definidos pelo domínio. PUT/PATCH
recebem o estado solicitado; a aplicação chama `pause`, `resume` ou `complete`
na entidade usando o Clock do servidor. O mapper não altera status nem calcula tempo.
PUT com `taskId = null` desvincula; PATCH com null mantém o vínculo. Vínculo e
estado são processados juntos sob bloqueio pessimista e na mesma transação.

As respostas usam apenas campos escalares e enums HTTP: User (`id`, `name`, `email`),
Project (`id`, `userId`, `name`, `description`), Task (`id`, `projectId`, `title`,
`description`, `status`, `priority`, `dueDate`) e FocusSession (`id`, `userId`,
`taskId`, `status`, `startedAt`, `endedAt`, `pausedAt`, `totalPausedSeconds`).

### Mapeamento

MapStruct 1.6.3 gera os quatro mappers Spring em `target/generated-sources/annotations`
durante a compilação com Java 21. Não versionar `*MapperImpl`. Os mappers ficam no
pacote das Entities, com acesso package-private. Criações de Project/Task usam
os construtores existentes; PUT aplica os campos editáveis e PATCH usa
`NullValuePropertyMappingStrategy.IGNORE`. IDs gerados e proprietário de Project
não são alterados. FocusSession possui somente conversão de saída, preservando
`Clock` e transições no domínio. Os enums HTTP ficam separados dos enums internos.

O registro constrói User explicitamente após BCrypt. Os demais Application
Services utilizam os mappers de entrada e saída. FocusSession
continua sendo construída explicitamente com Clock e possui somente mapeamento
de saída. DTOs/mappers não verificam duplicidade ou propriedade nem persistem.
Na atualização de User, a aplicação valida uma entidade candidata não gerenciada
antes de alterar a entidade persistida, evitando flush antecipado durante a
consulta de unicidade.

### Transações e ciclo de FocusSession

Métodos públicos de leitura da aplicação usam `@Transactional(readOnly = true)`;
criação, PUT, PATCH e exclusão usam `@Transactional`. Atualizações das entidades
gerenciadas persistem por dirty checking. As APIs bulk preservam
`Propagation.MANDATORY` e participam da transação do coordenador.

| Estado atual | RUNNING solicitado | PAUSED solicitado | COMPLETED solicitado |
|---|---|---|---|
| RUNNING | Sem alteração | Inicia pausa | Conclui |
| PAUSED | Encerra pausa e retoma | Sem alteração | Encerra pausa e conclui |
| COMPLETED | Operação inválida | Operação inválida | Sem alteração |

`startedAt` permanece imutável. `pausedAt` registra uma pausa aberta; retomar ou
concluir soma os segundos inteiros desse intervalo a `totalPausedSeconds` e
limpa `pausedAt`. A primeira conclusão define `endedAt`; repetições não alteram
os tempos. Instantes usam UTC e precisão de microssegundos. Sessões concluídas
podem ser vinculadas/desvinculadas sem reabrir seu histórico. Nenhum campo temporal
pode ser editado por DTO de entrada.

## Banco e execução

Requer JDK 21 e MySQL 8.4. Configure `JAVA_HOME` para o JDK antes de usar o Maven
Wrapper. A aplicação exige estas variáveis (não versionar credenciais):

- `FOCUSTASK_DB_URL`: URL JDBC, por exemplo `jdbc:mysql://localhost:3306/focustask`.
- `FOCUSTASK_DB_USERNAME`: usuário do banco.
- `FOCUSTASK_DB_PASSWORD`: senha do banco.
- `FOCUSTASK_JWT_SECRET`: secret aleatório com pelo menos 32 bytes UTF-8, sem valor padrão.

No IntelliJ: Run → Edit Configurations → configuração local do FocusTask →
Environment variables. Preencha as quatro variáveis; não compartilhe a configuração
com valores reais no Git. A aplicação falha na inicialização se o secret faltar ou
for fraco, sem revelar seu valor. Os testes usam configuração própria de teste.

Execute `./mvnw spring-boot:run` (Windows: `.\mvnw.cmd spring-boot:run`).

Flyway aplica V1 e `V2__add_user_authentication.sql`. A V2 acrescenta password_hash
obrigatório e role USER/ADMIN em VARCHAR. V1 permanece intacta. **A transição da
etapa 5 pressupõe a tabela users vazia**: foi escolhida a recriação manual do banco
descartável de desenvolvimento e novo cadastro pelo endpoint de registro.
Não é uma estratégia de migração de contas de produção sem senha. Antes de aplicar
a V2, prepare explicitamente um schema de desenvolvimento vazio; nunca aponte esse
procedimento a dados que devam ser preservados. A migration não apaga dados nem
gera senhas legadas. A aplicação não executa reset, clean, create-drop ou baseline
automático; Hibernate apenas valida (`ddl-auto=validate`).

Os instantes são tratados em UTC. Enums usam nomes em VARCHAR, com validações no
schema. Os IDs escalares não criam FKs pelo JPA: as quatro FKs são definidas
explicitamente pela migration, todas com `ON DELETE RESTRICT`.

## Regras entre features

- Controllers usam apenas o Application Service local; outras features usam APIs públicas.
- Project e FocusSession têm proprietário imutável nas operações comuns.
- Task pode mudar de projeto apenas dentro do mesmo usuário.
- FocusSession pode nascer sem Task e ser vinculada/desvinculada depois, inclusive
  após a conclusão. O service verifica o proprietário da Task.
- Mudanças de estado da sessão usam bloqueio pessimista para serializar alterações.
- As implementações de APIs internas não chamam de volta os services coordenadores;
  isso evita dependências circulares.

Exclusões, em uma única transação:

1. Task: desvincular sessões, depois apagar a Task.
2. Project: obter IDs das Tasks, desvincular sessões, apagar Tasks, apagar Project.
3. User: apagar suas sessões (inclusive avulsas), Tasks, Projects e então User.

As operações bulk exigem uma transação existente e limpam o contexto JPA. Depois
delas, os coordenadores continuam por IDs, sem salvar instâncias desatualizadas.
As FKs impedem exclusões diretas fora de ordem. A verificação de proprietário é
responsabilidade dos services, não de uma FK isolada.

## Testes

Execute `./mvnw clean test` (Windows: `.\mvnw.cmd clean test`) com Java 21.
Se o Wrapper não inicializar, execute `mvn clean test` com um Maven instalado
e registre essa alternativa ao informar os resultados.

- Testes de aplicação cobrem unicidade antes de mutação, contexto de recursos,
  movimentação, PUT/PATCH e ordem das exclusões. Os testes de domínio preservam
  invariantes e a matriz de transições da sessão.
- Testes MVC cobrem as 28 rotas, delegação, corpos, validação, códigos de sucesso,
  Location e tradução das exceptions para 400, 404, 409 e 422. Os testes do
  handler cobrem fallback 500 seguro, erros MVC, contexto de instance, content type
  e ausência de mensagens técnicas na resposta.
- A auditoria recursiva impede Entities nos contratos HTTP e verifica os 13 corpos
  validados. A auditoria de arquitetura verifica dependências dos controllers,
  transações na aplicação, ausência de HTTP nos services/exceções e montagem dos
  beans das features sem ciclos.
- Testes dos DTOs e mappers gerados continuam cobrindo limites, opcionais, enums,
  representação de sessões e a diferença entre PUT e PATCH.
- Testes que estendem `MySqlIntegrationTest` usam Testcontainers com `mysql:8.4`,
  aplicam as migrations reais e verificam consultas, FKs, exclusões e rollback,
  além do fluxo HTTP de autenticação e CRUD, commit de alterações, rollback após falha
  durante exclusões, preservação de vínculo após transição inválida e pausa concorrente.
- Sem Docker disponível, esses testes de integração são explicitamente marcados
  como **skipped**. Um build nessas condições não comprova a integração MySQL.
- Com Docker em execução, rode a suíte completa e confirme zero testes pulados
  antes de considerar a persistência validada. Nenhum teste usa seu banco de trabalho.

- Testes de Security usam a cadeia real: BCrypt, login, JWT inválido/expirado,
  RBAC, atualização de email/role, exclusão de conta, estado stateless e ausência de
  credenciais nas respostas. Testes de contrato fornecem explicitamente um principal
  tipado; os testes de JWT/login validam autenticação real com repositories simulados.
- Testes de ownership com Services reais verificam que ADMIN também não consegue
  ler, mudar, apagar ou vincular recursos privados de outro usuário.

### Smoke test no Insomnia

1. POST `/api/v1/auth/register`; confirmar 201 vazio.
2. POST `/api/v1/auth/login`; copiar token.
3. Configurar Authorization Bearer; consultar `/api/v1/me`.
4. Criar Project, Task e FocusSession; conferir os Locations e GETs.
5. Registrar uma segunda conta e tentar acessar/modificar recursos da primeira: 404.
6. GET `/api/v1/users` como USER: 403.
7. Promover uma conta de teste pelo procedimento SQL acima; novo login; GET `/users`: 200.
8. Confirmar que ADMIN ainda recebe 404 para recursos alheios.
9. Alterar email em `/me`; confirmar 401 com token antigo e novo login com email novo.
10. DELETE `/me`; confirmar exclusão coordenada e rejeição do token anterior.

Não há arquivo/workspace Insomnia versionado. Fora do escopo: login social, OAuth2,
refresh token, blacklist, MFA, recuperação de senha, gerenciamento administrativo
completo, endpoint de mudança de senha/role e frontend. Não foram adicionados
Spring Modulith, ArchUnit ou H2.

## Docker e Render

Veja [deploy com JWT](docs/RENDER.md) e [verificação](docs/VERIFICACAO.md).
