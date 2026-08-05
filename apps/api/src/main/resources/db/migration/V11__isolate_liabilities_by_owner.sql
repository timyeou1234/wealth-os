do $$
begin
    if exists (select 1 from liabilities) then
        raise exception 'Cannot add Liability ownership while legacy rows exist. Reset development financial data explicitly before migrating.';
    end if;
end
$$;

alter table liabilities
    add column owner_id uuid not null references users (id);

create index liabilities_owner_id_idx on liabilities (owner_id);
