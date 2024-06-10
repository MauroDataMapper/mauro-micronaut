
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

create view search_domains(id, domain_type, label, description, ts, date_created, last_updated, model_id) as
(SELECT data_type.id,
       'DataType'::text AS domain_type,
       data_type.label,
       data_type.description,
       data_type.ts,
       data_type.date_created,
       data_type.last_updated,
       data_type.data_model_id as model_id
FROM datamodel.data_type
UNION
SELECT data_class.id,
       'DataClass'::text AS domain_type,
       data_class.label,
       data_class.description,
       data_class.ts,
       data_class.date_created,
       data_class.last_updated,
       data_class.data_model_id as model_id
FROM datamodel.data_class
UNION
(SELECT data_element.id,
        'DataElement'::text AS domain_type,
        data_element.label,
        data_element.description,
        data_element.ts,
        data_element.date_created,
        data_element.last_updated,
        data_class.data_model_id as model_id
 FROM datamodel.data_element
          inner join datamodel.data_class on data_element.data_class_id = data_class.id)
UNION
SELECT data_model.id,
       'DataModel'::text AS domain_type,
       data_model.label,
       data_model.description,
       data_model.ts,
       data_model.date_created,
       data_model.last_updated,
       data_model.id as model_id
FROM datamodel.data_model
UNION
(SELECT enumeration_value.id,
        'EnumerationValue'::text AS domain_type,
        enumeration_value.label,
        enumeration_value.description,
        enumeration_value.ts,
        enumeration_value.date_created,
        enumeration_value.last_updated,
        data_type.data_model_id as model_id
 FROM datamodel.enumeration_value
          inner join datamodel.data_type on enumeration_value.enumeration_type_id = data_type.id)
UNION
SELECT terminology.id,
       'Terminology'::text AS domain_type,
       terminology.label,
       terminology.description,
       terminology.ts,
       terminology.date_created,
       terminology.last_updated,
       terminology.id as model_id
FROM terminology.terminology
UNION
SELECT term.id,
       'Term'::text AS domain_type,
       term.label,
       term.description,
       term.ts,
       term.date_created,
       term.last_updated,
       term.terminology_id as model_id
FROM terminology.term
UNION
SELECT code_set.id,
       'CodeSet'::text AS domain_type,
       code_set.label,
       code_set.description,
       code_set.ts,
       code_set.date_created,
       code_set.last_updated,
       code_set.id as model_id
FROM terminology.code_set
UNION
SELECT folder.id,
       'Folder'::text AS domain_type,
       folder.label,
       folder.description,
       folder.ts,
       folder.date_created,
       folder.last_updated,
       null as model_id
FROM core.folder)
;


