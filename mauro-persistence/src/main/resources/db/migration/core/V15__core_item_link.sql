create table core.item_link
(
    id                                 uuid primary key not null default gen_random_uuid(),
    version                            integer not null,
    date_created                       timestamp with time zone,
    last_updated                       timestamp with time zone,
    multi_facet_aware_item_domain_type varchar(255) not null,
    multi_facet_aware_item_id          uuid not null,
    created_by                         uuid,
    type                               varchar(255) not null,
    path                               text not null
);

create unique index idx_item_link_item_type_path
    on core.item_link (multi_facet_aware_item_id, type, path);

create index idx_item_link_type
    on core.item_link (type);
