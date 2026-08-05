delete from snapshot_asset_positions;
delete from snapshot_liability_positions;
delete from snapshots;

alter table snapshots
    add column owner_id uuid not null references users (id);

create index snapshots_owner_id_idx on snapshots (owner_id);
