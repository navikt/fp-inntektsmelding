package no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.task.OpprettDialogOgFerdigstillTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.task.FerdigstillDialogTask;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.task.FerdigstillSakTask;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.task.FellesTaskProperties;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.task.OppdaterDialogMedEndretInntektsmeldingTask;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.task.OppdaterSakMedEndretInntektsmeldingTask;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.task.OpprettDialogTask;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.task.OpprettOppgaveTask;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.task.OpprettSakTask;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.task.SettDialogTilUtgåttTask;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.task.SettSakTilUtgåttTask;
import no.nav.foreldrepenger.inntektsmelding.forvaltning.rest.InntektsmeldingForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.altinn.DialogportenTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.MinSideArbeidsgiverTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.metrikker.MetrikkerTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Saksnummer;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.ForespørselResultat;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.ForespørselStatusDto;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.NyBeskjedResultat;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.YtelseTypeDto;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ArbeidsgiverinitiertÅrsak;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;


/**
 * Okrestreringsklasse som håndterer alle endringer på en forespørsel, og synkroniserer dette på tvers av intern database og eksterne systemer.
 */
@ApplicationScoped
public class ForespørselBehandlingTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(ForespørselBehandlingTjeneste.class);

    private ForespørselTjeneste forespørselTjeneste;
    private MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste;
    private DialogportenTjeneste dialogportenTjeneste;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    public ForespørselBehandlingTjeneste() {
        // CDI
    }

    @Inject
    public ForespørselBehandlingTjeneste(ForespørselTjeneste forespørselTjeneste,
                                         MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste,
                                         DialogportenTjeneste dialogportenTjeneste,
                                         ProsessTaskTjeneste prosessTaskTjeneste) {
        this.forespørselTjeneste = forespørselTjeneste;
        this.minSideArbeidsgiverTjeneste = minSideArbeidsgiverTjeneste;
        this.dialogportenTjeneste = dialogportenTjeneste;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    public ForespørselResultat håndterInnkommendeForespørsel(LocalDate skjæringstidspunkt,
                                                             Ytelsetype ytelsetype,
                                                             AktørId aktørId,
                                                             Arbeidsgiver arbeidsgiver,
                                                             Saksnummer fagsakSaksnummer,
                                                             LocalDate førsteUttaksdato) {
        var finnesForespørsel = forespørselTjeneste.finnGjeldendeForespørsel(skjæringstidspunkt,
            ytelsetype,
            aktørId,
            arbeidsgiver,
            fagsakSaksnummer,
            førsteUttaksdato);

        if (finnesForespørsel.isPresent()) {
            LOG.info("Finnes allerede forespørsel for saksnummer: {} med orgnummer: {} med skjæringstidspunkt: {} og første uttaksdato: {}",
                fagsakSaksnummer,
                arbeidsgiver,
                skjæringstidspunkt,
                førsteUttaksdato);
            return ForespørselResultat.IKKE_OPPRETTET_FINNES_ALLEREDE;
        }

        settTidligereForespørslerForSaksnummerTilUtgått(fagsakSaksnummer, arbeidsgiver, aktørId);
        opprettForespørsel(ytelsetype, aktørId, fagsakSaksnummer, arbeidsgiver, skjæringstidspunkt, førsteUttaksdato);

        return ForespørselResultat.FORESPØRSEL_OPPRETTET;
    }

    public void sendMeldingOmAvvistInntektsmelding(ForespørselDto forespørselDto,
                                                   String feiltekst) {
        // Send transmission til dialogporten
        dialogportenTjeneste.sendMeldingOmAvvistInntektsmelding(forespørselDto, feiltekst);
        // Send melding til fager
        minSideArbeidsgiverTjeneste.sendNyBeskjedOmAvvistInntektsmelding(forespørselDto, feiltekst);
    }

    // Vi skal aldri ha mer enn en forespørsel til under_behandling eller ferdig for samme sak med samme orgnummer og aktørid
    private void settTidligereForespørslerForSaksnummerTilUtgått(Saksnummer fagsakSaksnummer,
                                                                 Arbeidsgiver arbeidsgiver,
                                                                 AktørId aktørId) {
        LOG.info("ForespørselBehandlingTjenesteImpl: settTidligereForespørslerForSaksnummerTilUtgått for saksnummer: {}, orgnummer: {}, aktørId: {}  ",
            fagsakSaksnummer,
            arbeidsgiver,
            aktørId);

        forespørselTjeneste.finnForespørslerForFagsak(fagsakSaksnummer).stream()
            .filter(forespørsel -> forespørsel.aktørId().equals(aktørId))
            .filter(forespørselDto -> arbeidsgiver.orgnr().equals(forespørselDto.arbeidsgiver().orgnr()))
            .filter(forespørselDto -> !ForespørselStatus.UTGÅTT.name().equals(forespørselDto.status().name()))
            .forEach(this::settForespørselTilUtgått);
    }

    public ForespørselDto ferdigstillForespørsel(UUID foresporselUuid,
                                                 AktørId aktorId,
                                                 Arbeidsgiver arbeidsgiver,
                                                 LocalDate startdato,
                                                 LukkeÅrsak årsak,
                                                 // inntektsmeldingUuid er optional fordi vi ikke har inntektsmeldingen lagret hvis den er innsendt via Altinn / LPS'er
                                                 Optional<UUID> inntektsmeldingUuid) {
        var forespørsel = forespørselTjeneste.hentForespørsel(foresporselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel for inntektsmelding, ugyldig tilstand"));

        var erFørstegangsinnsending = ForespørselStatus.UNDER_BEHANDLING.equals(forespørsel.status());

        forespørselTjeneste.ferdigstillForespørsel(forespørsel.arbeidsgiverNotifikasjonSakId());

        var ferdigstillSakTask = ProsessTaskData.forProsessTask(FerdigstillSakTask.class);
        ferdigstillSakTask.setProperty(FerdigstillSakTask.KEY_ER_FØRSTEGANGSINNSENDING, Boolean.toString(erFørstegangsinnsending));
        inntektsmeldingUuid.ifPresent(uuid -> ferdigstillSakTask.setProperty(FellesTaskProperties.KEY_INNTEKTSMELDING_UUID, uuid.toString()));

        //Det er sjekk på inntektsmeldingUuid lenger inn i koden. Derfor kalles denne uansett om verdien er tom eller ei
        var ferdigstillDialogTask = ProsessTaskData.forProsessTask(FerdigstillDialogTask.class);
        inntektsmeldingUuid.ifPresent(uuid -> ferdigstillDialogTask.setProperty(FellesTaskProperties.KEY_INNTEKTSMELDING_UUID, uuid.toString()));

        var taskGruppe = new ProsessTaskGruppe();
        taskGruppe.setProperty(FellesTaskProperties.KEY_FORESPOERSEL_UUID, foresporselUuid.toString());
        taskGruppe.setProperty(FellesTaskProperties.KEY_LUKKE_AARSAK, årsak.name());
        taskGruppe.addNesteSekvensiell(ferdigstillSakTask);
        taskGruppe.addNesteSekvensiell(ferdigstillDialogTask);
        prosessTaskTjeneste.lagre(taskGruppe);

        // Re-fetch to get updated status
        return forespørselTjeneste.hentForespørsel(foresporselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel etter ferdigstilling"));
    }

    public void opprettTasksForÅOppdaterePortaler(ForespørselDto forespørsel,
                                                  Optional<UUID> inntektsmeldingUuid) {
        var taskGruppe = new ProsessTaskGruppe();
        taskGruppe.setProperty(FellesTaskProperties.KEY_FORESPOERSEL_UUID, forespørsel.uuid().toString());

        // Kun relevant å oppdatere sak hos arbeidsgiverportalen dersom vi faktisk har en inntektsmelding å vise til
        inntektsmeldingUuid.ifPresent(imUuid -> {
            var oppdaterSakTask = ProsessTaskData.forProsessTask(OppdaterSakMedEndretInntektsmeldingTask.class);
            oppdaterSakTask.setProperty(FellesTaskProperties.KEY_INNTEKTSMELDING_UUID, imUuid.toString());
            taskGruppe.addNesteSekvensiell(oppdaterSakTask);
        });

        // Oppdatering av dialog hos Dialogporten opprettes uansett, i motsetning til sak-oppdateringen over.
        // OppdaterDialogMedEndretInntektsmeldingTask håndterer manglende inntektsmeldingUuid (Optional), mens
        // OppdaterSakMedEndretInntektsmeldingTask krever en verdi og vil feile uten
        var oppdaterDialogTask = ProsessTaskData.forProsessTask(OppdaterDialogMedEndretInntektsmeldingTask.class);
        inntektsmeldingUuid.ifPresent(imUuid -> oppdaterDialogTask.setProperty(FellesTaskProperties.KEY_INNTEKTSMELDING_UUID, imUuid.toString()));
        taskGruppe.addNesteSekvensiell(oppdaterDialogTask);

        prosessTaskTjeneste.lagre(taskGruppe);
    }

    public Optional<ForespørselDto> hentForespørsel(UUID forespørselUUID) {
        return forespørselTjeneste.hentForespørsel(forespørselUUID);
    }

    public List<ForespørselDto> finnForespørslerForAktørId(AktørId aktørId, Ytelsetype ytelsetype) {
        return forespørselTjeneste.finnForespørslerForAktørid(aktørId, ytelsetype);
    }

    public void settForespørselTilUtgått(ForespørselDto eksisterendeForespørsel) {
        forespørselTjeneste.settForespørselTilUtgått(eksisterendeForespørsel.arbeidsgiverNotifikasjonSakId());
        leggTilSettUtgåttTasks(eksisterendeForespørsel.uuid());

        var msg = String.format("Setter forespørsel til utgått, orgnr: %s, stp: %s, saksnummer: %s, ytelse: %s",
            eksisterendeForespørsel.arbeidsgiver(),
            eksisterendeForespørsel.skjæringstidspunkt(),
            eksisterendeForespørsel.fagsystemSaksnummer(),
            eksisterendeForespørsel.ytelseType());
        LOG.info(msg);
    }

    public void opprettForespørsel(Ytelsetype ytelsetype,
                                   AktørId aktørId,
                                   Saksnummer fagsakSaksnummer,
                                   Arbeidsgiver arbeidsgiver,
                                   LocalDate skjæringstidspunkt,
                                   LocalDate førsteUttaksdato) {
        var msg = String.format("Oppretter forespørsel, orgnr: %s, stp: %s, saksnummer: %s, ytelse: %s",
            arbeidsgiver,
            skjæringstidspunkt,
            fagsakSaksnummer.saksnummer(),
            ytelsetype);
        LOG.info(msg);

        // Oppretter forespørsel i lokal database
        var forespørselUuid = forespørselTjeneste.opprettForespørsel(skjæringstidspunkt,
            ytelsetype,
            aktørId,
            arbeidsgiver,
            fagsakSaksnummer,
            førsteUttaksdato);

        // oppretter tasker for eksterne kall
        var opprettSakTask = ProsessTaskData.forProsessTask(OpprettSakTask.class);
        var opprettOppgaveTask = ProsessTaskData.forProsessTask(OpprettOppgaveTask.class);
        var opprettDialogTask = ProsessTaskData.forProsessTask(OpprettDialogTask.class);

        var taskGruppe = new ProsessTaskGruppe();
        taskGruppe.setProperty(FellesTaskProperties.KEY_FORESPOERSEL_UUID, forespørselUuid.toString());
        taskGruppe.addNesteSekvensiell(opprettSakTask);
        taskGruppe.addNesteSekvensiell(opprettOppgaveTask);
        taskGruppe.addNesteSekvensiell(opprettDialogTask);
        prosessTaskTjeneste.lagre(taskGruppe);
    }

    public ForespørselDto opprettForespørselForArbeidsgiverInitiertIm(Ytelsetype ytelsetype,
                                                            AktørId aktørId,
                                                            Arbeidsgiver arbeidsgiver,
                                                            LocalDate førsteFraværsdato,
                                                            ArbeidsgiverinitiertÅrsak arbeidsgiverinitiertÅrsak,
                                                            LocalDate skjæringstidspunkt,
                                                            Saksnummer fagsystemSaksnummer) {
        var msg = String.format("Oppretter forespørsel for arbeidsgiverinitiert, orgnr: %s, ytelse: %s",
            arbeidsgiver,
            ytelsetype);
        LOG.info(msg);
        var forespørselType = utledForespørselType(arbeidsgiverinitiertÅrsak);
        var uuid = forespørselTjeneste.opprettForespørselArbeidsgiverinitiert(ytelsetype,
            aktørId,
            arbeidsgiver,
            førsteFraværsdato,
            forespørselType,
            skjæringstidspunkt,
            fagsystemSaksnummer);

        return forespørselTjeneste.hentForespørsel(uuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke opprettet arbeidsgiverinitiert forespørsel"));
    }

    private void leggTilSettUtgåttTasks(UUID forespørselUuid) {
        var settSakTilUtgåttTask = ProsessTaskData.forProsessTask(SettSakTilUtgåttTask.class);
        var settDialogTilUtgåttTask = ProsessTaskData.forProsessTask(SettDialogTilUtgåttTask.class);

        var taskGruppe = new ProsessTaskGruppe();
        taskGruppe.setProperty(FellesTaskProperties.KEY_FORESPOERSEL_UUID, forespørselUuid.toString());
        taskGruppe.addNesteSekvensiell(settSakTilUtgåttTask);
        taskGruppe.addNesteSekvensiell(settDialogTilUtgåttTask);
        prosessTaskTjeneste.lagre(taskGruppe);
    }

    private ForespørselType utledForespørselType(ArbeidsgiverinitiertÅrsak arbeidsgiverinitiertÅrsak) {
        return switch (arbeidsgiverinitiertÅrsak) {
            case UREGISTRERT -> ForespørselType.ARBEIDSGIVERINITIERT_UREGISTRERT;
            case NYANSATT -> ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT;
        };
    }

    public NyBeskjedResultat opprettNyBeskjedMedEksternVarsling(Saksnummer fagsakSaksnummer,
                                                                Arbeidsgiver arbeidsgiver) {
        var forespørsel = forespørselTjeneste.finnÅpenForespørslelForFagsak(fagsakSaksnummer, arbeidsgiver)
            .orElse(null);

        if (forespørsel == null) {
            return NyBeskjedResultat.FORESPØRSEL_FINNES_IKKE;
        }

        var msg = String.format("Oppretter ny beskjed med ekstern varsling, orgnr: %s, stp: %s, saksnummer: %s, ytelse: %s",
            arbeidsgiver,
            forespørsel.skjæringstidspunkt(),
            fagsakSaksnummer,
            forespørsel.ytelseType());
        LOG.info(msg);
        minSideArbeidsgiverTjeneste.sendNyBeskjedMedEksternVarsling(forespørsel);

        return NyBeskjedResultat.NY_BESKJED_SENDT;
    }

    public void lukkForespørsel(Saksnummer fagsakSaksnummer, Arbeidsgiver arbeidsgiver, LocalDate skjæringstidspunkt) {
        var forespørsler = hentÅpneForespørslerForFagsak(fagsakSaksnummer, arbeidsgiver, skjæringstidspunkt);

        // Alle inntektsmeldinger sendt inn via arbeidsgiverportal blir lukket umiddelbart etter innsending fra #InntektsmeldingTjeneste,
        // så forespørsler som enda er åpne her blir løst ved innsending fra andre systemer
        forespørsler.forEach(f -> {
            var lukketForespørsel = ferdigstillForespørsel(f.uuid(),
                f.aktørId(),
                f.arbeidsgiver(),
                f.førsteUttaksdato(),
                LukkeÅrsak.EKSTERN_INNSENDING, Optional.empty());
            MetrikkerTjeneste.loggForespørselLukkEkstern(lukketForespørsel);
        });
    }

    public void settForespørselTilUtgått(Saksnummer fagsakSaksnummer, Arbeidsgiver arbeidsgiver, LocalDate skjæringstidspunkt) {
        var forespørsler = hentÅpneForespørslerForFagsak(fagsakSaksnummer, arbeidsgiver, skjæringstidspunkt);

        forespørsler.forEach(this::settForespørselTilUtgått);
    }

    private List<ForespørselDto> hentÅpneForespørslerForFagsak(Saksnummer fagsakSaksnummer,
                                                               Arbeidsgiver arbeidsgiver,
                                                               LocalDate skjæringstidspunkt) {
        return forespørselTjeneste.finnÅpneForespørslerForFagsak(fagsakSaksnummer).stream()
            .filter(f -> arbeidsgiver == null || arbeidsgiver.orgnr().equals(f.arbeidsgiver().orgnr()))
            .filter(f -> skjæringstidspunkt == null || skjæringstidspunkt.equals(f.skjæringstidspunkt()))
            .toList();
    }

    public void settForespørselTilUtgåttForvaltning(UUID forespørselUuid) {
        var forespørselDto = hentForespørsel(forespørselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel med forespørselUuid: " + forespørselUuid));

        forespørselTjeneste.settForespørselTilUtgått(forespørselDto.arbeidsgiverNotifikasjonSakId());
        leggTilSettUtgåttTasks(forespørselUuid);

        var msg = String.format("Setter forespørsel til utgått, orgnr: %s, stp: %s, saksnummer: %s, ytelse: %s",
            forespørselDto.arbeidsgiver().orgnr(),
            forespørselDto.skjæringstidspunkt(),
            forespørselDto.fagsystemSaksnummer().saksnummer(),
            forespørselDto.ytelseType());
        LOG.info(msg);
    }

    public List<InntektsmeldingForespørselDto> finnForespørslerForFagsak(Saksnummer fagsakSaksnummer) {
        return forespørselTjeneste.finnForespørslerForFagsak(fagsakSaksnummer).stream().map(forespoersel ->
                new InntektsmeldingForespørselDto(
                    forespoersel.uuid(),
                    forespoersel.skjæringstidspunkt(),
                    forespoersel.arbeidsgiver().orgnr(),
                    forespoersel.aktørId().getAktørId(),
                    forespoersel.ytelseType().toString(),
                    forespoersel.førsteUttaksdato()))
            .toList();
    }

    public ForespørselDto oppdaterFørsteUttaksdato(ForespørselDto forespørselEnitet, LocalDate startdato) {
        return forespørselTjeneste.setFørsteUttaksdato(forespørselEnitet.uuid(), startdato);
    }

    public List<ForespørselDto> hentForespørsler(Arbeidsgiver arbeidsgiver,
                                                 AktørId aktørId,
                                                 ForespørselStatusDto status,
                                                 YtelseTypeDto ytelseType,
                                                 LocalDate fom,
                                                 LocalDate tom,
                                                 Long fraLoepenr) {
        return forespørselTjeneste.hentForespørsler(arbeidsgiver,
            aktørId,
            status == null ? null : KodeverkMapper.mapForespørselStatus(status),
            ytelseType == null ? null : KodeverkMapper.mapYtelsetype(ytelseType),
            fom,
            tom,
            fraLoepenr);
    }

    public void opprettTasksForOpprettOgFerdigstillAgi(ForespørselDto forespørselDto, UUID inntektsmeldingUuid) {
        // oppretter tasker for eksterne kall
        var opprettSakTask = ProsessTaskData.forProsessTask(OpprettSakTask.class);
        var opprettDialogOgFerdigstillTask = ProsessTaskData.forProsessTask(OpprettDialogOgFerdigstillTask.class);
        opprettDialogOgFerdigstillTask.setProperty(FellesTaskProperties.KEY_INNTEKTSMELDING_UUID, inntektsmeldingUuid.toString());

        var taskGruppe = new ProsessTaskGruppe();
        taskGruppe.setProperty(FellesTaskProperties.KEY_FORESPOERSEL_UUID, forespørselDto.uuid().toString());
        taskGruppe.addNesteSekvensiell(opprettSakTask);
        taskGruppe.addNesteSekvensiell(opprettDialogOgFerdigstillTask);
        prosessTaskTjeneste.lagre(taskGruppe);
    }
}
