create table fx_rates (
    id uuid primary key,
    original_currency varchar(3) not null,
    valuation_currency varchar(3) not null,
    rate numeric(38, 12) not null,
    rate_date date not null,
    provider varchar(32) not null,
    constraint fx_rates_original_currency_format check (original_currency ~ '^[A-Z]{3}$'),
    constraint fx_rates_valuation_currency_twd check (valuation_currency = 'TWD'),
    constraint fx_rates_original_not_twd check (original_currency <> valuation_currency),
    constraint fx_rates_rate_positive check (rate > 0),
    constraint fx_rates_provider_not_blank check (btrim(provider) <> ''),
    constraint fx_rates_natural_key unique (
        original_currency,
        valuation_currency,
        provider,
        rate_date
    )
);

create index fx_rates_as_of_lookup
    on fx_rates (original_currency, valuation_currency, provider, rate_date desc);
