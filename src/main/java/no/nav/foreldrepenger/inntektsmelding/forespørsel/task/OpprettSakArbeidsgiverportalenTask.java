package no.nav.foreldrepenger.inntektsmelding.forespørsel.task;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTekster;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.MinSideArbeidsgiverTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "opprett.sak.arbeidsgiverportalen")
public class OpprettSakArbeidsgiverportalenTask implements ProsessTaskHandler {
    private static final String FORESPØRSEL_UUID = "forespørselUuid";
    private static final Logger LOG = LoggerFactory.getLogger(OpprettSakArbeidsgiverportalenTask.class);
    private MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste;
    private ForespørselTjeneste forespørselTjeneste;

    OpprettSakArbeidsgiverportalenTask() {
        // CDI
    }

    @Inject
    OpprettSakArbeidsgiverportalenTask(MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste,
                                       ForespørselTjeneste forespørselTjeneste) {
        this.minSideArbeidsgiverTjeneste = minSideArbeidsgiverTjeneste;
        this.forespørselTjeneste = forespørselTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var forespørselUuid = UUID.fromString(prosessTaskData.getPropertyValue(FORESPØRSEL_UUID));
        var forespørselDto = forespørselTjeneste.hentForespørsel(forespørselUuid).orElseThrow();
        LOG.info("Starter task for opprettelse av sak i arbeidsgiverportalen for saksnummer {} og orgnr {}", forespørselDto.fagsystemSaksnummer(), forespørselDto.arbeidsgiver());

        var fagerSakId = minSideArbeidsgiverTjeneste.opprettSak(forespørselDto);
        var tilleggsinformasjon = ForespørselTekster.lagTilleggsInformasjon(LukkeÅrsak.ORDINÆR_INNSENDING, forespørselDto.førsteUttaksdato());
        minSideArbeidsgiverTjeneste.oppdaterSakTilleggsinformasjon(fagerSakId, tilleggsinformasjon);
        forespørselTjeneste.setArbeidsgiverNotifikasjonSakId(forespørselUuid, fagerSakId);

        // Trenger ikke bestille oppgaver for arbeidsgiverinitierte forespørsler
        if (ForespørselType.BESTILT_AV_FAGSYSTEM.equals(forespørselDto.forespørselType())) {
            String oppgaveId;
            try {
                oppgaveId = minSideArbeidsgiverTjeneste.opprettOppgave(forespørselDto);
            } catch (Exception e) {
                //Manuell rollback er nødvendig fordi sak og oppgave går i to forskjellige kall
                minSideArbeidsgiverTjeneste.slettSak(fagerSakId);
                throw e;
            }
            forespørselTjeneste.setOppgaveId(forespørselUuid, oppgaveId);
        }
        LOG.info("Sluttfører task for opprettelse av sak i arbeidsgiverportalen for saksnummer {} og orgnr {}", forespørselDto.fagsystemSaksnummer(), forespørselDto.arbeidsgiver());
    }

    public static ProsessTaskData lagTask(UUID forespørselUuid) {
        var task = ProsessTaskData.forProsessTask(OpprettSakArbeidsgiverportalenTask.class);
        // Viktig at tasker som skal oppdatere forespørselentitet alle deler samme gruppe (forespørselUuid) så vi ikke får lås
        task.setGruppe(forespørselUuid.toString());
        task.setProperty(FORESPØRSEL_UUID, forespørselUuid.toString());
        return task;
    }
}
