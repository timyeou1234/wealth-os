create table snapshots (
    id uuid primary key,
    as_of timestamp with time zone not null,
    recorded_at timestamp with time zone not null,
    supersedes_id uuid references snapshots (id),
    correction_reason varchar(255),
    constraint snapshots_recorded_at_not_before_as_of
        check (recorded_at >= as_of),
    constraint snapshots_do_not_supersede_themselves
        check (supersedes_id is null or supersedes_id <> id),
    constraint snapshots_correction_metadata_complete
        check (
            (supersedes_id is null and correction_reason is null)
            or (
                supersedes_id is not null
                and correction_reason is not null
                and btrim(correction_reason) <> ''
            )
        ),
    constraint snapshots_one_direct_successor unique (supersedes_id)
);

create table snapshot_asset_positions (
    snapshot_id uuid not null references snapshots (id),
    asset_id uuid not null,
    name varchar(255) not null,
    asset_type varchar(64) not null,
    liquidity varchar(32) not null,
    amount numeric(38, 3) not null,
    currency char(3) not null,
    effective_at timestamp with time zone not null,
    source varchar(100) not null,
    primary key (snapshot_id, asset_id),
    constraint snapshot_asset_position_name_not_blank
        check (btrim(name) <> ''),
    constraint snapshot_asset_position_amount_not_negative
        check (amount >= 0),
    constraint snapshot_asset_position_currency_format
        check (currency ~ '^[A-Z]{3}$'),
    constraint snapshot_asset_position_source_not_blank
        check (btrim(source) <> '')
);

create table snapshot_liability_positions (
    snapshot_id uuid not null references snapshots (id),
    liability_id uuid not null,
    name varchar(255) not null,
    amount numeric(38, 3) not null,
    currency char(3) not null,
    effective_at timestamp with time zone not null,
    source varchar(100) not null,
    primary key (snapshot_id, liability_id),
    constraint snapshot_liability_position_name_not_blank
        check (btrim(name) <> ''),
    constraint snapshot_liability_position_amount_not_negative
        check (amount >= 0),
    constraint snapshot_liability_position_currency_format
        check (currency ~ '^[A-Z]{3}$'),
    constraint snapshot_liability_position_source_not_blank
        check (btrim(source) <> '')
);
