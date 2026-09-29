# Apresentação no Insomnia — JWT

Importe FocusTask-colecao.json (formato Postman Collection). No ambiente da coleção, copie o objeto de ambiente.json. Use dados fictícios e troque test_email a cada rodada.

1. Pasta 01: saúde 200; cadastro 201 com corpo vazio; login 200. Copie o campo token retornado para a variável token, sem o prefixo Bearer. Consulte /api/v1/me.
2. Pasta 02: crie projeto, tarefa e sessão. Copie cada id retornado para project_id, task_id e session_id antes de continuar.
3. Pasta 03: consulte os recursos. A listagem /api/v1/users retorna 403 para USER; /me retorna a própria conta.
4. Pasta 04: PUT/PATCH. Mantenha o email para continuar usando o token atual; se mudar email, faça novo login. Mostre pausar/retomar sessão.
5. Pasta 05: erros 401 sem token/senha incorreta; 400 de validação; 404 inexistente; 409 duplicado; 405 POST antigo; 422 ao pausar sessão concluída. PATCH vazio retorna 200.
6. Pasta 06: exclua somente os dados desta rodada, na ordem. DELETE retorna 204 sem corpo; depois de excluir /me, o token dessa conta retorna 401.

A coleção herda Bearer {{token}}; cadastro/login/saúde e teste sem token usam No Auth. Tokens duram 15 minutos: refaça login quando expirarem. Não confunda a senha da conta fictícia com FOCUSTASK_JWT_SECRET, que existe somente no servidor.

As rotas de recursos não recebem mais user_id: o dono vem do token. POST Task continua /api/v1/projects/{{project_id}}/tasks. Requisições têm o status esperado no nome. IDs e tokens são copiados manualmente.
