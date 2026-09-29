# FocusTask

Para Docker, PostgreSQL e publicação no Render, consulte [o guia de deploy](docs/RENDER.md).
O perfil padrão `dev` usa MySQL; o perfil `prod` usa PostgreSQL.

Backend Spring Boot 4.1.1 / Java 21, organizado em `features.user`,
`features.project`, `features.task` e `features.focusSession`.

## Estado desta etapa

Entities e repositories são package-private. Vínculos entre features usam IDs;
as APIs `*InternalApi` expõem apenas IDs, DTOs e operações necessárias.
Os quatro Application Services coordenam os casos de uso, os mappers, a
persistência e as transações. Controllers dependem somente da aplicação local.

Os 27 endpoints estão conectados à persistência. GET, PUT e PATCH retornam 200
com DTOs; POST retorna 201 com corpo e Location; DELETE retorna 204 sem corpo.
A criação de Task usa `/api/v1/projects/{projectId}/tasks`; o antigo POST sob
usuário continua removido, sem alias. Não há autenticação nem frontend React.

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

O `projectId` da URL determina a associação. `TaskApplicationService` consulta
o proprietário persistido pela API interna e cria a entidade pelo mapper.
Somente `title`, `description` e `dueDate` participam da criação; campos extras
como `id`, `projectId`, `status` e `priority` não alteram identidade, vínculo ou
defaults. Status e prioridade começam em `TODO` e `MEDIUM`.

A criação retorna `TaskResponseDTO` e Location para
`/api/v1/users/{userId}/tasks/{taskId}`, cujo GET agora recupera a tarefa.
O record interno `TaskCreationResult` fornece o proprietário ao controller para
construir essa URI; somente o DTO da tarefa compõe o corpo HTTP. Consultar o
proprietário não autentica o solicitante.

Todos os 27 endpoints mantêm os DTOs HTTP da etapa 2. `UserDTO` e `ProjectDTO`
continuam sendo contratos internos das features.

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
| Falha inesperada | 500 | `An unexpected error occurred. Please try again later.` |

O POST de Task em projeto inexistente volta a responder 404 com `Project not found`.
Recursos fora do contexto do usuário continuam retornando 404, sem revelar sua
existência em outro contexto. Pausar ou retomar uma sessão concluída responde 422.
Duplicidade e integridade usam 409; a mensagem técnica do banco não é enviada.
Erros próprios do MVC, como 405 e 415, preservam seus status e headers.

As respostas utilizam `ProblemDetail` compatível com RFC 9457 e
`application/problem+json`, com `type: about:blank`, título do status HTTP,
`status`, `detail` e `instance`. O Spring preenche `instance` com o caminho da
requisição, incluindo o contexto e sem query string, e negocia o content type.
Não há wrapper, timestamp ou DTO paralelo de erro.

Exemplo de validação (as mensagens seguem as constraints e o locale da requisição):

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "One or more fields are invalid.",
  "instance": "/api/v1/users/7",
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

Cada feature tem `CreateDTO`, `UpdateDTO`, `PatchDTO` e `ResponseDTO`. Os 12 corpos
de entrada usam `@Valid`; erros estruturais retornam HTTP 400 com ProblemDetail
e mensagens agrupadas por campo no handler centralizado. Nomes/títulos têm até 255 caracteres; email é validado
e tem até 254. IDs presentes nos DTOs devem ser positivos. `dueDate` é opcional
e pode estar no passado. Descrições continuam opcionais, sem limite adicional.

| Feature | CREATE | PUT | PATCH |
|---|---|---|---|
| User | `name`, `email` obrigatórios | Ambos obrigatórios | Ambos opcionais |
| Project | `name` obrigatório; `description` opcional | Mesmo conjunto | Ambos opcionais |
| Task | `title` obrigatório; `description`, `dueDate` opcionais | `projectId`, `title`, `status`, `priority` obrigatórios; descrição/prazo opcionais | Todos opcionais |
| FocusSession | `taskId` opcional | `status` obrigatório; `taskId` opcional | Ambos opcionais |

PATCH interpreta ausência e `null` como **não alterar**; `{}` é válido. Valores
presentes ainda devem ser válidos, inclusive nomes/títulos não vazios nem só espaços.
PUT representa substituição dos campos editáveis: `null` limpa os opcionais.
Essas diferenças são aplicadas pelos casos de uso e testadas nos mappers e na
aplicação. Nenhum DTO aplica novos defaults ou regras de negócio.

FocusSession pode nascer avulsa com `{}` no POST sob usuário. O POST sob Task,
`/api/v1/users/{userId}/tasks/{taskId}/focus-sessions`, não recebe corpo: os IDs vêm
do path. Datas, duração e estado inicial são definidos pelo domínio. PUT/PATCH
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
pacote das Entities, com acesso package-private. Criações de User/Project/Task usam
os construtores existentes; PUT aplica os campos editáveis e PATCH usa
`NullValuePropertyMappingStrategy.IGNORE`. IDs gerados e proprietário de Project
não são alterados. FocusSession possui somente conversão de saída, preservando
`Clock` e transições no domínio. Os enums HTTP ficam separados dos enums internos.

Os Application Services utilizam os mappers de entrada e saída. FocusSession
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

Execute `./mvnw spring-boot:run` (Windows: `.\mvnw.cmd spring-boot:run`).

Flyway aplica `db/migration/mysql/V1__create_domain_tables.sql` em um schema vazio previamente
criado. Hibernate apenas valida o schema (`ddl-auto=validate`). Não há criação
automática do banco, `clean`, `create-drop` ou baseline automático. Se já existir
um schema com dados, revisar sua estrutura e preparar a migração correspondente
antes de apontar a aplicação para ele; não apagar as tabelas para contornar isso.

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
- Testes MVC cobrem as 27 rotas, delegação, corpos, validação, códigos de sucesso,
  Location e tradução das exceptions para 400, 404, 409 e 422. Os testes do
  handler cobrem fallback 500 seguro, erros MVC, contexto de instance, content type
  e ausência de mensagens técnicas na resposta.
- A auditoria recursiva impede Entities nos contratos HTTP e verifica os 12 corpos
  validados. A auditoria de arquitetura verifica dependências dos controllers,
  transações na aplicação, ausência de HTTP nos services/exceções e montagem dos
  beans das features sem ciclos.
- Testes dos DTOs e mappers gerados continuam cobrindo limites, opcionais, enums,
  representação de sessões e a diferença entre PUT e PATCH.
- Testes que estendem `MySqlIntegrationTest` usam Testcontainers com `mysql:8.4`,
  aplicam a migration real e verificam consultas, FKs, exclusões e rollback,
  além do fluxo HTTP pelos 27 endpoints, commit de alterações, rollback após falha
  durante exclusões, preservação de vínculo após transição inválida e pausa concorrente.
- Sem Docker disponível, esses testes de integração são explicitamente marcados
  como **skipped**. Um build nessas condições não comprova a integração MySQL.
- Com Docker em execução, rode a suíte completa e confirme zero testes pulados
  antes de considerar a persistência validada. Nenhum teste usa seu banco de trabalho.

Não foram adicionados Spring Modulith, ArchUnit ou H2.
