delete from assets;

alter table assets
    add column owner_id uuid not null references users (id);

create index assets_owner_id_idx on assets (owner_id);
