INSERT INTO semantic.semantic_indexing_control (name, enabled)
VALUES ('auto-reconcile', FALSE)
ON CONFLICT (name) DO NOTHING;
