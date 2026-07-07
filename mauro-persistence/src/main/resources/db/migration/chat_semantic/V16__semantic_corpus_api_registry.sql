ALTER TABLE semantic.semantic_corpus
    ADD COLUMN IF NOT EXISTS origin VARCHAR(255) NOT NULL DEFAULT 'internal',
    ADD COLUMN IF NOT EXISTS api_visible BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS api_manageable BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS chunk_delete_api_enabled BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE semantic.semantic_corpus
    DROP CONSTRAINT IF EXISTS semantic_corpus_origin;

ALTER TABLE semantic.semantic_corpus
    ADD CONSTRAINT semantic_corpus_origin
    CHECK (origin IN ('internal', 'api'));

UPDATE semantic.semantic_corpus
SET origin = 'api',
    api_visible = TRUE,
    api_manageable = TRUE,
    chunk_delete_api_enabled = TRUE,
    updated_at = now()
WHERE name = 'catalogue-items';
