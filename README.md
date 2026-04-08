# fp-inntektsmelding
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=navikt_fp-inntektsmelding&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=navikt_fp-inntektsmelding)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=navikt_fp-inntektsmelding&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=navikt_fp-inntektsmelding)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=navikt_fp-inntektsmelding&metric=bugs)](https://sonarcloud.io/summary/new_code?id=navikt_fp-inntektsmelding)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=navikt_fp-inntektsmelding&metric=coverage)](https://sonarcloud.io/summary/new_code?id=navikt_fp-inntektsmelding)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=navikt_fp-inntektsmelding&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=navikt_fp-inntektsmelding)

***

Backend for inntektsmelding for Team Foreldrepenger

## Oppdatere graphql skjema til Arbeidsgiver Notifikasjon API

Bytt ut [produsent.graphql](./src/main/resources/graphql/produsent.graphql) med SDL skjemaet som man kan lastes ned fra playground i
arbeidsgiver-notifikasjon sitt [notifikasjon-fake-produsent-api](https://notifikasjon-fake-produsent-api.ekstern.dev.nav.no/)

Oppdatert `schema.graphql` kan hentes [herfra](https://github.com/navikt/arbeidsgiver-notifikasjon-produsent-api/blob/main/app/src/main/resources/produsent.graphql).

## Architecture Summary

**REST Layer (11 endpoints):**
- `InntektsmeldingDialogRest` — Main employer dialog (TokenX) `/imdialog`
- `ArbeidsgiverinitiertDialogRest` — Employer-initiated flows (TokenX) `/arbeidsgiverinitiert`
- `KvitteringRest` — PDF receipt downloads (TokenX) `/bekreftelse`
- `ForespørselRest` — Forespørsel management from fpsak (Azure) `/foresporsel`
- `ForespørselApiRest` — External forespørsel queries (Azure) `/imapi/foresporsel`
- `InntektsmeldingApiRest` — Fetch inntektsmelding by UUID (Azure) `/imapi/inntektsmelding`
- `InntektsmeldingFpsakRest` — Override inntektsmelding from saksbehandler (Azure) `/overstyring`
- `OppgaverForvaltningRestTjeneste` — Admin/drift task management (Azure) `/forvaltningOppgaver`
- `DialogportenForvaltningRestTjeneste` — Dialogporten admin (Azure) `/dialogporten`
- `ProsessTaskRestTjeneste` — Prosesstask admin (Azure) `/prosesstask`
- `ForespørselVtpRest` — VTP test support (Azure) `/foresporsel`

**Service Layer (13 tjenester):**
- `ForespørselBehandlingTjeneste` — Main orchestrator for forespørsel lifecycle
- `ForespørselTjeneste` — Thin CRUD wrapper around ForespørselRepository
- `ForespørselApiTjeneste` — External API queries (wraps ForespørselBehandlingTjeneste + PersonTjeneste)
- `InntektsmeldingMottakTjeneste` — Receives and processes new inntektsmeldinger
- `InntektsmeldingTjeneste` — CRUD for inntektsmeldinger
- `InntektsmeldingOverstyringTjeneste` — Handles overridden inntektsmeldinger
- `InntektsmeldingXMLTjeneste` — XML generation for inntektsmeldinger
- `GrunnlagDtoTjeneste` — Assembles dialog DTOs from multiple sources
- `KvitteringTjeneste` — PDF generation for receipts
- `DokumentGeneratorTjeneste` — Document generation via fp-dokgen
- `TilgangTjeneste` — Access control orchestration
- `PipTjeneste` — Policy Information Point for access control
- `AltinnTilgangTjeneste` — Altinn permission checks

**Repository Layer (2 repositories → PostgreSQL):**
- `ForespørselRepository` — Manages `ForespørselEntitet` (forespørsel table)
- `InntektsmeldingRepository` — Manages `InntektsmeldingEntitet` (inntektsmelding table)
