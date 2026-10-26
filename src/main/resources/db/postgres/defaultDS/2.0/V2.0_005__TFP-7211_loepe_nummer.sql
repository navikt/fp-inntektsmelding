-- Første steg: løpenummer tildeles av ny applikasjonskode ved opprettelse.
-- Historiske rader beholder null, og API-ene bruker fortsatt database-ID.
alter table forespoersel
    add column loepe_nummer bigint;

comment on column forespoersel.loepe_nummer is 'Løpenummer fra egen sekvens. Historiske rader etterfylles i et senere steg';

alter table inntektsmelding
    add column loepe_nummer bigint;

comment on column inntektsmelding.loepe_nummer is 'Løpenummer fra egen sekvens. Historiske rader etterfylles i et senere steg';

-- Bufferen må kontrolleres før etterfylling dersom gammel kode har opprettet flere rader.
do $$
declare
    forespoersel_start bigint;
    inntektsmelding_start bigint;
begin
    select coalesce(max(id), 0) + 150000 into forespoersel_start from forespoersel;
    select coalesce(max(id), 0) + 150000 into inntektsmelding_start from inntektsmelding;

    execute format(
        'create sequence seq_forespoersel_loepe_nummer minvalue %s start with %s increment by 1 cache 1 no cycle',
        forespoersel_start, forespoersel_start);
    execute format(
        'create sequence seq_inntektsmelding_loepe_nummer minvalue %s start with %s increment by 1 cache 1 no cycle',
        inntektsmelding_start, inntektsmelding_start);
end;
$$;
