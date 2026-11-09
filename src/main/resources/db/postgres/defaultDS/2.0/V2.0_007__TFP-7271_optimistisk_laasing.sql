alter table forespoersel
    add column versjon bigint not null default 0;

comment on column forespoersel.versjon is 'Versjon for optimistisk låsing';

alter table inntektsmelding
    add column versjon bigint not null default 0;

comment on column inntektsmelding.versjon is 'Versjon for optimistisk låsing';
