package no.nav.foreldrepenger.inntektsmelding.imapi.rest.tjenester;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.inntektsmelding.felles.AvsenderSystemDto;
import no.nav.foreldrepenger.inntektsmelding.felles.BortfaltNaturalytelseDto;
import no.nav.foreldrepenger.inntektsmelding.felles.EndringsårsakDto;
import no.nav.foreldrepenger.inntektsmelding.felles.EndringsårsakerDto;
import no.nav.foreldrepenger.inntektsmelding.felles.FødselsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.felles.InnsendingstypeDto;
import no.nav.foreldrepenger.inntektsmelding.felles.InnsendingsårsakDto;
import no.nav.foreldrepenger.inntektsmelding.felles.KontaktpersonDto;
import no.nav.foreldrepenger.inntektsmelding.felles.NaturalytelsetypeDto;
import no.nav.foreldrepenger.inntektsmelding.felles.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.felles.SøktRefusjonDto;
import no.nav.foreldrepenger.inntektsmelding.felles.YtelseTypeDto;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.foreldrepenger.inntektsmelding.imapi.inntektsmelding.HentInntektsmeldingResponse;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Kildesystem;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;

@ApplicationScoped
public class InntektsmeldingKontraktMapper {

    private final ForespørselTjeneste forespørselTjeneste;
    private final PersonTjeneste personTjeneste;

    @Inject
    public InntektsmeldingKontraktMapper(ForespørselTjeneste forespørselTjeneste,
                                     PersonTjeneste personTjeneste) {
        this.forespørselTjeneste = forespørselTjeneste;
        this.personTjeneste = personTjeneste;
    }

    public HentInntektsmeldingResponse mapTilKontrakt(InntektsmeldingDto inntektsmelding) {
        // Slå opp forespørsel basert på inntektsmeldingen's egenskaper
        var forespørselOpt = forespørselTjeneste.finnForespørsler(
                inntektsmelding.getAktørId(),
                inntektsmelding.getYtelse(),
                inntektsmelding.getArbeidsgiver().orgnr())
            .stream()
            .filter(f -> inntektsmelding.getStartdato().equals(f.førsteUttaksdato()))
            .findFirst();
        var forespørselUuid = forespørselOpt.orElseThrow().uuid();

        // Slå opp fødselsnummer fra aktørId
        var fnr = personTjeneste.finnPersonIdentForAktørId(inntektsmelding.getAktørId());

        var innsendingsårsak = InnsendingsårsakDto.NY; // TODO trenger vi noe annet her?
        var innsendingstype = InnsendingstypeDto.FORESPURT;

        return new HentInntektsmeldingResponse(
            inntektsmelding.getInntektsmeldingUuid(),
            forespørselUuid,
            new FødselsnummerDto(fnr.getIdent()),
            mapKodeverk(inntektsmelding.getYtelse()),
            new OrganisasjonsnummerDto(inntektsmelding.getArbeidsgiver().orgnr()),
            new KontaktpersonDto(inntektsmelding.getKontaktperson().navn(), inntektsmelding.getKontaktperson().telefonnummer()),
            inntektsmelding.getStartdato(),
            inntektsmelding.getMånedInntekt(),
            innsendingsårsak,
            innsendingstype,
            inntektsmelding.getInnsendtTidspunkt(),
            inntektsmelding.getMånedRefusjon(),
            inntektsmelding.getOpphørsdatoRefusjon(),
            inntektsmelding.getAvsenderSystem() != null
                ? new AvsenderSystemDto(inntektsmelding.getAvsenderSystem().navn(), inntektsmelding.getAvsenderSystem().versjon())
                : null,
            inntektsmelding.getSøkteRefusjonsperioder().stream()
                .map(r -> new SøktRefusjonDto(r.fom(), r.beløp()))
                .toList(),
            inntektsmelding.getBortfaltNaturalytelsePerioder().stream()
                .map(b -> new BortfaltNaturalytelseDto(b.fom(), b.tom(), NaturalytelsetypeDto.valueOf(b.naturalytelsetype().name()), b.beløp()))
                .toList(),
            inntektsmelding.getEndringAvInntektÅrsaker().stream()
                .map(e -> new EndringsårsakerDto(EndringsårsakDto.valueOf(e.årsak().name()), e.fom(), e.tom(), e.bleKjentFom()))
                .toList()
        );
    }

    private YtelseTypeDto mapKodeverk(Ytelsetype ytelse) {
        return switch (ytelse) {
            case FORELDREPENGER -> YtelseTypeDto.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> YtelseTypeDto.SVANGERSKAPSPENGER;
        };
    }
}
