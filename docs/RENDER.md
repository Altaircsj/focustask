# FocusTask no Render com Docker

Preparado a partir de `Altaircsj/focustask`, commit `35b9dfcd8b947e0e728fea1de1c20bf6be8dc668`, seguindo o tutorial:
[Spring Boot em produção: Render + Docker — Gabriel Gadelha](https://www.youtube.com/watch?v=7skxHfKAX0M).

## O que foi aplicado

- Perfis `dev` (MySQL) e `prod` (PostgreSQL), com `dev` como padrão.
- Credenciais fornecidas por variáveis de ambiente.
- Docker em duas etapas: Maven/JDK 21 para compilar; JRE 21 para executar.
- Heap máximo de 300 MB, pool de banco e número de threads reduzidos. O heap não é toda a memória da JVM; não há garantia de ausência de OOM. A escolha deixa mais margem que os 380 MB mostrados no vídeo.
- `.dockerignore` e proteção de arquivos `.env` no Git.
- Flyway e `ddl-auto=validate` em ambos os ambientes. Preservamos esse comportamento já existente, em vez de voltar a `update` no desenvolvimento.
- Migração PostgreSQL própria. Ela adapta identidade, índices, timestamps e cálculo de duração; não basta trocar o driver.
- `/actuator/health` para o Render verificar aplicação e banco sem revelar detalhes.
- Teste de integração PostgreSQL e workflow que exige testes sem skips antes de construir a imagem.

O vídeo apresenta login/JWT já implementados. Esta versão do FocusTask ainda não contém essa funcionalidade. Não existe `TOKEN_SECRET` para configurar: adicionar uma variável sem código que a utilize não protege a API. A publicação desta etapa serve para demonstração com dados fictícios; os endpoints de CRUD ainda estão sem autenticação. Para reproduzir também login e Bearer token, é necessária a etapa de Spring Security/JWT/RBAC.

## 1. Publicar os arquivos no GitHub

Use a branch preparada para esta alteração, ou integre-a após revisar os testes. Dockerfile e pom.xml precisam estar na raiz escolhida para o serviço no Render. Não publique `.env`, credenciais ou dados locais.

A migration MySQL original foi apenas movida para `db/migration/mysql`, mantendo nome e conteúdo. Não apague o histórico Flyway de um banco existente. A migration PostgreSQL inicial destina-se a um banco PostgreSQL novo/vazio: ela não transfere os dados do MySQL.

## 2. Criar o PostgreSQL no Render

No painel: **New → Postgres**.

- Name: `focustask-db`.
- Database: `focustask`.
- Versão: PostgreSQL 17 (igual ao ambiente de teste preparado).
- Região: escolha uma e use a mesma para a API; o Blueprint incluído usa Oregon.
- Plano: Free, se disponível na conta. Confira a seleção antes de criar.

Aguarde o banco ficar disponível. Na página do banco, localize Hostname interno, Port, Database, Username e Password. Preencha esses dados somente no painel do serviço, não no repositório ou no chat.

## 3. Criar o serviço Docker

No painel: **New → Web Service**. Conecte o repositório `https://github.com/Altaircsj/focustask` e selecione a branch com as alterações Docker.

| Campo | Valor |
|---|---|
| Name | `focustask` ou outro disponível |
| Language / Runtime | Docker |
| Region | Mesma do PostgreSQL |
| Root Directory | Em branco, se pom.xml está na raiz |
| Dockerfile Path | `./Dockerfile` |
| Docker Build Context | `.` |
| Docker Command | Em branco; usar ENTRYPOINT da imagem |
| Instance Type | Free, se disponível |
| Health Check Path | `/actuator/health` |

Não preencha Build Command/Start Command de runtime Java nativo: o Dockerfile controla a compilação e a execução.

Variáveis do serviço:

| Nome | Valor |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `PORT` | `8080` |
| `DB_URL` | `jdbc:postgresql://HOST_INTERNO:5432/NOME_DO_BANCO?sslmode=require` |
| `DB_USERNAME` | Username do PostgreSQL |
| `DB_PASSWORD` | Password do PostgreSQL |

Monte `DB_URL` usando os campos separados. Não cole a URL `postgresql://usuario:senha@...` do Render diretamente. A URL JDBC não deve conter senha. Usar `localhost` no Render apontaria para o próprio contêiner da API, não para seu banco.

O `render.yaml` é uma alternativa para criar SOMENTE o Web Service via Blueprint depois de criar o banco. Ele pede essas mesmas três variáveis de banco e desativa auto-deploy. Não use Blueprint e criação manual para criar dois serviços duplicados. Se escolher região diferente de Oregon, ajuste o YAML antes de usar Blueprint.

## 4. Acompanhar e comprovar o deploy

Nos logs, confirme:

1. A imagem compila com Java 21 e copia o JAR para a etapa JRE.
2. O perfil ativo é `prod`.
3. Flyway conecta ao PostgreSQL e aplica a migration inicial.
4. Hibernate valida o schema e a aplicação inicia na porta configurada.
5. O Render marca o serviço como disponível.

Abra `https://SEU-SERVICO.onrender.com/actuator/health`. Esperado: HTTP 200 e `{"status":"UP"}`. O caminho `/` não tem controller e pode responder 404; isso não significa falha do deploy.

No Insomnia, duplique o ambiente local e mude somente `base_url` para `https://SEU-SERVICO.onrender.com`, sem `/api/v1`. Zere os IDs e crie usuário, projeto, tarefa e sessão novamente: o banco da nuvem é outro. Execute CRUD e os casos 400/404/409/422 já preparados. Verifique que os headers Location usam HTTPS e apontam para o serviço publicado.

## 5. Docker local com o mesmo PostgreSQL

Com Docker Desktop/Engine e Compose funcionando, na raiz do projeto:

```sh
cp .env.example .env
# Edite .env e substitua POSTGRES_PASSWORD por uma senha local.
docker compose up --build -d
docker compose logs -f api
```

No Windows, copie `.env.example` para `.env` pelo explorador ou `Copy-Item .env.example .env` no PowerShell. Depois use os mesmos comandos Docker.

A API fica em `http://localhost:8080`; PostgreSQL não publica porta no host. Se 8080 já está ocupada, defina `APP_PORT=8081` no `.env` e use `http://localhost:8081` no Insomnia. O banco mantém os dados em volume. Para parar preservando dados: `docker compose down`.

Depois da primeira inicialização, mudar POSTGRES_PASSWORD no `.env` não altera a senha já gravada no volume; use a credencial que criou esse banco ou altere-a conscientemente no banco.

## 6. Executar os testes

Com JDK 21 e Docker funcionando:

```sh
bash mvnw -B -ntp clean verify
```

No Windows: `.\mvnw.cmd -B -ntp clean verify`.

Os testes antigos usam MySQL; `PostgresDeploymentTests` usa PostgreSQL 17 com o perfil prod e o contexto MVC real, passando por Flyway, Hibernate, CRUD, erros e histórico de sessão. Sem Docker, as integrações são puladas: não apresentar isso como validação completa. O workflow `.github/workflows/verify.yml` falha se qualquer teste for pulado.

O Dockerfile usa `-DskipTests` para empacotar, pois o banco de testes não está disponível dentro da construção da imagem. Ele ainda compila os testes. O build da imagem não substitui a suíte de integração.

## Diagnóstico

- `Driver ... claims to not accept jdbcUrl`: confira `jdbc:postgresql://` e o perfil prod.
- Falha de autenticação no banco: confira username e password sem espaços extras.
- Erro Flyway com `AUTO_INCREMENT`/`ENGINE`: perfil ou caminho de migration incorreto; SQL MySQL não deve executar no PostgreSQL.
- Porta/health check: confirme PORT, `/actuator/health` e banco acessível.
- OOM/exit 137: examine o consumo real; heap de 300 MB não limita toda a JVM.
- Primeiro pedido demorado: o plano Free pode estar retomando após inatividade.
- 404 em `/`: teste a rota de health ou um endpoint `/api/v1/...`.

## Limites atuais do plano gratuito e fontes

A documentação consultada informa que o Web Service Free suspende após 15 minutos sem tráfego e o PostgreSQL Free expira após 30 dias. Verifique os limites na conta antes da apresentação; não presuma armazenamento gratuito permanente. Não foi contratado nenhum plano neste preparo.

- [Docker no Render](https://render.com/docs/docker)
- [Porta e Web Services](https://render.com/docs/web-services)
- [PostgreSQL: conexão e região](https://render.com/docs/postgresql-creating-connecting)
- [Limitações Free](https://render.com/docs/free)
- [Docker multistage](https://docs.docker.com/build/building/multi-stage/)
