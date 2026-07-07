ALTER TABLE semantic.semantic_index_job
    DROP CONSTRAINT IF EXISTS semantic_index_job_status;

ALTER TABLE semantic.semantic_index_job
    ADD CONSTRAINT semantic_index_job_status
    CHECK (status IN ('QUEUED', 'TO_RESTART', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED', 'INTERRUPTED'));
