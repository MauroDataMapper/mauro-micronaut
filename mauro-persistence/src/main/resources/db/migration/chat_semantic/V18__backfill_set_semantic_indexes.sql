UPDATE semantic.semantic_model_index
SET status = 'STALE',
    stale_requested_at = now(),
    last_error = NULL,
    updated_at = now()
WHERE enabled = TRUE
  AND status <> 'INDEXING';
