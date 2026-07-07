ALTER TABLE semantic.semantic_model_index
    ADD COLUMN IF NOT EXISTS label TEXT;
