
ALTER TABLE datamodel.data_class ADD COLUMN ts tsvector
    GENERATED ALWAYS AS
    (setweight(to_tsvector('english', coalesce(label, '')), 'A') ||
     setweight(to_tsvector('english', coalesce(description, '')), 'B')) STORED;

CREATE INDEX data_class_ts_idx ON datamodel.data_class USING GIN (ts);


ALTER TABLE datamodel.data_element ADD COLUMN ts tsvector
    GENERATED ALWAYS AS
        (setweight(to_tsvector('english', coalesce(label, '')), 'A') ||
         setweight(to_tsvector('english', coalesce(description, '')), 'B')) STORED;

CREATE INDEX data_element_ts_idx ON datamodel.data_element USING GIN (ts);

ALTER TABLE datamodel.data_model ADD COLUMN ts tsvector
    GENERATED ALWAYS AS
        (setweight(to_tsvector('english', coalesce(label, '')), 'A') ||
         setweight(to_tsvector('english', coalesce(description, '')), 'B')) STORED;

CREATE INDEX data_model_ts_idx ON datamodel.data_model USING GIN (ts);

ALTER TABLE datamodel.data_type ADD COLUMN ts tsvector
    GENERATED ALWAYS AS
        (setweight(to_tsvector('english', coalesce(label, '')), 'A') ||
         setweight(to_tsvector('english', coalesce(description, '')), 'B')) STORED;

CREATE INDEX data_type_ts_idx ON datamodel.data_type USING GIN (ts);

ALTER TABLE datamodel.enumeration_value ADD COLUMN ts tsvector
    GENERATED ALWAYS AS
        (setweight(to_tsvector('english', coalesce(key, '')), 'B') ||
         setweight(to_tsvector('english', coalesce(value, '')), 'A')) STORED;

CREATE INDEX enumeration_value_ts_idx ON datamodel.enumeration_value USING GIN (ts);

ALTER TABLE terminology.terminology ADD COLUMN ts tsvector
    GENERATED ALWAYS AS
        (setweight(to_tsvector('english', coalesce(label, '')), 'A') ||
         setweight(to_tsvector('english', coalesce(description, '')), 'B')) STORED;

CREATE INDEX terminology_ts_idx ON terminology.terminology USING GIN (ts);

ALTER TABLE terminology.code_set ADD COLUMN ts tsvector
    GENERATED ALWAYS AS
        (setweight(to_tsvector('english', coalesce(label, '')), 'A') ||
         setweight(to_tsvector('english', coalesce(description, '')), 'B')) STORED;

CREATE INDEX code_set_ts_idx ON terminology.code_set USING GIN (ts);

ALTER TABLE terminology.term ADD COLUMN ts tsvector
    GENERATED ALWAYS AS
        (setweight(to_tsvector('english', coalesce(label, '')), 'A') ||
         setweight(to_tsvector('english', coalesce(description, '')), 'B')) STORED;

CREATE INDEX term_ts_idx ON terminology.term USING GIN (ts);

ALTER TABLE core.metadata ADD COLUMN ts tsvector
    GENERATED ALWAYS AS
        (setweight(to_tsvector('english', coalesce(value, '')), 'C')) STORED;

CREATE INDEX metadata_ts_idx ON core.metadata USING GIN (ts);

ALTER TABLE core.annotation ADD COLUMN ts tsvector
    GENERATED ALWAYS AS
        (setweight(to_tsvector('english', coalesce(label, '')), 'C') ||
         setweight(to_tsvector('english', coalesce(description, '')), 'D')) STORED;

CREATE INDEX annotation_ts_idx ON core.annotation USING GIN (ts);

ALTER TABLE core.folder ADD COLUMN ts tsvector
    GENERATED ALWAYS AS
        (setweight(to_tsvector('english', coalesce(label, '')), 'A') ||
         setweight(to_tsvector('english', coalesce(description, '')), 'B')) STORED;

CREATE INDEX folder_ts_idx ON core.folder USING GIN (ts);