package no.nav.familie.inntektsmelding.forespørsel.rest.ekstern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.typer.dto.ForespørselStatusDto;
import no.nav.familie.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ForespørselEksternTjeneste {
    private PersonTjeneste personTjeneste;
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    ForespørselEksternTjeneste() {
        // CDI
    }

    @Inject
    public ForespørselEksternTjeneste(PersonTjeneste personTjeneste,
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
        var fnr = personTjeneste.finnPersonIdentForAktørId(fp.getAktørId()).getIdent();
        return new ForespørselDto(fp.getUuid(),
            new OrganisasjonsnummerDto(fp.getOrganisasjonsnummer()),
            fnr,
            fp.getFørsteUttaksdato(),
            //todo skal det være mulig å sende inn arbeidsgiverinitert fp gjennom api. Det er kun da denne kan være null
            fp.getSkjæringstidspunkt().orElse(null),
            KodeverkMapper.mapForespørselStatus(fp.getStatus()),
            KodeverkMapper.mapYtelsetype(fp.getYtelseType()),
            fp.getOpprettetTidspunkt());
    }

}
