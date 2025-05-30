-- # Federation subscribed_catalogue
create table if not exists federation."subscribed_catalogue" (
    "id"                              uuid primary key not null default gen_random_uuid(),
    "version"                         integer          not null,
    "date_created"                    timestamp with time zone,
    "last_updated"                    timestamp with time zone,
    "readable_by_authenticated_users" boolean          default false,
    "readable_by_everyone"            boolean          default false,
    "created_by"                      uuid,
    "label"                           text             not null,
    "description"                     text,
    "url"                             text             not null,
    "refresh_period"                  integer,
    "last_read"                       timestamp with time zone,
    "subscribed_catalogue_type"       text             not null default 'MAURO_JSON',
    "subscribed_catalogue_authentication_type" text    not null default 'NO_AUTHENTICATION',
    "api_key"                         text,
    "token_url"                       text,
    "client_id"                       text,
    "client_secret"                   text,
    "access_token"                    text,
    "access_token_expiry_time"        timestamp
);

create table if not exists federation."subscribed_model" (
    "id"                              uuid primary key not null default gen_random_uuid(),
    "version"                         integer          not null,
    "date_created"                    timestamp with time zone,
    "last_updated"                    timestamp with time zone,
    "readable_by_authenticated_users" boolean          not null default true,
    "readable_by_everyone"            boolean          not null default false,
    "created_by"                      uuid,
    "subscribed_catalogue_id"         uuid             not null references federation.subscribed_catalogue (id) initially deferred,
    "subscribed_model_id"             text             not null,
    "subscribed_model_type"           text,
    "folder_id"                       uuid             not null,
    "last_read"                       timestamp with time zone,
    "local_model_id"                  uuid
);

create index "idx_subscribed_model_subscribed_catalogue_id" on federation."subscribed_model" (subscribed_catalogue_id);

