package no.nav.foreldrepenger.inntektsmelding.forespørsel.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.MinSideArbeidsgiverTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "forespørsel.sak.oppdater")
public class OppdaterSakMedEndretInntektsmeldingTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(OppdaterSakMedEndretInntektsmeldingTask.class);

    private ForespørselTaskTjeneste forespørselTaskTjeneste;
    private MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste;

    OppdaterSakMedEndretInntektsmeldingTask() {
        // CDI
    }

    @Inject
    public OppdaterSakMedEndretInntektsmeldingTask(ForespørselTaskTjeneste forespørselTaskTjeneste, MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste) {
        this.forespørselTaskTjeneste = forespørselTaskTjeneste;
        this.minSideArbeidsgiverTjeneste = minSideArbeidsgiverTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var forespørsel = forespørselTaskTjeneste.hentForespørsel(prosessTaskData);
        var inntektsmeldingUuid = forespørselTaskTjeneste.hentInntektsmeldingUuid(prosessTaskData)
            .orElseThrow(() -> new IllegalStateException("Mangler inntektsmeldingUuid for forespørsel " + forespørsel.uuid()));

        LOG.info("Oppdaterer sak hos arbeidsgiverportalen med endret inntektsmelding for forespørsel {}", forespørsel.uuid());
        minSideArbeidsgiverTjeneste.sendBeskjedOmOppdatertInntektsmelding(forespørsel, inntektsmeldingUuid);
        LOG.info("Oppdaterte sak hos arbeidsgiverportalen med endret inntektsmelding for forespørsel {}", forespørsel.uuid());
    }
}
