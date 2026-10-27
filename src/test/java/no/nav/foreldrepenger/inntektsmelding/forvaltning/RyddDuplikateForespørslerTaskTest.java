package no.nav.foreldrepenger.inntektsmelding.forvaltning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.database.JpaExtension;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.lager.ForespørselEntitet;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.lager.ForespørselRepository;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.task.SettDialogTilUtgåttTask;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.task.SettSakTilUtgåttTask;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.altinn.DialogportenTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.MinSideArbeidsgiverTjeneste;
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
class RyddDuplikateForespørslerTaskTest extends EntityManagerAwareTest {

    private static final String SAKSNUMMER = "RYDD-SAK";
    private static final String ORGNUMMER = "999999999";
    private static final LocalDateTime OPPRETTET = LocalDateTime.of(2026, 1, 1, 12, 0);

    @Mock
    private ProsessTaskTjeneste prosessTaskTjeneste;
    @Mock
    private MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste;
    @Mock
    private DialogportenTjeneste dialogportenTjeneste;

    private ForespørselRepository repository;
    private RyddDuplikateForespørslerTask task;

    @BeforeEach
    void setUp() {
        repository = new ForespørselRepository(getEntityManager());
        var behandling = new ForespørselBehandlingTjeneste(new ForespørselTjeneste(repository),
            minSideArbeidsgiverTjeneste, dialogportenTjeneste, prosessTaskTjeneste);
        task = new RyddDuplikateForespørslerTask(getEntityManager(), behandling);
    }

    @ParameterizedTest
    @EnumSource(value = ForespørselStatus.class, names = {"UNDER_BEHANDLING", "FERDIG"})
    void skal_utgå_nyansatt_og_beholde_fagsystem_selv_om_nyansatt_er_nyest(ForespørselStatus status) {
        var fagsystem = lagre(ForespørselType.BESTILT_AV_FAGSYSTEM, ForespørselStatus.FERDIG, OPPRETTET);
        var nyansatt = lagre(ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT, status, OPPRETTET.plusDays(1));

        task.doTask(data(false));

        assertStatus(nyansatt, ForespørselStatus.UTGÅTT);
        assertStatus(fagsystem, ForespørselStatus.FERDIG);
        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(prosessTaskTjeneste).lagre(captor.capture());
        assertThat(captor.getValue().getTasks()).extracting(e -> e.task().taskType()).containsExactly(
            TaskType.forProsessTask(SettSakTilUtgåttTask.class),
            TaskType.forProsessTask(SettDialogTilUtgåttTask.class));
        verifyNoInteractions(minSideArbeidsgiverTjeneste, dialogportenTjeneste);
    }

    @Test
    void skal_beholde_nyeste_fagsystemforespørsel() {
        var eldst = lagre(ForespørselType.BESTILT_AV_FAGSYSTEM, ForespørselStatus.UNDER_BEHANDLING, OPPRETTET);
        var nyest = lagre(ForespørselType.BESTILT_AV_FAGSYSTEM, ForespørselStatus.FERDIG, OPPRETTET.plusDays(1));

        task.doTask(data(false));

        assertStatus(eldst, ForespørselStatus.UTGÅTT);
        assertStatus(nyest, ForespørselStatus.FERDIG);
    }

    @Test
    void skal_beholde_høyeste_id_ved_likt_opprettelsestidspunkt() {
        var først = lagre(ForespørselType.BESTILT_AV_FAGSYSTEM, ForespørselStatus.UNDER_BEHANDLING, OPPRETTET);
        var sist = lagre(ForespørselType.BESTILT_AV_FAGSYSTEM, ForespørselStatus.UNDER_BEHANDLING, OPPRETTET);

        task.doTask(data(false));

        assertStatus(først, ForespørselStatus.UTGÅTT);
        assertStatus(sist, ForespørselStatus.UNDER_BEHANDLING);
    }

    @Test
    void skal_rydde_flere_enn_to_og_ikke_gjenta_lukking_ved_ny_kjøring() {
        var eldst = lagre(ForespørselType.BESTILT_AV_FAGSYSTEM, ForespørselStatus.FERDIG, OPPRETTET);
        var nyest = lagre(ForespørselType.BESTILT_AV_FAGSYSTEM, ForespørselStatus.UNDER_BEHANDLING, OPPRETTET.plusDays(1));
        var nyansatt = lagre(ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT, ForespørselStatus.FERDIG, OPPRETTET.plusDays(2));
        var utgått = lagre(ForespørselType.BESTILT_AV_FAGSYSTEM, ForespørselStatus.UTGÅTT, OPPRETTET.plusDays(3));

        task.doTask(data(false));
        task.doTask(data(false));

        assertStatus(eldst, ForespørselStatus.UTGÅTT);
        assertStatus(nyansatt, ForespørselStatus.UTGÅTT);
        assertStatus(nyest, ForespørselStatus.UNDER_BEHANDLING);
        assertStatus(utgått, ForespørselStatus.UTGÅTT);
        verify(prosessTaskTjeneste, times(2)).lagre(any(ProsessTaskGruppe.class));
    }

    @Test
    void mastertask_skal_skille_sak_og_orgnr_og_ignorere_utgåtte_og_manglende_saksnummer() {
        lagre(ForespørselType.BESTILT_AV_FAGSYSTEM, ForespørselStatus.FERDIG, OPPRETTET);
        lagre(ForespørselType.BESTILT_AV_FAGSYSTEM, ForespørselStatus.UTGÅTT, OPPRETTET);
        lagre(ForespørselType.BESTILT_AV_FAGSYSTEM, ForespørselStatus.FERDIG, OPPRETTET, "ANNEN-SAK", ORGNUMMER);
        lagre(ForespørselType.BESTILT_AV_FAGSYSTEM, ForespørselStatus.FERDIG, OPPRETTET, SAKSNUMMER, "888888888");
        lagre(ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT, ForespørselStatus.FERDIG, OPPRETTET, null, ORGNUMMER);
        lagre(ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT, ForespørselStatus.FERDIG, OPPRETTET, null, ORGNUMMER);
        var hovedtask = new RyddDuplikateÅpneForespørslerTask(getEntityManager(), prosessTaskTjeneste);
        var data = ProsessTaskData.forProsessTask(RyddDuplikateÅpneForespørslerTask.class);
        data.setProperty(RyddDuplikateÅpneForespørslerTask.DRY_RUN, Boolean.toString(false));
        data.setProperty(RyddDuplikateÅpneForespørslerTask.FRA_OG_MED_ID, Integer.toString(0));
        data.setProperty(RyddDuplikateÅpneForespørslerTask.TIL_OG_MED_ID, Integer.toString(6));

        hovedtask.doTask(data);

        verifyNoInteractions(prosessTaskTjeneste);
    }

    @Test
    void skal_beholde_nyeste_når_alle_er_arbeidsgiverinitiert() {
        var første = lagre(ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT, ForespørselStatus.FERDIG, OPPRETTET);
        var andre = lagre(ForespørselType.ARBEIDSGIVERINITIERT_UREGISTRERT, ForespørselStatus.UNDER_BEHANDLING, OPPRETTET.plusDays(1));

        task.doTask(data(false));

        assertStatus(første, ForespørselStatus.UTGÅTT);
        assertStatus(andre, ForespørselStatus.UNDER_BEHANDLING);
    }

    private ForespørselEntitet lagre(ForespørselType type, ForespørselStatus status, LocalDateTime opprettet) {
        return lagre(type, status, opprettet, SAKSNUMMER, ORGNUMMER);
    }

    private ForespørselEntitet lagre(ForespørselType type, ForespørselStatus status, LocalDateTime opprettet,
                                   String saksnummer, String orgnummer) {
        var dato = LocalDate.of(2026, 1, 1);
        var forespørsel = new ForespørselEntitet(orgnummer, dato, AktørIdEntitet.dummy(), Ytelsetype.FORELDREPENGER,
            saksnummer, dato, type);
        forespørsel.setStatus(status);
        repository.lagreForespørsel(forespørsel);
        getEntityManager().createNativeQuery("update forespoersel set opprettet_tid = :tid where id = :id")
            .setParameter("tid", Timestamp.valueOf(opprettet))
            .setParameter("id", forespørsel.getId())
            .executeUpdate();
        getEntityManager().refresh(forespørsel);
        return forespørsel;
    }

    private void assertStatus(ForespørselEntitet forespørsel, ForespørselStatus status) {
        getEntityManager().clear();
        assertThat(repository.hentForespørsel(forespørsel.getUuid()).orElseThrow().getStatus()).isEqualTo(status);
    }

    private static ProsessTaskData data(boolean dryRun) {
        var data = ProsessTaskData.forProsessTask(RyddDuplikateForespørslerTask.class);
        data.setProperty(RyddDuplikateÅpneForespørslerTask.DRY_RUN, Boolean.toString(dryRun));
        data.setProperty(RyddDuplikateForespørslerTask.SAKSNUMMER, SAKSNUMMER);
        data.setProperty(RyddDuplikateForespørslerTask.ORGNUMMER, ORGNUMMER);
        return data;
    }
}
