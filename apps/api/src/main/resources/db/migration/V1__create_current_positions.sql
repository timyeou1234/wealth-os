create table assets (
    id uuid primary key,
    name varchar(255) not null,
    asset_type varchar(64) not null,
    liquidity varchar(32) not null,
    constraint assets_name_not_blank check (btrim(name) <> '')
);

create table liabilities (
    id uuid primary key,
    name varchar(255) not null,
    constraint liabilities_name_not_blank check (btrim(name) <> '')
);
