# FocusTask no Render com Docker e JWT

Java 21, PostgreSQL 18, perfil prod. Desenvolvimento local usa MySQL com perfil dev. A branch de deploy integra a autenticação da main; a main não é modificada por este processo.

## Render

- Web Service Docker, contexto `.`, Dockerfile `./Dockerfile`, região do banco, plano Free.
- Health check: `/actuator/health`. Somente esse GET e POST de cadastro/login são públicos.
- Auto-deploy desligado: publicar manualmente após o workflow passar.
- Variáveis: `SPRING_PROFILES_ACTIVE=prod`, `PORT=8080`, `DB_URL=jdbc:postgresql://HOST:5432/BANCO?sslmode=require`, `DB_USERNAME`, `DB_PASSWORD`, `FOCUSTASK_JWT_SECRET`.
- Gere um segredo JWT aleatório com pelo menos 32 bytes; por exemplo `openssl rand -hex 32`. Salve somente como variável secreta no Render. Não envie o segredo para Git, logs, coleção Insomnia ou mensagens.
- `render.yaml` pode gerar automaticamente o segredo para novos serviços via Blueprint; em serviço existente configurado manualmente é necessário adicionar a variável.

O segredo não tem valor padrão: sem ele a aplicação falha ao iniciar. Alterá-lo invalida os tokens existentes.

## Migrações

As migrations ficam em `db/migration/mysql` e `db/migration/postgresql`; cada perfil executa apenas seu diretório. V1 preserva o schema já implantado; V2 inclui password_hash e role. O conteúdo da V2 MySQL foi preservado da atualização original.

V2 exige tabela users vazia: usuários antigos não têm senha. Se houver registros, não apague nem atribua senha coletiva; prepare uma migração de contas com redefinição segura antes de publicar. O deploy de demonstração foi verificado sem usuários antes da atualização, mas a condição deve ser reconferida no momento do deploy.

## Docker local

Copie `.env.example` para `.env`, defina POSTGRES_PASSWORD e FOCUSTASK_JWT_SECRET. Execute `docker compose up --build` e consulte `http://localhost:8080/actuator/health`. O volume PostgreSQL 18 usa `/var/lib/postgresql`. Não remova o volume para resolver erros de migração.

O Dockerfile compila com Maven/Java 21 e executa com JRE 21 e usuário não privilegiado. Heap limitado a 300 MB para o plano de 512 MB. Os testes são executados fora do build da imagem, pelo workflow.

## Validação e Insomnia

O workflow executa todos os testes com MySQL/PostgreSQL, rejeita testes ignorados e constrói a imagem. `PostgresDeploymentTests` verifica migrations, saúde pública, cadastro/login, CRUD com token, erros e regras de sessão.

Importe `docs/insomnia/FocusTask-colecao.json` e configure as variáveis descritas em `docs/insomnia/GUIA.md`. Primeiro registre, faça login e copie o token. Rotas protegidas exigem `Authorization: Bearer <token>`; ele expira em 15 minutos. GET /api/v1/users exige ADMIN; cadastro sempre cria USER. A própria conta fica em /api/v1/me.

No Render Free a instância adormece por inatividade. O PostgreSQL gratuito tem prazo limitado; consulte a data no painel. Use dados fictícios na demonstração.
