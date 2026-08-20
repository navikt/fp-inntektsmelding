package no.nav.foreldrepenger.inntektsmelding.forespørsel.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.MinSideArbeidsgiverTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(value = "forespørsel.sak.utgått")
public class SettSakTilUtgåttTask extends ForespørselProsessTask {
    private static final Logger LOG = LoggerFactory.getLogger(SettSakTilUtgåttTask.class);

    private MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste;

    SettSakTilUtgåttTask() {
        // CDI
    }

    @Inject
    public SettSakTilUtgåttTask(ForespørselTjeneste forespørselTjeneste, MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste) {
        super(forespørselTjeneste);
        this.minSideArbeidsgiverTjeneste = minSideArbeidsgiverTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var forespørsel = hentForespørsel(prosessTaskData);

        LOG.info("Setter sak hos arbeidsgiverportalen til utgått for forespørsel {}", forespørsel.uuid());
        minSideArbeidsgiverTjeneste.settSakTilUtgått(forespørsel);
        LOG.info("Satte sak hos arbeidsgiverportalen til utgått for forespørsel {}", forespørsel.uuid());
    }
}
