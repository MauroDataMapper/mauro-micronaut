DROP VIEW IF EXISTS core.search_domains;

DROP AGGREGATE IF EXISTS core.tsvector_agg(tsvector);

/* Create a generalised aggregation function to take a set of tsvectors and combine them into a single tsvector */
CREATE AGGREGATE search.tsvector_agg(tsvector) (
    STYPE = pg_catalog.tsvector,
    SFUNC = pg_catalog.tsvector_concat,
    INITCOND = ''
    );


CREATE MATERIALIZED VIEW search.metadata_agg AS
SELECT
    multi_facet_aware_item_id,
    search.tsvector_agg(ts) AS metadata_ts
FROM core.metadata
GROUP BY multi_facet_aware_item_id;

CREATE UNIQUE INDEX metadata_agg_pk
    ON search.metadata_agg (multi_facet_aware_item_id);

CREATE INDEX metadata_agg_ts_gin
    ON search.metadata_agg
    USING GIN (metadata_ts);


CREATE MATERIALIZED VIEW search.search_domains AS
SELECT
    sd.id,
    sd.domain_type,
    sd.label,
    sd.description,
    sd.date_created,
    sd.last_updated,
    sd.model_id,
    sd.ts,
    ma.metadata_ts,
    tsvector_concat(
            COALESCE(ma.metadata_ts, ''::tsvector),
            sd.ts
    ) AS combined_ts

FROM ( SELECT data_type.id,
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
          FROM core.folder) sd
     LEFT JOIN search.metadata_agg ma
               ON ma.multi_facet_aware_item_id = sd.id;

-- required for concurrent refresh
CREATE UNIQUE INDEX search_domains_pk
    ON search.search_domains (domain_type, id);

-- single GIN index for search
CREATE INDEX search_domains_combined_ts_gin
    ON search.search_domains
    USING GIN (combined_ts);

-- filters
CREATE INDEX search_domains_model_id_idx
    ON search.search_domains (model_id);

CREATE INDEX search_domains_domain_type_idx
    ON search.search_domains (domain_type);

CREATE INDEX search_domains_date_created_idx
    ON search.search_domains (date_created);

CREATE INDEX search_domains_last_updated_idx
    ON search.search_domains (last_updated);

CREATE TABLE search.search_domains_dirty_flag (
    id BOOLEAN PRIMARY KEY DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    last_updated TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);