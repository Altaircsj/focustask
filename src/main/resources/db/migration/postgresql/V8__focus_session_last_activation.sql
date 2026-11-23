-- Instante do último Play (criação ou última retomada), usado para saber qual sessão foi ativada por último.
ALTER TABLE focus_sessions ADD COLUMN last_activated_at TIMESTAMP(6) WITH TIME ZONE;

-- Registros antigos: o único instante conhecido é o início, então ele passa a ser a última ativação.
UPDATE focus_sessions SET last_activated_at = started_at;

-- Contas com mais de uma sessão RUNNING: continua rodando a de ativação mais recente (empate: maior id).
-- As demais ficam PAUSED, sem perder histórico, no instante em que a vencedora foi ativada
-- (nunca antes do tempo ativo que a própria sessão já acumulou, para respeitar ck_focus_sessions_duration).
WITH winners AS (
    SELECT DISTINCT ON (user_id) user_id, id, last_activated_at
    FROM focus_sessions
    WHERE status = 'RUNNING'
    ORDER BY user_id, last_activated_at DESC, id DESC
)
UPDATE focus_sessions s
SET status = 'PAUSED',
    paused_at = GREATEST(w.last_activated_at, s.started_at + s.total_paused_seconds * INTERVAL '1 second')
FROM winners w
WHERE s.user_id = w.user_id AND s.status = 'RUNNING' AND s.id <> w.id;

ALTER TABLE focus_sessions ALTER COLUMN last_activated_at SET NOT NULL;
