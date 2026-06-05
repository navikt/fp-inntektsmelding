# fp-inntektsmelding

Backend for employer inntektsmelding flows.

## Shared context

- Source of truth for shared domain, architecture, and conventions: `navikt/fp-context`
- Copilot Space: `navikt/TeamForeldrepenger`

## Repo-specific context

| Topic      | Details                                                                                        |
|------------|------------------------------------------------------------------------------------------------|
| Role       | Handles forespørsler from `fp-sak`, bridge to FAGER, receives inntektsmeldinger from employers |
| Consumers  | `fp-sak`, `fp-inntektsmelding-frontend`, `fp-inntektsmelding-api`                              |
| Tech stack | Standard fp Java backend using `fp-prosesstask`                                                |
| Data       | PostgreSQL forepørsler and received inntektsmeldinger                                          |

Team FAGER is responsible for Nav's employer portal (MinSide Arbeidsgiver): Inntektsmelding flows are included in this portal: tasks, messages, and links to `fp-inntektsmelding-frontend` for submitting inntektsmelding. FAGER use AltInn for distributing sms and emails to employers. 

Data sources / REST integrations besides PDL:
- Aa-register: employment data
- EREG: Company registration data (source Brreg)
- Inntekt: Monthly income - proxy for Skatteetaten Inntekt API
- MinSideArbeidsgiver (FAGER): Creates and queries requests for inntektmelding.
- Joark: Journaling incoming inntektsmelding
- `fp-sak`: Sak information for non-requested inntektsmeldinger
- `fp-dokgen`: Generating PDF for inntektsmelding journaling, and receipts

Multi-authority authentication (TokenX and Azure/Entra). Authorization using AltInn tilganger and maskinporten.

## Domain model

- `ForespørselEntitet`: requests for inntektsmelding originating from `fp-sak` 
- `InntektsmeldingEntitet`: received inntektsmeldinger from `fp-inntektsmelding-frontend` or `fp-inntektsmelding-api`

## Entry points

- `ForespørselRest`: create and close forespørsler from `fp-sak`
- `InntektsmeldingDialogRest`: Support for requested inntektsmelding in `fp-inntektsmelding-frontend`
- `ArbeidsgiverinitiertDialogRest`: Support for non-requested inntektsmelding in `fp-inntektsmelding-frontend`
- `InntektsmeldingFpsakRest`: Management service used by `fp-sak` to submit correct inntektsmelding
- `PdfDokumentRest`: Return PDF version of inntektsmelding to consumers
- `ForespørselApiRest`: Service for retreiveing forespørsel used by `fp-inntektsmelding-api`
- `InntektsmeldingApiRest`: Service for submitting inntektsmelding used by `fp-inntektsmelding-api`

## Kontrakter sub-build

`kontrakter/` is released separately as `inntektsmelding-kontrakt` and consumed by `fp-sak` and `fp-inntektsmelding-api`.

## Verification

- For integration impact, verify via `navikt/fp-autotest`.
- Most relevant suite: `verdikjede`.
