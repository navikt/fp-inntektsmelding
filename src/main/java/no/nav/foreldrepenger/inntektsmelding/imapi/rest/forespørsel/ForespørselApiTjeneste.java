package no.nav.foreldrepenger.inntektsmelding.imapi.rest.forespørsel;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.inntektsmelding.felles.ForespørselStatusDto;
import no.nav.foreldrepenger.inntektsmelding.felles.FødselsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.felles.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.felles.YtelseTypeDto;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.imapi.forespørsel.ForespørselResponse;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Fødselsnummer;
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


    public Optional<ForespørselResponse> hentForesørselDto(UUID forespørselUuid) {
        return forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid).map(this::mapTilResponseDto);
    }


    public List<ForespørselResponse> hentForespørslerDto(Arbeidsgiver arbeidsgiver,
                                                             Fødselsnummer fnr,
                                                             no.nav.foreldrepenger.inntektsmelding.typer.dto.ForespørselStatusDto status,
                                                             no.nav.foreldrepenger.inntektsmelding.typer.dto.YtelseTypeDto ytelseTypeDto,
                                                             LocalDate fom,
                                                             LocalDate tom) {
        var resultater = forespørselBehandlingTjeneste.hentForespørsler(arbeidsgiver, fnr, status, ytelseTypeDto, fom, tom);
        return resultater.stream().map(this::mapTilResponseDto).toList();
    }

    private ForespørselResponse mapTilResponseDto(ForespørselDto fp) {
        var fnr = personTjeneste.finnPersonIdentForAktørId(fp.aktørId()).getIdent();
        return new ForespørselResponse(fp.uuid(),
            new OrganisasjonsnummerDto(fp.arbeidsgiver().orgnr()),
            new FødselsnummerDto(fnr),
            fp.førsteUttaksdato(),
            fp.skjæringstidspunkt(),
            mapForespørselStatus(fp.status()),
            mapYtelsetype(fp.ytelseType()),
            fp.opprettetTidspunkt());
    }

    private YtelseTypeDto mapYtelsetype(Ytelsetype ytelseType) {
        return switch (ytelseType) {
            case FORELDREPENGER -> YtelseTypeDto.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> YtelseTypeDto.SVANGERSKAPSPENGER;
        };
    }

    private ForespørselStatusDto mapForespørselStatus(ForespørselStatus status) {
        return switch (status) {
            case UNDER_BEHANDLING -> ForespørselStatusDto.UNDER_BEHANDLING;
            case FERDIG -> ForespørselStatusDto.FERDIG;
            case UTGÅTT -> ForespørselStatusDto.UTGÅTT;
        };
    }

}
