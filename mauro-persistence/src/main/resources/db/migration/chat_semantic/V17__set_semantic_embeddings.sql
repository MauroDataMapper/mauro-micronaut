CREATE TABLE IF NOT EXISTS semantic.set_semantic_embedding (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    corpus_id UUID NOT NULL REFERENCES semantic.semantic_corpus(id) ON DELETE CASCADE,
    embedding_profile_id UUID NOT NULL REFERENCES semantic.embedding_profile(id) ON DELETE CASCADE,
    set_id UUID NOT NULL,
    set_domain_type VARCHAR(255) NOT NULL,
    set_label TEXT,
    mauro_model_id UUID,
    vector_family VARCHAR(255) NOT NULL,
    centroid_kind VARCHAR(255) NOT NULL,
    region_ordinal INTEGER NOT NULL DEFAULT 0,
    member_count INTEGER NOT NULL,
    residual_radius DOUBLE PRECISION,
    local_radius DOUBLE PRECISION,
    source_member_count INTEGER NOT NULL,
    source_fingerprint TEXT NOT NULL,
    embedding vector NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT set_semantic_embedding_vector_family CHECK (vector_family IN ('meaning', 'identifier')),
    CONSTRAINT set_semantic_embedding_centroid_kind CHECK (centroid_kind IN ('overall', 'region')),
    CONSTRAINT set_semantic_embedding_member_count_positive CHECK (member_count > 0),
    CONSTRAINT set_semantic_embedding_source_member_count_positive CHECK (source_member_count > 0),
    UNIQUE (corpus_id, embedding_profile_id, set_id, vector_family, centroid_kind, region_ordinal)
);

CREATE INDEX IF NOT EXISTS set_semantic_embedding_profile_idx
    ON semantic.set_semantic_embedding (embedding_profile_id);

CREATE INDEX IF NOT EXISTS set_semantic_embedding_set_idx
    ON semantic.set_semantic_embedding (corpus_id, set_domain_type, set_id);

CREATE INDEX IF NOT EXISTS set_semantic_embedding_model_idx
    ON semantic.set_semantic_embedding (mauro_model_id);

CREATE INDEX IF NOT EXISTS set_semantic_embedding_family_idx
    ON semantic.set_semantic_embedding (vector_family, centroid_kind);

DO $$
DECLARE
    profile_id UUID;
BEGIN
    SELECT id INTO profile_id
    FROM semantic.embedding_profile
    WHERE name = 'test-hash-384';

    IF profile_id IS NOT NULL THEN
        EXECUTE format(
            'CREATE INDEX IF NOT EXISTS set_semantic_embedding_test_hash_384_hnsw_idx ON semantic.set_semantic_embedding USING hnsw ((embedding::vector(384)) vector_cosine_ops) WHERE embedding_profile_id = %L',
            profile_id
        );
    END IF;

    SELECT id INTO profile_id
    FROM semantic.embedding_profile
    WHERE name = 'ollama-nomic-embed-text';

    IF profile_id IS NOT NULL THEN
        EXECUTE format(
            'CREATE INDEX IF NOT EXISTS set_semantic_embedding_ollama_nomic_embed_text_hnsw_idx ON semantic.set_semantic_embedding USING hnsw ((embedding::vector(768)) vector_cosine_ops) WHERE embedding_profile_id = %L',
            profile_id
        );
    END IF;
END $$;
