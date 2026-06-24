package no.nav.foreldrepenger.inntektsmelding.forespørsel.task;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTekster;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.MinSideArbeidsgiverTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "utgått.sak.arbeidsgiverportalen")
public class UtgåSakArbeidsgiverportalenTask implements ProsessTaskHandler {
    private static final String FORESPØRSEL_UUID = "forespørselUuid";
    private static final Logger LOG = LoggerFactory.getLogger(UtgåSakArbeidsgiverportalenTask.class);
    private MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste;
    private ForespørselTjeneste forespørselTjeneste;

    UtgåSakArbeidsgiverportalenTask() {
        // CDI
    }

    @Inject
    UtgåSakArbeidsgiverportalenTask(MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste,
                                    ForespørselTjeneste forespørselTjeneste) {
        this.minSideArbeidsgiverTjeneste = minSideArbeidsgiverTjeneste;
        this.forespørselTjeneste = forespørselTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var forespørselUuid = UUID.fromString(prosessTaskData.getPropertyValue(FORESPØRSEL_UUID));
        var forespørselDto = forespørselTjeneste.hentForespørsel(forespørselUuid).orElseThrow();
        LOG.info("Starter task for å sette sak til utgått i arbeidsgiverportalen for saksnummer {} og orgnr {}", forespørselDto.fagsystemSaksnummer(), forespørselDto.arbeidsgiver());

        // Utgå oppgave
        Optional.ofNullable(forespørselDto.oppgaveId())
            .ifPresent(oppgaveId -> minSideArbeidsgiverTjeneste.oppgaveUtgått(oppgaveId, OffsetDateTime.now()));

        // Oppdater sak med melding om at den er utgått
        minSideArbeidsgiverTjeneste.oppdaterSakTilleggsinformasjon(forespørselDto.arbeidsgiverNotifikasjonSakId(),
            ForespørselTekster.lagTilleggsInformasjon(LukkeÅrsak.UTGÅTT, forespørselDto.førsteUttaksdato()));

        LOG.info("Sluttfører task for å sette sak til utgått i arbeidsgiverportalen for saksnummer {} og orgnr {}", forespørselDto.fagsystemSaksnummer(), forespørselDto.arbeidsgiver());
    }

    public static ProsessTaskData lagTask(UUID forespørselUuid) {
        var task = ProsessTaskData.forProsessTask(UtgåSakArbeidsgiverportalenTask.class);
        // Viktig at tasker som skal oppdatere forespørselentitet alle deler samme gruppe (forespørselUuid) så vi ikke får lås
        task.setGruppe(forespørselUuid.toString());
        task.setProperty(FORESPØRSEL_UUID, forespørselUuid.toString());
        return task;
    }
}
