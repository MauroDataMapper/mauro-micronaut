-- # Core

-- Authority
create table core."authority" (
    "id"                              uuid primary key not null default uuid_generate_v4(),
    "version"                         integer          not null,
    "date_created"                    timestamp,
    "last_updated"                    timestamp,
    "readable_by_everyone"            boolean          not null,
    "readable_by_authenticated_users" boolean          not null,
    "label"                           text             not null,
    "description"                     text,
    "aliases_string"                  text,
    "created_by"                      varchar(255),
    "breadcrumb_tree_id"              uuid,
    "url"                             varchar(255)     not null
);

-- Metadata
create table "core"."metadata" (
    "id"                                 uuid primary key not null default uuid_generate_v4(),
    "version"                            integer          not null,
    "date_created"                       timestamp,
    "last_updated"                       timestamp,
    "multi_facet_aware_item_domain_type" varchar(255)     not null,
    "multi_facet_aware_item_id"          uuid             not null,
    "created_by"                         varchar(255),
    "namespace"                          text             not null,
    "key"                                text             not null,
    "value"                              text             not null
);
create unique index "idx_metadata_multi_facet_aware_item_id_namespace_key" on "core"."metadata"(multi_facet_aware_item_id, namespace, key);


-- Folder
create table core."folder" (
    "id"                              uuid primary key not null default uuid_generate_v4(),
    "version"                         integer          not null,
    "date_created"                    timestamp,
    "last_updated"                    timestamp,
    "readable_by_everyone"            boolean          not null,
    "readable_by_authenticated_users" boolean          not null,
    "label"                           text             not null,
    "description"                     text,
    "created_by"                      varchar(255),
    "finalised"                       boolean          not null,
    "date_finalised"                  timestamp,
    "documentation_version"           varchar(255),
    "deleted"                         boolean          not null,
    "authority_id"                    uuid /*NOT NULL REFERENCES authority(id) initially deferred*/,
    "branch_name"                     varchar(255),
    "model_version"                   varchar(255),
    "model_version_tag"               varchar(255),
    "parent_folder_id"                uuid,
    "class"                           varchar(255)
);
create index "idx_folder_parent_folder_id" on core."folder"(parent_folder_id);


-- # Terminology

create table terminology."terminology" (
    "id"                              uuid primary key not null default uuid_generate_v4(),
    "version"                         integer          not null,
    "date_created"                    timestamp,
    "last_updated"                    timestamp,
    "readable_by_everyone"            boolean          not null,
    "readable_by_authenticated_users" boolean          not null,
    "label"                           varchar(255)     not null,
    "description"                     varchar(255),
    "aliases_string"                  varchar(255),
    "created_by"                      varchar(255),
    "breadcrumb_tree_id"              uuid,
    "finalised"                       boolean          not null,
    "date_finalised"                  timestamp,
    "documentation_version"           varchar(255),
    "model_type"                      varchar(255)     not null,
    "organisation"                    varchar(255),
    "deleted"                         boolean          not null,
    "author"                          varchar(255),
    "folder_id"                       uuid             not null references core.folder(id) initially deferred,
    "authority_id"                    uuid /*NOT NULL REFERENCES authority(id) initially deferred*/,
    "branch_name"                     varchar(255),
    "model_version"                   varchar(255),
    "model_version_tag"               varchar(255)
);
create unique index "idx_terminology_folder_id_label_branch_name_model_version" on terminology."terminology"(folder_id, label, branch_name, model_version);

create table terminology."term_relationship_type" (
    "id"                    uuid primary key not null default uuid_generate_v4(),
    "version"               integer          not null,
    "date_created"          timestamp,
    "last_updated"          timestamp,
    "label"                 text             not null,
    "description"           text,
    "aliases_string"        varchar(255),
    "created_by"            varchar(255),
    "breadcrumb_tree_id"    uuid,
    "idx"                   integer,
    "terminology_id"        uuid             not null references terminology.terminology(id) initially deferred,
    "parental_relationship" boolean,
    "child_relationship"    boolean
);
create unique index "idx_term_relationship_type_terminology_id_label" on terminology."term_relationship_type"(terminology_id, label);


create table terminology."term" (
    "id"                 uuid primary key not null default uuid_generate_v4(),
    "version"            integer          not null,
    "date_created"       timestamp,
    "last_updated"       timestamp,
    "label"              text             not null,
    "description"        text,
    "aliases_string"     varchar(255),
    "created_by"         varchar(255),
    "breadcrumb_tree_id" uuid,
    "idx"                integer,
    "terminology_id"     uuid             not null references terminology.terminology(id) initially deferred,
    "code"               varchar(255)     not null,
    "definition"         text             not null,
    "url"                varchar(255),
    "is_parent"          boolean,
    "depth"              integer
);
create unique index "idx_term_terminology_id_code" on terminology."term"(terminology_id, code);

create table terminology."term_relationship" (
    "id"                   uuid primary key not null default uuid_generate_v4(),
    "version"              integer          not null,
    "date_created"         timestamp,
    "last_updated"         timestamp,
    "label"                text             not null,
    "description"          text,
    "aliases_string"       varchar(255),
    "created_by"           varchar(255),
    "breadcrumb_tree_id"   uuid,
    "idx"                  integer,
    "terminology_id"       uuid             not null references terminology.terminology(id) initially deferred,
    "source_term_id"       uuid             not null references terminology.term(id) initially deferred,
    "target_term_id"       uuid             not null references terminology.term(id) initially deferred,
    "relationship_type_id" uuid             not null references terminology.term_relationship_type(id) initially deferred
);
create index "idx_term_relationship_terminology_id" on terminology."term_relationship"(terminology_id);
create index "idx_term_relationship_source_term_id" on terminology."term_relationship"(source_term_id);
create index "idx_term_relationship_target_term_id" on terminology."term_relationship"(target_term_id);
create index "idx_term_relationship_relationship_type_id" on terminology."term_relationship"(relationship_type_id);

-- Test tables
create table "test" (
    "id"                              uuid primary key not null default uuid_generate_v4(),
    "version"                         integer          not null,
    "date_created"                    timestamp,
    "last_updated"                    timestamp,
    "readable_by_everyone"            boolean          not null,
    "readable_by_authenticated_users" boolean          not null,
    "label"                           varchar(255)     not null,
    "description"                     varchar(255),
    "aliases_string"                  varchar(255),
    "created_by"                      varchar(255),
    "breadcrumb_tree_id"              uuid,
    "finalised"                       boolean          not null,
    "date_finalised"                  timestamp,
    "documentation_version"           varchar(255),
    "model_type"                      varchar(255)     not null,
    "organisation"                    varchar(255),
    "deleted"                         boolean          not null,
    "author"                          varchar(255),
    "folder_id"                       uuid             not null,
    "authority_id"                    uuid,
    "branch_name"                     varchar(255),
    "model_version"                   varchar(255),
    "model_version_tag"               varchar(255)
);
create table "test_term" (
    "test_id" uuid not null,
    "term_id" uuid not null
);
