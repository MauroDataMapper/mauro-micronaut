-- # Core
create table if not exists core."summary_metadata" (
    "id"                              uuid primary key not null default uuid_generate_v4(),
    "version"                         integer          not null,
    "date_created"                    timestamp,
    "last_updated"                    timestamp,
    "multi_facet_aware_item_domain_type" varchar(255)  not null,
    "multi_facet_aware_item_id"       uuid             not null,
    "created_by"                      varchar(255),
    "summary_metadata_type"           varchar(255)     not null
);
create unique index "idx_summary_metadata_multi_facet_aware_item_id_summary_metadata_type" on "core"."summary_metadata" (multi_facet_aware_item_id, summary_metadata_type);

create table if not exists core.summary_metadata_report (
    "id"                              uuid primary key not null default uuid_generate_v4(),
    "version"                         integer          not null,
    "summary_metadata_type"           varchar(255)     not null,
    "date_created"                    timestamp,
    "last_updated"                    timestamp,
    "report_date"                     timestamp,
    "created_by"                      varchar(255),
    "report_value"                    text              not null,
    "summary_metadata_id"             uuid              not null references core.summary_metadata (id) initially deferred
);

