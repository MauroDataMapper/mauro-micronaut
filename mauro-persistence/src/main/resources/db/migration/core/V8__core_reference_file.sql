-- # Core
create table if not exists core."reference_file" (
    "id"                              uuid primary key not null default gen_random_uuid(),
    "version"                         integer          not null,
    "date_created"                    timestamp with time zone,
    "last_updated"                    timestamp with time zone,
    "multi_facet_aware_item_domain_type" varchar(255)  not null,
    "multi_facet_aware_item_id"       uuid             not null,
    "file_size"                       integer          not null,
    "file_type"                       varchar(255)     not null,
    "file_name"                       varchar(255)     not null,
    "file_contents"                   bytea            not null,
    "created_by"                      text,
    "stable_id"                       uuid
);
create index "idx_reference_file_multi_facet_aware_item_id" on "core"."reference_file" (multi_facet_aware_item_id);



