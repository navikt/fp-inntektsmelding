package no.nav.foreldrepenger.inntektsmelding.forespørsel.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.MinSideArbeidsgiverTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;


@ApplicationScoped
@ProsessTask(value = "forespørsel.sak.ferdigstill")
public class FerdigstillSakTask implements ProsessTaskHandler {
    public static final String KEY_ER_FØRSTEGANGSINNSENDING = "erFoerstegangsinnsending";
    private static final Logger LOG = LoggerFactory.getLogger(FerdigstillSakTask.class);

    private ForespørselTjeneste forespørselTjeneste;
    private MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste;

    FerdigstillSakTask() {
        // CDI
    }

    @Inject
    public FerdigstillSakTask(ForespørselTjeneste forespørselTjeneste, MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste) {
        this.forespørselTjeneste = forespørselTjeneste;
        this.minSideArbeidsgiverTjeneste = minSideArbeidsgiverTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var forespørsel = ForespørselTaskTjeneste.hentForespørsel(forespørselTjeneste, prosessTaskData);
        var årsak = LukkeÅrsak.valueOf(prosessTaskData.getPropertyValue(ForespørselTaskTjeneste.KEY_LUKKE_AARSAK));
        var inntektsmeldingUuid = ForespørselTaskTjeneste.hentInntektsmeldingUuid(prosessTaskData);
        var erFørstegangsinnsending = Boolean.parseBoolean(prosessTaskData.getPropertyValue(KEY_ER_FØRSTEGANGSINNSENDING));

        LOG.info("Ferdigstiller sak hos arbeidsgiverportalen for forespørsel {}", forespørsel.uuid());
        minSideArbeidsgiverTjeneste.ferdigstillSak(forespørsel, årsak, inntektsmeldingUuid, erFørstegangsinnsending);
        LOG.info("Ferdigstilte sak hos arbeidsgiverportalen for forespørsel {}", forespørsel.uuid());
    }
}
