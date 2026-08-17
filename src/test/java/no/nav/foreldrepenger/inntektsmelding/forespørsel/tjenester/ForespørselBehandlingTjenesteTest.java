package no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.database.JpaExtension;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.lager.ForespørselEntitet;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.lager.ForespørselRepository;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.task.FerdigstillDialogTask;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.task.FerdigstillSakTask;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.task.FellesTaskProperties;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.task.OpprettDialogOgFerdigstillTask;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.task.OpprettDialogTask;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.task.OpprettOppgaveTask;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.task.OpprettSakTask;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.task.SettDialogTilUtgåttTask;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.task.SettSakTilUtgåttTask;
import no.nav.foreldrepenger.inntektsmelding.forvaltning.rest.InntektsmeldingForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.altinn.DialogportenTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.MinSideArbeidsgiverTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Saksnummer;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.ForespørselResultat;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.NyBeskjedResultat;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ArbeidsgiverinitiertÅrsak;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.foreldrepenger.inntektsmelding.typer.lager.AktørIdEntitet;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith({JpaExtension.class, MockitoExtension.class})
class ForespørselBehandlingTjenesteTest extends EntityManagerAwareTest {

    private static final String BRREG_ORGNUMMER = "974760673";
    private static final String AKTØR_ID = "1234567891234";
    private static final String SAK_ID = "1";
    private static final String OPPGAVE_ID = "2";
    private static final String SAK_ID_2 = "3";
    private static final String SAKSNUMMER = "FAGSAK_SAKEN";
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now().minusYears(1);
    private static final LocalDate FØRSTE_UTTAKSDATO = LocalDate.now().minusYears(1).plusDays(1);
    private static final Ytelsetype YTELSETYPE = Ytelsetype.FORELDREPENGER;
    @Mock
    private MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste;
    @Mock
    private DialogportenTjeneste dialogportenTjeneste;
    @Mock
    private ProsessTaskTjeneste prosessTaskTjeneste;

    private ForespørselRepository forespørselRepository;
    private ForespørselTjeneste forespørselTjeneste;
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    @BeforeEach
    void setUp() {
        this.forespørselRepository = new ForespørselRepository(getEntityManager());
        this.forespørselTjeneste = new ForespørselTjeneste(forespørselRepository);
        this.forespørselBehandlingTjeneste = new ForespørselBehandlingTjeneste(forespørselTjeneste,
            minSideArbeidsgiverTjeneste,
            dialogportenTjeneste,
            prosessTaskTjeneste);
    }

    // Simulerer at prosesstasken for å opprette sak/oppgave hos arbeidsgiverportalen har kjørt, slik den ville gjort
    // kort tid etter i produksjon. Nødvendig i disse testene fordi enkelte påfølgende operasjoner (f.eks. ferdigstille/
    // sette utgått) fortsatt slår opp forespørselen på arbeidsgiverNotifikasjonSakId og oppgaveId
    private void kjørOpprettSakTask(UUID forespørselUuid) {
        var task = new OpprettSakTask(forespørselTjeneste, minSideArbeidsgiverTjeneste);
        var taskData = ProsessTaskData.forProsessTask(OpprettSakTask.class);
        taskData.setProperty(FellesTaskProperties.KEY_FORESPOERSEL_UUID, forespørselUuid.toString());
        task.doTask(taskData);
    }

    private void kjørOpprettOppgaveTask(UUID forespørselUuid) {
        var task = new OpprettOppgaveTask(forespørselTjeneste, minSideArbeidsgiverTjeneste);
        var taskData = ProsessTaskData.forProsessTask(OpprettOppgaveTask.class);
        taskData.setProperty(FellesTaskProperties.KEY_FORESPOERSEL_UUID, forespørselUuid.toString());
        task.doTask(taskData);
    }

    @Test
    void skal_opprette_forespørsel_og_opprette_tasks_for_sak_oppgave_og_dialog() {
        var arbeidsgiver = Arbeidsgiver.fra(BRREG_ORGNUMMER);

        var resultat = forespørselBehandlingTjeneste.håndterInnkommendeForespørsel(SKJÆRINGSTIDSPUNKT,
            YTELSETYPE,
            AktørId.fra(AKTØR_ID),
            arbeidsgiver,
            Saksnummer.fra(SAKSNUMMER),
            SKJÆRINGSTIDSPUNKT
        );

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørslerPåSak(SAKSNUMMER);

        assertThat(resultat).isEqualTo(ForespørselResultat.FORESPØRSEL_OPPRETTET);
        assertThat(lagret).hasSize(1);
        // Sak/oppgave hos arbeidsgiverportalen og dialog hos Dialogporten opprettes asynkront via prosesstasks,
        // og skal derfor ikke være satt synkront på forespørselen ennå
        assertThat(lagret.getFirst().getArbeidsgiverNotifikasjonSakId()).isNull();
        assertThat(lagret.getFirst().getOppgaveId()).isEmpty();

        var taskGruppeCaptor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(prosessTaskTjeneste).lagre(taskGruppeCaptor.capture());
        var opprettedeTasks = taskGruppeCaptor.getValue().getTasks().stream().map(ProsessTaskGruppe.Entry::task).toList();

        // Verifiserer at taskene kjøres i riktig sekvens: sak → oppgave → dialog
        // Siden dialogporten-oppdateringen forutsetter at saken allerede finnes hos arbeidsgiverportalen
        assertThat(opprettedeTasks).hasSize(3);
        assertThat(opprettedeTasks.get(0).taskType()).isEqualTo(TaskType.forProsessTask(OpprettSakTask.class));
        assertThat(opprettedeTasks.get(1).taskType()).isEqualTo(TaskType.forProsessTask(OpprettOppgaveTask.class));
        assertThat(opprettedeTasks.get(2).taskType()).isEqualTo(TaskType.forProsessTask(OpprettDialogTask.class));

        var forespørselUuid = lagret.getFirst().getUuid().toString();
        assertThat(opprettedeTasks.get(0).getPropertyValue(FellesTaskProperties.KEY_FORESPOERSEL_UUID)).isEqualTo(forespørselUuid);
        assertThat(opprettedeTasks.get(1).getPropertyValue(FellesTaskProperties.KEY_FORESPOERSEL_UUID)).isEqualTo(forespørselUuid);
        assertThat(opprettedeTasks.get(2).getPropertyValue(FellesTaskProperties.KEY_FORESPOERSEL_UUID)).isEqualTo(forespørselUuid);
    }

    @Test
    void eksisterende_forespørsel_på_samme_stp_skal_gi_nei() {
        lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMER, SKJÆRINGSTIDSPUNKT,
            ForespørselType.BESTILT_AV_FAGSYSTEM);

        getEntityManager().clear();

        var resultat = forespørselBehandlingTjeneste.håndterInnkommendeForespørsel(SKJÆRINGSTIDSPUNKT,
            YTELSETYPE,
            AktørId.fra(AKTØR_ID),
            Arbeidsgiver.fra(BRREG_ORGNUMMER),
            Saksnummer.fra(SAKSNUMMER),
            SKJÆRINGSTIDSPUNKT
        );

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørslerPåSak(SAKSNUMMER);
        assertThat(resultat).isEqualTo(ForespørselResultat.IKKE_OPPRETTET_FINNES_ALLEREDE);
        assertThat(lagret).hasSize(1);
    }

    @Test
    void skal_ikke_opprette_forespørsel_når_finnes_allerede_for_stp_og_første_uttaksdato() {
        mockInfoForOpprettelse(SAK_ID);
        var arbeidsgiver = Arbeidsgiver.fra(BRREG_ORGNUMMER);

        forespørselBehandlingTjeneste.håndterInnkommendeForespørsel(SKJÆRINGSTIDSPUNKT,
            YTELSETYPE,
            AktørId.fra(AKTØR_ID),
            arbeidsgiver,
            Saksnummer.fra(SAKSNUMMER),
            FØRSTE_UTTAKSDATO
        );

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørslerPåSak(SAKSNUMMER).getFirst();
        kjørOpprettSakTask(lagret.getUuid());
        kjørOpprettOppgaveTask(lagret.getUuid());
        var fpEntitet = forespørselBehandlingTjeneste.ferdigstillForespørsel(lagret.getUuid(),
            AktørId.fra(lagret.getAktørId().getAktørId()),
            Arbeidsgiver.fra(lagret.getOrganisasjonsnummer()),
            lagret.getFørsteUttaksdato(),
            LukkeÅrsak.EKSTERN_INNSENDING, Optional.empty());

        assertThat(fpEntitet.status()).isEqualTo(ForespørselStatus.FERDIG);

        var resultat2 = forespørselBehandlingTjeneste.håndterInnkommendeForespørsel(SKJÆRINGSTIDSPUNKT,
            YTELSETYPE,
            AktørId.fra(AKTØR_ID),
            arbeidsgiver,
            Saksnummer.fra(SAKSNUMMER),
            FØRSTE_UTTAKSDATO
        );

        assertThat(resultat2).isEqualTo(ForespørselResultat.IKKE_OPPRETTET_FINNES_ALLEREDE);
    }

    @Test
    void skal_opprette_forespørsel_når_finnes_allerede_for_samme_stp_og_ulik_uttaksdato() {
        mockInfoForOpprettelse(SAK_ID);
        var arbeidsgiver = Arbeidsgiver.fra(BRREG_ORGNUMMER);

        forespørselBehandlingTjeneste.håndterInnkommendeForespørsel(SKJÆRINGSTIDSPUNKT,
            YTELSETYPE,
            AktørId.fra(AKTØR_ID),
            arbeidsgiver,
            Saksnummer.fra(SAKSNUMMER),
            FØRSTE_UTTAKSDATO
        );

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørslerPåSak(SAKSNUMMER).getFirst();
        kjørOpprettSakTask(lagret.getUuid());
        kjørOpprettOppgaveTask(lagret.getUuid());

        var fpEntitet = forespørselBehandlingTjeneste.ferdigstillForespørsel(lagret.getUuid(),
            AktørId.fra(lagret.getAktørId().getAktørId()),
            Arbeidsgiver.fra(lagret.getOrganisasjonsnummer()),
            lagret.getFørsteUttaksdato(),
            LukkeÅrsak.EKSTERN_INNSENDING, Optional.empty());

        assertThat(fpEntitet.status()).isEqualTo(ForespørselStatus.FERDIG);

        mockInfoForOpprettelse(SAK_ID_2);
        var resultat2 = forespørselBehandlingTjeneste.håndterInnkommendeForespørsel(SKJÆRINGSTIDSPUNKT,
            YTELSETYPE,
            AktørId.fra(AKTØR_ID),
            arbeidsgiver,
            Saksnummer.fra(SAKSNUMMER),
            FØRSTE_UTTAKSDATO.plusDays(1)
        );

        assertThat(resultat2).isEqualTo(ForespørselResultat.FORESPØRSEL_OPPRETTET);
    }

    @Test
    void skal_sette_forrige_forespørsel_med_status_ferdig_til_utgått_når_ny_forespørsel_opprettes() {
        mockInfoForOpprettelse(SAK_ID);
        var arbeidsgiver = Arbeidsgiver.fra(BRREG_ORGNUMMER);
        var saksnummer = Saksnummer.fra(SAKSNUMMER);

        forespørselBehandlingTjeneste.håndterInnkommendeForespørsel(SKJÆRINGSTIDSPUNKT,
            YTELSETYPE,
            AktørId.fra(AKTØR_ID),
            arbeidsgiver,
            saksnummer,
            FØRSTE_UTTAKSDATO
        );

        var lagret = forespørselRepository.hentForespørslerPåSak(saksnummer.saksnummer()).getFirst();
        kjørOpprettSakTask(lagret.getUuid());
        kjørOpprettOppgaveTask(lagret.getUuid());

        var fpEntitet = forespørselBehandlingTjeneste.ferdigstillForespørsel(lagret.getUuid(),
            AktørId.fra(lagret.getAktørId().getAktørId()),
            Arbeidsgiver.fra(lagret.getOrganisasjonsnummer()),
            lagret.getFørsteUttaksdato(),
            LukkeÅrsak.ORDINÆR_INNSENDING, Optional.empty());

        assertThat(fpEntitet.status()).isEqualTo(ForespørselStatus.FERDIG);

        mockInfoForOpprettelse(SAK_ID_2);
        var resultat2 = forespørselBehandlingTjeneste.håndterInnkommendeForespørsel(SKJÆRINGSTIDSPUNKT.plusMonths(2),
            YTELSETYPE,
            AktørId.fra(AKTØR_ID),
            arbeidsgiver,
            saksnummer,
            FØRSTE_UTTAKSDATO
        );

        var forrigeForespørsel = forespørselRepository.hentForespørsel(lagret.getUuid());

        clearHibernateCache();
        assertThat(forrigeForespørsel.map(ForespørselEntitet::getStatus)).isEqualTo(Optional.of(ForespørselStatus.UTGÅTT));
        assertThat(resultat2).isEqualTo(ForespørselResultat.FORESPØRSEL_OPPRETTET);
    }

    @Test
    void skal_sette_forrige_forespørsel_med_status_under_behandling_til_utgått_når_ny_forespørsel_opprettes() {
        mockInfoForOpprettelse(SAK_ID);
        var arbeidsgiver = Arbeidsgiver.fra(BRREG_ORGNUMMER);

        var saksnummer = Saksnummer.fra(SAKSNUMMER);
        forespørselBehandlingTjeneste.håndterInnkommendeForespørsel(SKJÆRINGSTIDSPUNKT,
            YTELSETYPE,
            AktørId.fra(AKTØR_ID),
            arbeidsgiver,
            saksnummer,
            FØRSTE_UTTAKSDATO
        );

        var lagret = forespørselRepository.hentForespørslerPåSak(saksnummer.saksnummer()).getFirst();
        kjørOpprettSakTask(lagret.getUuid());
        kjørOpprettOppgaveTask(lagret.getUuid());

        assertThat(lagret.getStatus()).isEqualTo(ForespørselStatus.UNDER_BEHANDLING);

        mockInfoForOpprettelse(SAK_ID_2);
        var resultat2 = forespørselBehandlingTjeneste.håndterInnkommendeForespørsel(SKJÆRINGSTIDSPUNKT.plusMonths(2),
            YTELSETYPE,
            AktørId.fra(AKTØR_ID),
            arbeidsgiver,
            saksnummer,
            FØRSTE_UTTAKSDATO
        );

        var forrigeForespørsel = forespørselRepository.hentForespørsel(lagret.getUuid());

        clearHibernateCache();
        assertThat(forrigeForespørsel.map(ForespørselEntitet::getStatus)).isEqualTo(Optional.of(ForespørselStatus.UTGÅTT));
        assertThat(resultat2).isEqualTo(ForespørselResultat.FORESPØRSEL_OPPRETTET);
    }

    @Test
    void skal_opprette_arbeidsgiverinitert_forespørsel_uten_oppgave() {
        var forespørselDto = forespørselBehandlingTjeneste.opprettForespørselForArbeidsgiverInitiertIm(YTELSETYPE,
            AktørId.fra(AKTØR_ID),
            Arbeidsgiver.fra(BRREG_ORGNUMMER),
            FØRSTE_UTTAKSDATO, ArbeidsgiverinitiertÅrsak.NYANSATT,
            null,
            null);

        var lagret = forespørselRepository.hentForespørsel(forespørselDto.uuid()).orElseThrow();

        clearHibernateCache();
        assertThat(lagret.getStatus()).isEqualTo(ForespørselStatus.UNDER_BEHANDLING);
        assertThat(lagret.getOppgaveId()).isEmpty();
        assertThat(lagret.getFørsteUttaksdato()).isEqualTo(FØRSTE_UTTAKSDATO);
    }

    @Test
    void skal_opprette_arbeidsgiverinitert_forespørsel_med_skjæringstidspunkt() {
        var forventetSkjæringstidspunkt = FØRSTE_UTTAKSDATO.minusDays(1);
        var forespørselDto = forespørselBehandlingTjeneste.opprettForespørselForArbeidsgiverInitiertIm(YTELSETYPE,
            AktørId.fra(AKTØR_ID),
            Arbeidsgiver.fra(BRREG_ORGNUMMER),
            FØRSTE_UTTAKSDATO,
            ArbeidsgiverinitiertÅrsak.UREGISTRERT,
            forventetSkjæringstidspunkt,
            Saksnummer.fra(SAKSNUMMER));

        var lagret = forespørselRepository.hentForespørsel(forespørselDto.uuid()).orElseThrow();

        clearHibernateCache();
        assertThat(lagret.getStatus()).isEqualTo(ForespørselStatus.UNDER_BEHANDLING);
        assertThat(lagret.getOppgaveId()).isEmpty();
        assertThat(lagret.getFørsteUttaksdato()).isEqualTo(FØRSTE_UTTAKSDATO);
        assertThat(lagret.getSkjæringstidspunkt()).isEqualTo(Optional.of(forventetSkjæringstidspunkt));
        assertThat(lagret.getFagsystemSaksnummer()).contains(SAKSNUMMER);
    }

    @Test
    void skal_opprette_tasker_for_å_opprette_og_ferdigstille_agi() {
        var forespørselUuid = lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMER,
            FØRSTE_UTTAKSDATO, ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT);
        var forespørselDto = forespørselTjeneste.hentForespørsel(forespørselUuid).orElseThrow();
        var inntektsmeldingUuid = UUID.randomUUID();

        forespørselBehandlingTjeneste.opprettTasksForOpprettOgFerdigstillAgi(forespørselDto, inntektsmeldingUuid);

        // Oppretter sak hos arbeidsgiverportalen først, deretter opprett-dialog-og-ferdigstill, i sekvens
        var taskGruppeCaptor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(prosessTaskTjeneste).lagre(taskGruppeCaptor.capture());
        var taskGruppe = taskGruppeCaptor.getValue();

        var tasks = taskGruppe.getTasks().stream().map(ProsessTaskGruppe.Entry::task).toList();
        assertThat(tasks).hasSize(2);
        var opprettSakTask = tasks.getFirst();
        var opprettDialogOgFerdigstillTask = tasks.getLast();

        assertThat(opprettSakTask.taskType()).isEqualTo(TaskType.forProsessTask(OpprettSakTask.class));
        assertThat(opprettSakTask.getPropertyValue(FellesTaskProperties.KEY_FORESPOERSEL_UUID)).isEqualTo(forespørselUuid.toString());

        assertThat(opprettDialogOgFerdigstillTask.taskType()).isEqualTo(TaskType.forProsessTask(OpprettDialogOgFerdigstillTask.class));
        assertThat(opprettDialogOgFerdigstillTask.getPropertyValue(FellesTaskProperties.KEY_FORESPOERSEL_UUID)).isEqualTo(forespørselUuid.toString());
        assertThat(opprettDialogOgFerdigstillTask.getPropertyValue(FellesTaskProperties.KEY_INNTEKTSMELDING_UUID)).isEqualTo(inntektsmeldingUuid.toString());
    }

    @Test
    void skal_ferdigstille_forespørsel() {
        var forespørselUuid = lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMER,
            SKJÆRINGSTIDSPUNKT, ForespørselType.BESTILT_AV_FAGSYSTEM);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);

        forespørselBehandlingTjeneste.ferdigstillForespørsel(forespørselUuid,
            AktørId.fra(AKTØR_ID),
            Arbeidsgiver.fra(BRREG_ORGNUMMER),
            SKJÆRINGSTIDSPUNKT,
            LukkeÅrsak.EKSTERN_INNSENDING, Optional.empty());

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsel(forespørselUuid);
        assertThat(lagret.map(ForespørselEntitet::getStatus)).isEqualTo(Optional.of(ForespørselStatus.FERDIG));

        // Ferdigstilling av sak hos arbeidsgiverportalen og dialog hos Dialogporten skal skje asynkront via
        // prosesstask, i sekvens (sak/oppgave hos arbeidsgiverportalen først, deretter dialog)
        var taskGruppeCaptor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(prosessTaskTjeneste).lagre(taskGruppeCaptor.capture());
        var tasks = taskGruppeCaptor.getValue().getTasks().stream().map(ProsessTaskGruppe.Entry::task).toList();

        assertThat(tasks).hasSize(2);
        var ferdigstillSakTask = tasks.getFirst();
        var ferdigstillDialogTask = tasks.getLast();
        assertThat(ferdigstillSakTask.taskType()).isEqualTo(TaskType.forProsessTask(FerdigstillSakTask.class));
        assertThat(ferdigstillSakTask.getPropertyValue(FellesTaskProperties.KEY_FORESPOERSEL_UUID)).isEqualTo(forespørselUuid.toString());
        assertThat(ferdigstillSakTask.getPropertyValue(FellesTaskProperties.KEY_LUKKE_AARSAK)).isEqualTo(LukkeÅrsak.EKSTERN_INNSENDING.name());
        assertThat(ferdigstillSakTask.getPropertyValue(FerdigstillSakTask.KEY_ER_FØRSTEGANGSINNSENDING)).isEqualTo("true");
        assertThat(ferdigstillSakTask.getPropertyValue(FellesTaskProperties.KEY_INNTEKTSMELDING_UUID)).isNull();

        assertThat(ferdigstillDialogTask.taskType()).isEqualTo(TaskType.forProsessTask(FerdigstillDialogTask.class));
        assertThat(ferdigstillDialogTask.getPropertyValue(FellesTaskProperties.KEY_FORESPOERSEL_UUID)).isEqualTo(forespørselUuid.toString());
        assertThat(ferdigstillDialogTask.getPropertyValue(FellesTaskProperties.KEY_LUKKE_AARSAK)).isEqualTo(LukkeÅrsak.EKSTERN_INNSENDING.name());
        assertThat(ferdigstillDialogTask.getPropertyValue(FellesTaskProperties.KEY_INNTEKTSMELDING_UUID)).isNull();
    }

    @Test
    void skal_ferdigstille_forespørsel_ulik_stp_og_startdato() {
        var forespørselUuid = lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMER,
            FØRSTE_UTTAKSDATO, ForespørselType.BESTILT_AV_FAGSYSTEM);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);

        forespørselBehandlingTjeneste.ferdigstillForespørsel(forespørselUuid,
            AktørId.fra(AKTØR_ID),
            Arbeidsgiver.fra(BRREG_ORGNUMMER),
            FØRSTE_UTTAKSDATO,
            LukkeÅrsak.EKSTERN_INNSENDING, Optional.empty());

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsel(forespørselUuid);
        assertThat(lagret.map(ForespørselEntitet::getStatus)).isEqualTo(Optional.of(ForespørselStatus.FERDIG));
    }

    @Test
    void skal_sette_alle_forespørspørsler_for_sak_til_ferdig() {
        var forespørselUuid = lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMER,
            FØRSTE_UTTAKSDATO, ForespørselType.BESTILT_AV_FAGSYSTEM);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);
        var forespørselUuid2 = lagreForespørsel(SKJÆRINGSTIDSPUNKT.plusDays(2),
            YTELSETYPE,
            AKTØR_ID,
            BRREG_ORGNUMMER,
            SAKSNUMMER,
            FØRSTE_UTTAKSDATO.plusDays(1), ForespørselType.BESTILT_AV_FAGSYSTEM);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid2, "2");

        forespørselBehandlingTjeneste.lukkForespørsel(Saksnummer.fra(SAKSNUMMER), Arbeidsgiver.fra(BRREG_ORGNUMMER), null);

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsel(forespørselUuid);
        assertThat(lagret.map(ForespørselEntitet::getStatus)).isEqualTo(Optional.of(ForespørselStatus.FERDIG));
        var lagret2 = forespørselRepository.hentForespørsel(forespørselUuid2);
        assertThat(lagret2.map(ForespørselEntitet::getStatus)).isEqualTo(Optional.of(ForespørselStatus.FERDIG));
    }

    @Test
    void skal_sette_alle_forespørspørsler_for_sak_til_utgått() {
        var forespørselUuid = lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMER,
            FØRSTE_UTTAKSDATO, ForespørselType.BESTILT_AV_FAGSYSTEM);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);
        var forespørselUuid2 = lagreForespørsel(SKJÆRINGSTIDSPUNKT.plusDays(2),
            YTELSETYPE,
            AKTØR_ID,
            BRREG_ORGNUMMER,
            SAKSNUMMER,
            FØRSTE_UTTAKSDATO, ForespørselType.BESTILT_AV_FAGSYSTEM);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid2, "2");

        forespørselBehandlingTjeneste.settForespørselTilUtgått(Saksnummer.fra(SAKSNUMMER), null, null);

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsel(forespørselUuid);
        assertThat(lagret.map(ForespørselEntitet::getStatus)).isEqualTo(Optional.of(ForespørselStatus.UTGÅTT));
        var lagret2 = forespørselRepository.hentForespørsel(forespørselUuid2);
        assertThat(lagret2.map(ForespørselEntitet::getStatus)).isEqualTo(Optional.of(ForespørselStatus.UTGÅTT));

        // Setting av sak til utgått hos arbeidsgiverportalen og dialog til utgått hos Dialogporten skal skje
        // asynkront via prosesstask, én taskGruppe (med to sekvensielle tasks) per forespørsel
        var taskGruppeCaptor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(prosessTaskTjeneste, Mockito.times(2)).lagre(taskGruppeCaptor.capture());
        var taskGrupper = taskGruppeCaptor.getAllValues();

        var forespørselUuiderMedTaskGruppe = taskGrupper.stream()
            .map(gruppe -> gruppe.getTasks().getFirst().task().getPropertyValue(FellesTaskProperties.KEY_FORESPOERSEL_UUID))
            .toList();
        assertThat(forespørselUuiderMedTaskGruppe).containsExactlyInAnyOrder(forespørselUuid.toString(), forespørselUuid2.toString());

        for (var taskGruppe : taskGrupper) {
            var tasks = taskGruppe.getTasks().stream().map(ProsessTaskGruppe.Entry::task).toList();
            assertThat(tasks).hasSize(2);
            assertThat(tasks.getFirst().taskType()).isEqualTo(TaskType.forProsessTask(SettSakTilUtgåttTask.class));
            assertThat(tasks.getLast().taskType()).isEqualTo(TaskType.forProsessTask(SettDialogTilUtgåttTask.class));
        }
    }

    @Test
    void skal_lukke_forespørsel_for_sak_med_gitt_stp() {
        var forespørselUuid = lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMER,
            SKJÆRINGSTIDSPUNKT, ForespørselType.BESTILT_AV_FAGSYSTEM);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);

        var forespørselUuid2 = lagreForespørsel(SKJÆRINGSTIDSPUNKT.plusDays(2),
            YTELSETYPE,
            AKTØR_ID,
            BRREG_ORGNUMMER,
            SAKSNUMMER,
            SKJÆRINGSTIDSPUNKT, ForespørselType.BESTILT_AV_FAGSYSTEM);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, "2");

        forespørselBehandlingTjeneste.lukkForespørsel(Saksnummer.fra(SAKSNUMMER),
            Arbeidsgiver.fra(BRREG_ORGNUMMER),
            SKJÆRINGSTIDSPUNKT);

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsel(forespørselUuid);
        assertThat(lagret.map(ForespørselEntitet::getStatus)).isEqualTo(Optional.of(ForespørselStatus.FERDIG));
        var lagret2 = forespørselRepository.hentForespørsel(forespørselUuid2);
        assertThat(lagret2.map(ForespørselEntitet::getStatus)).isEqualTo(Optional.of(ForespørselStatus.UNDER_BEHANDLING));
    }

    @Test
    void skal_sette_forespørsel_og_oppgave_til_gitt_forespørselUuid_til_utgått() {
        var forespørselUuid = lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMER,
            SKJÆRINGSTIDSPUNKT, ForespørselType.BESTILT_AV_FAGSYSTEM);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);

        forespørselBehandlingTjeneste.settForespørselTilUtgåttForvaltning(forespørselUuid);

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsel(forespørselUuid).orElseThrow();
        assertThat(lagret.getStatus()).isEqualTo(ForespørselStatus.UTGÅTT);

        // Setting av sak til utgått hos arbeidsgiverportalen og dialog til utgått hos Dialogporten skal skje
        // asynkront via prosesstask, i sekvens
        var taskGruppeCaptor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(prosessTaskTjeneste).lagre(taskGruppeCaptor.capture());
        var tasks = taskGruppeCaptor.getValue().getTasks().stream().map(ProsessTaskGruppe.Entry::task).toList();

        assertThat(tasks).hasSize(2);
        var settSakTilUtgåttTask = tasks.getFirst();
        var settDialogTilUtgåttTask = tasks.getLast();
        assertThat(settSakTilUtgåttTask.taskType()).isEqualTo(TaskType.forProsessTask(SettSakTilUtgåttTask.class));
        assertThat(settSakTilUtgåttTask.getPropertyValue(FellesTaskProperties.KEY_FORESPOERSEL_UUID)).isEqualTo(forespørselUuid.toString());
        assertThat(settDialogTilUtgåttTask.taskType()).isEqualTo(TaskType.forProsessTask(SettDialogTilUtgåttTask.class));
        assertThat(settDialogTilUtgåttTask.getPropertyValue(FellesTaskProperties.KEY_FORESPOERSEL_UUID)).isEqualTo(forespørselUuid.toString());
    }

    @Test
    void skal_opprette_ny_beskjed_med_ekstern_varsling() {
        var forespørselUuid = lagreForespørsel(SKJÆRINGSTIDSPUNKT,
            Ytelsetype.FORELDREPENGER,
            AKTØR_ID,
            BRREG_ORGNUMMER,
            SAKSNUMMER,
            SKJÆRINGSTIDSPUNKT, ForespørselType.BESTILT_AV_FAGSYSTEM);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);

        var arbeidsgiver = Arbeidsgiver.fra(BRREG_ORGNUMMER);

        var resultat = forespørselBehandlingTjeneste.opprettNyBeskjedMedEksternVarsling(Saksnummer.fra(SAKSNUMMER),
            arbeidsgiver);

        clearHibernateCache();

        assertThat(resultat).isEqualTo(NyBeskjedResultat.NY_BESKJED_SENDT);
        verify(minSideArbeidsgiverTjeneste, Mockito.times(1)).sendNyBeskjedMedEksternVarsling(any(ForespørselDto.class));
    }

    @Test
    void skal_opprette_ny_beskjed_med_kvitteringslenke() {
        var forespørselUuid = lagreForespørsel(SKJÆRINGSTIDSPUNKT,
            Ytelsetype.FORELDREPENGER,
            AKTØR_ID,
            BRREG_ORGNUMMER,
            SAKSNUMMER,
            SKJÆRINGSTIDSPUNKT, ForespørselType.BESTILT_AV_FAGSYSTEM);
        var imUuid = UUID.randomUUID();
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);

        var res = forespørselBehandlingTjeneste.ferdigstillForespørsel(forespørselUuid,
            AktørId.fra(AKTØR_ID),
            Arbeidsgiver.fra(BRREG_ORGNUMMER),
            SKJÆRINGSTIDSPUNKT,
            LukkeÅrsak.EKSTERN_INNSENDING, Optional.of(imUuid));

        clearHibernateCache();

        assertThat(res).isNotNull();
        var taskGruppeCaptor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(prosessTaskTjeneste).lagre(taskGruppeCaptor.capture());
        var ferdigstillSakTask = taskGruppeCaptor.getValue().getTasks().getFirst().task();
        assertThat(ferdigstillSakTask.taskType()).isEqualTo(TaskType.forProsessTask(FerdigstillSakTask.class));
        assertThat(ferdigstillSakTask.getPropertyValue(FellesTaskProperties.KEY_LUKKE_AARSAK)).isEqualTo(LukkeÅrsak.EKSTERN_INNSENDING.name());
        assertThat(ferdigstillSakTask.getPropertyValue(FellesTaskProperties.KEY_INNTEKTSMELDING_UUID)).isEqualTo(imUuid.toString());
        assertThat(ferdigstillSakTask.getPropertyValue(FerdigstillSakTask.KEY_ER_FØRSTEGANGSINNSENDING)).isEqualTo("true");
    }

    @Test
    void skal_opprette_ny_beskjed_med_kvitteringslenke_ved_oppdatert_inntektsmelding() {
        var forespørselUuid = lagreForespørsel(SKJÆRINGSTIDSPUNKT,
            Ytelsetype.FORELDREPENGER,
            AKTØR_ID,
            BRREG_ORGNUMMER,
            SAKSNUMMER,
            SKJÆRINGSTIDSPUNKT, ForespørselType.BESTILT_AV_FAGSYSTEM);
        var imUuid = UUID.randomUUID();
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);
        forespørselRepository.ferdigstillForespørsel(SAK_ID);

        var res = forespørselBehandlingTjeneste.ferdigstillForespørsel(forespørselUuid,
            AktørId.fra(AKTØR_ID),
            Arbeidsgiver.fra(BRREG_ORGNUMMER),
            SKJÆRINGSTIDSPUNKT,
            LukkeÅrsak.EKSTERN_INNSENDING, Optional.of(imUuid));

        clearHibernateCache();

        assertThat(res).isNotNull();
        var taskGruppeCaptor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(prosessTaskTjeneste).lagre(taskGruppeCaptor.capture());
        var ferdigstillSakTask = taskGruppeCaptor.getValue().getTasks().getFirst().task();
        assertThat(ferdigstillSakTask.taskType()).isEqualTo(TaskType.forProsessTask(FerdigstillSakTask.class));
        assertThat(ferdigstillSakTask.getPropertyValue(FellesTaskProperties.KEY_LUKKE_AARSAK)).isEqualTo(LukkeÅrsak.EKSTERN_INNSENDING.name());
        assertThat(ferdigstillSakTask.getPropertyValue(FellesTaskProperties.KEY_INNTEKTSMELDING_UUID)).isEqualTo(imUuid.toString());
        assertThat(ferdigstillSakTask.getPropertyValue(FerdigstillSakTask.KEY_ER_FØRSTEGANGSINNSENDING)).isEqualTo("false");
    }
    @Test
    void skal_gi_riktig_resultat_om_det_ikke_finnes_en_åpen_forespørsel() {
        var forespørselUuid = lagreForespørsel(SKJÆRINGSTIDSPUNKT,
            Ytelsetype.FORELDREPENGER,
            AKTØR_ID,
            BRREG_ORGNUMMER,
            SAKSNUMMER,
            SKJÆRINGSTIDSPUNKT, ForespørselType.BESTILT_AV_FAGSYSTEM);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);
        forespørselRepository.ferdigstillForespørsel(SAK_ID);

        var resultat = forespørselBehandlingTjeneste.opprettNyBeskjedMedEksternVarsling(Saksnummer.fra(SAKSNUMMER),
            Arbeidsgiver.fra(BRREG_ORGNUMMER));
        clearHibernateCache();
        assertThat(resultat).isEqualTo(NyBeskjedResultat.FORESPØRSEL_FINNES_IKKE);
    }


    @Test
    void skal_oppdatere_førsteUttaksdato_for_arbeidsgiverinitert() {
        var forespørselUuid = lagreForespørsel(SKJÆRINGSTIDSPUNKT,
            Ytelsetype.FORELDREPENGER,
            AKTØR_ID,
            BRREG_ORGNUMMER,
            SAKSNUMMER,
            FØRSTE_UTTAKSDATO,
            ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);
        forespørselRepository.ferdigstillForespørsel(SAK_ID);

        var forespørsel = forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid).orElseThrow();
        var nyFørsteUttaksdato = FØRSTE_UTTAKSDATO.plusWeeks(1);

        var resultat = forespørselBehandlingTjeneste.oppdaterFørsteUttaksdato(forespørsel, nyFørsteUttaksdato);

        clearHibernateCache();

        var oppdatertForespørsel = forespørselRepository.hentForespørsel(forespørselUuid).orElseThrow();

        assertThat(resultat.aktørId().getAktørId()).isEqualTo(oppdatertForespørsel.getAktørId().getAktørId());
        assertThat(resultat.førsteUttaksdato()).isEqualTo(nyFørsteUttaksdato);
        assertThat(resultat.uuid()).isEqualTo(oppdatertForespørsel.getUuid());
        assertThat(resultat.arbeidsgiverNotifikasjonSakId()).isEqualTo(oppdatertForespørsel.getArbeidsgiverNotifikasjonSakId());
        assertThat(resultat.status()).isEqualTo(oppdatertForespørsel.getStatus());
        assertThat(resultat.arbeidsgiver().orgnr()).isEqualTo(oppdatertForespørsel.getOrganisasjonsnummer());
        assertThat(resultat.ytelseType()).isEqualTo(oppdatertForespørsel.getYtelseType());
        assertThat(resultat.forespørselType()).isEqualTo(oppdatertForespørsel.getForespørselType());
    }
    @Test
    void skal_returnere_liste_av_inntektsmeldingdto_for_forespørsler() {

        var forespørsel1sak1 = new ForespørselEntitet(BRREG_ORGNUMMER,
            LocalDate.of(2025, 1, 1),
            new AktørIdEntitet(AKTØR_ID),
            Ytelsetype.FORELDREPENGER,
            SAK_ID,
            LocalDate.of(2025, 1, 1), ForespørselType.BESTILT_AV_FAGSYSTEM);
        var forespørsel1sak2 = new ForespørselEntitet(BRREG_ORGNUMMER,
            LocalDate.of(2025, 2, 1),
            new AktørIdEntitet(AKTØR_ID),
            Ytelsetype.FORELDREPENGER,
            SAK_ID_2,
            LocalDate.of(2025, 2, 1), ForespørselType.BESTILT_AV_FAGSYSTEM);
        var forespørsel2sak1 = new ForespørselEntitet(BRREG_ORGNUMMER,
            LocalDate.of(2025, 3, 1),
            new AktørIdEntitet(AKTØR_ID),
            Ytelsetype.FORELDREPENGER,
            SAK_ID,
            LocalDate.of(2025, 3, 1), ForespørselType.BESTILT_AV_FAGSYSTEM);
        var forespørsel2sak2 = new ForespørselEntitet(BRREG_ORGNUMMER,
            LocalDate.of(2025, 4, 1),
            new AktørIdEntitet(AKTØR_ID),
            Ytelsetype.FORELDREPENGER,
            SAK_ID_2,
            LocalDate.of(2025, 4, 1), ForespørselType.BESTILT_AV_FAGSYSTEM);

        getEntityManager().persist(forespørsel1sak1);
        getEntityManager().persist(forespørsel1sak2);
        getEntityManager().persist(forespørsel2sak1);
        getEntityManager().persist(forespørsel2sak2);
        getEntityManager().flush();

        List<InntektsmeldingForespørselDto> inntektsmeldingForespørselDtos = forespørselBehandlingTjeneste.finnForespørslerForFagsak(Saksnummer.fra(
            SAK_ID));

        assertThat(inntektsmeldingForespørselDtos).hasSize(2);
        var dto1 = inntektsmeldingForespørselDtos.stream()
            .filter(forespørsel -> forespørsel.skjæringstidspunkt().equals(forespørsel1sak1.getSkjæringstidspunkt().orElse(null)))
            .findAny()
            .orElseThrow();
        var dto2 = inntektsmeldingForespørselDtos.stream()
            .filter(forespørsel -> forespørsel.skjæringstidspunkt().equals(forespørsel2sak1.getSkjæringstidspunkt().orElse(null)))
            .findAny()
            .orElseThrow();

        assertThat(dto1.aktørid()).isEqualTo(forespørsel1sak1.getAktørId().getAktørId());
        assertThat(dto1.skjæringstidspunkt()).isEqualTo(forespørsel1sak1.getSkjæringstidspunkt().orElse(null));
        assertThat(dto1.ytelsetype()).isEqualTo(forespørsel1sak1.getYtelseType().toString());
        assertThat(dto1.uuid()).isEqualTo(forespørsel1sak1.getUuid());
        assertThat(dto1.arbeidsgiverident()).isEqualTo(forespørsel1sak1.getOrganisasjonsnummer());

        assertThat(dto2.aktørid()).isEqualTo(forespørsel2sak1.getAktørId().getAktørId());
        assertThat(dto2.skjæringstidspunkt()).isEqualTo(forespørsel2sak1.getSkjæringstidspunkt().orElse(null));
        assertThat(dto2.ytelsetype()).isEqualTo(forespørsel2sak1.getYtelseType().toString());
        assertThat(dto2.uuid()).isEqualTo(forespørsel2sak1.getUuid());
        assertThat(dto2.arbeidsgiverident()).isEqualTo(forespørsel2sak1.getOrganisasjonsnummer());

    }

    @Test
    void skal_ikke_få_resultat_hvis_aktørid_ikke_matcher() {
        lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMER, SKJÆRINGSTIDSPUNKT,
            ForespørselType.BESTILT_AV_FAGSYSTEM);
        var feilAktørId = new no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId("1111111111111");

        getEntityManager().clear();

        clearHibernateCache();

        var resultat = forespørselBehandlingTjeneste.hentForespørsler(Arbeidsgiver.fra(BRREG_ORGNUMMER), feilAktørId, null, null, null, null, null);

        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_søke_etter_forespørsler_hvis_fnr_er_null() {
        lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMER, SKJÆRINGSTIDSPUNKT,
            ForespørselType.BESTILT_AV_FAGSYSTEM);

        getEntityManager().clear();

        clearHibernateCache();

        var resultat = forespørselBehandlingTjeneste.hentForespørsler(Arbeidsgiver.fra(BRREG_ORGNUMMER), null, null, null, null, null, null);

        assertThat(resultat).hasSize(1);
        assertThat(resultat.getFirst().arbeidsgiver().orgnr()).isEqualTo(BRREG_ORGNUMMER);
    }

    private void clearHibernateCache() {
        // Fjerne hibernate cachen før assertions skal evalueres - hibernate ignorerer alle updates som er markert med updatable = false ved skriving mot databasen
        // men objektene i cachen blir oppdatert helt greit likevel.
        // På denne måten evaluerer vi faktisk tilstanden som blir til slutt lagret i databasen.
        getEntityManager().clear();
    }

    private UUID lagreForespørsel(LocalDate skjæringstidspunkt, Ytelsetype ytelsetype, String aktørId, String orgnr, String saksnummer,
                                  LocalDate førsteUttaksdato, ForespørselType type) {
        return forespørselRepository.lagreForespørsel(new ForespørselEntitet(orgnr, skjæringstidspunkt, new AktørIdEntitet(aktørId), ytelsetype, saksnummer, førsteUttaksdato, type));
    }

    private void mockInfoForOpprettelse(String sakId) {
        lenient().when(minSideArbeidsgiverTjeneste.opprettSak(any(ForespørselDto.class))).thenReturn(sakId);
        lenient().when(minSideArbeidsgiverTjeneste.opprettOppgave(any(ForespørselDto.class))).thenReturn(OPPGAVE_ID);
    }
}
