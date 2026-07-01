CREATE INDEX IF NOT EXISTS semantic_chunk_corpus_id_idx
    ON semantic.semantic_chunk (corpus_id, id);

CREATE INDEX IF NOT EXISTS semantic_chunk_corpus_domain_source_idx
    ON semantic.semantic_chunk (corpus_id, source_domain_type, source_id);

CREATE INDEX IF NOT EXISTS semantic_embedding_profile_chunk_hash_idx
    ON semantic.semantic_embedding (embedding_profile_id, chunk_id, content_hash);
