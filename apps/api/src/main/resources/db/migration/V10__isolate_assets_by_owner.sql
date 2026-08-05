do $$
begin
    if exists (select 1 from assets) then
        raise exception 'Cannot add Asset ownership while legacy rows exist. Reset development financial data explicitly before migrating.';
    end if;
end
$$;

alter table assets
    add column owner_id uuid not null references users (id);

create index assets_owner_id_idx on assets (owner_id);
