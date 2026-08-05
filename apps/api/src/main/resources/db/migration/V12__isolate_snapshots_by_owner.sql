do $$
begin
    if exists (select 1 from snapshots)
        or exists (select 1 from snapshot_asset_positions)
        or exists (select 1 from snapshot_liability_positions) then
        raise exception 'Cannot add Snapshot ownership while legacy rows exist. Reset development financial data explicitly before migrating.';
    end if;
end
$$;

alter table snapshots
    add column owner_id uuid not null references users (id);

create index snapshots_owner_id_idx on snapshots (owner_id);
