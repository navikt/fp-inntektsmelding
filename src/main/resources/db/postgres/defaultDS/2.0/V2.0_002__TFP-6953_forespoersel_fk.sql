alter table inntektsmelding
    add column forespoersel_id bigint;

alter table inntektsmelding
    add constraint fk_inntektsmelding_forespoersel
        foreign key (forespoersel_id) references forespoersel (id);

create index idx_inntektsmelding_forespoersel_id
    on inntektsmelding (forespoersel_id);

comment on column inntektsmelding.forespoersel_id is 'Foreign Key til forespørselen som inntektsmeldingen er svar på';
