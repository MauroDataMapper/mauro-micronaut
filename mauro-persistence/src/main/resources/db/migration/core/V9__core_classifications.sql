-- # Core classification_scheme
-- # Core classifier
-- # Core join_administered_item_to_classifier

create table if not exists core."classification_scheme" (
    "id"                              uuid primary key not null default gen_random_uuid(),
    "version"                         integer          not null,
    "date_created"                    timestamp with time zone,
    "last_updated"                    timestamp with time zone,
    "readable_by_everyone"            boolean          not null,
    "readable_by_authenticated_users" boolean          not null,
    "label"                           text             not null,
    "description"                     text,
    "aliases_string"                  text,
    "created_by"                      uuid,
    "breadcrumb_tree_id"              uuid,
    "finalised"                       boolean          not null,
    "date_finalised"                  timestamp with time zone,
    "documentation_version"           varchar(255),
    "model_type"                      varchar(255)     not null,
    "organisation"                    varchar(255),
    "deleted"                         boolean          not null,
    "author"                          varchar(255),
    "folder_id"                       uuid             not null references core.folder (id) initially deferred,
    "authority_id"                    uuid,
    "branch_name"                     varchar(255),
    "model_version"                   varchar(255),
    "model_version_tag"               varchar(255),
    "stable_id"                       uuid
);

create table if not exists core."classifier" (
    "id"                              uuid             primary key not null default gen_random_uuid(),
     "version"                         integer          not null,
    "date_created"                    timestamp with time zone,
    "last_updated"                    timestamp with time zone,
    "idx"                             integer,
    "label"                           text             not null,
    "description"                     text,
    "aliases_string"                  text,
    "created_by"                      uuid,
    "classification_scheme_id"        uuid             not null references core.classification_scheme(id) initially deferred,
    "parent_classifier_id"            uuid             null references core.classifier(id) initially deferred,
    "stable_id"                       uuid
);

create index "idx_classifier_classification_scheme_id" on core."classifier" (classification_scheme_id);
create index "idx_classifier_parent_classifier_id" on core."classifier" (parent_classifier_id);

create table if not exists core.join_administered_item_to_classifier (
    "date_created"                    timestamp        not null default now(),
    "last_updated"                    timestamp        not null default now(),
    "catalogue_item_id"               uuid             not null,
    "catalogue_item_domain_type"      varchar(255)     not null,
    "classifier_id"                   uuid             not null references core.classifier(id) initially deferred
);

create index "idx_join_administered_item_id_to_classifier" on "core"."join_administered_item_to_classifier" (catalogue_item_id);
create index "idx_join_administered_item_to_classifier_id" on "core"."join_administered_item_to_classifier" (classifier_id);
create unique index "idx_join_admin_item_id_domainType_classifier_id" on "core"."join_administered_item_to_classifier" (catalogue_item_id, catalogue_item_domain_type, classifier_id);

