package no.nav.foreldrepenger.inntektsmelding.forespørsel.task;

import java.util.Comparator;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselEndringHistorikkDto;

import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.MinSideArbeidsgiverTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "forespørsel.sak.endreUttaksdato")
public class OppdaterSakMedEndretFørsteUttaksdatoTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(OppdaterSakMedEndretFørsteUttaksdatoTask.class);

    private ForespørselTjeneste forespørselTjeneste;
    private MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste;

    OppdaterSakMedEndretFørsteUttaksdatoTask() {
        // CDI
    }

    @Inject
    public OppdaterSakMedEndretFørsteUttaksdatoTask(ForespørselTjeneste forespørselTjeneste,
                                                  MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste) {
        this.forespørselTjeneste = forespørselTjeneste;
        this.minSideArbeidsgiverTjeneste = minSideArbeidsgiverTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var forespørselUuid = UUID.fromString(prosessTaskData.getPropertyValue(FellesTaskProperties.KEY_FORESPOERSEL_UUID));
        var forespørsel = forespørselTjeneste.hentForespørsel(forespørselUuid);
        if (forespørsel.status().equals(ForespørselStatus.UTGÅTT)) {
            LOG.info("Forespørsel {} er utgått, oppdaterer ikke sak", forespørselUuid);
            return;
        }
        var sisteEndring = forespørsel.historiskeEndringer().stream().max(Comparator.comparing(ForespørselEndringHistorikkDto::opprettetTid)).orElseThrow();
        minSideArbeidsgiverTjeneste.sendBeskjedOmEndretFørsteUttaksdato(forespørsel, sisteEndring.førsteUttaksdato(), forespørsel.førsteUttaksdato());
        LOG.info("Oppdaterte arbeidsgiverportalen med endret første uttaksdato for forespørsel {}", forespørselUuid);
    }
}
