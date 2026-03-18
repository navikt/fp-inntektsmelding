package no.nav.foreldrepenger.inntektsmelding.forespørsel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.inntektsmelding.felles.ForespørselResultat;
import no.nav.foreldrepenger.inntektsmelding.felles.OrganisasjonsnummerDto;

import java.util.List;

public record OpprettForespørselRespons(@NotNull List<@Valid OrganisasjonsnummerMedStatus> organisasjonsnumreMedStatus) {
    public record OrganisasjonsnummerMedStatus(@NotNull @Valid OrganisasjonsnummerDto organisasjonsnummerDto, ForespørselResultat status) {}
}
