package no.nav.familie.inntektsmelding.server.tilgangsstyring;

import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;

import java.util.UUID;

public interface Tilgang {
    /**
     * Her hentes organisasjonsnummeret fra forespørselen sendt inn av fagsystemet (fra PIP-tjenesten),
     * og deretter sjekkes det i Altinn om brukeren som initierte kallet, har tilgang til den aktuelle bedriften.
     *
     * @param forespørselUuid - IM forespørsel som ble generert når fagsystemet bestilte IM.
     * @throws no.nav.vedtak.exception.ManglerTilgangException om tilgangen ikke er gitt.
     */
    void sjekkAtArbeidsgiverHarTilgangTilBedrift(UUID forespørselUuid);
    /**
     * Her sjekkes tilgang i Altinn gitt orgnummer for å se om brukeren som initierte kallet,
     * har tilgang til den aktuelle bedriften.
     * @param organisasjonsnummer - organisasjonsnummer som det forsøkes å sende inntektsmelding for.
     * @throws no.nav.vedtak.exception.ManglerTilgangException om tilgangen ikke er gitt.
     */
    void sjekkAtArbeidsgiverHarTilgangTilBedrift(OrganisasjonsnummerDto organisasjonsnummer);
    /**
     * Her hentes organisasjonsnummer knyttet til en tidligere innsendt inntektsmelding (fra PIP tjenesten),
     * og deretter sjekkes det i Altinn om brukeren som initierte kallet, har tilgang til den aktuelle bedriften.
     *
     * @param inntektsmeldingId - IM id som ble tidligere innsendt.
     * @throws no.nav.vedtak.exception.ManglerTilgangException om tilgangen ikke er gitt.
     */
    void sjekkAtArbeidsgiverHarTilgangTilBedrift(long inntektsmeldingId);

    /**
     * Her hentes organisasjonsnummer knyttet til en tidligere innsendt inntektsmelding (fra PIP tjenesten),
     * og deretter sjekkes det i Altinn om brukeren som initierte kallet, har tilgang til den aktuelle bedriften.
     *
     * @param inntektsmeldingUuid - UUID som unikt identifiserer en tidligere innsendt inntektsmelding.
     * @throws no.nav.vedtak.exception.ManglerTilgangException om tilgangen ikke er gitt.
     */
    void sjekkAtArbeidsgiverHarTilgangTilInntektsmelding(UUID inntektsmeldingUuid);

    /**
     * Sjekker om den Nav ansatte som prøver å utføre operasjonen har en DRIFT rolle.
     * Brukes kun i swagger sammenheng.
     *
     * @throws no.nav.vedtak.exception.ManglerTilgangException om tilgangen ikke er gitt.
     */
    void sjekkAtAnsattHarRollenDrift();

    /**
     * Sjekker om den Nav ansatte som prøver å utføre operasjonen har en SAKSBEHANDLER rolle.
     * Brukes når det gjøres synkrone restkall fra fagsystemet initiert av en saksbehandler.
     *
     * @throws no.nav.vedtak.exception.ManglerTilgangException om tilgangen ikke er gitt.
     */
    void sjekkAtAnsattHarRollenSaksbehandler();

    /**
     * Sjekker at det er et systembruker som står bak kallet.
     *
     * @throws no.nav.vedtak.exception.ManglerTilgangException om tilgangen ikke er gitt.
     */
    void sjekkErSystembruker();
}
