package no.nav.foreldrepenger.inntektsmelding.forvaltning;

import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Saksnummer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

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

        var stp = forespørsel.skjæringstidspunkt();
        var saksnummer = forespørsel.fagsystemSaksnummer();
        var arbeidsgiver = forespørsel.arbeidsgiver();

        forespørselBehandlingTjeneste.slettForespørsel(saksnummer, arbeidsgiver, stp);

        LOG.info("FEILAKTIGE_FORESPØRSLER: Forespørsel {} med oppgaveid {} for saksnummer {} med orgnummer {} og skjæringstidspunkt {} er slettet",
            forespørsel.uuid(),
            forespørsel.oppgaveId(),
            saksnummer.saksnummer(),
            arbeidsgiver.orgnr(),
            stp);
    }
}

