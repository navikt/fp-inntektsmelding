package no.nav.foreldrepenger.inntektsmelding.imapi.rest.forespørsel;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.ArbeidsgiverDto;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.FødselsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.HentForespørselResponse;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.YtelseType;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Fødselsnummer;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.ForespørselStatusDto;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.YtelseTypeDto;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;

@ApplicationScoped
public class ForespørselApiTjeneste {
    private PersonTjeneste personTjeneste;
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    ForespørselApiTjeneste() {
        // CDI
    }

    @Inject
    public ForespørselApiTjeneste(PersonTjeneste personTjeneste,
                                  ForespørselBehandlingTjeneste forespørselBehandlingTjeneste) {
        this.personTjeneste = personTjeneste;
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
    }


    public Optional<HentForespørselResponse> hentForesørselDto(UUID forespørselUuid) {
        return forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid).map(this::mapTilResponseDto);
    }


    public List<HentForespørselResponse> hentForespørslerDto(Arbeidsgiver arbeidsgiver,
                                                             Fødselsnummer fnr,
                                                             ForespørselStatusDto status,
                                                             YtelseTypeDto ytelseTypeDto,
                                                             LocalDate fom,
                                                             LocalDate tom) {
        var resultater = forespørselBehandlingTjeneste.hentForespørsler(arbeidsgiver, fnr, status, ytelseTypeDto, fom, tom);
        return resultater.stream().map(this::mapTilResponseDto).toList();
    }

    private HentForespørselResponse mapTilResponseDto(ForespørselDto fp) {
        var fnr = personTjeneste.finnPersonIdentForAktørId(fp.aktørId()).getIdent();
        return new HentForespørselResponse(fp.uuid(),
            new ArbeidsgiverDto(fp.arbeidsgiver().orgnr()),
            new FødselsnummerDto(fnr),
            fp.førsteUttaksdato(),
            fp.skjæringstidspunkt(),
            mapForespørselStatus(fp.status()),
            mapYtelsetype(fp.ytelseType()),
            fp.opprettetTidspunkt());
    }

    private YtelseType mapYtelsetype(Ytelsetype ytelseType) {
        return switch (ytelseType) {
            case FORELDREPENGER -> YtelseType.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> YtelseType.SVANGERSKAPSPENGER;
        };
    }

    private HentForespørselResponse.Status mapForespørselStatus(ForespørselStatus status) {
        return switch (status) {
            case UNDER_BEHANDLING -> HentForespørselResponse.Status.UNDER_BEHANDLING;
            case FERDIG -> HentForespørselResponse.Status.FERDIG;
            case UTGÅTT -> HentForespørselResponse.Status.UTGÅTT;
        };
    }

}
