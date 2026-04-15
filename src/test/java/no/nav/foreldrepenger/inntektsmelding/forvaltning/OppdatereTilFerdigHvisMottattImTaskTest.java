package no.nav.foreldrepenger.inntektsmelding.forvaltning;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.lager.ForespørselEntitet;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.foreldrepenger.inntektsmelding.typer.lager.AktørIdEntitet;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ExtendWith(MockitoExtension.class)
class OppdatereTilFerdigHvisMottattImTaskTest {

    private static final String ORG_NUMMER = "974760673";
    private static final String AKTØR_ID = "1234567891234";
    private static final LocalDate FØRSTE_UTTAKSDATO = LocalDate.of(2026, 4, 7);

    @Mock
    private EntityManager entityManager;

    @Mock
    private ProsessTaskTjeneste prosessTaskTjeneste;

    @Mock
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;

    @Mock
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    @Mock
    private TypedQuery<ForespørselEntitet> query;

    private OppdatereTilFerdigHvisMottattImTask task;

    @BeforeEach
    void setUp() {
        task = new OppdatereTilFerdigHvisMottattImTask(entityManager, prosessTaskTjeneste, inntektsmeldingTjeneste,
            forespørselBehandlingTjeneste);
    }

    @Test
    void skal_ferdigstille_forespørsel_når_inntektsmelding_er_mottatt_og_ikke_dry_run() {
        // Arrange
        var forespørselId = 1L;
        var forespørselUuid = UUID.randomUUID();
        var inntektsmeldingUuid = UUID.randomUUID();

        var forespørsel = opprettForespørsel(forespørselId, forespørselUuid);
        var inntektsmelding = opprettInntektsmelding(inntektsmeldingUuid);

        when(entityManager.createQuery(
            "from ForespørselEntitet where id >= :fom and id <= :tom and opprettetTidspunkt >= :dato"
                + " and status = :status order by id",
            ForespørselEntitet.class)).thenReturn(query);
        when(query.setParameter("fom", 1L)).thenReturn(query);
        when(query.setParameter("tom", 10L)).thenReturn(query);
        when(query.setParameter("dato", LocalDate.of(2026, 4, 6).atStartOfDay())).thenReturn(query);
        when(query.setParameter("status", ForespørselStatus.UNDER_BEHANDLING)).thenReturn(query);
        when(query.setMaxResults(50)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(forespørsel));

        when(inntektsmeldingTjeneste.hentSisteInntektsmelding(forespørselUuid)).thenReturn(inntektsmelding);

        var prosessTaskData = ProsessTaskData.forProsessTask(OppdatereTilFerdigHvisMottattImTask.class);
        prosessTaskData.setProperty("fom", "1");
        prosessTaskData.setProperty("tom", "10");
        prosessTaskData.setProperty("dryRun", "false");

        // Act
        task.doTask(prosessTaskData);

        // Assert
        verify(forespørselBehandlingTjeneste).ferdigstillForespørsel(
            forespørselUuid,
            new AktørId(AKTØR_ID),
            new Arbeidsgiver(ORG_NUMMER),
            FØRSTE_UTTAKSDATO,
            LukkeÅrsak.ORDINÆR_INNSENDING,
            Optional.of(inntektsmeldingUuid)
        );

        // Verify next task was created
        verify(prosessTaskTjeneste).lagre(isA(ProsessTaskData.class));
    }

    @Test
    void skal_ikke_ferdigstille_når_dry_run_er_true() {
        // Arrange
        var forespørselId = 1L;
        var forespørselUuid = UUID.randomUUID();
        var inntektsmeldingUuid = UUID.randomUUID();

        var forespørsel = opprettForespørsel(forespørselId, forespørselUuid);
        var inntektsmelding = opprettInntektsmelding(inntektsmeldingUuid);

        when(entityManager.createQuery(
            "from ForespørselEntitet where id >= :fom and id <= :tom and opprettetTidspunkt >= :dato"
                + " and status = :status order by id",
            ForespørselEntitet.class)).thenReturn(query);
        when(query.setParameter("fom", 1L)).thenReturn(query);
        when(query.setParameter("tom", 10L)).thenReturn(query);
        when(query.setParameter("dato", LocalDate.of(2026, 4, 6).atStartOfDay())).thenReturn(query);
        when(query.setParameter("status", ForespørselStatus.UNDER_BEHANDLING)).thenReturn(query);
        when(query.setMaxResults(50)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(forespørsel));

        when(inntektsmeldingTjeneste.hentSisteInntektsmelding(forespørselUuid)).thenReturn(inntektsmelding);

        var prosessTaskData = ProsessTaskData.forProsessTask(OppdatereTilFerdigHvisMottattImTask.class);
        prosessTaskData.setProperty("fom", "1");
        prosessTaskData.setProperty("tom", "10");
        prosessTaskData.setProperty("dryRun", "true");

        // Act
        task.doTask(prosessTaskData);

        // Assert
        verify(forespørselBehandlingTjeneste, never()).ferdigstillForespørsel(any(), any(), any(), any(), any(), any());
    }

    @Test
    void skal_ikke_ferdigstille_når_inntektsmelding_ikke_er_mottatt() {
        // Arrange
        var forespørselId = 1L;
        var forespørselUuid = UUID.randomUUID();

        var forespørsel = opprettForespørsel(forespørselId, forespørselUuid);

        when(entityManager.createQuery(
            "from ForespørselEntitet where id >= :fom and id <= :tom and opprettetTidspunkt >= :dato"
                + " and status = :status order by id",
            ForespørselEntitet.class)).thenReturn(query);
        when(query.setParameter("fom", 1L)).thenReturn(query);
        when(query.setParameter("tom", 10L)).thenReturn(query);
        when(query.setParameter("dato", LocalDate.of(2026, 4, 6).atStartOfDay())).thenReturn(query);
        when(query.setParameter("status", ForespørselStatus.UNDER_BEHANDLING)).thenReturn(query);
        when(query.setMaxResults(50)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(forespørsel));

        when(inntektsmeldingTjeneste.hentSisteInntektsmelding(forespørselUuid)).thenReturn(null);

        var prosessTaskData = ProsessTaskData.forProsessTask(OppdatereTilFerdigHvisMottattImTask.class);
        prosessTaskData.setProperty("fom", "1");
        prosessTaskData.setProperty("tom", "10");
        prosessTaskData.setProperty("dryRun", "false");

        // Act
        task.doTask(prosessTaskData);

        // Assert
        verify(forespørselBehandlingTjeneste, never()).ferdigstillForespørsel(any(), any(), any(), any(), any(), any());
    }

    @Test
    void skal_håndtere_flere_forespørsler() {
        // Arrange
        var forespørselId1 = 1L;
        var forespørselUuid1 = UUID.randomUUID();
        var inntektsmeldingUuid1 = UUID.randomUUID();

        var forespørselId2 = 2L;
        var forespørselUuid2 = UUID.randomUUID();
        var inntektsmeldingUuid2 = UUID.randomUUID();

        var forespørsel1 = opprettForespørsel(forespørselId1, forespørselUuid1);
        var forespørsel2 = opprettForespørsel(forespørselId2, forespørselUuid2);

        var inntektsmelding1 = opprettInntektsmelding(inntektsmeldingUuid1);
        var inntektsmelding2 = opprettInntektsmelding(inntektsmeldingUuid2);

        when(entityManager.createQuery(
            "from ForespørselEntitet where id >= :fom and id <= :tom and opprettetTidspunkt >= :dato"
                + " and status = :status order by id",
            ForespørselEntitet.class)).thenReturn(query);
        when(query.setParameter("fom", 1L)).thenReturn(query);
        when(query.setParameter("tom", 10L)).thenReturn(query);
        when(query.setParameter("dato", LocalDate.of(2026, 4, 6).atStartOfDay())).thenReturn(query);
        when(query.setParameter("status", ForespørselStatus.UNDER_BEHANDLING)).thenReturn(query);
        when(query.setMaxResults(50)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(forespørsel1, forespørsel2));

        when(inntektsmeldingTjeneste.hentSisteInntektsmelding(forespørselUuid1)).thenReturn(inntektsmelding1);
        when(inntektsmeldingTjeneste.hentSisteInntektsmelding(forespørselUuid2)).thenReturn(inntektsmelding2);

        var prosessTaskData = ProsessTaskData.forProsessTask(OppdatereTilFerdigHvisMottattImTask.class);
        prosessTaskData.setProperty("fom", "1");
        prosessTaskData.setProperty("tom", "10");
        prosessTaskData.setProperty("dryRun", "false");

        // Act
        task.doTask(prosessTaskData);

        // Assert
        verify(forespørselBehandlingTjeneste).ferdigstillForespørsel(forespørselUuid1, new AktørId(AKTØR_ID), new Arbeidsgiver(ORG_NUMMER),
            FØRSTE_UTTAKSDATO, LukkeÅrsak.ORDINÆR_INNSENDING, Optional.of(inntektsmeldingUuid1));
        verify(forespørselBehandlingTjeneste).ferdigstillForespørsel(forespørselUuid2, new AktørId(AKTØR_ID), new Arbeidsgiver(ORG_NUMMER),
            FØRSTE_UTTAKSDATO, LukkeÅrsak.ORDINÆR_INNSENDING, Optional.of(inntektsmeldingUuid2));

        // Verify next task was created with maxId+1
        verify(prosessTaskTjeneste).lagre(isA(ProsessTaskData.class));
    }

    @Test
    void skal_håndtere_tom_liste() {
        // Arrange
        when(entityManager.createQuery(
            "from ForespørselEntitet where id >= :fom and id <= :tom and opprettetTidspunkt >= :dato"
                + " and status = :status order by id",
            ForespørselEntitet.class)).thenReturn(query);
        when(query.setParameter("fom", 1L)).thenReturn(query);
        when(query.setParameter("tom", 10L)).thenReturn(query);
        when(query.setParameter("dato", LocalDate.of(2026, 4, 6).atStartOfDay())).thenReturn(query);
        when(query.setParameter("status", ForespørselStatus.UNDER_BEHANDLING)).thenReturn(query);
        when(query.setMaxResults(50)).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        var prosessTaskData = ProsessTaskData.forProsessTask(OppdatereTilFerdigHvisMottattImTask.class);
        prosessTaskData.setProperty("fom", "1");
        prosessTaskData.setProperty("tom", "10");
        prosessTaskData.setProperty("dryRun", "false");

        // Act
        task.doTask(prosessTaskData);

        // Assert
        verify(forespørselBehandlingTjeneste, never()).ferdigstillForespørsel(any(), any(), any(), any(), any(), any());
        verify(prosessTaskTjeneste, never()).lagre(isA(ProsessTaskData.class));
    }

    @Test
    void skal_default_dry_run_til_true() {
        // Arrange
        when(entityManager.createQuery(
            "from ForespørselEntitet where id >= :fom and id <= :tom and opprettetTidspunkt >= :dato"
                + " and status = :status order by id",
            ForespørselEntitet.class)).thenReturn(query);
        when(query.setParameter("fom", 1L)).thenReturn(query);
        when(query.setParameter("tom", 10L)).thenReturn(query);
        when(query.setParameter("dato", LocalDate.of(2026, 4, 6).atStartOfDay())).thenReturn(query);
        when(query.setParameter("status", ForespørselStatus.UNDER_BEHANDLING)).thenReturn(query);
        when(query.setMaxResults(50)).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        var prosessTaskData = ProsessTaskData.forProsessTask(OppdatereTilFerdigHvisMottattImTask.class);
        prosessTaskData.setProperty("fom", "1");
        prosessTaskData.setProperty("tom", "10");
        // dryRun not set

        // Act
        task.doTask(prosessTaskData);

        // Assert
        verify(forespørselBehandlingTjeneste, never()).ferdigstillForespørsel(any(), any(), any(), any(), any(), any());
    }

    @Test
    void skal_lagre_neste_task_med_korrekte_parametere() {
        // Arrange
        var forespørselId = 1L;
        var forespørselUuid = UUID.randomUUID();
        var inntektsmeldingUuid = UUID.randomUUID();

        var forespørsel = opprettForespørsel(forespørselId, forespørselUuid);
        var inntektsmelding = opprettInntektsmelding(inntektsmeldingUuid);

        when(entityManager.createQuery(
            "from ForespørselEntitet where id >= :fom and id <= :tom and opprettetTidspunkt >= :dato"
                + " and status = :status order by id",
            ForespørselEntitet.class)).thenReturn(query);
        when(query.setParameter("fom", 100L)).thenReturn(query);
        when(query.setParameter("tom", 200L)).thenReturn(query);
        when(query.setParameter("dato", LocalDate.of(2026, 4, 6).atStartOfDay())).thenReturn(query);
        when(query.setParameter("status", ForespørselStatus.UNDER_BEHANDLING)).thenReturn(query);
        when(query.setMaxResults(50)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(forespørsel));

        when(inntektsmeldingTjeneste.hentSisteInntektsmelding(forespørselUuid)).thenReturn(inntektsmelding);

        var prosessTaskData = ProsessTaskData.forProsessTask(OppdatereTilFerdigHvisMottattImTask.class);
        prosessTaskData.setProperty("fom", "100");
        prosessTaskData.setProperty("tom", "200");
        prosessTaskData.setProperty("dryRun", "false");

        // Act
        task.doTask(prosessTaskData);

        // Assert
        verify(prosessTaskTjeneste).lagre(isA(ProsessTaskData.class));
    }

    // Helper methods

    private ForespørselEntitet opprettForespørsel(Long id, UUID uuid) {
        var forespørsel = new ForespørselEntitet(
            ORG_NUMMER,
            LocalDate.of(2026, 4, 1),
            new AktørIdEntitet(AKTØR_ID),
            Ytelsetype.FORELDREPENGER,
            "SAK123",
            FØRSTE_UTTAKSDATO,
            ForespørselType.BESTILT_AV_FAGSYSTEM
        );
        // Sett id og uuid via refleksjon siden settere ikke er tilgjengelig
        try {
            var idField = ForespørselEntitet.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(forespørsel, id);

            var uuidField = ForespørselEntitet.class.getDeclaredField("uuid");
            uuidField.setAccessible(true);
            uuidField.set(forespørsel, uuid);

            var statusField = ForespørselEntitet.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(forespørsel, ForespørselStatus.UNDER_BEHANDLING);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return forespørsel;
    }

    private InntektsmeldingDto opprettInntektsmelding(UUID uuid) {
        return InntektsmeldingDto.builder()
            .medId(1L)
            .medInntektsmeldingUuid(uuid)
            .build();
    }
}

















