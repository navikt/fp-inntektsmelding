package no.nav.familie.inntektsmelding.forvaltning;

import jakarta.enterprise.context.ApplicationScoped;

import jakarta.inject.Inject;

import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@ProsessTask(value = "slettForesporsel", maxFailedRuns = 1)
public class SlettForespørselTask implements ProsessTaskHandler {
    static final String FORESPØRSEL_UUID = "foresporselUuid";
    private static final Logger LOG = LoggerFactory.getLogger(SlettForespørselTask.class);
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    SlettForespørselTask() {
        // for CDI proxy
    }

    @Inject
    public SlettForespørselTask(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var forespørselUuid = Optional.ofNullable(prosessTaskData.getPropertyValue(FORESPØRSEL_UUID)).map(String::valueOf).orElseThrow();
        var forespørsel = forespørselBehandlingTjeneste.hentForespørsel(UUID.fromString(forespørselUuid)).orElseThrow();

        var stp = forespørsel.getSkjæringstidspunkt().orElseThrow();
        forespørselBehandlingTjeneste.slettForespørsel(new SaksnummerDto(forespørsel.getFagsystemSaksnummer().orElseThrow()),
            new OrganisasjonsnummerDto(forespørsel.getOrganisasjonsnummer()),
            stp);
        LOG.info("FEILAKTIGE_FORESPØRSLER: Forespørsel {} med oppgaveid {} for saksnummer {} med orgnummer {} og skjæringstidspunkt {} er slettet",
            forespørsel.getUuid(),
            Optional.ofNullable(forespørsel.getOppgaveId()),
            forespørsel.getFagsystemSaksnummer().orElse(null),
            forespørsel.getOrganisasjonsnummer(),
            stp);
    }
}

