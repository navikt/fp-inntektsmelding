package no.nav.foreldrepenger.inntektsmelding.forvaltning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.lager.ForespørselEntitet;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.foreldrepenger.inntektsmelding.typer.lager.AktørIdEntitet;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

/**
 * Tester master-tasken sin del av oppryddingen: paginering, dedup av (saksnummer, orgnummer)-kombinasjoner og at
 * riktig {@link ProsessTaskGruppe} sendes til lagring. Selve fp-sak-vurderingen og lukkingen testes i
 * {@link RyddForespørselTaskTest}.
 */
@ExtendWith(MockitoExtension.class)
class RyddForespørslerTaskTest {

    private static final String ORG_NUMMER = "999999999";
    private static final LocalDate FØRSTE_UTTAKSDATO = LocalDate.of(2026, 4, 7);

    @Mock
    private EntityManager entityManager;
    @Mock
    private ProsessTaskTjeneste prosessTaskTjeneste;
    @Mock
    private TypedQuery<ForespørselEntitet> query;

    private RyddForespørslerTask task;

    @BeforeEach
    void setUp() {
        task = new RyddForespørslerTask(entityManager, prosessTaskTjeneste);
    }

    @Test
    void skal_opprette_en_sub_task_per_par_med_riktige_properties() {
        var forespørselA = lagForespørsel(1L, UUID.randomUUID(), "SAK-A", LocalDateTime.now());
        var forespørselB = lagForespørsel(2L, UUID.randomUUID(), "SAK-B", LocalDateTime.now());
        mockSide(0L, List.of(forespørselA, forespørselB));

        task.doTask(lagProsessTaskData(0L, false));

        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(prosessTaskTjeneste).lagre(captor.capture());
        var entries = captor.getValue().getTasks();

        assertThat(entries).hasSize(2).allMatch(e -> e.task().taskType().value().equals("rydd.forespørsel"));
        assertThat(entries.stream().map(ProsessTaskGruppe.Entry::sekvens).distinct()).hasSize(1);

        var saksnumreLoggetPåTasks = entries.stream()
            .map(e -> e.task().getPropertyValue(RyddForespørselTask.SAKSNUMMER))
            .toList();
        assertThat(saksnumreLoggetPåTasks).containsExactlyInAnyOrder("SAK-A", "SAK-B");
        assertThat(entries).allMatch(e -> ORG_NUMMER.equals(e.task().getPropertyValue(RyddForespørselTask.ORGNUMMER)))
            .allMatch(e -> "false".equals(e.task().getPropertyValue(RyddForespørselTask.DRY_RUN)));
    }

    @Test
    void skal_planlegge_neste_master_task_sekvensielt_etter_full_side() {
        var rader = new ArrayList<ForespørselEntitet>();
        for (var i = 1; i <= 50; i++) {
            rader.add(lagForespørsel(i, UUID.randomUUID(), "SAK-FULL-" + i, LocalDateTime.now()));
        }
        mockSide(0L, rader);

        task.doTask(lagProsessTaskData(0L, false));

        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(prosessTaskTjeneste).lagre(captor.capture());
        var entries = captor.getValue().getTasks();

        var subTaskEntries = entries.stream().filter(e -> e.task().taskType().value().equals("rydd.forespørsel")).toList();
        var masterTaskEntries = entries.stream().filter(e -> e.task().taskType().value().equals("rydd.forespørsler")).toList();

        assertThat(subTaskEntries).hasSize(50);
        assertThat(masterTaskEntries).hasSize(1);
        var nesteMasterTask = masterTaskEntries.getFirst();
        assertThat(nesteMasterTask.task().getPropertyValue("fraId")).isEqualTo("50");
        var subTaskSekvens = Integer.parseInt(subTaskEntries.getFirst().sekvens());
        assertThat(Integer.parseInt(nesteMasterTask.sekvens())).isGreaterThan(subTaskSekvens);
    }

    private void mockSide(long fraId, List<ForespørselEntitet> resultat) {
        when(entityManager.createQuery("from ForespørselEntitet where id > :fraId and status = :status order by id",
            ForespørselEntitet.class)).thenReturn(query);
        when(query.setParameter("fraId", fraId)).thenReturn(query);
        when(query.setParameter("status", ForespørselStatus.UNDER_BEHANDLING)).thenReturn(query);
        when(query.setMaxResults(50)).thenReturn(query);
        when(query.getResultList()).thenReturn(resultat);
    }

    private ProsessTaskData lagProsessTaskData(long fraId, boolean dryRun) {
        var prosessTaskData = ProsessTaskData.forProsessTask(RyddForespørslerTask.class);
        prosessTaskData.setProperty("fraId", String.valueOf(fraId));
        prosessTaskData.setProperty("dryRun", String.valueOf(dryRun));
        return prosessTaskData;
    }

    private ForespørselEntitet lagForespørsel(long id, UUID uuid, String saksnummer, LocalDateTime opprettetTidspunkt) {
        var forespørsel = new ForespørselEntitet(ORG_NUMMER, LocalDate.of(2026, 4, 1), AktørIdEntitet.dummy(), Ytelsetype.FORELDREPENGER,
            saksnummer, FØRSTE_UTTAKSDATO, ForespørselType.BESTILT_AV_FAGSYSTEM);
        try {
            var idField = ForespørselEntitet.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(forespørsel, id);

            var uuidField = ForespørselEntitet.class.getDeclaredField("uuid");
            uuidField.setAccessible(true);
            uuidField.set(forespørsel, uuid);

            var opprettetTidspunktField = ForespørselEntitet.class.getDeclaredField("opprettetTidspunkt");
            opprettetTidspunktField.setAccessible(true);
            opprettetTidspunktField.set(forespørsel, opprettetTidspunkt);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return forespørsel;
    }
}
