-- # Core
create table if not exists core."summary_metadata" (
    "id"                              uuid primary key not null default uuid_generate_v4(),
    "version"                         integer          not null,
    "date_created"                    timestamp with time zone,
    "last_updated"                    timestamp with time zone,
    "multi_facet_aware_item_domain_type" varchar(255)  not null,
    "multi_facet_aware_item_id"       uuid             not null,
    "created_by"                      varchar(255),
    "summary_metadata_type"           varchar(255)     not null,
    "label"                           text             not null,
    "description"                     text
);
create unique index "idx_summary_metadata_multi_facet_aware_item_id_summary_metadata_type" on "core"."summary_metadata" (multi_facet_aware_item_id, label, summary_metadata_type);

create table if not exists core.summary_metadata_report (
    "id"                              uuid primary key not null default uuid_generate_v4(),
    "version"                         integer          not null,
    "date_created"                    timestamp with time zone,
    "last_updated"                    timestamp with time zone,
    "report_date"                     timestamp with time zone,
    "created_by"                      varchar(255),
    "report_value"                    text              not null,
    "summary_metadata_id"             uuid              not null references core.summary_metadata (id) initially deferred
);
create index "idx_summary_metadata_report_summary_metadata_id" on "core"."summary_metadata_report" (summary_metadata_id);

