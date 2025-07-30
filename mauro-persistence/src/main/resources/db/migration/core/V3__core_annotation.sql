-- # Core
create table if not exists core."annotation" (
    "id"                              uuid primary key not null default gen_random_uuid(),
    "version"                         integer          not null,
    "date_created"                    timestamp  with time zone,
    "last_updated"                    timestamp  with time zone,
    "multi_facet_aware_item_domain_type" varchar(255)  not null,
    "multi_facet_aware_item_id"       uuid             not null,
    "parent_annotation_id"            uuid,
    "created_by"                      text,
    "label"                           text,
    "description"                     text,
    "stable_id"                       uuid
);
create index "idx_annotation_multi_facet_aware_item_id" on "core"."annotation" (multi_facet_aware_item_id);

create index "idx_annotation_parent_annodation_id" on "core"."annotation" (parent_annotation_id);


