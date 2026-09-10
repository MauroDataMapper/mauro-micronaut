-- Derived indexes have an independent queue. Do not invalidate catalogue embeddings.
CREATE TABLE semantic.set_semantic_index_state (
    corpus_id UUID NOT NULL REFERENCES semantic.semantic_corpus(id) ON DELETE CASCADE,
    embedding_profile_id UUID NOT NULL REFERENCES semantic.embedding_profile(id) ON DELETE CASCADE,
    mauro_model_id UUID NOT NULL,
    revision BIGINT NOT NULL DEFAULT 1,
    indexed_revision BIGINT NOT NULL DEFAULT 0,
    configuration_fingerprint TEXT,
    explicit_request BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(32) NOT NULL DEFAULT 'QUEUED'
        CHECK (status IN ('QUEUED', 'RUNNING', 'READY', 'PARTIAL', 'FAILED')),
    requested_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    started_at TIMESTAMP WITHOUT TIME ZONE,
    completed_at TIMESTAMP WITHOUT TIME ZONE,
    last_error TEXT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    PRIMARY KEY (corpus_id, embedding_profile_id, mauro_model_id)
);
CREATE INDEX set_semantic_index_queue_idx ON semantic.set_semantic_index_state (status, requested_at);

CREATE FUNCTION semantic.invalidate_set_owner(owner_id UUID) RETURNS void LANGUAGE sql AS $$
    UPDATE semantic.set_semantic_index_state
    SET revision = revision + 1,
        status = CASE WHEN status = 'RUNNING' THEN status ELSE 'QUEUED' END,
        requested_at = now()
    WHERE mauro_model_id = owner_id AND status <> 'QUEUED';
$$;

CREATE FUNCTION semantic.invalidate_set_member(member_id UUID, member_type TEXT) RETURNS void LANGUAGE plpgsql AS $$
DECLARE owner_id UUID;
BEGIN
    IF member_type = 'Term' THEN
        FOR owner_id IN
            SELECT terminology_id FROM terminology.term WHERE id = member_id
            UNION SELECT code_set_id FROM terminology.code_set_term WHERE term_id = member_id
        LOOP PERFORM semantic.invalidate_set_owner(owner_id); END LOOP;
    ELSIF member_type = 'EnumerationValue' THEN
        PERFORM semantic.invalidate_set_owner(dt.data_model_id)
        FROM datamodel.enumeration_value ev JOIN datamodel.data_type dt ON dt.id = ev.enumeration_type_id
        WHERE ev.id = member_id;
    END IF;
END $$;

CREATE FUNCTION semantic.set_member_changed() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_TABLE_NAME = 'code_set_term' THEN
        IF TG_OP <> 'INSERT' THEN PERFORM semantic.invalidate_set_owner(OLD.code_set_id); END IF;
        IF TG_OP <> 'DELETE' THEN PERFORM semantic.invalidate_set_owner(NEW.code_set_id); END IF;
    ELSIF TG_TABLE_NAME = 'term' THEN
        IF TG_OP <> 'INSERT' THEN
            PERFORM semantic.invalidate_set_owner(OLD.terminology_id);
            PERFORM semantic.invalidate_set_member(OLD.id, 'Term');
        END IF;
        IF TG_OP <> 'DELETE' THEN PERFORM semantic.invalidate_set_owner(NEW.terminology_id); END IF;
    ELSE
        IF TG_OP <> 'INSERT' THEN
            PERFORM semantic.invalidate_set_owner(data_model_id) FROM datamodel.data_type WHERE id = OLD.enumeration_type_id;
        END IF;
        IF TG_OP <> 'DELETE' THEN
            PERFORM semantic.invalidate_set_owner(data_model_id) FROM datamodel.data_type WHERE id = NEW.enumeration_type_id;
        END IF;
    END IF;
    RETURN COALESCE(NEW, OLD);
END $$;
CREATE TRIGGER set_term_changed BEFORE INSERT OR UPDATE OR DELETE ON terminology.term
    FOR EACH ROW EXECUTE FUNCTION semantic.set_member_changed();
CREATE TRIGGER set_membership_changed BEFORE INSERT OR UPDATE OR DELETE ON terminology.code_set_term
    FOR EACH ROW EXECUTE FUNCTION semantic.set_member_changed();
CREATE TRIGGER set_enumeration_value_changed BEFORE INSERT OR UPDATE OR DELETE ON datamodel.enumeration_value
    FOR EACH ROW EXECUTE FUNCTION semantic.set_member_changed();

CREATE FUNCTION semantic.set_source_changed() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE source RECORD;
BEGIN
    IF TG_TABLE_NAME = 'semantic_chunk' THEN
        IF TG_OP <> 'INSERT' THEN PERFORM semantic.invalidate_set_member(OLD.source_id, OLD.source_domain_type); END IF;
        IF TG_OP <> 'DELETE' THEN PERFORM semantic.invalidate_set_member(NEW.source_id, NEW.source_domain_type); END IF;
    ELSE
        FOR source IN SELECT source_id, source_domain_type FROM semantic.semantic_chunk
            WHERE id IN (CASE WHEN TG_OP <> 'INSERT' THEN OLD.chunk_id END,
                         CASE WHEN TG_OP <> 'DELETE' THEN NEW.chunk_id END)
        LOOP PERFORM semantic.invalidate_set_member(source.source_id, source.source_domain_type); END LOOP;
    END IF;
    RETURN COALESCE(NEW, OLD);
END $$;
CREATE TRIGGER set_chunk_changed BEFORE INSERT OR UPDATE OR DELETE ON semantic.semantic_chunk
    FOR EACH ROW EXECUTE FUNCTION semantic.set_source_changed();
CREATE TRIGGER set_embedding_changed BEFORE INSERT OR UPDATE OR DELETE ON semantic.semantic_embedding
    FOR EACH ROW EXECUTE FUNCTION semantic.set_source_changed();

CREATE FUNCTION semantic.set_owner_changed() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP <> 'INSERT' THEN
        IF TG_TABLE_NAME = 'data_type' THEN
            PERFORM semantic.invalidate_set_owner(OLD.data_model_id);
        ELSE
            PERFORM semantic.invalidate_set_owner(OLD.id);
        END IF;
        IF TG_OP = 'DELETE' THEN
            DELETE FROM semantic.set_semantic_embedding WHERE set_id = OLD.id OR mauro_model_id = OLD.id;
            DELETE FROM semantic.set_semantic_index_state WHERE mauro_model_id = OLD.id;
        ELSIF TG_TABLE_NAME = 'data_type' THEN
            IF NEW.domain_type <> 'EnumerationType' THEN
                DELETE FROM semantic.set_semantic_embedding WHERE set_id = OLD.id AND set_domain_type = 'DataType';
            END IF;
        END IF;
    END IF;
    IF TG_OP <> 'DELETE' THEN
        IF TG_TABLE_NAME = 'data_type' THEN PERFORM semantic.invalidate_set_owner(NEW.data_model_id);
        ELSE PERFORM semantic.invalidate_set_owner(NEW.id);
        END IF;
    END IF;
    RETURN COALESCE(NEW, OLD);
END $$;
CREATE TRIGGER set_terminology_changed BEFORE INSERT OR UPDATE OR DELETE ON terminology.terminology
    FOR EACH ROW EXECUTE FUNCTION semantic.set_owner_changed();
CREATE TRIGGER set_codeset_changed BEFORE INSERT OR UPDATE OR DELETE ON terminology.code_set
    FOR EACH ROW EXECUTE FUNCTION semantic.set_owner_changed();
CREATE TRIGGER set_datatype_changed BEFORE INSERT OR UPDATE OR DELETE ON datamodel.data_type
    FOR EACH ROW EXECUTE FUNCTION semantic.set_owner_changed();
CREATE TRIGGER set_datamodel_changed BEFORE DELETE ON datamodel.data_model
    FOR EACH ROW EXECUTE FUNCTION semantic.set_owner_changed();

-- Profile IDs identify an embedding space. Reusing a populated ID for another model
-- would make both individual and derived vectors silently incompatible with queries.
CREATE FUNCTION semantic.protect_indexed_embedding_space() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF (NEW.provider, NEW.embedding_model, NEW.dimension, NEW.distance_metric)
       IS DISTINCT FROM (OLD.provider, OLD.embedding_model, OLD.dimension, OLD.distance_metric)
       AND (EXISTS (SELECT 1 FROM semantic.semantic_embedding WHERE embedding_profile_id = OLD.id)
            OR EXISTS (SELECT 1 FROM semantic.set_semantic_embedding WHERE embedding_profile_id = OLD.id)) THEN
        RAISE EXCEPTION 'Create a new embedding profile for a different model, version, dimension or distance metric'
            USING ERRCODE = '22023';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER protect_indexed_embedding_space BEFORE UPDATE ON semantic.embedding_profile
    FOR EACH ROW EXECUTE FUNCTION semantic.protect_indexed_embedding_space();
