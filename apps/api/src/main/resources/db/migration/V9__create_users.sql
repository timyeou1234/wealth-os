create table users (
    id uuid primary key,
    issuer varchar(512) not null,
    subject varchar(255) not null,
    email varchar(255) not null,
    constraint users_issuer_not_blank check (btrim(issuer) <> ''),
    constraint users_subject_not_blank check (btrim(subject) <> ''),
    constraint users_email_not_blank check (btrim(email) <> ''),
    constraint users_external_identity_unique unique (issuer, subject)
);
