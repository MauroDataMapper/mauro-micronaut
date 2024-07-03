create table if not exists "dataflow"."data_flow" (
    "id"                              uuid primary key not null default uuid_generate_v4(),
    "version"                         integer          not null,
    "date_created"                    timestamp,
    "last_updated"                    timestamp,
    "label"                           text             not null,
    "idx"                             integer,
    "source_id"                       uuid,
    "target_id"                       uuid,
    "description"                     text,
    "created_by"                      uuid,
    "aliases_string"                  text,
    "definition"                      text,
    "diagram_layout"                  text
);

create table if not exists "dataflow"."data_class_component" (
    "id"                              uuid primary key not null default uuid_generate_v4(),
    "version"                         integer       not null,
    "date_created"                    timestamp,
    "last_updated"                    timestamp,
    "data_flow_id"                    uuid not null references dataflow.data_flow(id) initially deferred,
    "idx"                             integer,
    "label"                           text             not null,
    "description"                     text,
    "aliases_string"                  text,
    "created_by"                      uuid,
    "definition"                      text
);

create index "idx_data_class_component_data_flow_id" on "dataflow"."data_class_component" (data_flow_id);

create table if not exists "dataflow"."data_element_component" (
    "id"                              uuid primary key not null default uuid_generate_v4(),
    "version"                         integer          not null,
    "date_created"                    timestamp,
    "last_updated"                    timestamp,
    "data_class_component_id"         uuid  not null references dataflow.data_class_component (id) initially deferred,
    "idx"                             integer,
    "label"                           text             not null,
    "description"                     text,
    "aliases_string"                  text,
    "created_by"                      uuid,
    "definition"                      text
);

create index "idx_data_element_component_data_class_component_id" on "dataflow"."data_element_component" (data_class_component_id);

create table if not exists "dataflow"."data_class_component_target_data_class"
(
    "data_class_component_id"         uuid not null references dataflow.data_class_component (id) initially deferred,
    "data_class_id"                   uuid not null references datamodel.data_class (id) initially deferred
);

create unique index "idx_data_class_component_target_data_class_id_unique" on dataflow.data_class_component_target_data_class (data_class_component_id, data_class_id);
create index "idx_data_class_component_target_data_class_id" on  dataflow.data_class_component_target_data_class (data_class_component_id);

create table if not exists "dataflow"."data_class_component_source_data_class"
(
    "data_class_component_id"         uuid not null references dataflow.data_class_component (id) initially deferred,
    "data_class_id"                   uuid not null references datamodel.data_class (id) initially deferred
);

create unique index "idx_data_class_component_source_data_class_id_unique" on dataflow.data_class_component_source_data_class (data_class_component_id, data_class_id);
create index "idx_data_class_component_source_data_class_id" on  dataflow.data_class_component_source_data_class (data_class_component_id);

create table if not exists "dataflow"."data_element_component_target_data_element"
(
    "data_element_component_id"         uuid not null references dataflow.data_element_component (id) initially deferred,
    "data_element_id"                   uuid not null references datamodel.data_element (id) initially deferred
);
create unique index "idx_data_element_component_target_data_element_id_unique" on dataflow.data_element_component_target_data_element (data_element_component_id, data_element_id);
create index "idx_data_element_component_target_data_element_id" on  dataflow.data_element_component_target_data_element (data_element_component_id);

create table if not exists "dataflow"."data_element_component_source_data_element"
(
    "data_element_component_id"         uuid not null references dataflow.data_element_component (id) initially deferred,
    "data_element_id"                   uuid not null references datamodel.data_element (id) initially deferred
);
create unique index "idx_data_element_component_source_data_element_id_unique" on dataflow.data_element_component_source_data_element (data_element_component_id, data_element_id);
create index "idx_data_element_component_source_data_element_id" on  dataflow.data_element_component_source_data_element (data_element_component_id);