package no.nav.foreldrepenger.inntektsmelding.imapi.rest.tjenester;

import no.nav.foreldrepenger.inntektsmelding.felles.AvsenderSystemDto;
import no.nav.foreldrepenger.inntektsmelding.felles.BortfaltNaturalytelseDto;
import no.nav.foreldrepenger.inntektsmelding.felles.EndringsårsakDto;
import no.nav.foreldrepenger.inntektsmelding.felles.EndringsårsakerDto;
import no.nav.foreldrepenger.inntektsmelding.felles.FødselsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.felles.InnsendingstypeDto;
import no.nav.foreldrepenger.inntektsmelding.felles.InntektsmeldingStatusDto;
import no.nav.foreldrepenger.inntektsmelding.felles.KontaktpersonDto;
import no.nav.foreldrepenger.inntektsmelding.felles.NaturalytelsetypeDto;
import no.nav.foreldrepenger.inntektsmelding.felles.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.felles.SøktRefusjonDto;
import no.nav.foreldrepenger.inntektsmelding.felles.YtelseTypeDto;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.imapi.inntektsmelding.HentInntektsmeldingResponse;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.InntektsmeldingStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Kildesystem;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;


public class InntektsmeldingKontraktMapper {

    private InntektsmeldingKontraktMapper() {
        // Skjuler default
    }

    public static InntektsmeldingStatusDto mapTilKontrakt(InntektsmeldingStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case AVVIST -> InntektsmeldingStatusDto.AVVIST;
            case VENTER_VURDERING -> InntektsmeldingStatusDto.VENTER_VURDERING;
            case GODKJENT -> InntektsmeldingStatusDto.GODKJENT;
            case UTDATERT -> InntektsmeldingStatusDto.UTDATERT;
        };
    }

    public static HentInntektsmeldingResponse mapTilKontrakt(InntektsmeldingDto inntektsmelding, PersonIdent personIdent) {
        return new HentInntektsmeldingResponse(
            inntektsmelding.getId(),
            inntektsmelding.getInntektsmeldingUuid(),
            inntektsmelding.getForespørsel().map(ForespørselDto::uuid).orElse(null),
            new FødselsnummerDto(personIdent.getIdent()),
            mapKodeverk(inntektsmelding.getYtelse()),
            new OrganisasjonsnummerDto(inntektsmelding.getArbeidsgiver().orgnr()),
            new KontaktpersonDto(inntektsmelding.getKontaktperson().navn(), inntektsmelding.getKontaktperson().telefonnummer()),
            inntektsmelding.getStartdato(),
            inntektsmelding.getMånedInntekt(),
            inntektsmelding.getInnsendtTidspunkt(),
            inntektsmelding.getMånedRefusjon(),
            inntektsmelding.getOpphørsdatoRefusjon(),
            inntektsmelding.getAvsenderSystem() != null
            ? new AvsenderSystemDto(inntektsmelding.getAvsenderSystem().navn(), inntektsmelding.getAvsenderSystem().versjon())
            : new AvsenderSystemDto("NAV_NO", "1.0"),
            inntektsmelding.getSøkteRefusjonsperioder().stream()
                .map(r -> new SøktRefusjonDto(r.fom(), r.beløp()))
                .toList(),
            inntektsmelding.getBortfaltNaturalytelsePerioder().stream()
                .map(b -> new BortfaltNaturalytelseDto(b.fom(), b.tom(), NaturalytelsetypeDto.valueOf(b.naturalytelsetype().name()), b.beløp()))
                .toList(),
            inntektsmelding.getEndringAvInntektÅrsaker().stream()
                .map(e -> new EndringsårsakerDto(EndringsårsakDto.valueOf(e.årsak().name()), e.fom(), e.tom(), e.bleKjentFom()))
                .toList(),
            mapStatus(inntektsmelding.getStatus()),
            mapInnsendingstype(inntektsmelding.getKildesystem(), inntektsmelding.getForespørsel().orElse(null), inntektsmelding.getId()),
            inntektsmelding.getForespørsel().map(ForespørselDto::skjæringstidspunkt).orElse(null)
        );
    }

    private static InnsendingstypeDto mapInnsendingstype(Kildesystem kildesystem, ForespørselDto forespørsel, Long inntektsmeldingId ) {
        if (forespørsel == null) {
            throw new IllegalStateException("InntektsmeldingDtoMapper: Finner ikke forespørsel for inntektsmelding med id:" + inntektsmeldingId);
        }

        if (ForespørselType.ARBEIDSGIVERINITIERT_UREGISTRERT.equals(forespørsel.forespørselType()) || ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT.equals(
            forespørsel.forespørselType())) {
            return InnsendingstypeDto.ARBEIDSGIVER_INITIERT;
        }  else if (Kildesystem.LØNN_OG_PERSONAL_SYSTEM.equals(kildesystem)) {
            return InnsendingstypeDto.FORESPURT_EKSTERN;
        } else {
            return InnsendingstypeDto.FORESPURT;
        }
    }

    private static YtelseTypeDto mapKodeverk(Ytelsetype ytelse) {
        return switch (ytelse) {
            case FORELDREPENGER -> YtelseTypeDto.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> YtelseTypeDto.SVANGERSKAPSPENGER;
        };
    }

    private static InntektsmeldingStatusDto mapStatus(InntektsmeldingStatus status) {
        return switch (status) {
            case AVVIST -> InntektsmeldingStatusDto.AVVIST;
            case VENTER_VURDERING -> InntektsmeldingStatusDto.VENTER_VURDERING;
            case GODKJENT -> InntektsmeldingStatusDto.GODKJENT;
            case UTDATERT -> InntektsmeldingStatusDto.UTDATERT;
        };
    }
}
