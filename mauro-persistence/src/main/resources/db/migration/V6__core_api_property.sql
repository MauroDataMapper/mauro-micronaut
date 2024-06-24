-- API Property
create table core.api_property
(
    "id"               uuid primary key not null default uuid_generate_v4(),
    "version"          integer          not null,
    "date_created"     timestamp,
    "last_updated"     timestamp,
    "created_by"       uuid,
    "key"              varchar(255),
    "value"            text,
    "publicly_visible" boolean          not null,
    "category"         varchar(255),
    "last_updated_by"  uuid
);

create unique index "idx_api_property_key" on core.api_property (key);