package no.nav.foreldrepenger.inntektsmelding.forespørsel.task;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTekster;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.MinSideArbeidsgiverTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "ferdigstill.sak.arbeidsgiverportalen")
public class FerdigstillSakArbeidsgiverportalenTask implements ProsessTaskHandler {
    private static final String FORESPØRSEL_UUID = "forespørselUuid";
    private static final String INNTEKTSMELDING_UUID = "inntektsmeldingUuid"; // Nullable, da inntektsmelding kan komme fra eksternt system
    private static final String LUKKE_ÅRSAK = "lukkeÅrsak"; // Kan denne utledes fra forespørsel istedenfor å sendes med?
    private static final Logger LOG = LoggerFactory.getLogger(FerdigstillSakArbeidsgiverportalenTask.class);
    private MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste;
    private ForespørselTjeneste forespørselTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;

    FerdigstillSakArbeidsgiverportalenTask() {
        // CDI
    }

    @Inject
    FerdigstillSakArbeidsgiverportalenTask(MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste,
                                           ForespørselTjeneste forespørselTjeneste,
                                           InntektsmeldingTjeneste inntektsmeldingTjeneste) {
        this.minSideArbeidsgiverTjeneste = minSideArbeidsgiverTjeneste;
        this.forespørselTjeneste = forespørselTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var forespørselUuid = UUID.fromString(prosessTaskData.getPropertyValue(FORESPØRSEL_UUID));
        var inntektsmeldingUuid = Optional.ofNullable(prosessTaskData.getPropertyValue(INNTEKTSMELDING_UUID)).map(UUID::fromString);
        var lukkeÅrsak = LukkeÅrsak.valueOf(prosessTaskData.getPropertyValue(LUKKE_ÅRSAK));
        var forespørselDto = forespørselTjeneste.hentForespørsel(forespørselUuid).orElseThrow();
        LOG.info("Starter task for å ferdigstille sak i arbeidsgiverportalen for saksnummer {} og orgnr {}", forespørselDto.fagsystemSaksnummer(), forespørselDto.arbeidsgiver());

        // Arbeidsgiverinitierte forespørsler har ingen oppgave
        if (forespørselDto.oppgaveId() != null) {
            minSideArbeidsgiverTjeneste.oppgaveUtført(forespørselDto.oppgaveId(), OffsetDateTime.now());
        }

        var erArbeidsgiverinitiert = forespørselDto.forespørselType().equals(ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT)
            || forespørselDto.forespørselType().equals(ForespørselType.ARBEIDSGIVERINITIERT_UREGISTRERT);
        minSideArbeidsgiverTjeneste.ferdigstillSak(forespørselDto.arbeidsgiverNotifikasjonSakId(), erArbeidsgiverinitiert);

        minSideArbeidsgiverTjeneste.oppdaterSakTilleggsinformasjon(forespørselDto.arbeidsgiverNotifikasjonSakId(),
            ForespørselTekster.lagTilleggsInformasjon(lukkeÅrsak, forespørselDto.førsteUttaksdato()));

        // Sende med lenke til kvitteringsside
        inntektsmeldingUuid.ifPresent(imUuid -> {
            var beskjedTekst = erFørsteInnsending(forespørselDto, imUuid)
                               ? ForespørselTekster.lagBeskjedOmKvitteringFørsteInnsendingTekst()
                               : ForespørselTekster.lagBeskjedOmOppdatertInntektsmelding();
            minSideArbeidsgiverTjeneste.sendNyBeskjedMedKvittering(forespørselDto, imUuid, beskjedTekst);
        });

        LOG.info("Sluttfører task for å ferdigstille sak i arbeidsgiverportalen for saksnummer {} og orgnr {}", forespørselDto.fagsystemSaksnummer(), forespørselDto.arbeidsgiver());
    }

    private boolean erFørsteInnsending(ForespørselDto forespørselDto, UUID inntektsmeldingUuid) {
        var inntektsmelding = inntektsmeldingTjeneste.hentInntektsmelding(inntektsmeldingUuid);
        var alleInntektsmeldinger = inntektsmeldingTjeneste.hentInntektsmeldinger(forespørselDto.uuid());
        return alleInntektsmeldinger.stream().noneMatch(im -> im.getInnsendtTidspunkt().isBefore(inntektsmelding.getInnsendtTidspunkt()));
    }

    public static ProsessTaskData lagTask(UUID forespørselUuid, LukkeÅrsak lukkeÅrsak, Optional<UUID> inntektsmeldingUuid) {
        var task = ProsessTaskData.forProsessTask(FerdigstillSakArbeidsgiverportalenTask.class);
        // Viktig at tasker som skal oppdatere forespørselentitet alle deler samme gruppe (forespørselUuid) så vi ikke får lås
        task.setGruppe(forespørselUuid.toString());
        task.setProperty(FORESPØRSEL_UUID, forespørselUuid.toString());
        task.setProperty(LUKKE_ÅRSAK, lukkeÅrsak.toString());
        inntektsmeldingUuid.ifPresent(imUUid -> task.setProperty(INNTEKTSMELDING_UUID, inntektsmeldingUuid.toString()));
        return task;
    }
}
