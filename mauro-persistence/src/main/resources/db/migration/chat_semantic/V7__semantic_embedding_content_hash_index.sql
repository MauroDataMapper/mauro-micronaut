CREATE INDEX IF NOT EXISTS semantic_embedding_profile_content_hash_updated_idx
    ON semantic.semantic_embedding (embedding_profile_id, content_hash, updated_at DESC);
