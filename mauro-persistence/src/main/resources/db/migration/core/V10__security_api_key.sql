create table security.api_key (
    "id"                uuid primary key    not null default gen_random_uuid(),
    "version"           integer             not null,
    "refreshable"       boolean             not null,
    "date_created"      timestamp with time zone,
    "expiry_date"       date                not null,
    "last_updated"      timestamp with time zone,
    "disabled"          boolean             not null,
    "catalogue_user_id" uuid                not null        references security.catalogue_user (id) initially deferred,
    "name"              varchar(255)        not null,
    "created_by"        uuid
);
create index apiKey_created_by_idx ON security.api_key(created_by);
create index apiKey_catalogue_user_idx ON security.api_key(catalogue_user_id);

create unique index "idx_api_key_catalogue_user_id_name_unique" on  security.api_key (catalogue_user_id, name);
