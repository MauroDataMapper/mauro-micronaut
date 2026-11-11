create table if not exists core.overlay_rule (
    id uuid primary key not null,
    overlay_id uuid not null references core.overlay(id) on delete cascade,
    rule_order bigint not null,
    match_definition jsonb not null,
    actions_definition jsonb not null default '[]'::jsonb,
    first_node_prefix varchar(64),
    first_node_identifier text,
    first_node_identifier_suffix boolean not null default false,
    match_path_suffix boolean not null default false,
    selector_node_count integer not null default 0,
    last_node_prefix varchar(64),
    last_node_identifier text,
    constraint uq_overlay_rule_order unique (overlay_id, rule_order)
);

create index if not exists idx_overlay_rule_overlay_order on core.overlay_rule(overlay_id, rule_order);
create index if not exists idx_overlay_rule_first_node_selector
    on core.overlay_rule(first_node_prefix, first_node_identifier, first_node_identifier_suffix,
                         selector_node_count, last_node_prefix, last_node_identifier);

alter table core.overlay drop column if exists overlay_rules;
