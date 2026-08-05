delete from liabilities;

alter table liabilities
    add column owner_id uuid not null references users (id);

create index liabilities_owner_id_idx on liabilities (owner_id);
