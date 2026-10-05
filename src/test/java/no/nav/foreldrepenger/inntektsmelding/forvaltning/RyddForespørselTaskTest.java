package no.nav.foreldrepenger.inntektsmelding.forvaltning;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.lager.ForespørselEntitet;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.fpsak.FpsakKlient;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Saksnummer;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

/**
 * Tester forretningslogikken til sub-tasken: fp-sak-vurdering per (saksnummer, orgnummer)-kombinasjon, hvilke
 * duplikater som lukkes, og den pessimistiske lås-sjekken rett før lukking. Paginering og opprettelse av
 * sub-tasks testes i {@link RyddForespørslerTaskTest}.
 */
@ExtendWith(MockitoExtension.class)
class RyddForespørselTaskTest {

    private static final String ORG_NUMMER = "999999999";

    @Mock
    private EntityManager entityManager;
    @Mock
    private TypedQuery<ForespørselEntitet> query;
    @Mock
    private ForespørselTjeneste forespørselTjeneste;
    @Mock
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    @Mock
    private FpsakKlient fpsakKlient;

    private final Map<UUID, ForespørselEntitet> låsteEntiteter = new HashMap<>();
    private UUID sistSpurteUuid;

    private RyddForespørselTask task;

    @BeforeEach
    void setUp() {
        task = new RyddForespørselTask(entityManager, forespørselTjeneste, forespørselBehandlingTjeneste, fpsakKlient);

        lenient().when(entityManager.createQuery(anyString(), eq(ForespørselEntitet.class))).thenReturn(query);
        lenient().when(query.setLockMode(LockModeType.PESSIMISTIC_WRITE)).thenReturn(query);
        lenient().when(query.setParameter(eq("uuid"), any(UUID.class))).thenAnswer(invocation -> {
            sistSpurteUuid = invocation.getArgument(1);
            return query;
        });
        lenient().when(query.getSingleResult()).thenAnswer(invocation -> låsteEntiteter.get(sistSpurteUuid));
    }

    @Test
    void skal_ikke_gjøre_noe_når_ingen_åpne_forespørsler_finnes() {
        when(forespørselTjeneste.finnÅpneForespørslerForFagsak(new Saksnummer("SAK0"))).thenReturn(List.of());

        task.doTask(lagProsessTaskData("SAK0", ORG_NUMMER, false));

        verify(fpsakKlient, never()).sjekkForespørselStatus(any(), any());
        verify(forespørselBehandlingTjeneste, never()).settForespørselTilUtgåttForvaltning(any());
    }

    @Test
    void skal_lukke_alle_forespørsler_for_kombinasjonen_ved_trengs_ikke_og_ikke_dry_run() {
        var uuid1 = UUID.randomUUID();
        var uuid2 = UUID.randomUUID();
        var dto1 = lagDto(1L, uuid1, "SAK1", LocalDateTime.now().minusDays(1));
        var dto2 = lagDto(2L, uuid2, "SAK1", LocalDateTime.now());
        when(forespørselTjeneste.finnÅpneForespørslerForFagsak(new Saksnummer("SAK1"))).thenReturn(List.of(dto1, dto2));

        when(fpsakKlient.sjekkForespørselStatus("SAK1", ORG_NUMMER)).thenReturn(false);

        mockLåstEntitet(uuid1, ForespørselStatus.UNDER_BEHANDLING);
        mockLåstEntitet(uuid2, ForespørselStatus.UNDER_BEHANDLING);

        task.doTask(lagProsessTaskData("SAK1", ORG_NUMMER, false));

        verify(forespørselBehandlingTjeneste).settForespørselTilUtgåttForvaltning(uuid1);
        verify(forespørselBehandlingTjeneste).settForespørselTilUtgåttForvaltning(uuid2);
    }

    @Test
    void skal_beholde_nyeste_forespørsel_og_lukke_eldre_duplikater_ved_trengs() {
        var uuidGammel = UUID.randomUUID();
        var uuidNyest = UUID.randomUUID();
        var dtoGammel = lagDto(1L, uuidGammel, "SAK2", LocalDateTime.now().minusDays(5));
        var dtoNyest = lagDto(2L, uuidNyest, "SAK2", LocalDateTime.now());
        when(forespørselTjeneste.finnÅpneForespørslerForFagsak(new Saksnummer("SAK2"))).thenReturn(List.of(dtoGammel, dtoNyest));

        when(fpsakKlient.sjekkForespørselStatus("SAK2", ORG_NUMMER)).thenReturn(true);

        mockLåstEntitet(uuidGammel, ForespørselStatus.UNDER_BEHANDLING);

        task.doTask(lagProsessTaskData("SAK2", ORG_NUMMER, false));

        verify(forespørselBehandlingTjeneste).settForespørselTilUtgåttForvaltning(uuidGammel);
        verify(forespørselBehandlingTjeneste, never()).settForespørselTilUtgåttForvaltning(uuidNyest);
    }

    @Test
    void skal_ikke_lukke_noe_ved_trengs_og_kun_en_åpen_forespørsel() {
        var uuid = UUID.randomUUID();
        var dto = lagDto(1L, uuid, "SAK2B", LocalDateTime.now());
        when(forespørselTjeneste.finnÅpneForespørslerForFagsak(new Saksnummer("SAK2B"))).thenReturn(List.of(dto));

        when(fpsakKlient.sjekkForespørselStatus("SAK2B", ORG_NUMMER)).thenReturn(true);

        task.doTask(lagProsessTaskData("SAK2B", ORG_NUMMER, false));

        verify(forespørselBehandlingTjeneste, never()).settForespørselTilUtgåttForvaltning(any());
        verify(entityManager, never()).createQuery(anyString(), eq(ForespørselEntitet.class));
    }

    @Test
    void skal_ikke_lukke_noe_i_dry_run_men_fortsatt_spørre_fpsak() {
        var dto1 = lagDto(1L, UUID.randomUUID(), "SAK4", LocalDateTime.now().minusDays(1));
        var dto2 = lagDto(2L, UUID.randomUUID(), "SAK4", LocalDateTime.now());
        when(forespørselTjeneste.finnÅpneForespørslerForFagsak(new Saksnummer("SAK4"))).thenReturn(List.of(dto1, dto2));

        when(fpsakKlient.sjekkForespørselStatus("SAK4", ORG_NUMMER)).thenReturn(false);

        var prosessTaskData = ProsessTaskData.forProsessTask(RyddForespørselTask.class);
        prosessTaskData.setProperty("saksnummer", "SAK4");
        prosessTaskData.setProperty("orgnummer", ORG_NUMMER);

        task.doTask(prosessTaskData);

        verify(fpsakKlient).sjekkForespørselStatus("SAK4", ORG_NUMMER);
        verify(forespørselBehandlingTjeneste, never()).settForespørselTilUtgåttForvaltning(any());
        verify(entityManager, never()).createQuery(anyString(), eq(ForespørselEntitet.class));
    }

    @Test
    void skal_ikke_lukke_forespørsel_som_ikke_lenger_er_under_behandling_idempotens() {
        var uuidAlleredeLukket = UUID.randomUUID();
        var uuidÅpen = UUID.randomUUID();
        var dto1 = lagDto(1L, uuidAlleredeLukket, "SAK5", LocalDateTime.now().minusDays(1));
        var dto2 = lagDto(2L, uuidÅpen, "SAK5", LocalDateTime.now());
        when(forespørselTjeneste.finnÅpneForespørslerForFagsak(new Saksnummer("SAK5"))).thenReturn(List.of(dto1, dto2));

        when(fpsakKlient.sjekkForespørselStatus("SAK5", ORG_NUMMER)).thenReturn(false);

        mockLåstEntitet(uuidAlleredeLukket, ForespørselStatus.FERDIG);
        mockLåstEntitet(uuidÅpen, ForespørselStatus.UNDER_BEHANDLING);

        task.doTask(lagProsessTaskData("SAK5", ORG_NUMMER, false));

        verify(forespørselBehandlingTjeneste, never()).settForespørselTilUtgåttForvaltning(uuidAlleredeLukket);
        verify(forespørselBehandlingTjeneste).settForespørselTilUtgåttForvaltning(uuidÅpen);
    }

    @Test
    void skal_ikke_lukke_forespørsel_hvis_pessimistisk_lås_viser_annen_status_enn_snapshotet() {
        var uuid = UUID.randomUUID();
        var dto = lagDto(1L, uuid, "SAK9", LocalDateTime.now());
        when(forespørselTjeneste.finnÅpneForespørslerForFagsak(new Saksnummer("SAK9"))).thenReturn(List.of(dto));

        when(fpsakKlient.sjekkForespørselStatus("SAK9", ORG_NUMMER)).thenReturn(false);

        mockLåstEntitet(uuid, ForespørselStatus.FERDIG);

        task.doTask(lagProsessTaskData("SAK9", ORG_NUMMER, false));

        verify(query).setParameter("uuid", uuid);
        verify(query).setLockMode(LockModeType.PESSIMISTIC_WRITE);
        verify(forespørselBehandlingTjeneste, never()).settForespørselTilUtgåttForvaltning(any());
    }

    private void mockLåstEntitet(UUID uuid, ForespørselStatus status) {
        var entitet = mock(ForespørselEntitet.class);
        lenient().when(entitet.getStatus()).thenReturn(status);
        låsteEntiteter.put(uuid, entitet);
    }

    private ProsessTaskData lagProsessTaskData(String saksnummer, String orgnummer, boolean dryRun) {
        var prosessTaskData = ProsessTaskData.forProsessTask(RyddForespørselTask.class);
        prosessTaskData.setProperty("saksnummer", saksnummer);
        prosessTaskData.setProperty("orgnummer", orgnummer);
        prosessTaskData.setProperty("dryRun", String.valueOf(dryRun));
        return prosessTaskData;
    }

    private ForespørselDto lagDto(long id, UUID uuid, String saksnummer, LocalDateTime opprettetTidspunkt) {
        return ForespørselDto.builder()
            .loepenr(id)
            .uuid(uuid)
            .arbeidsgiver(Arbeidsgiver.fra(ORG_NUMMER))
            .status(ForespørselStatus.UNDER_BEHANDLING)
            .opprettetTidspunkt(opprettetTidspunkt)
            .fagsystemSaksnummer(new Saksnummer(saksnummer))
            .build();
    }
}
