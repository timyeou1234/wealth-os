alter table snapshot_asset_positions
    add column conversion_original_amount numeric(38, 6);
alter table snapshot_asset_positions
    add column conversion_original_currency char(3);
alter table snapshot_asset_positions
    add column conversion_exchange_rate_basis varchar(200);
alter table snapshot_asset_positions
    add column conversion_effective_at timestamp with time zone;
alter table snapshot_asset_positions
    add constraint snapshot_asset_conversion_complete check (
        (conversion_original_amount is null
            and conversion_original_currency is null
            and conversion_exchange_rate_basis is null
            and conversion_effective_at is null)
        or
        (conversion_original_amount is not null
            and conversion_original_currency is not null
            and conversion_exchange_rate_basis is not null
            and conversion_effective_at is not null
            and conversion_original_amount >= 0
            and conversion_original_currency ~ '^[A-Z]{3}$'
            and btrim(conversion_exchange_rate_basis) <> '')
    );

alter table snapshot_liability_positions
    add column conversion_original_amount numeric(38, 6);
alter table snapshot_liability_positions
    add column conversion_original_currency char(3);
alter table snapshot_liability_positions
    add column conversion_exchange_rate_basis varchar(200);
alter table snapshot_liability_positions
    add column conversion_effective_at timestamp with time zone;
alter table snapshot_liability_positions
    add constraint snapshot_liability_conversion_complete check (
        (conversion_original_amount is null
            and conversion_original_currency is null
            and conversion_exchange_rate_basis is null
            and conversion_effective_at is null)
        or
        (conversion_original_amount is not null
            and conversion_original_currency is not null
            and conversion_exchange_rate_basis is not null
            and conversion_effective_at is not null
            and conversion_original_amount >= 0
            and conversion_original_currency ~ '^[A-Z]{3}$'
            and btrim(conversion_exchange_rate_basis) <> '')
    );
