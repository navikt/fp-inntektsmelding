create table forespoersel_endring_historikk
(
    id                  bigint not null
        constraint pk_forespoersel_endring_historikk_id
            primary key,
    forespoersel_id      bigint
        constraint fk_forespoersel_endring_historikk
            references forespoersel (id),
    skjaeringstidspunkt  date,
    forste_uttaksdato    date,
    opprettet_tid       timestamp(3) default CURRENT_TIMESTAMP not null
);

create index idx_forespoersel_endring_historikk_forespoersel_id
    on forespoersel_endring_historikk (forespoersel_id);

comment on table forespoersel_endring_historikk is 'Endringshistorikk for forespørsler om inntektsmelding';

comment on column forespoersel_endring_historikk.id is 'PK';

comment on column forespoersel_endring_historikk.forespoersel_id is 'Referanse til forespørselen';

comment on column forespoersel_endring_historikk.skjaeringstidspunkt is 'Tidligere skjæringstidspunkt for forespørselen';

comment on column forespoersel_endring_historikk.forste_uttaksdato is 'Tidligere første uttaksdato for forespørselen';

comment on column forespoersel_endring_historikk.opprettet_tid is 'Tidspunkt da endringshistorikken ble opprettet';
