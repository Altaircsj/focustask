# FocusTask

Backend Spring Boot 4.1.1 / Java 21, organizado em `features.user`,
`features.project`, `features.task` e `features.focusSession`.

## Estado desta etapa

Entities e repositories são package-private. Vínculos entre features usam IDs;
as APIs `*InternalApi` expõem apenas IDs, DTOs e operações necessárias.
Os services implementam a persistência e as exclusões transacionais.

O POST de Task está funcional em `/api/v1/projects/{projectId}/tasks`, substituindo
`POST /api/v1/users/{userId}/tasks` (sem alias). Os demais endpoints ainda são
esboços. Permanecem 27 rotas distintas, incluindo GET de usuários e PATCH de
User/Project. Não há autenticação nem frontend React neste checkout.

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

O `projectId` da URL determina a associação. O controller consulta o proprietário
do projeto pela API interna e reutiliza `TaskService.create`. Somente `title`,
`description` e `dueDate` do corpo são encaminhados; `id` e `projectId` enviados
no corpo não determinam a identidade nem a associação da nova Task. Status e
prioridade começam em `TODO` e `MEDIUM`, conforme o service existente.

Projeto inexistente retorna `404`. A criação retorna `201 Created`, `TaskResponseDTO`
e `Location` para `/api/v1/users/{userId}/tasks/{taskId}`, usando o proprietário
persistido e o ID gerado. O GET dessa URI ainda é um esboço e não recupera a Task.
A consulta do proprietário não autentica o solicitante.

Todos os 27 endpoints usam contratos HTTP com records nos subpacotes `dto`;
nenhum recebe ou retorna Entities. `UserDTO` e `ProjectDTO` continuam sendo
contratos internos das features. Somente o POST de Task executa um caso de uso;
os demais métodos mantêm a execução pendente, mesmo quando aceitam DTOs válidos.

### Contratos de entrada e validação

Cada feature tem `CreateDTO`, `UpdateDTO`, `PatchDTO` e `ResponseDTO`. Os 12 corpos
de entrada usam `@Valid`; erros estruturais retornam o HTTP 400 padrão do Spring,
sem handler customizado. Nomes/títulos têm até 255 caracteres; email é validado
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
Essas diferenças já são testadas nos mappers; a execução HTTP de PUT/PATCH permanece
para a etapa de casos de uso. Nenhum DTO aplica novos defaults ou regras de negócio.

FocusSession pode nascer avulsa com `{}` no POST sob usuário. O POST sob Task,
`/api/v1/users/{userId}/tasks/{taskId}/focus-sessions`, não recebe corpo: os IDs vêm
do path. Datas, duração e estado inicial são definidos pelo domínio. PUT/PATCH
recebem o estado solicitado, mas as transições futuras deverão usar as operações
do service; o mapper não altera status nem calcula tempo. Ambos os POSTs de sessão
ainda são esboços.

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

Somente `TaskMapper.toResponse` está ligado a um fluxo HTTP funcional nesta etapa.
As demais conversões estão preparadas e testadas, sem conectar novos casos de uso.
O POST de Task continua usando o service atual e ignora campos extras do JSON,
inclusive `id`, `projectId`, `status` e `priority`, por compatibilidade.

DTOs/mappers não verificam duplicidade ou propriedade nem executam persistência.
Application/Domain Services, CRUD pendente, handlers de erro e autenticação ficam
para etapas posteriores.

## Banco e execução

Requer JDK 21 e MySQL 8.4. Configure `JAVA_HOME` para o JDK antes de usar o Maven
Wrapper. A aplicação exige estas variáveis (não versionar credenciais):

- `FOCUSTASK_DB_URL`: URL JDBC, por exemplo `jdbc:mysql://localhost:3306/focustask`.
- `FOCUSTASK_DB_USERNAME`: usuário do banco.
- `FOCUSTASK_DB_PASSWORD`: senha do banco.

Execute `./mvnw spring-boot:run` (Windows: `.\mvnw.cmd spring-boot:run`).

Flyway aplica `V1__create_domain_tables.sql` em um schema vazio previamente
criado. Hibernate apenas valida o schema (`ddl-auto=validate`). Não há criação
automática do banco, `clean`, `create-drop` ou baseline automático. Se já existir
um schema com dados, revisar sua estrutura e preparar a migração correspondente
antes de apontar a aplicação para ele; não apagar as tabelas para contornar isso.

Os instantes são tratados em UTC. Enums usam nomes em VARCHAR, com validações no
schema. Os IDs escalares não criam FKs pelo JPA: as quatro FKs são definidas
explicitamente pela migration, todas com `ON DELETE RESTRICT`.

## Regras entre features

- Controllers usam seus services locais; outras features usam somente APIs públicas.
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

Execute `./mvnw test` (Windows: `.\mvnw.cmd test`).

- Testes unitários cobrem transições de sessão, validações de propriedade e ordem
  das exclusões. O teste de mapeamentos verifica as 27 rotas e a substituição do POST.
- Testes MVC do POST de Task verificam associação pela URL, identidade gerada,
  valores iniciais, `201`, `Location` com contexto, `404`, validação e rejeição da rota antiga.
- Testes dos demais contratos verificam `@Valid` e preservam a execução pendente.
  A auditoria recursiva verifica os 27 endpoints e impede Entities em retornos,
  corpos, coleções, wrappers e componentes de records.
- Testes de DTOs e dos mappers gerados cobrem limites, campos opcionais, enums,
  representação de sessões e a diferença entre PUT e PATCH.
- Testes que estendem `MySqlIntegrationTest` usam Testcontainers com `mysql:8.4`,
  aplicam a migration real e verificam consultas, FKs, exclusões e rollback,
  além da persistência pelo novo POST sem sobrescrever Tasks existentes.
- Sem Docker disponível, esses testes de integração são explicitamente marcados
  como **skipped**. Um build nessas condições não comprova a integração MySQL.
- Com Docker em execução, rode a suíte completa e confirme zero testes pulados
  antes de considerar a persistência validada. Nenhum teste usa seu banco de trabalho.

Não foram adicionados Spring Modulith, ArchUnit ou H2.
