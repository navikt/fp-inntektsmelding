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
@ProsessTask(value = "forespørsel.sak.oppdater")
public class OppdaterSakMedEndretInntektsmeldingTask extends ForespørselProsessTask {
    private static final Logger LOG = LoggerFactory.getLogger(OppdaterSakMedEndretInntektsmeldingTask.class);

    private MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste;

    OppdaterSakMedEndretInntektsmeldingTask() {
        // CDI
    }

    @Inject
    public OppdaterSakMedEndretInntektsmeldingTask(ForespørselTjeneste forespørselTjeneste, MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste) {
        super(forespørselTjeneste);
        this.minSideArbeidsgiverTjeneste = minSideArbeidsgiverTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var forespørsel = hentForespørsel(prosessTaskData);
        var inntektsmeldingUuid = hentInntektsmeldingUuid(prosessTaskData)
            .orElseThrow(() -> new IllegalStateException("Mangler inntektsmeldingUuid for forespørsel " + forespørsel.uuid()));

        LOG.info("Oppdaterer sak hos arbeidsgiverportalen med endret inntektsmelding for forespørsel {}", forespørsel.uuid());
        minSideArbeidsgiverTjeneste.sendBeskjedOmOppdatertInntektsmelding(forespørsel, inntektsmeldingUuid);
        LOG.info("Oppdaterte sak hos arbeidsgiverportalen med endret inntektsmelding for forespørsel {}", forespørsel.uuid());
    }
}
