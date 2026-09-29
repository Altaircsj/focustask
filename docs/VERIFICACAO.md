# Verificação do deploy autenticado

A atualização integra JWT à configuração Docker/Render e mantém a main intacta.

Execute `bash mvnw -B -ntp clean verify` em ambiente com Docker para validar MySQL e PostgreSQL. O workflow rejeita qualquer teste ignorado e constrói a imagem. A execução local sem Docker não substitui essa validação.

O teste PostgreSQL verifica: V1/V2, saúde sem token e sem detalhes, 401 sem autenticação, cadastro/login reais, armazenamento BCrypt, 403 para lista administrativa com USER, CRUD com Bearer, validação 400, duplicidade 409, recurso ausente 404 e transição inválida 422.

Após publicar, repetir a coleção autenticada contra a URL pública. As evidências e a execução exata de CI estão na descrição do PR.
