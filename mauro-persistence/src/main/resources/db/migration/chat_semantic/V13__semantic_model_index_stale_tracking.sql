ALTER TABLE semantic.semantic_model_index
    ADD COLUMN IF NOT EXISTS stale_requested_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS indexing_started_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS last_checked_at TIMESTAMP WITHOUT TIME ZONE;

CREATE INDEX IF NOT EXISTS semantic_model_index_stale_requested_idx
    ON semantic.semantic_model_index (stale_requested_at);

CREATE INDEX IF NOT EXISTS semantic_index_job_active_declaration_idx
    ON semantic.semantic_index_job (corpus_id, mauro_model_id, embedding_profile_id, status)
    WHERE status IN ('QUEUED', 'TO_RESTART', 'RUNNING');
