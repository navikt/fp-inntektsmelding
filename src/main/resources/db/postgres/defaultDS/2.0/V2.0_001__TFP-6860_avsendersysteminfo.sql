alter table INNTEKTSMELDING
    add column if not exists lps_system_navn varchar(255);
comment on column INNTEKTSMELDING.lps_system_navn is 'Navn på system som har sendt inntektsmeldingen.';

alter table INNTEKTSMELDING
    add column if not exists lps_system_versjon varchar(255);
comment on column INNTEKTSMELDING.lps_system_versjon is 'Versjon på system som har sendt inntektsmeldingen.';
