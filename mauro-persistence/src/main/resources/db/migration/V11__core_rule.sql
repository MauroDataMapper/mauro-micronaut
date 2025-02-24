-- # Core
create table if not exists core."rule" (
    "id"                                 uuid primary key not null default uuid_generate_v4(),
    "version"                            integer          not null,
    "date_created"                       timestamp,
    "last_updated"                       timestamp,
    "multi_facet_aware_item_domain_type" varchar(255)  not null,
    "multi_facet_aware_item_id"          uuid             not null,
    "created_by"                         varchar(255),
    "name"                               text             not null,
    "description"                        text
);
create unique index "idx_rule_multi_facet_aware_item_id_rule" on "core"."rule" (multi_facet_aware_item_id, name);

create table if not exists core.rule_representation (
    "id"                                 uuid primary key not null default uuid_generate_v4(),
    "version"                            integer          not null,
    "date_created"                       timestamp,
    "last_updated"                       timestamp,
    "created_by"                         varchar(255),
    "language"                           varchar(255)      not null,
    "representation"                     text,
    "rule_id"                            uuid              not null references core.rule (id) initially deferred
);
create index "idx_rule_representation_rule_id" on "core"."rule_representation" (rule_id);

