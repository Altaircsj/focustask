-- Instante do último Play (criação ou última retomada), usado para saber qual sessão foi ativada por último.
ALTER TABLE focus_sessions ADD COLUMN last_activated_at DATETIME(6) NULL;

-- Registros antigos: o único instante conhecido é o início, então ele passa a ser a última ativação.
UPDATE focus_sessions SET last_activated_at = started_at;

-- Contas com mais de uma sessão RUNNING: continua rodando a de ativação mais recente (empate: maior id).
-- As demais ficam PAUSED, sem perder histórico, no instante em que a vencedora foi ativada
-- (nunca antes do tempo ativo que a própria sessão já acumulou, para respeitar ck_focus_sessions_duration).
WITH ranked AS (
    SELECT id, user_id, last_activated_at,
           ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY last_activated_at DESC, id DESC) AS rn
    FROM focus_sessions
    WHERE status = 'RUNNING'
)
UPDATE focus_sessions s
JOIN ranked loser ON loser.id = s.id AND loser.rn > 1
JOIN ranked winner ON winner.user_id = s.user_id AND winner.rn = 1
SET s.status = 'PAUSED',
    s.paused_at = GREATEST(winner.last_activated_at, DATE_ADD(s.started_at, INTERVAL s.total_paused_seconds SECOND));

ALTER TABLE focus_sessions MODIFY COLUMN last_activated_at DATETIME(6) NOT NULL;
