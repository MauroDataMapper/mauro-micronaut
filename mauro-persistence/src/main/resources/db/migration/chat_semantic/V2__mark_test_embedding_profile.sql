DO $$
DECLARE
    local_profile_id UUID;
    test_profile_id UUID;
BEGIN
    SELECT id INTO local_profile_id
    FROM semantic.embedding_profile
    WHERE name = 'local-hash-384'
      AND provider = 'local'
      AND embedding_model = 'hash-384';

    SELECT id INTO test_profile_id
    FROM semantic.embedding_profile
    WHERE name = 'test-hash-384';

    IF local_profile_id IS NOT NULL AND test_profile_id IS NULL THEN
        UPDATE semantic.embedding_profile
        SET name = 'test-hash-384',
            provider = 'test',
            updated_at = now()
        WHERE id = local_profile_id;
    ELSIF local_profile_id IS NOT NULL AND test_profile_id IS NOT NULL THEN
        INSERT INTO semantic.semantic_index_profile (semantic_index_id, embedding_profile_id)
        SELECT semantic_index_id, test_profile_id
        FROM semantic.semantic_index_profile
        WHERE embedding_profile_id = local_profile_id
        ON CONFLICT DO NOTHING;

        DELETE FROM semantic.semantic_index_profile
        WHERE embedding_profile_id = local_profile_id;

        DELETE FROM semantic.semantic_embedding
        WHERE embedding_profile_id = local_profile_id;

        DELETE FROM semantic.embedding_profile
        WHERE id = local_profile_id;

        UPDATE semantic.embedding_profile
        SET provider = 'test',
            embedding_model = 'hash-384',
            dimension = 384,
            distance_metric = 'cosine',
            updated_at = now()
        WHERE id = test_profile_id;
    END IF;
END $$;
