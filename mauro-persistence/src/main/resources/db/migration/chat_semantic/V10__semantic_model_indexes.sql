CREATE TABLE IF NOT EXISTS semantic.semantic_model_index (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    corpus_id UUID NOT NULL REFERENCES semantic.semantic_corpus(id) ON DELETE CASCADE,
    mauro_model_id UUID NOT NULL,
    embedding_profile_id UUID NOT NULL REFERENCES semantic.embedding_profile(id) ON DELETE CASCADE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(255) NOT NULL DEFAULT 'STALE',
    last_indexed_at TIMESTAMP WITHOUT TIME ZONE,
    last_error TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT semantic_model_index_status CHECK (status IN ('STALE', 'INDEXING', 'READY', 'FAILED')),
    UNIQUE (corpus_id, mauro_model_id, embedding_profile_id)
);

CREATE INDEX IF NOT EXISTS semantic_model_index_model_idx
    ON semantic.semantic_model_index (mauro_model_id);

CREATE INDEX IF NOT EXISTS semantic_model_index_profile_idx
    ON semantic.semantic_model_index (embedding_profile_id);

CREATE INDEX IF NOT EXISTS semantic_model_index_enabled_status_idx
    ON semantic.semantic_model_index (enabled, status);

CREATE TABLE IF NOT EXISTS semantic.semantic_index_job (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    model_index_id UUID REFERENCES semantic.semantic_model_index(id) ON DELETE SET NULL,
    corpus_id UUID REFERENCES semantic.semantic_corpus(id) ON DELETE SET NULL,
    mauro_model_id UUID NOT NULL,
    embedding_profile_id UUID NOT NULL REFERENCES semantic.embedding_profile(id) ON DELETE CASCADE,
    status VARCHAR(255) NOT NULL DEFAULT 'QUEUED',
    force BOOLEAN NOT NULL DEFAULT FALSE,
    max_rows INTEGER,
    batch_size INTEGER,
    result JSONB NOT NULL DEFAULT '{}'::jsonb,
    error TEXT,
    started_at TIMESTAMP WITHOUT TIME ZONE,
    completed_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT semantic_index_job_status CHECK (status IN ('QUEUED', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED'))
);

CREATE INDEX IF NOT EXISTS semantic_index_job_model_idx
    ON semantic.semantic_index_job (mauro_model_id);

CREATE INDEX IF NOT EXISTS semantic_index_job_status_idx
    ON semantic.semantic_index_job (status, created_at);

CREATE TABLE IF NOT EXISTS semantic.semantic_indexing_control (
    name VARCHAR(255) PRIMARY KEY,
    enabled BOOLEAN NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);

INSERT INTO semantic.semantic_indexing_control (name, enabled)
VALUES ('global', FALSE)
ON CONFLICT (name) DO NOTHING;
