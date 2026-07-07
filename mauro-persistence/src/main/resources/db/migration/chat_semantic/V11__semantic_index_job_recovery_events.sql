ALTER TABLE semantic.semantic_index_job
    DROP CONSTRAINT IF EXISTS semantic_index_job_status;

ALTER TABLE semantic.semantic_index_job
    ADD CONSTRAINT semantic_index_job_status
    CHECK (status IN ('QUEUED', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED', 'INTERRUPTED'));

CREATE TABLE IF NOT EXISTS semantic.semantic_index_job_event (
    id BIGSERIAL PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES semantic.semantic_index_job(id) ON DELETE CASCADE,
    status VARCHAR(255) NOT NULL,
    event JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS semantic_index_job_event_job_idx
    ON semantic.semantic_index_job_event (job_id, id);
