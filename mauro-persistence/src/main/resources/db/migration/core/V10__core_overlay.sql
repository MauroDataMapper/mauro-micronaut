create table if not exists core.overlay (
    id uuid primary key not null default gen_random_uuid(),
    version integer not null,
    date_created timestamp with time zone,
    last_updated timestamp with time zone,
    readable_by_everyone boolean not null default false,
    readable_by_authenticated_users boolean not null default false,
    label text not null,
    description text,
    aliases_string text,
    created_by uuid,
    breadcrumb_tree_id uuid,
    finalised boolean not null default false,
    date_finalised timestamp with time zone,
    documentation_version varchar(255),
    model_type varchar(255) not null,
    organisation varchar(255),
    deleted boolean not null default false,
    author varchar(255),
    folder_id uuid not null references core.folder(id) initially deferred,
    authority_id uuid,
    branch_name varchar(255),
    model_version varchar(255),
    model_version_tag varchar(255),
    rules jsonb not null default '[]'::jsonb
);

create index if not exists idx_overlay_folder_id on core.overlay(folder_id);
