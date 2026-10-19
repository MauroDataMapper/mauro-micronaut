create table datamodel."data_component" (
    "id"                              uuid primary key not null,
    "version"                         integer          not null,
    "date_created"                    timestamp with time zone,
    "last_updated"                    timestamp with time zone,
    "breadcrumb_tree_id"              uuid,
    "data_model_id"                   uuid             not null references datamodel.data_model(id) initially deferred,
    "idx"                             integer,
    "created_by"                      uuid,
    "type"                            varchar(255)     not null,
    "label"                           text             not null,
    "description"                     text,
    "aliases_string"                  text
);

create index "idx_data_component_data_model_id" on datamodel."data_component" (data_model_id);
create unique index "idx_data_component_path_identifier" on datamodel."data_component" (data_model_id, type, label);
