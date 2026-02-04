# fp-inntektsmelding
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=navikt_fp-inntektsmelding&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=navikt_fp-inntektsmelding)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=navikt_fp-inntektsmelding&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=navikt_fp-inntektsmelding)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=navikt_fp-inntektsmelding&metric=bugs)](https://sonarcloud.io/summary/new_code?id=navikt_fp-inntektsmelding)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=navikt_fp-inntektsmelding&metric=coverage)](https://sonarcloud.io/summary/new_code?id=navikt_fp-inntektsmelding)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=navikt_fp-inntektsmelding&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=navikt_fp-inntektsmelding)

Backend for inntektsmelding for Team Foreldrepenger

## Oppdatere graphql skjema til Arbeidsgiver Notifikasjon API

Bytt ut [produsent.graphql](./src/main/resources/graphql/produsent.graphql) med SDL skjemaet som man kan lastes ned fra playground i
arbeidsgiver-notifikasjon sitt [notifikasjon-fake-produsent-api](https://notifikasjon-fake-produsent-api.ekstern.dev.nav.no/)

Oppdatert `schema.graphql` kan hentes [herfra](https://github.com/navikt/arbeidsgiver-notifikasjon-produsent-api/blob/main/app/src/main/resources/produsent.graphql).
