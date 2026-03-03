create sequence seq_prosess_task
    minvalue 1000000
    increment by 50;

create sequence seq_prosess_task_gruppe
    minvalue 10000000
    increment by 1000000;

create sequence seq_global_pk
    minvalue 2000000
    increment by 50;

create table if not exists forespoersel
(
    id                   bigint                                                         not null
    constraint pk_foresporsel_id
    primary key,
    uuid                 uuid                                                           not null,
    sak_id               varchar(36),
    oppgave_id           varchar(36),
    skjaeringstidspunkt  date,
    orgnr                varchar(12)                                                    not null,
    bruker_aktoer_id     varchar(13)                                                    not null,
    ytelse_type          varchar(100)                                                   not null,
    fagsystem_saksnummer varchar(19),
    opprettet_tid        timestamp(3) default CURRENT_TIMESTAMP                         not null,
    endret_tid           timestamp(3),
    status               varchar(100) default 'UNDER_BEHANDLING'::character varying     not null,
    forste_uttaksdato    date,
    forespoersel_type    varchar(50)  default 'BESTILT_AV_FAGSYSTEM'::character varying not null,
    dialogporten_uuid    uuid
    );

comment on table forespoersel is 'En forespørsel til arbeidsgiver';

comment on column forespoersel.id is 'PK';

comment on column forespoersel.uuid is 'Forespørsel UUID som kan eksponeres';

comment on column forespoersel.sak_id is 'Ekstern sak-id hos arbeidsgivernotifikasjon';

comment on column forespoersel.oppgave_id is 'Ekstern oppgave-id hos arbeidsgivernotifikasjon';

comment on column forespoersel.skjaeringstidspunkt is 'Skjæringstidspunkt forespørselen gjelder for';

comment on column forespoersel.orgnr is 'Orgnr for arbeidsgiver';

comment on column forespoersel.bruker_aktoer_id is 'Aktørid for bruker';

comment on column forespoersel.ytelse_type is 'Ytelsetype som forespørsel gjelder for';

comment on column forespoersel.fagsystem_saksnummer is 'Saksnummer fra fagsystem';

comment on column forespoersel.status is 'Status på forespørselen.';

comment on column forespoersel.forste_uttaksdato is 'Dato med første frævarsdag.';

comment on column forespoersel.forespoersel_type is 'Hvilken hendelse som ledet til opprettelse av forespørselen';

comment on column forespoersel.dialogporten_uuid is 'DialogId for forespørselen til bruk i Altinn - Dialogporten';

create unique index if not exists uidx_forespoersel_uuid
    on forespoersel (uuid);

create index if not exists idx_forespoersel_stp_orgnr_bruker_ytelse_saksnr
    on forespoersel (skjaeringstidspunkt, orgnr, bruker_aktoer_id, ytelse_type);

create index if not exists idx_forespoersel_bruker_aktoer_id
    on forespoersel (bruker_aktoer_id);

create index if not exists idx_forespoersel_fagsystem_saksnummer
    on forespoersel (fagsystem_saksnummer);

create unique index if not exists uidx_forespoersel_sak_id
    on forespoersel (sak_id);

create table if not exists inntektsmelding
(
    id                    bigint                                 not null
    constraint pk_inntektsmelding_id
    primary key,
    start_dato_permisjon  date                                   not null,
    arbeidsgiver_ident    varchar(13)                            not null,
    bruker_aktoer_id      varchar(13)                            not null,
    ytelse_type           varchar(100)                           not null,
    maaned_inntekt        numeric(19, 2)                         not null,
    opprettet_tid         timestamp(3) default CURRENT_TIMESTAMP not null,
    maaned_refusjon       numeric(19, 2),
    refusjon_opphoersdato date,
    opprettet_av          varchar(100),
    kildesystem           varchar(100),
    uuid                  uuid
    );

comment on table inntektsmelding is 'En inntektsmelding fra en arbeidsgiver';

comment on column inntektsmelding.id is 'PK';

comment on column inntektsmelding.start_dato_permisjon is 'Startdato for ytelsen';

comment on column inntektsmelding.arbeidsgiver_ident is 'Identifikator for arbeidsgiver, orgnr for bedrifter og aktørId for privatpersoner';

comment on column inntektsmelding.bruker_aktoer_id is 'Aktørid for arbeidstaker';

comment on column inntektsmelding.ytelse_type is 'Ytelsetype som inntektsmeldingen gjelder for';

comment on column inntektsmelding.maaned_inntekt is 'Arbeidstakers månedsinntekt';

comment on column inntektsmelding.opprettet_tid is 'Timestamp da inntektsmeldingen ble lagret i databasen';

comment on column inntektsmelding.maaned_refusjon is 'Refusjonskrav pr måned fra arbeidsgiver';

comment on column inntektsmelding.refusjon_opphoersdato is 'Siste dag med refusjon';

comment on column inntektsmelding.opprettet_av is 'Referanse til hvem som opprettet inntektsmeldingen';

comment on column inntektsmelding.kildesystem is 'Systemet som stod for innsending av inntektsmeldingen';

comment on column inntektsmelding.uuid is 'En unik uuid som identifiserer inntektsmeldingen';

create index if not exists idx_inntektsmelding_start_dato_arbeidsgiver_ident_bruker_ytelse
    on inntektsmelding (start_dato_permisjon, arbeidsgiver_ident, bruker_aktoer_id, ytelse_type);

create unique index if not exists uidx_inntektsmelding_uuid
    on inntektsmelding (uuid);

create table if not exists kontaktperson
(
    id                 bigint       not null
    constraint pk_kontaktperson_id
    primary key,
    inntektsmelding_id bigint       not null
    constraint fk_kontaktperson
    references inntektsmelding,
    telefonnummer      varchar(100) not null,
    navn               varchar(100) not null
    );

comment on table kontaktperson is 'En kontaktperson for inntektsmeldingen';

comment on column kontaktperson.id is 'PK';

comment on column kontaktperson.inntektsmelding_id is 'Foreign Key til inntektsmelding';

comment on column kontaktperson.telefonnummer is 'Telefonnummer til kontakperson';

comment on column kontaktperson.navn is 'Navn på kontaktperson';

create index if not exists idx_kontaktperson_inntektsmelding_id
    on kontaktperson (inntektsmelding_id);

create table if not exists prosess_task
(
    id                        numeric                                                                            not null,
    task_type                 varchar(50)                                                                        not null,
    prioritet                 numeric(1)   default 0                                                             not null,
    status                    varchar(20)  default 'KLAR'::character varying                                     not null,
    task_parametere           varchar(4000),
    task_payload              text,
    task_gruppe               varchar(250),
    task_sekvens              varchar(100) default '1'::character varying                                        not null,
    partition_key             varchar(4)   default to_char((CURRENT_DATE)::timestamp with time zone, 'MM'::text) not null,
    neste_kjoering_etter      timestamp(0) default CURRENT_TIMESTAMP,
    feilede_forsoek           numeric(5)   default 0,
    siste_kjoering_ts         timestamp(6),
    siste_kjoering_feil_kode  varchar(50),
    siste_kjoering_feil_tekst text,
    siste_kjoering_server     varchar(50),
    opprettet_av              varchar(20)  default 'VL'::character varying                                       not null,
    opprettet_tid             timestamp(6) default CURRENT_TIMESTAMP                                             not null,
    blokkert_av               numeric,
    versjon                   numeric      default 0                                                             not null,
    siste_kjoering_slutt_ts   timestamp(6),
    siste_kjoering_plukk_ts   timestamp(6),
    constraint pk_prosess_task
    primary key (id, status, partition_key)
    )
    partition by LIST (status);

comment on table prosess_task is 'Inneholder tasks som skal kjøres i bakgrunnen';

comment on column prosess_task.id is 'Primary Key';

comment on column prosess_task.task_type is 'navn på task. Brukes til å matche riktig implementasjon';

comment on column prosess_task.prioritet is 'prioritet på task.  Høyere tall har høyere prioritet';

comment on column prosess_task.status is 'status på task: KLAR, NYTT_FORSOEK, FEILET, VENTER_SVAR, FERDIG';

comment on column prosess_task.task_parametere is 'parametere angitt for en task';

comment on column prosess_task.task_payload is 'inputdata for en task';

comment on column prosess_task.task_gruppe is 'angir en unik id som grupperer flere ';

comment on column prosess_task.task_sekvens is 'angir rekkefølge på task innenfor en gruppe ';

comment on column prosess_task.neste_kjoering_etter is 'tasken skal ikke kjøeres før tidspunkt er passert';

comment on column prosess_task.feilede_forsoek is 'antall feilede forsøk';

comment on column prosess_task.siste_kjoering_ts is 'siste gang tasken ble forsøkt kjørt (før kjøring)';

comment on column prosess_task.siste_kjoering_feil_kode is 'siste feilkode tasken fikk';

comment on column prosess_task.siste_kjoering_feil_tekst is 'siste feil tasken fikk';

comment on column prosess_task.siste_kjoering_server is 'navn på node som sist kjørte en task (server@pid)';

comment on column prosess_task.blokkert_av is 'Id til ProsessTask som blokkerer kjøring av denne (når status=VETO)';

comment on column prosess_task.versjon is 'angir versjon for optimistisk låsing';

comment on column prosess_task.siste_kjoering_slutt_ts is 'siste gang tasken ble forsøkt plukket (klargjort til kjøring)';

create index if not exists idx_prosess_task_2
    on prosess_task (task_type);

create index if not exists idx_prosess_task_3
    on prosess_task (neste_kjoering_etter);

create index if not exists idx_prosess_task_5
    on prosess_task (task_gruppe);

create index if not exists idx_prosess_task_1
    on prosess_task (status);

create index if not exists idx_prosess_task_4
    on prosess_task (id);

create index if not exists idx_prosess_task_7
    on prosess_task (partition_key);

create unique index if not exists uidx_prosess_task
    on prosess_task (id, status, partition_key);

create index idx_prosess_task_6 on prosess_task (BLOKKERT_AV);

create table if not exists prosess_task_partition_default
    partition of prosess_task
    DEFAULT;

create table if not exists prosess_task_partition_ferdig
    partition of prosess_task
    FOR VALUES IN ('FERDIG')
    partition by LIST (partition_key);

create table if not exists prosess_task_partition_ferdig_01
    partition of prosess_task_partition_ferdig
    FOR VALUES IN ('01');

create table if not exists prosess_task_partition_ferdig_02
    partition of prosess_task_partition_ferdig
    FOR VALUES IN ('02');

create table if not exists prosess_task_partition_ferdig_03
    partition of prosess_task_partition_ferdig
    FOR VALUES IN ('03');

create table if not exists prosess_task_partition_ferdig_04
    partition of prosess_task_partition_ferdig
    FOR VALUES IN ('04');

create table if not exists prosess_task_partition_ferdig_05
    partition of prosess_task_partition_ferdig
    FOR VALUES IN ('05');

create table if not exists prosess_task_partition_ferdig_06
    partition of prosess_task_partition_ferdig
    FOR VALUES IN ('06');

create table if not exists prosess_task_partition_ferdig_07
    partition of prosess_task_partition_ferdig
    FOR VALUES IN ('07');

create table if not exists prosess_task_partition_ferdig_08
    partition of prosess_task_partition_ferdig
    FOR VALUES IN ('08');

create table if not exists prosess_task_partition_ferdig_09
    partition of prosess_task_partition_ferdig
    FOR VALUES IN ('09');

create table if not exists prosess_task_partition_ferdig_10
    partition of prosess_task_partition_ferdig
    FOR VALUES IN ('10');

create table if not exists prosess_task_partition_ferdig_11
    partition of prosess_task_partition_ferdig
    FOR VALUES IN ('11');

create table if not exists prosess_task_partition_ferdig_12
    partition of prosess_task_partition_ferdig
    FOR VALUES IN ('12');

create table if not exists refusjon_endring
(
    id                 bigint         not null
    constraint pk_refusjon_endring_id
    primary key,
    inntektsmelding_id bigint         not null
    constraint fk_refusjon_endring
    references inntektsmelding,
    fom                date           not null,
    maaned_refusjon    numeric(19, 2) not null
    );

comment on table refusjon_endring is 'Endringer i refusjon';

comment on column refusjon_endring.id is 'PK';

comment on column refusjon_endring.inntektsmelding_id is 'Inntektsmeldingen endringen er oppgitt i';

comment on column refusjon_endring.fom is 'Endringen gjelder fra og med denne datoen';

comment on column refusjon_endring.maaned_refusjon is 'Hva refusjonsbeløpet pr mnd endres til';

create index if not exists idx_refusjon_endring_inntektsmelding_id
    on refusjon_endring (inntektsmelding_id);

create table if not exists bortfalt_naturalytelse
(
    id                 bigint         not null
    constraint pk_bortfalt_naturalytelse_id
    primary key,
    inntektsmelding_id bigint         not null
    constraint fk_bortfalt_naturalytelse
    references inntektsmelding,
    fom                date           not null,
    tom                date           not null,
    maaned_beloep      numeric(19, 2) not null,
    type               varchar(100)   not null
    );

comment on table bortfalt_naturalytelse is 'En naturalytelse som bortfaller for en periode';

comment on column bortfalt_naturalytelse.id is 'PK';

comment on column bortfalt_naturalytelse.inntektsmelding_id is 'Foreign Key til inntektsmelding';

comment on column bortfalt_naturalytelse.fom is 'Fra og med dato for når naturalytelse bortfaller';

comment on column bortfalt_naturalytelse.tom is 'Til og med dato for når naturalytelse bortfaller';

comment on column bortfalt_naturalytelse.maaned_beloep is 'Det månedlige beløpet som skal kompenseres';

comment on column bortfalt_naturalytelse.type is 'Type naturalytelse som er bortfalt';

create index if not exists idx_bortfalt_naturalytelse_inntektsmelding_id
    on bortfalt_naturalytelse (inntektsmelding_id);

create table if not exists endringsaarsak
(
    id                 bigint       not null
    constraint pk_endringsaarsak_id
    primary key,
    inntektsmelding_id bigint       not null
    constraint fk_endringsaarsak
    references inntektsmelding,
    aarsak             varchar(100) not null,
    fom                date,
    tom                date,
    ble_kjent_fom      date
    );

comment on table endringsaarsak is 'Endringsårsaker som begrunner hvorfor snittlønn de siste tre måneder ikke kan brukes i inntektsmeldingen';

comment on column endringsaarsak.id is 'PK';

comment on column endringsaarsak.inntektsmelding_id is 'Inntektsmeldingen endringsårsaken er oppgitt i';

comment on column endringsaarsak.aarsak is 'Endringsårsaken som er oppgitt';

comment on column endringsaarsak.fom is 'Endringsårsaken gjelder fra og med denne datoen';

comment on column endringsaarsak.tom is 'Endringsårsaken gjelder til og med denne datoen';

comment on column endringsaarsak.ble_kjent_fom is 'Endringsårsaken ble kjent denne datoen';

create index if not exists idx_endringsaarsak_inntektsmelding_id
    on endringsaarsak (inntektsmelding_id);

