alter table snapshot_asset_positions add column applied_original_amount numeric(38, 6);
alter table snapshot_asset_positions add column applied_original_currency varchar(3);
alter table snapshot_asset_positions add column applied_rate numeric(38, 12);
alter table snapshot_asset_positions add column applied_rate_date date;
alter table snapshot_asset_positions add column applied_provider varchar(32);
alter table snapshot_asset_positions add column applied_rate_type varchar(32);
alter table snapshot_asset_positions add column applied_basis varchar(200);
alter table snapshot_asset_positions add column applied_rounding_mode varchar(32);
alter table snapshot_asset_positions
    add constraint snapshot_asset_applied_conversion_complete check (
        (applied_original_amount is null and applied_original_currency is null and applied_rate is null
            and applied_rate_date is null and applied_provider is null and applied_rate_type is null
            and applied_basis is null and applied_rounding_mode is null)
        or
        (applied_original_amount is not null and applied_original_currency is not null and applied_rate > 0
            and applied_rate_date is not null and btrim(applied_provider) <> '' and applied_rate_type is not null
            and applied_rounding_mode is not null
            and currency = 'TWD' and applied_original_currency <> 'TWD'
            and applied_rate_type in ('REFERENCE_RATE', 'USER_DECLARED')
            and applied_rounding_mode = 'HALF_EVEN'
            and (applied_rate_type <> 'USER_DECLARED'
                or (applied_basis is not null and btrim(applied_basis) <> '')))
    );

alter table snapshot_liability_positions add column applied_original_amount numeric(38, 6);
alter table snapshot_liability_positions add column applied_original_currency varchar(3);
alter table snapshot_liability_positions add column applied_rate numeric(38, 12);
alter table snapshot_liability_positions add column applied_rate_date date;
alter table snapshot_liability_positions add column applied_provider varchar(32);
alter table snapshot_liability_positions add column applied_rate_type varchar(32);
alter table snapshot_liability_positions add column applied_basis varchar(200);
alter table snapshot_liability_positions add column applied_rounding_mode varchar(32);
alter table snapshot_liability_positions
    add constraint snapshot_liability_applied_conversion_complete check (
        (applied_original_amount is null and applied_original_currency is null and applied_rate is null
            and applied_rate_date is null and applied_provider is null and applied_rate_type is null
            and applied_basis is null and applied_rounding_mode is null)
        or
        (applied_original_amount is not null and applied_original_currency is not null and applied_rate > 0
            and applied_rate_date is not null and btrim(applied_provider) <> '' and applied_rate_type is not null
            and applied_rounding_mode is not null
            and currency = 'TWD' and applied_original_currency <> 'TWD'
            and applied_rate_type in ('REFERENCE_RATE', 'USER_DECLARED')
            and applied_rounding_mode = 'HALF_EVEN'
            and (applied_rate_type <> 'USER_DECLARED'
                or (applied_basis is not null and btrim(applied_basis) <> '')))
    );
