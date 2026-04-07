create table if not exists lps_system_informasjon
(
    inntektsmelding_id bigint       not null
        constraint pk_lps_system_id
            primary key
        constraint fk_lps_system_id
            references inntektsmelding,
    navn               varchar(100) not null,
    versjon            varchar(100) not null
);

comment on table lps_system_informasjon is 'Informasjon om LPS systemet som sendte inntektsmeldingen';
comment on column lps_system_informasjon.inntektsmelding_id is 'Natural Private Key og Foreign Key til inntektsmelding';
comment on column lps_system_informasjon.navn is 'Navn på system som har sendt inntektsmeldingen.';
comment on column lps_system_informasjon.versjon is 'Versjon på system som har sendt inntektsmeldingen.';
