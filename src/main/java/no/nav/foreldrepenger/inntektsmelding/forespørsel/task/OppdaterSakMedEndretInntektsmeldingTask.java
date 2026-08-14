package no.nav.foreldrepenger.inntektsmelding.forespørsel.task;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.MinSideArbeidsgiverTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "forespørsel.sak.oppdater")
public class OppdaterSakMedEndretInntektsmeldingTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(OppdaterSakMedEndretInntektsmeldingTask.class);

    private ForespørselTjeneste forespørselTjeneste;
    private MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste;

    OppdaterSakMedEndretInntektsmeldingTask() {
        // CDI
    }

    @Inject
    public OppdaterSakMedEndretInntektsmeldingTask(ForespørselTjeneste forespørselTjeneste, MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste) {
        this.forespørselTjeneste = forespørselTjeneste;
        this.minSideArbeidsgiverTjeneste = minSideArbeidsgiverTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var forespørselUuid = UUID.fromString(prosessTaskData.getPropertyValue(FellesTaskProperties.KEY_FORESPOERSEL_UUID));
        var forespørsel = forespørselTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel " + forespørselUuid + " ved oppdatering av sak"));
        var inntektsmeldingUuid = UUID.fromString(prosessTaskData.getPropertyValue(FellesTaskProperties.KEY_INNTEKTSMELDING_UUID));

        LOG.info("Oppdaterer sak hos arbeidsgiverportalen med endret inntektsmelding for forespørsel {}", forespørselUuid);
        minSideArbeidsgiverTjeneste.sendBeskjedOmOppdatertInntektsmelding(forespørsel, inntektsmeldingUuid);
        LOG.info("Oppdaterte sak hos arbeidsgiverportalen med endret inntektsmelding for forespørsel {}", forespørselUuid);
    }
}
