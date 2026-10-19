-- Eget løpenummer for eksterne konsumenter. Sekvensene øker med 1 (ingen blokkreservering fra Hibernate).
create sequence if not exists seq_forespoersel_loepe_nummer
    increment by 1
    minvalue 1;

create sequence if not exists seq_inntektsmelding_loepe_nummer
    increment by 1
    minvalue 1;

alter table forespoersel
    add column if not exists loepe_nummer bigint;

alter table inntektsmelding
    add column if not exists loepe_nummer bigint;

-- Eksisterende rader nummereres om i opprettelsesrekkefølge: første rad får max(id) + 10 000, deretter økes med 1
update forespoersel f
set loepe_nummer = n.nr
from (select id,
             row_number() over (order by opprettet_tid, id) - 1 + (select coalesce(max(id), 0) + 10000 from forespoersel) as nr
      from forespoersel) n
where f.id = n.id
  and f.loepe_nummer is null;

update inntektsmelding i
set loepe_nummer = n.nr
from (select id,
             row_number() over (order by opprettet_tid, id) - 1 + (select coalesce(max(id), 0) + 10000 from inntektsmelding) as nr
      from inntektsmelding) n
where i.id = n.id
  and i.loepe_nummer is null;

select setval('seq_forespoersel_loepe_nummer',
              greatest((select coalesce(max(loepe_nummer), 0) from forespoersel),
                       (select coalesce(max(id), 0) + 10000 from forespoersel)),
              true);

select setval('seq_inntektsmelding_loepe_nummer',
              greatest((select coalesce(max(loepe_nummer), 0) from inntektsmelding),
                       (select coalesce(max(id), 0) + 10000 from inntektsmelding)),
              true);

alter table forespoersel
    alter column loepe_nummer set default nextval('seq_forespoersel_loepe_nummer'),
    alter column loepe_nummer set not null;

alter table inntektsmelding
    alter column loepe_nummer set default nextval('seq_inntektsmelding_loepe_nummer'),
    alter column loepe_nummer set not null;

create unique index if not exists uidx_forespoersel_loepe_nummer
    on forespoersel (loepe_nummer);

create unique index if not exists uidx_inntektsmelding_loepe_nummer
    on inntektsmelding (loepe_nummer);

comment on column forespoersel.loepe_nummer is 'Løpenummer eksponert til eksterne konsumenter. Egen sekvens, øker med 1';

comment on column inntektsmelding.loepe_nummer is 'Løpenummer eksponert til eksterne konsumenter. Egen sekvens, øker med 1';
