-- # Terminology
-- # Add terminology.code_set

create table if not exists terminology.code_set (
    "id"                              uuid primary key not null default uuid_generate_v4(),
    "version"                         integer          not null,
    "date_created"                    timestamp        not null default now(),
    "last_updated"                    timestamp        not null default now(),
    "readable_by_everyone"            boolean          not null,
    "readable_by_authenticated_users" boolean          not null,
    "label"                           text             not null,
    "description"                     text,
    "aliases_string"                  text,
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
    "authority_id"                    uuid             /*NOT NULL REFERENCES authority(id) initially deferred*/,
    "branch_name"                     varchar(255),
    "model_version"                   varchar(255),
    "model_version_tag"               varchar(255)
);
create unique index "idx_codeset_folder_id_label_branch_name_model_version" on terminology."code_set"(folder_id, label, branch_name, model_version);

create table if not exists terminology."code_set_term"
(
   "term_id" uuid not null,
    "code_set_id" uuid not null
);
create unique index "idx_code_set_term_id_unique" on terminology.code_set_term (term_id, code_set_id);
create index "idx_code_set_term_code_set_id" on terminology.code_set_term (code_set_id);
