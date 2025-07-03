-- # Core

-- Authority
create table core."authority"
(
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
    "url"                             varchar(255)     not null,
    "default_authority"               boolean          default false,
    "stable_id"                       uuid
);

-- Metadata
create table "core"."metadata"
(
    "id"                                 uuid primary key not null default gen_random_uuid(),
    "version"                            integer          not null,
    "date_created"                       timestamp with time zone,
    "last_updated"                       timestamp with time zone,
    "multi_facet_aware_item_domain_type" varchar(255)     not null,
    "multi_facet_aware_item_id"          uuid             not null,
    "created_by"                         uuid,
    "namespace"                          text             not null,
    "key"                                text             not null,
    "value"                              text             not null,
    "stable_id"                       uuid
);
create unique index "idx_metadata_multi_facet_aware_item_id_namespace_key" on "core"."metadata" (multi_facet_aware_item_id, namespace, key);

-- VersionLink
create table "core"."version_link"
(
    "id"                                 uuid primary key not null default gen_random_uuid(),
    "version"                            integer          not null,
    "date_created"                       timestamp with time zone,
    "last_updated"                       timestamp with time zone,
    "multi_facet_aware_item_domain_type" varchar(255)     not null,
    "multi_facet_aware_item_id"          uuid             not null,
    "created_by"                         uuid,
    "version_link_type"                  varchar(255)     not null,
    "target_model_id"                    uuid             not null,
    "target_model_domain_type"           varchar(255)     not null,
    "stable_id"                       uuid
);
create unique index "idx_version_link_multi_facet_aware_item_id_target_model_id" on "core"."version_link" (multi_facet_aware_item_id, target_model_id);

-- Folder
create table core."folder"
(
    "id"                              uuid primary key not null default gen_random_uuid(),
    "version"                         integer          not null,
    "date_created"                    timestamp with time zone,
    "last_updated"                    timestamp with time zone,
    "readable_by_everyone"            boolean          not null,
    "readable_by_authenticated_users" boolean          not null,
    "label"                           text             not null,
    "description"                     text,
    "created_by"                      uuid,
    "finalised"                       boolean          not null,
    "date_finalised"                  timestamp with time zone,
    "documentation_version"           varchar(255),
    "organisation"                    varchar(255),
    "author"                          varchar(255),
    "deleted"                         boolean          not null,
    "authority_id"                    uuid /*NOT NULL REFERENCES authority(id) initially deferred*/,
    "branch_name"                     varchar(255),
    "model_version"                   varchar(255),
    "model_version_tag"               varchar(255),
    "parent_folder_id"                uuid,
    "class"                           varchar(255),
    "stable_id"                       uuid
);
create index "idx_folder_parent_folder_id" on core."folder" (parent_folder_id);

-- # Security

create table security."catalogue_user"
(
    "id"               uuid primary key not null default gen_random_uuid(),
    "version"          integer          not null,
    "date_created"     timestamp with time zone,
    "last_updated"     timestamp with time zone,
    "created_by"       uuid,
    "email_address"    varchar(255)     not null,
    "first_name"       varchar(255)     not null,
    "last_name"        varchar(255)     not null,
    "job_title"        varchar(255),
    "organisation"     varchar(255),
    "pending"          boolean          not null,
    "disabled"         boolean          not null,
    "profile_picture"  varchar(255),
    "user_preferences" text,
    "reset_token"      uuid,
    "creation_method"  varchar(255)     not null,
    "last_login"       timestamp with time zone,
    "salt"             bytea            not null,
    "password"         bytea,
    "temp_password"    varchar(255),
    "stable_id"                       uuid
);
create unique index "idx_catalogue_user_email_address" on "security"."catalogue_user" (trim(lower(email_address)));

create table security."user_group"
(
    "id"               uuid primary key not null default gen_random_uuid(),
    "version"          integer          not null,
    "date_created"     timestamp with time zone,
    "last_updated"     timestamp with time zone,
    "created_by"       uuid,
    "name"             varchar(255)     not null,
    "description"      text,
    "undeletable"      boolean                   default false,
    "application_role" varchar(255),
    "stable_id"                       uuid
);
create unique index "idx_security_user_group_name" on security.user_group (trim(lower(name)));

create table security."securable_resource_group_role"
(
    "id"                             uuid primary key not null default gen_random_uuid(),
    "version"                        integer          not null,
    "date_created"                   timestamp with time zone,
    "last_updated"                   timestamp with time zone,
    "created_by"                     uuid,
    "securable_resource_domain_type" varchar(255)     not null,
    "securable_resource_id"          uuid             not null,
    "user_group_id"                  uuid             not null references security.user_group (id) initially deferred,
    "role"                           varchar(255)     not null,
    "stable_id"                       uuid
);
create unique index "idx_securable_resource_group_role_securable_resource_user_group_id" on security.securable_resource_group_role (securable_resource_domain_type, securable_resource_id, user_group_id);

create table security."user_group_catalogue_user"
(
    "catalogue_user_id" uuid not null,
    "user_group_id" uuid not null
);
create unique index "idx_user_group_catalogue_user_unique" on security.user_group_catalogue_user (catalogue_user_id, user_group_id);
create index "idx_user_group_catalogue_user_user_group_id" on security.user_group_catalogue_user (user_group_id);

-- # Terminology

create table terminology."terminology"
(
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
    "authority_id"                    uuid /*NOT NULL REFERENCES authority(id) initially deferred*/,
    "branch_name"                     varchar(255),
    "model_version"                   varchar(255),
    "model_version_tag"               varchar(255),
    "stable_id"                       uuid
);
create unique index "idx_terminology_folder_id_label_branch_name_model_version" on terminology."terminology" (folder_id, label, branch_name, model_version);

create table terminology."term_relationship_type"
(
    "id"                    uuid primary key not null default gen_random_uuid(),
    "version"               integer          not null,
    "date_created"          timestamp with time zone,
    "last_updated"          timestamp with time zone,
    "label"                 text             not null,
    "description"           text,
    "aliases_string"        text,
    "created_by"            uuid,
    "breadcrumb_tree_id"    uuid,
    "idx"                   integer,
    "terminology_id"        uuid             not null references terminology.terminology (id) initially deferred,
    "parental_relationship" boolean,
    "child_relationship"    boolean,
    "stable_id"             uuid
);
create unique index "idx_term_relationship_type_terminology_id_label" on terminology."term_relationship_type" (terminology_id, label);


create table terminology."term"
(
    "id"                 uuid primary key not null default gen_random_uuid(),
    "version"            integer          not null,
    "date_created"       timestamp with time zone,
    "last_updated"       timestamp with time zone,
    "label"              text             not null,
    "description"        text,
    "aliases_string"     text,
    "created_by"         uuid,
    "breadcrumb_tree_id" uuid,
    "idx"                integer,
    "terminology_id"     uuid             not null references terminology.terminology (id) initially deferred,
    "code"               varchar(255)     not null,
    "definition"         text             not null,
    "url"                varchar(255),
    "is_parent"          boolean,
    "depth"              integer,
    "stable_id"          uuid
);
create unique index "idx_term_terminology_id_code" on terminology."term" (terminology_id, code);

create table terminology."term_relationship"
(
    "id"                   uuid primary key not null default gen_random_uuid(),
    "version"              integer          not null,
    "date_created"         timestamp with time zone,
    "last_updated"         timestamp with time zone,
    "label"                text             not null,
    "description"          text,
    "aliases_string"       text,
    "created_by"           uuid,
    "breadcrumb_tree_id"   uuid,
    "idx"                  integer,
    "terminology_id"       uuid             not null references terminology.terminology (id) initially deferred,
    "source_term_id"       uuid             not null references terminology.term (id) initially deferred,
    "target_term_id"       uuid             not null references terminology.term (id) initially deferred,
    "relationship_type_id" uuid             not null references terminology.term_relationship_type (id) initially deferred,
    "stable_id"            uuid
);

create index "idx_term_relationship_terminology_id" on terminology."term_relationship" (terminology_id);
create index "idx_term_relationship_source_term_id" on terminology."term_relationship" (source_term_id);
create index "idx_term_relationship_target_term_id" on terminology."term_relationship" (target_term_id);
create index "idx_term_relationship_relationship_type_id" on terminology."term_relationship" (relationship_type_id);

-- # DataModel

create table datamodel."data_model" (
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
    "folder_id"                       uuid             not null references core.folder(id) initially deferred,
    "authority_id"                    uuid /*NOT NULL REFERENCES authority(id) initially deferred*/,
    "branch_name"                     varchar(255),
    "model_version"                   varchar(255),
    "model_version_tag"               varchar(255),
    "stable_id"                       uuid
);


create index "idx_data_model_folder_id" on datamodel."data_model"(folder_id);



create table datamodel."data_class" (
    "id"                              uuid primary key not null default gen_random_uuid(),
    "version"                         integer          not null,
    "date_created"                    timestamp with time zone,
    "last_updated"                    timestamp with time zone,
    "breadcrumb_tree_id"              uuid,
    "data_model_id"                   uuid             not null references datamodel.data_model(id) initially deferred,
    "parent_data_class_id"            uuid             null references datamodel.data_class(id) initially deferred,
    "idx"                             integer,          /* not null */
    "created_by"                      uuid,
    "label"                           text             not null,
    "description"                     text,
    "aliases_string"                  text,
    "min_multiplicity"                integer,
    "max_multiplicity"                integer,
    "stable_id"                       uuid
);

create index "idx_data_class_data_model_id" on datamodel."data_class"(data_model_id);
create index "idx_data_class_parent_data_class_id" on datamodel."data_class"(parent_data_class_id);

create table datamodel."data_type" (
    "id"                              uuid primary key not null default gen_random_uuid(),
    "version"                         integer          not null,
    "date_created"                    timestamp with time zone,
    "last_updated"                    timestamp with time zone,
    "domain_type"                     varchar(15)      not null,
    "breadcrumb_tree_id"              uuid,
    "data_model_id"                   uuid             not null references datamodel.data_model(id) initially deferred,
    "idx"                             integer,          /* not null */
    "created_by"                      uuid,
    "label"                           text             not null,
    "description"                     text,
    "aliases_string"                  text,
    "units"                           varchar(255),
    "reference_class_id"              uuid             references datamodel.data_class(id) initially deferred,
    "model_resource_id"               uuid,
    "model_resource_domain_type"      varchar(255),
    "stable_id"                       uuid
);

create index "idx_data_type_data_model_id" on datamodel."data_type"(data_model_id);
create index "idx_data_type_reference_class_id_not_null" on datamodel."data_type"(reference_class_id) where reference_class_id is not null;

create table datamodel."data_element" (
    "id"                              uuid primary key not null default gen_random_uuid(),
    "version"                         integer          not null,
    "date_created"                    timestamp with time zone,
    "last_updated"                    timestamp with time zone,
    "breadcrumb_tree_id"              uuid,
    "data_class_id"                   uuid             not null references datamodel.data_class(id) initially deferred,
    "data_type_id"                    uuid             not null references datamodel.data_type(id) initially deferred,
    "idx"                             integer,          /* not null */
    "created_by"                      uuid,
    "label"                           text             not null,
    "description"                     text,
    "aliases_string"                  text,
    "min_multiplicity"                integer,
    "max_multiplicity"                integer,
    "stable_id"                       uuid
);
create index "idx_data_element_data_class_id" on datamodel."data_element"(data_class_id);
create index "idx_data_element_data_type_id" on datamodel."data_element"(data_type_id);


create table datamodel."enumeration_value" (
    "id"                              uuid primary key not null default gen_random_uuid(),
    "version"                         integer          not null,
    "date_created"                    timestamp with time zone,
    "last_updated"                    timestamp with time zone,
    "breadcrumb_tree_id"              uuid,
    "enumeration_type_id"             uuid             not null references datamodel.data_type(id) initially deferred,
    "idx"                             integer,          /* not null */
    "created_by"                      uuid,
    "label"                           text             not null,
    "description"                     text,
    "aliases_string"                  text,
    "category"                        text,
    "key"                             text             not null,
    "value"                           text,
    "stable_id"                       uuid
);

create index "idx_enumeration_value_enumeration_type_id" on datamodel."enumeration_value"(enumeration_type_id);

create table datamodel."join_dataclass_to_extended_data_class" (
    "dataclass_id"                    uuid            not null references datamodel.data_class(id),
    "extended_dataclass_id"           uuid            not null references datamodel.data_class(id)
);

create index "idx_join_dataclass_to_extended_data_class_id" on datamodel."join_dataclass_to_extended_data_class"(dataclass_id);
create index "idx_join_dataclass_to_extended_data_class_extended_dataclass_id" on datamodel."join_dataclass_to_extended_data_class"(extended_dataclass_id);

create table "core"."edit" (
    "id"                                 uuid primary key         not null default gen_random_uuid(),
    "version"                            integer                  not null,
    "date_created"                       timestamp with time zone not null,
    "last_updated"                       timestamp with time zone not null,
    "multi_facet_aware_item_domain_type" varchar(255)             not null,
    "multi_facet_aware_item_id"          uuid                     not null,
    "created_by"                         uuid                     not null,
    "description"                        text                     not null,
    "title"                              varchar(255)             not null,
    "stable_id"                       uuid
);

create table "core"."semantic_link"
(
    "id"                                 uuid primary key not null default gen_random_uuid(),
    "version"                            integer          not null,
    "date_created"                       timestamp with time zone,
    "last_updated"                       timestamp with time zone,
    "multi_facet_aware_item_domain_type" varchar(255)     not null,
    "multi_facet_aware_item_id"          uuid             not null,
    "created_by"                         uuid,
    "link_type"                 varchar(255)     not null,
    "target_multi_facet_aware_item_id"   uuid             not null,
    "target_multi_facet_aware_item_domain_type" varchar(255)     not null,
    "unconfirmed"                        boolean          not null,
    "stable_id"                       uuid
);
