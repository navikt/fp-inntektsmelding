-- Eget løpenummer for eksterne konsumenter. Sekvensene øker med 1 (ingen blokkreservering fra Hibernate).
-- Applikasjonen skal være skalert ned til 0 før migreringen kjøres, slik at datagrunnlaget ikke endres under konverteringen.
create sequence seq_forespoersel_loepe_nummer
    increment by 1
    minvalue 1
    cache 1;

create sequence seq_inntektsmelding_loepe_nummer
    increment by 1
    minvalue 1
    cache 1;

alter table forespoersel
    add column loepe_nummer bigint;

alter table inntektsmelding
    add column loepe_nummer bigint;

-- Eksisterende rader beholder dagens eksterne løpenummer (databaseid), slik at konsumentene ikke får duplikater
update forespoersel
set loepe_nummer = id;

update inntektsmelding
set loepe_nummer = id;

-- Nye rader får neste tall over høyeste eksisterende løpenummer
select setval('seq_forespoersel_loepe_nummer', greatest((select coalesce(max(loepe_nummer), 0) from forespoersel), 1), true);

select setval('seq_inntektsmelding_loepe_nummer', greatest((select coalesce(max(loepe_nummer), 0) from inntektsmelding), 1), true);

alter table forespoersel
    alter column loepe_nummer set not null;

alter table inntektsmelding
    alter column loepe_nummer set not null;

create unique index uidx_forespoersel_loepe_nummer
    on forespoersel (loepe_nummer);

create unique index uidx_inntektsmelding_loepe_nummer
    on inntektsmelding (loepe_nummer);

comment on column forespoersel.loepe_nummer is 'Løpenummer eksponert til eksterne konsumenter. Egen sekvens, øker med 1';

comment on column inntektsmelding.loepe_nummer is 'Løpenummer eksponert til eksterne konsumenter. Egen sekvens, øker med 1';
