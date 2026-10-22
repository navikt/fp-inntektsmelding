package no.nav.foreldrepenger.inntektsmelding.forvaltning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.lager.ForespørselEntitet;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.foreldrepenger.inntektsmelding.typer.lager.AktørIdEntitet;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ExtendWith(MockitoExtension.class)
class RyddDuplikateForespørslerTaskTest {

    private static final String GRUPPE_TASK_TYPE = "rydd.duplikate.forespørsler.gruppe";

    @Mock
    private EntityManager entityManager;
    @Mock
    private ProsessTaskTjeneste prosessTaskTjeneste;
    @Mock
    private TypedQuery<ForespørselEntitet> query;

    private RyddDuplikateForespørslerTask task;

    @BeforeEach
    void setUp() {
        task = new RyddDuplikateForespørslerTask(entityManager, prosessTaskTjeneste);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"true", "TRUE", "", "flase", " false "})
    void andre_verdier_enn_false_skal_gi_dry_run_på_sub_task(String dryRunVerdi) {
        mockQuery();
        when(query.getResultList()).thenReturn(List.of(forespørsel(2, "SAK-A", "999999999")));

        task.doTask(data("0", "100", dryRunVerdi));

        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(prosessTaskTjeneste).lagre(captor.capture());
        assertThat(captor.getValue().getTasks()).hasSize(1);
        assertThat(captor.getValue().getTasks().getFirst().task().getPropertyValue(RyddDuplikateForespørslerTask.DRY_RUN))
            .isEqualTo("true");
        verify(query).setParameter(RyddDuplikateForespørslerTask.FRA_ID, 0L);
    }

    @Test
    void dedupliserer_grupper_og_planlegger_neste_side_sekvensielt_når_siden_er_full() {
        mockQuery();
        var rader = new ArrayList<ForespørselEntitet>();
        for (var id = 15; id < 65; id++) {
            rader.add(id == 17 ? forespørsel(id, "SAMME-SAK", "888888888")
                : forespørsel(id, id < 17 ? "SAMME-SAK" : "SAK-" + id, "999999999"));
        }
        when(query.getResultList()).thenReturn(rader);

        task.doTask(data("10", "100", "false"));

        var gruppeCaptor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(prosessTaskTjeneste).lagre(gruppeCaptor.capture());
        var entries = gruppeCaptor.getValue().getTasks();
        var barn = entries.stream().filter(e -> e.task().taskType().value().equals(GRUPPE_TASK_TYPE)).toList();
        var fortsettelse = entries.stream().filter(e -> e.task().taskType().value().equals("rydd.duplikate.forespørsler")).toList();
        assertThat(barn).hasSize(49);
        assertThat(barn.stream()
            .map(e -> e.task().getPropertyValue(RyddDuplikateForespørslerGruppeTask.SAKSNUMMER) + "|"
                + e.task().getPropertyValue(RyddDuplikateForespørslerGruppeTask.ORGNUMMER))
            .filter(key -> key.startsWith("SAMME-SAK|"))
            .toList()).containsExactlyInAnyOrder("SAMME-SAK|999999999", "SAMME-SAK|888888888");
        assertThat(barn).allMatch(e -> "false".equals(e.task().getPropertyValue(RyddDuplikateForespørslerTask.DRY_RUN)));
        assertThat(fortsettelse).hasSize(1);
        assertThat(Integer.parseInt(fortsettelse.getFirst().sekvens()))
            .isGreaterThan(Integer.parseInt(barn.getFirst().sekvens()));

        var neste = fortsettelse.getFirst().task();
        assertThat(neste.getPropertyValue(RyddDuplikateForespørslerTask.FRA_ID)).isEqualTo("64");
        assertThat(neste.getPropertyValue(RyddDuplikateForespørslerTask.TIL_ID)).isEqualTo("100");
        assertThat(neste.getPropertyValue(RyddDuplikateForespørslerTask.DRY_RUN)).isEqualTo("false");

        verify(query).setParameter(RyddDuplikateForespørslerTask.FRA_ID, 10L);
        verify(query).setParameter(RyddDuplikateForespørslerTask.TIL_ID, 100L);
        verify(query).setParameter("statuser", RyddDuplikateForespørslerGruppeTask.AKTIVE_STATUSER);
        verify(query).setMaxResults(50);
    }

    @Test
    void siste_side_planlegger_ikke_ny_master_task() {
        mockQuery();
        when(query.getResultList()).thenReturn(List.of(forespørsel(2, "SAK-A", "999999999")));

        task.doTask(data("1", "10", "false"));

        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(prosessTaskTjeneste).lagre(captor.capture());
        assertThat(captor.getValue().getTasks()).hasSize(1);
    }

    @ParameterizedTest
    @CsvSource(value = {"null,100", "1,null"}, nullValues = "null")
    void kaster_feil_når_fraId_eller_tilId_mangler(String fraId, String tilId) {
        var data = data(fraId, tilId, "false");

        assertThatThrownBy(() -> task.doTask(data)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(prosessTaskTjeneste);
    }

    @Test
    void tomt_resultat_lagrer_ikke_task_gruppe() {
        mockQuery();
        when(query.getResultList()).thenReturn(List.of());

        task.doTask(data("1", "10", "false"));

        verifyNoInteractions(prosessTaskTjeneste);
    }

    private ProsessTaskData data(String fraId, String tilId, String dryRun) {
        var data = ProsessTaskData.forProsessTask(RyddDuplikateForespørslerTask.class);
        if (fraId != null) {
            data.setProperty(RyddDuplikateForespørslerTask.FRA_ID, fraId);
        }
        if (tilId != null) {
            data.setProperty(RyddDuplikateForespørslerTask.TIL_ID, tilId);
        }
        if (dryRun != null) {
            data.setProperty(RyddDuplikateForespørslerTask.DRY_RUN, dryRun);
        }
        return data;
    }

    private void mockQuery() {
        when(entityManager.createQuery(anyString(), eq(ForespørselEntitet.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.setMaxResults(50)).thenReturn(query);
    }

    private ForespørselEntitet forespørsel(long id, String saksnummer, String orgnummer) {
        var forespørsel = new ForespørselEntitet(orgnummer, LocalDate.of(2026, 4, 1), AktørIdEntitet.dummy(),
            Ytelsetype.FORELDREPENGER, saksnummer, LocalDate.of(2026, 4, 7), ForespørselType.BESTILT_AV_FAGSYSTEM);
        try {
            var idField = ForespørselEntitet.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(forespørsel, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return forespørsel;
    }
}
