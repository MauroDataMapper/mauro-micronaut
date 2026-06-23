ALTER TABLE semantic.embedding_profile
    ADD COLUMN IF NOT EXISTS description TEXT;

UPDATE semantic.embedding_profile
SET description = 'Deterministic test-only hash embeddings for semantic-search plumbing tests. Not suitable for evaluating semantic result quality.',
    updated_at = now()
WHERE name = 'test-hash-384';

INSERT INTO semantic.embedding_profile (
    name,
    provider,
    embedding_model,
    dimension,
    distance_metric,
    enabled,
    description
)
VALUES (
    'ollama-nomic-embed-text',
    'ollama',
    'nomic-embed-text',
    768,
    'cosine',
    FALSE,
    'Ollama nomic-embed-text embeddings. Enable and link this profile to a semantic index after pulling the model locally.'
)
ON CONFLICT (name) DO UPDATE
SET provider = EXCLUDED.provider,
    embedding_model = EXCLUDED.embedding_model,
    dimension = EXCLUDED.dimension,
    distance_metric = EXCLUDED.distance_metric,
    description = EXCLUDED.description,
    updated_at = now();
