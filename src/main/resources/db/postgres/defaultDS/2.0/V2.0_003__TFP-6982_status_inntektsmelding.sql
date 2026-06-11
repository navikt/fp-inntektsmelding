alter table inntektsmelding
    add column status varchar(50);

update inntektsmelding
    set status = 'GODKJENT'
    where status is null;

alter table inntektsmelding
    alter column status set not null;

comment on column inntektsmelding.status is 'Status for inntektsmeldingen';
