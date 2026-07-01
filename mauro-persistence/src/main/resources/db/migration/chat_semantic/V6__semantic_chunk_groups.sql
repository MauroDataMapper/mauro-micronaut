ALTER TABLE semantic.semantic_chunk
    ADD COLUMN IF NOT EXISTS chunk_group VARCHAR(255) NOT NULL DEFAULT 'catalogue';

ALTER TABLE semantic.semantic_embedding
    ADD COLUMN IF NOT EXISTS chunk_group VARCHAR(255) NOT NULL DEFAULT 'catalogue';

UPDATE semantic.semantic_chunk
SET chunk_group = CASE
    WHEN chunk_kind IN (
        'metadata-key-value',
        'annotation',
        'classification'
    ) OR chunk_kind LIKE 'semantic-link-%'
        THEN 'context'
    ELSE 'catalogue'
END;

UPDATE semantic.semantic_embedding embedding
SET chunk_group = chunk.chunk_group
FROM semantic.semantic_chunk chunk
WHERE chunk.id = embedding.chunk_id
  AND embedding.chunk_group IS DISTINCT FROM chunk.chunk_group;

CREATE INDEX IF NOT EXISTS semantic_chunk_group_idx
    ON semantic.semantic_chunk (chunk_group);

DO $$
DECLARE
    profile_id UUID;
BEGIN
    SELECT id INTO profile_id
    FROM semantic.embedding_profile
    WHERE name = 'test-hash-384';

    IF profile_id IS NOT NULL THEN
        EXECUTE format(
            'CREATE INDEX IF NOT EXISTS semantic_embedding_test_hash_384_catalogue_hnsw_idx ON semantic.semantic_embedding USING hnsw ((embedding::vector(384)) vector_cosine_ops) WHERE embedding_profile_id = %L AND chunk_group = %L',
            profile_id,
            'catalogue'
        );
        EXECUTE format(
            'CREATE INDEX IF NOT EXISTS semantic_embedding_test_hash_384_context_hnsw_idx ON semantic.semantic_embedding USING hnsw ((embedding::vector(384)) vector_cosine_ops) WHERE embedding_profile_id = %L AND chunk_group = %L',
            profile_id,
            'context'
        );
    END IF;

    SELECT id INTO profile_id
    FROM semantic.embedding_profile
    WHERE name = 'ollama-nomic-embed-text';

    IF profile_id IS NOT NULL THEN
        EXECUTE format(
            'CREATE INDEX IF NOT EXISTS semantic_embedding_ollama_nomic_embed_text_catalogue_hnsw_idx ON semantic.semantic_embedding USING hnsw ((embedding::vector(768)) vector_cosine_ops) WHERE embedding_profile_id = %L AND chunk_group = %L',
            profile_id,
            'catalogue'
        );
        EXECUTE format(
            'CREATE INDEX IF NOT EXISTS semantic_embedding_ollama_nomic_embed_text_context_hnsw_idx ON semantic.semantic_embedding USING hnsw ((embedding::vector(768)) vector_cosine_ops) WHERE embedding_profile_id = %L AND chunk_group = %L',
            profile_id,
            'context'
        );
    END IF;
END $$;
