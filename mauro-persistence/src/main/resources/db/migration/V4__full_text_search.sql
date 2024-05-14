
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

/* Create a generalised aggregation function to take a set of tsvectors and combine them into a single tsvector */
CREATE AGGREGATE core.tsvector_agg(tsvector) (
    STYPE = pg_catalog.tsvector,
    SFUNC = pg_catalog.tsvector_concat,
    INITCOND = ''
    );

create view core.search_domains as (
    select id, 'DataType' as domain_type, label, description, ts, date_created, last_updated FROM datamodel.data_type
    union
    select id, 'DataClass' as domain_type, label, description, ts, date_created, last_updated FROM datamodel.data_class
    union
    select id, 'DataElement' as domain_type, label, description, ts, date_created, last_updated FROM datamodel.data_element
    union
    select id, 'DataModel' as domain_type, label, description, ts, date_created, last_updated FROM datamodel.data_model
    union
    select id, 'EnumerationValue' as domain_type, label, description, ts, date_created, last_updated FROM datamodel.enumeration_value
    union
    select id, 'Terminology' as domain_type, label, description, ts, date_created, last_updated FROM terminology.terminology
    union
    select id, 'Term' as domain_type, label, description, ts, date_created, last_updated FROM terminology.term
    union
    select id, 'CodeSet' as domain_type, label, description, ts, date_created, last_updated FROM terminology.code_set
    union
    select id, 'Folder' as domain_type, label, description, ts, date_created, last_updated FROM core.folder
);