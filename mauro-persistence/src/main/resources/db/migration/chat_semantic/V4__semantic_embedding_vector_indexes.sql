DO $$
DECLARE
    profile_id UUID;
BEGIN
    SELECT id INTO profile_id
    FROM semantic.embedding_profile
    WHERE name = 'test-hash-384';

    IF profile_id IS NOT NULL THEN
        EXECUTE format(
            'CREATE INDEX IF NOT EXISTS semantic_embedding_test_hash_384_hnsw_idx ON semantic.semantic_embedding USING hnsw ((embedding::vector(384)) vector_cosine_ops) WHERE embedding_profile_id = %L',
            profile_id
        );
    END IF;

    SELECT id INTO profile_id
    FROM semantic.embedding_profile
    WHERE name = 'ollama-nomic-embed-text';

    IF profile_id IS NOT NULL THEN
        EXECUTE format(
            'CREATE INDEX IF NOT EXISTS semantic_embedding_ollama_nomic_embed_text_hnsw_idx ON semantic.semantic_embedding USING hnsw ((embedding::vector(768)) vector_cosine_ops) WHERE embedding_profile_id = %L',
            profile_id
        );
    END IF;
END $$;
