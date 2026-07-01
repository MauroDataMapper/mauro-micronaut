CREATE MATERIALIZED VIEW IF NOT EXISTS search.administered_item_context AS
WITH RECURSIVE transitive_links AS (
    SELECT semantic_link.multi_facet_aware_item_id AS source_id,
           semantic_link.multi_facet_aware_item_domain_type AS source_domain_type,
           semantic_link.target_multi_facet_aware_item_id AS target_id,
           semantic_link.target_multi_facet_aware_item_domain_type AS target_domain_type,
           semantic_link.link_type,
           1 AS relationship_depth,
           ARRAY[semantic_link.multi_facet_aware_item_id, semantic_link.target_multi_facet_aware_item_id] AS visited_ids,
           semantic_link.date_created,
           semantic_link.last_updated
    FROM core.semantic_link semantic_link
    WHERE semantic_link.link_type IN ('REFINES', 'ABSTRACTS')

    UNION ALL
    SELECT transitive.source_id,
           transitive.source_domain_type,
           next_link.target_multi_facet_aware_item_id AS target_id,
           next_link.target_multi_facet_aware_item_domain_type AS target_domain_type,
           transitive.link_type,
           transitive.relationship_depth + 1 AS relationship_depth,
           transitive.visited_ids || next_link.target_multi_facet_aware_item_id AS visited_ids,
           LEAST(transitive.date_created, next_link.date_created) AS date_created,
           GREATEST(transitive.last_updated, next_link.last_updated) AS last_updated
    FROM transitive_links transitive
         JOIN core.semantic_link next_link
           ON next_link.multi_facet_aware_item_id = transitive.target_id
          AND next_link.multi_facet_aware_item_domain_type = transitive.target_domain_type
          AND next_link.link_type = transitive.link_type
    WHERE NOT next_link.target_multi_facet_aware_item_id = ANY (transitive.visited_ids)
      AND transitive.relationship_depth < 8
)
SELECT metadata.multi_facet_aware_item_id AS source_id,
       metadata.multi_facet_aware_item_domain_type AS source_domain_type,
       'metadata-key-value'::varchar(255) AS context_kind,
       concat_ws(' ', 'Metadata', metadata.namespace, metadata.key, metadata.value) AS context_text,
       metadata.id AS context_id,
       NULL::varchar(255) AS relationship_type,
       NULL::uuid AS target_id,
       NULL::varchar(255) AS target_domain_type,
       0::integer AS relationship_depth,
       FALSE AS transitive,
       metadata.date_created,
       metadata.last_updated
FROM core.metadata metadata

UNION ALL
SELECT annotation.multi_facet_aware_item_id AS source_id,
       annotation.multi_facet_aware_item_domain_type AS source_domain_type,
       'annotation'::varchar(255) AS context_kind,
       concat_ws(' ', 'Annotation', annotation.label, annotation.description) AS context_text,
       annotation.id AS context_id,
       NULL::varchar(255) AS relationship_type,
       NULL::uuid AS target_id,
       NULL::varchar(255) AS target_domain_type,
       CASE WHEN annotation.parent_annotation_id IS NULL THEN 0 ELSE 1 END AS relationship_depth,
       FALSE AS transitive,
       annotation.date_created,
       annotation.last_updated
FROM core.annotation annotation

UNION ALL
SELECT classified.catalogue_item_id AS source_id,
       classified.catalogue_item_domain_type AS source_domain_type,
       'classification'::varchar(255) AS context_kind,
       concat_ws(
           ' ',
           'Classification',
           scheme.label,
           classifier.label,
           CASE WHEN parent_classifier.id IS NULL THEN NULL ELSE concat('parent classifier ', parent_classifier.label) END
       ) AS context_text,
       classifier.id AS context_id,
       'Classified By'::varchar(255) AS relationship_type,
       classifier.id AS target_id,
       'Classifier'::varchar(255) AS target_domain_type,
       CASE WHEN parent_classifier.id IS NULL THEN 1 ELSE 2 END AS relationship_depth,
       FALSE AS transitive,
       classified.date_created,
       classified.last_updated
FROM core.join_administered_item_to_classifier classified
     JOIN core.classifier classifier ON classifier.id = classified.classifier_id
     JOIN core.classification_scheme scheme ON scheme.id = classifier.classification_scheme_id
     LEFT JOIN core.classifier parent_classifier ON parent_classifier.id = classifier.parent_classifier_id

UNION ALL
SELECT semantic_link.multi_facet_aware_item_id AS source_id,
       semantic_link.multi_facet_aware_item_domain_type AS source_domain_type,
       ('semantic-link-' || lower(replace(semantic_link.link_type, '_', '-')))::varchar(255) AS context_kind,
       concat_ws(
           ' ',
           'Semantic link',
           semantic_link.link_type,
           semantic_link.target_multi_facet_aware_item_domain_type,
           target.label
       ) AS context_text,
       semantic_link.id AS context_id,
       semantic_link.link_type AS relationship_type,
       semantic_link.target_multi_facet_aware_item_id AS target_id,
       semantic_link.target_multi_facet_aware_item_domain_type AS target_domain_type,
       1::integer AS relationship_depth,
       FALSE AS transitive,
       semantic_link.date_created,
       semantic_link.last_updated
FROM core.semantic_link semantic_link
     LEFT JOIN search.search_domains target
       ON target.id = semantic_link.target_multi_facet_aware_item_id
      AND target.domain_type = semantic_link.target_multi_facet_aware_item_domain_type

UNION ALL
SELECT transitive.source_id,
       transitive.source_domain_type,
       ('semantic-link-transitive-' || lower(transitive.link_type))::varchar(255) AS context_kind,
       concat_ws(' ', 'Transitive semantic link', transitive.link_type, transitive.target_domain_type, target.label) AS context_text,
       transitive.target_id AS context_id,
       transitive.link_type AS relationship_type,
       transitive.target_id,
       transitive.target_domain_type,
       transitive.relationship_depth,
       TRUE AS transitive,
       transitive.date_created,
       transitive.last_updated
FROM transitive_links transitive
     LEFT JOIN search.search_domains target
       ON target.id = transitive.target_id
      AND target.domain_type = transitive.target_domain_type
WHERE transitive.relationship_depth > 1;

CREATE UNIQUE INDEX IF NOT EXISTS administered_item_context_pk
    ON search.administered_item_context (source_domain_type, source_id, context_kind, context_id, relationship_depth);

CREATE INDEX IF NOT EXISTS administered_item_context_source_idx
    ON search.administered_item_context (source_domain_type, source_id);

CREATE INDEX IF NOT EXISTS administered_item_context_target_idx
    ON search.administered_item_context (target_domain_type, target_id)
    WHERE target_id IS NOT NULL;
