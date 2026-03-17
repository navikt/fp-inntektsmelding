package no.nav.foreldrepenger.inntektsmelding.imapi.rest.forespørsel;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.lager.ForespørselEntitet;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.ArbeidsgiverInformasjonDto;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.imapi.rest.kontrakt.YtelseType;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.ForespørselStatusDto;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.YtelseTypeDto;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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


    public Optional<ForespørselDto> hentForesørselDto(UUID forespørselUuid) {
        var forespørselEntitet = forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid);
        return forespørselEntitet.map(this::mapTilDto);
    }


    public List<ForespørselDto> hentForespørslerDto(OrganisasjonsnummerDto orgnr,
                                                    String fnr,
                                                    ForespørselStatusDto status,
                                                    YtelseTypeDto ytelseTypeDto,
                                                    LocalDate fom,
                                                    LocalDate tom) {
        var resultater = forespørselBehandlingTjeneste.hentForespørsler(orgnr, fnr, status, ytelseTypeDto, fom, tom);
        return resultater.stream().map(this::mapTilDto).toList();
    }

    private ForespørselDto mapTilDto(ForespørselEntitet fp) {
        var fnr = personTjeneste.finnPersonIdentForAktørId(new AktørId(fp.getAktørId().getAktørId())).getIdent();
        return new ForespørselDto(fp.getUuid(),
            new ArbeidsgiverInformasjonDto(fp.getOrganisasjonsnummer()),
            fnr,
            fp.getFørsteUttaksdato(),
            //todo skal det være mulig å sende inn arbeidsgiverinitert fp gjennom api. Det er kun da denne kan være null
            fp.getSkjæringstidspunkt().orElse(null),
            mapForespørselStatus(fp.getStatus()),
            mapYtelsetype(fp.getYtelseType()),
            fp.getOpprettetTidspunkt());
    }

    private YtelseType mapYtelsetype(Ytelsetype ytelseType) {
        return switch (ytelseType) {
            case FORELDREPENGER -> YtelseType.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> YtelseType.SVANGERSKAPSPENGER;
        };
    }

    private ForespørselDto.Status mapForespørselStatus(ForespørselStatus status) {
        return switch (status) {
            case UNDER_BEHANDLING -> ForespørselDto.Status.UNDER_BEHANDLING;
            case FERDIG -> ForespørselDto.Status.FERDIG;
            case UTGÅTT -> ForespørselDto.Status.UTGÅTT;
        };
    }


}
