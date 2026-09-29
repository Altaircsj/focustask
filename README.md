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

Projeto inexistente retorna `404`. A criação retorna `201 Created`, a Task salva
e `Location` para `/api/v1/users/{userId}/tasks/{taskId}`, usando o proprietário
persistido e o ID gerado. O GET dessa URI ainda é um esboço e não recupera a Task.
A consulta do proprietário não autentica o solicitante.

O uso de Entity como entrada/saída HTTP é temporário; DTOs HTTP e Bean Validation
ficam para a próxima etapa.

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
  valores iniciais, `201`, `Location` com contexto, `404` e rejeição da rota antiga.
- Testes que estendem `MySqlIntegrationTest` usam Testcontainers com `mysql:8.4`,
  aplicam a migration real e verificam consultas, FKs, exclusões e rollback,
  além da persistência pelo novo POST sem sobrescrever Tasks existentes.
- Sem Docker disponível, esses testes de integração são explicitamente marcados
  como **skipped**. Um build nessas condições não comprova a integração MySQL.
- Com Docker em execução, rode a suíte completa e confirme zero testes pulados
  antes de considerar a persistência validada. Nenhum teste usa seu banco de trabalho.

Não foram adicionados Spring Modulith, ArchUnit ou H2.
