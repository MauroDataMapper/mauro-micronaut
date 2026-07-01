DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector') THEN
        RAISE EXCEPTION 'The pgvector extension must be installed before running V15__semantic_search.sql. Create it with a privileged role using: CREATE EXTENSION IF NOT EXISTS vector;';
    END IF;
END $$;

CREATE SCHEMA IF NOT EXISTS semantic;

CREATE TABLE IF NOT EXISTS semantic.embedding_profile (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    provider VARCHAR(255) NOT NULL,
    embedding_model VARCHAR(255) NOT NULL,
    dimension INTEGER NOT NULL,
    distance_metric VARCHAR(255) NOT NULL DEFAULT 'cosine',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT embedding_profile_dimension_positive CHECK (dimension > 0),
    CONSTRAINT embedding_profile_distance_metric CHECK (distance_metric IN ('cosine', 'l2', 'inner_product'))
);

CREATE TABLE IF NOT EXISTS semantic.semantic_corpus (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    source VARCHAR(255) NOT NULL,
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS semantic.semantic_index (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    corpus_id UUID NOT NULL REFERENCES semantic.semantic_corpus(id) ON DELETE CASCADE,
    status VARCHAR(255) NOT NULL DEFAULT 'STALE',
    last_indexed_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT semantic_index_status CHECK (status IN ('STALE', 'INDEXING', 'READY', 'FAILED'))
);

CREATE TABLE IF NOT EXISTS semantic.semantic_index_profile (
    semantic_index_id UUID NOT NULL REFERENCES semantic.semantic_index(id) ON DELETE CASCADE,
    embedding_profile_id UUID NOT NULL REFERENCES semantic.embedding_profile(id) ON DELETE CASCADE,
    PRIMARY KEY (semantic_index_id, embedding_profile_id)
);

CREATE TABLE IF NOT EXISTS semantic.semantic_chunk (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    corpus_id UUID NOT NULL REFERENCES semantic.semantic_corpus(id) ON DELETE CASCADE,
    source_type VARCHAR(255) NOT NULL,
    source_id UUID NOT NULL,
    source_domain_type VARCHAR(255),
    source_label TEXT,
    mauro_model_id UUID,
    chunk_kind VARCHAR(255) NOT NULL,
    chunk_ordinal INTEGER NOT NULL,
    source_text TEXT NOT NULL,
    content_hash TEXT NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    date_created TIMESTAMP WITHOUT TIME ZONE,
    last_updated TIMESTAMP WITHOUT TIME ZONE,
    indexed_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE (corpus_id, source_type, source_id, chunk_kind, chunk_ordinal)
);

CREATE INDEX IF NOT EXISTS semantic_chunk_source_idx
    ON semantic.semantic_chunk (corpus_id, source_type, source_id);

CREATE INDEX IF NOT EXISTS semantic_chunk_domain_type_idx
    ON semantic.semantic_chunk (source_domain_type);

CREATE INDEX IF NOT EXISTS semantic_chunk_mauro_model_id_idx
    ON semantic.semantic_chunk (mauro_model_id);

CREATE INDEX IF NOT EXISTS semantic_chunk_content_hash_idx
    ON semantic.semantic_chunk (content_hash);

CREATE TABLE IF NOT EXISTS semantic.semantic_embedding (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chunk_id UUID NOT NULL REFERENCES semantic.semantic_chunk(id) ON DELETE CASCADE,
    embedding_profile_id UUID NOT NULL REFERENCES semantic.embedding_profile(id) ON DELETE CASCADE,
    content_hash TEXT NOT NULL,
    embedding vector NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE (chunk_id, embedding_profile_id)
);

CREATE INDEX IF NOT EXISTS semantic_embedding_profile_idx
    ON semantic.semantic_embedding (embedding_profile_id);

CREATE INDEX IF NOT EXISTS semantic_embedding_chunk_idx
    ON semantic.semantic_embedding (chunk_id);

CREATE TABLE IF NOT EXISTS semantic.item_affinity (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    corpus_id UUID NOT NULL REFERENCES semantic.semantic_corpus(id) ON DELETE CASCADE,
    source_type VARCHAR(255) NOT NULL,
    source_id UUID NOT NULL,
    target_type VARCHAR(255) NOT NULL,
    target_id UUID NOT NULL,
    affinity_type VARCHAR(255) NOT NULL,
    strength DOUBLE PRECISION,
    embedding_profile_id UUID REFERENCES semantic.embedding_profile(id) ON DELETE SET NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE (corpus_id, source_type, source_id, target_type, target_id, affinity_type, embedding_profile_id)
);

CREATE INDEX IF NOT EXISTS item_affinity_source_idx
    ON semantic.item_affinity (corpus_id, source_type, source_id);

CREATE INDEX IF NOT EXISTS item_affinity_target_idx
    ON semantic.item_affinity (corpus_id, target_type, target_id);

INSERT INTO semantic.embedding_profile (name, provider, embedding_model, dimension, distance_metric)
VALUES ('local-hash-384', 'local', 'hash-384', 384, 'cosine')
ON CONFLICT (name) DO NOTHING;

INSERT INTO semantic.semantic_corpus (name, source, description)
VALUES ('catalogue-items', 'search.search_domains', 'Semantic chunks derived from the catalogue keyword search domain view.')
ON CONFLICT (name) DO NOTHING;

INSERT INTO semantic.semantic_index (name, corpus_id, status)
SELECT 'catalogue-items-default', c.id, 'STALE'
FROM semantic.semantic_corpus c
WHERE c.name = 'catalogue-items'
ON CONFLICT (name) DO NOTHING;

INSERT INTO semantic.semantic_index_profile (semantic_index_id, embedding_profile_id)
SELECT i.id, p.id
FROM semantic.semantic_index i
     CROSS JOIN semantic.embedding_profile p
WHERE i.name = 'catalogue-items-default'
  AND p.name = 'local-hash-384'
ON CONFLICT DO NOTHING;
