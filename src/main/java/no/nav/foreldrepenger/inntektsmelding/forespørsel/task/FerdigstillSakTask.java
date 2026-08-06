package no.nav.foreldrepenger.inntektsmelding.forespørsel.task;

import java.util.Optional;
import java.util.UUID;

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
@ProsessTask(value = "forespørsel.ferdigstillSak")
public class FerdigstillSakTask implements ProsessTaskHandler {
    public static final String KEY_LUKKE_AARSAK = "lukkeAarsak";
    public static final String KEY_INNTEKTSMELDING_UUID = "inntektsmeldingUuid";
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
        var forespørselUuid = UUID.fromString(prosessTaskData.getPropertyValue(ForespørselTaskProperties.KEY_FORESPOERSEL_UUID));
        var forespørsel = forespørselTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel " + forespørselUuid + " ved ferdigstilling av sak"));

        var årsak = LukkeÅrsak.valueOf(prosessTaskData.getPropertyValue(KEY_LUKKE_AARSAK));
        var inntektsmeldingUuid = Optional.ofNullable(prosessTaskData.getPropertyValue(KEY_INNTEKTSMELDING_UUID)).map(UUID::fromString);
        var erFørstegangsinnsending = Boolean.parseBoolean(prosessTaskData.getPropertyValue(KEY_ER_FØRSTEGANGSINNSENDING));

        LOG.info("Ferdigstiller sak hos arbeidsgiverportalen for forespørsel {}", forespørselUuid);
        minSideArbeidsgiverTjeneste.ferdigstillSak(forespørsel, årsak, inntektsmeldingUuid, erFørstegangsinnsending);
        LOG.info("Ferdigstilte sak hos arbeidsgiverportalen for forespørsel {}", forespørselUuid);
    }
}
