UPDATE semantic.embedding_profile
SET name = 'test-hash-384',
    provider = 'test',
    updated_at = now()
WHERE name = 'local-hash-384'
  AND provider = 'local'
  AND embedding_model = 'hash-384';
