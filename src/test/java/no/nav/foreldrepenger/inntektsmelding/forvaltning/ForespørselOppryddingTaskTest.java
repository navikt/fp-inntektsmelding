package no.nav.foreldrepenger.inntektsmelding.forvaltning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ch.qos.logback.classic.spi.ILoggingEvent;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.lager.ForespørselEntitet;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.fpsak.FpsakKlient;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Saksnummer;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.foreldrepenger.inntektsmelding.typer.lager.AktørIdEntitet;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.log.util.MemoryAppender;

@ExtendWith(MockitoExtension.class)
class ForespørselOppryddingTaskTest {

    private static final String ORG_NUMMER = "999999999";
    private static final LocalDate FØRSTE_UTTAKSDATO = LocalDate.of(2026, 4, 7);

    @Mock
    private EntityManager entityManager;
    @Mock
    private ProsessTaskTjeneste prosessTaskTjeneste;
    @Mock
    private ForespørselTjeneste forespørselTjeneste;
    @Mock
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    @Mock
    private FpsakKlient fpsakKlient;
    @Mock
    private TypedQuery<ForespørselEntitet> query;

    private ForespørselOppryddingTask task;
    private static MemoryAppender logSniffer;

    @BeforeEach
    void setUp() {
        task = new ForespørselOppryddingTask(entityManager, prosessTaskTjeneste, forespørselTjeneste, forespørselBehandlingTjeneste, fpsakKlient);
        logSniffer = MemoryAppender.sniff(ForespørselOppryddingTask.class);
    }

    @AfterEach
    void afterEach() {
        logSniffer.reset();
    }

    @Test
    void skal_ikke_gjøre_noe_ved_tom_side() {
        mockSide(0L, List.of());

        task.doTask(lagProsessTaskData(0L, false));

        verify(fpsakKlient, never()).sjekkForespørselStatus(any());
        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskData.class));
        verify(forespørselBehandlingTjeneste, never()).settForespørselTilUtgåttForvaltning(any());
    }

    @Test
    void skal_hoppe_over_forespørsler_uten_saksnummer_og_avslutte() {
        var forespørsel = opprettForespørselUtenSaksnummer(1L);
        mockSide(0L, List.of(forespørsel));

        task.doTask(lagProsessTaskData(0L, false));

        verify(fpsakKlient, never()).sjekkForespørselStatus(any());
        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskData.class));
    }

    @Test
    void skal_lukke_alle_forespørsler_for_kombinasjonen_ved_trengs_ikke_og_ikke_dry_run() {
        var uuid1 = UUID.randomUUID();
        var uuid2 = UUID.randomUUID();
        var forespørsel1 = opprettForespørsel(1L, uuid1, "SAK1", LocalDateTime.now().minusDays(1));
        var forespørsel2 = opprettForespørsel(2L, uuid2, "SAK1", LocalDateTime.now());

        mockSide(0L, List.of(forespørsel1, forespørsel2));

        var dto1 = byggDto(1L, uuid1, "SAK1", forespørsel1.getOpprettetTidspunkt());
        var dto2 = byggDto(2L, uuid2, "SAK1", forespørsel2.getOpprettetTidspunkt());
        when(forespørselTjeneste.finnÅpneForespørslerForFagsak(new Saksnummer("SAK1"))).thenReturn(List.of(dto1, dto2));

        when(fpsakKlient.sjekkForespørselStatus(any())).thenReturn(
            List.of(new FpsakKlient.ForespørselStatusResponse("SAK1", ORG_NUMMER, FpsakKlient.ForespørselStatusResponse.Vurdering.TRENGS_IKKE,
                FpsakKlient.ForespørselStatusResponse.Årsak.SAK_AVSLUTTET)));

        mockLåstEntitet(forespørsel1);
        mockLåstEntitet(forespørsel2);

        task.doTask(lagProsessTaskData(0L, false));

        verify(forespørselBehandlingTjeneste).settForespørselTilUtgåttForvaltning(uuid1);
        verify(forespørselBehandlingTjeneste).settForespørselTilUtgåttForvaltning(uuid2);
        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskData.class));

        var loggetForespørsel1 = logSniffer.getLoggedEvents().stream()
            .map(ILoggingEvent::getFormattedMessage)
            .filter(m -> m.contains("id=1 saksnummer=SAK1 orgnummer=" + ORG_NUMMER))
            .toList();
        assertThat(loggetForespørsel1).isNotEmpty();
        assertThat(loggetForespørsel1.getFirst()).contains("TRENGS_IKKE").doesNotContain("aktørId").doesNotContain("fnr");
    }

    @Test
    void skal_beholde_nyeste_forespørsel_og_lukke_eldre_duplikater_ved_trengs() {
        var uuidGammel = UUID.randomUUID();
        var uuidNyest = UUID.randomUUID();
        var forespørselGammel = opprettForespørsel(1L, uuidGammel, "SAK2", LocalDateTime.now().minusDays(5));
        var forespørselNyest = opprettForespørsel(2L, uuidNyest, "SAK2", LocalDateTime.now());

        mockSide(0L, List.of(forespørselGammel, forespørselNyest));

        var dtoGammel = byggDto(1L, uuidGammel, "SAK2", forespørselGammel.getOpprettetTidspunkt());
        var dtoNyest = byggDto(2L, uuidNyest, "SAK2", forespørselNyest.getOpprettetTidspunkt());
        when(forespørselTjeneste.finnÅpneForespørslerForFagsak(new Saksnummer("SAK2"))).thenReturn(List.of(dtoGammel, dtoNyest));

        when(fpsakKlient.sjekkForespørselStatus(any())).thenReturn(
            List.of(new FpsakKlient.ForespørselStatusResponse("SAK2", ORG_NUMMER, FpsakKlient.ForespørselStatusResponse.Vurdering.TRENGS,
                FpsakKlient.ForespørselStatusResponse.Årsak.IM_MANGLER)));

        mockLåstEntitet(forespørselGammel);

        task.doTask(lagProsessTaskData(0L, false));

        verify(forespørselBehandlingTjeneste).settForespørselTilUtgåttForvaltning(uuidGammel);
        verify(forespørselBehandlingTjeneste, never()).settForespørselTilUtgåttForvaltning(uuidNyest);
    }

    @Test
    void skal_ikke_lukke_noe_ved_ukjent_vurdering() {
        var uuid1 = UUID.randomUUID();
        var forespørsel1 = opprettForespørsel(1L, uuid1, "SAK3", LocalDateTime.now());
        mockSide(0L, List.of(forespørsel1));

        var dto1 = byggDto(1L, uuid1, "SAK3", forespørsel1.getOpprettetTidspunkt());
        when(forespørselTjeneste.finnÅpneForespørslerForFagsak(new Saksnummer("SAK3"))).thenReturn(List.of(dto1));

        when(fpsakKlient.sjekkForespørselStatus(any())).thenReturn(
            List.of(new FpsakKlient.ForespørselStatusResponse("SAK3", ORG_NUMMER, FpsakKlient.ForespørselStatusResponse.Vurdering.UKJENT,
                FpsakKlient.ForespørselStatusResponse.Årsak.SAK_IKKE_FUNNET)));

        task.doTask(lagProsessTaskData(0L, false));

        verify(forespørselBehandlingTjeneste, never()).settForespørselTilUtgåttForvaltning(any());
    }

    @Test
    void dry_run_skal_aldri_gi_sideeffekter_men_skal_fortsatt_spørre_fpsak() {
        var uuid1 = UUID.randomUUID();
        var uuid2 = UUID.randomUUID();
        var forespørsel1 = opprettForespørsel(1L, uuid1, "SAK4", LocalDateTime.now().minusDays(1));
        var forespørsel2 = opprettForespørsel(2L, uuid2, "SAK4", LocalDateTime.now());
        mockSide(0L, List.of(forespørsel1, forespørsel2));

        var dto1 = byggDto(1L, uuid1, "SAK4", forespørsel1.getOpprettetTidspunkt());
        var dto2 = byggDto(2L, uuid2, "SAK4", forespørsel2.getOpprettetTidspunkt());
        when(forespørselTjeneste.finnÅpneForespørslerForFagsak(new Saksnummer("SAK4"))).thenReturn(List.of(dto1, dto2));

        when(fpsakKlient.sjekkForespørselStatus(any())).thenReturn(
            List.of(new FpsakKlient.ForespørselStatusResponse("SAK4", ORG_NUMMER, FpsakKlient.ForespørselStatusResponse.Vurdering.TRENGS_IKKE,
                FpsakKlient.ForespørselStatusResponse.Årsak.SAK_AVSLUTTET)));

        // dryRun-property settes ikke -> skal default til true
        var prosessTaskData = ProsessTaskData.forProsessTask(ForespørselOppryddingTask.class);
        prosessTaskData.setProperty("fraId", "0");

        task.doTask(prosessTaskData);

        verify(fpsakKlient).sjekkForespørselStatus(any());
        verify(forespørselBehandlingTjeneste, never()).settForespørselTilUtgåttForvaltning(any());
        verify(entityManager, never()).find(eq(ForespørselEntitet.class), any(), eq(LockModeType.PESSIMISTIC_WRITE));
    }

    @Test
    void skal_ikke_lukke_forespørsel_som_ikke_lenger_er_under_behandling_idempotens() {
        var uuidAlleredeLukket = UUID.randomUUID();
        var uuidÅpen = UUID.randomUUID();
        var forespørsel1 = opprettForespørsel(1L, uuidAlleredeLukket, "SAK5", LocalDateTime.now().minusDays(1));
        var forespørsel2 = opprettForespørsel(2L, uuidÅpen, "SAK5", LocalDateTime.now());
        mockSide(0L, List.of(forespørsel1, forespørsel2));

        var dto1 = byggDto(1L, uuidAlleredeLukket, "SAK5", forespørsel1.getOpprettetTidspunkt());
        var dto2 = byggDto(2L, uuidÅpen, "SAK5", forespørsel2.getOpprettetTidspunkt());
        when(forespørselTjeneste.finnÅpneForespørslerForFagsak(new Saksnummer("SAK5"))).thenReturn(List.of(dto1, dto2));

        when(fpsakKlient.sjekkForespørselStatus(any())).thenReturn(
            List.of(new FpsakKlient.ForespørselStatusResponse("SAK5", ORG_NUMMER, FpsakKlient.ForespørselStatusResponse.Vurdering.TRENGS_IKKE,
                FpsakKlient.ForespørselStatusResponse.Årsak.SAK_AVSLUTTET)));

        // Forespørsel 1 har i mellomtiden blitt FERDIG av en annen prosess (kappløp). Den pessimistiske låsen leser
        // den ferske statusen rett før lukking, og skal derfor hoppe over denne selv om den var UNDER_BEHANDLING
        // da kombinasjonen ble hentet.
        settFelter(forespørsel1, 1L, uuidAlleredeLukket, ForespørselStatus.FERDIG, forespørsel1.getOpprettetTidspunkt());
        mockLåstEntitet(forespørsel1);
        mockLåstEntitet(forespørsel2);

        task.doTask(lagProsessTaskData(0L, false));

        verify(forespørselBehandlingTjeneste, never()).settForespørselTilUtgåttForvaltning(uuidAlleredeLukket);
        verify(forespørselBehandlingTjeneste).settForespørselTilUtgåttForvaltning(uuidÅpen);
    }

    @Test
    void skal_planlegge_neste_task_ved_full_side() {
        var rader = new ArrayList<ForespørselEntitet>();
        for (var i = 1; i <= 500; i++) {
            rader.add(opprettForespørsel(i, UUID.randomUUID(), "SAK-FULL", LocalDateTime.now()));
        }
        mockSide(0L, rader);

        when(forespørselTjeneste.finnÅpneForespørslerForFagsak(any())).thenReturn(List.of());
        when(fpsakKlient.sjekkForespørselStatus(any())).thenReturn(
            List.of(new FpsakKlient.ForespørselStatusResponse("SAK-FULL", ORG_NUMMER, FpsakKlient.ForespørselStatusResponse.Vurdering.TRENGS_IKKE,
                FpsakKlient.ForespørselStatusResponse.Årsak.SAK_AVSLUTTET)));

        // dryRun default = true her, vi tester kun paginering
        var prosessTaskData = ProsessTaskData.forProsessTask(ForespørselOppryddingTask.class);
        prosessTaskData.setProperty("fraId", "0");

        task.doTask(prosessTaskData);

        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskTjeneste).lagre(captor.capture());
        assertThat(captor.getValue().getPropertyValue("fraId")).isEqualTo("500");
    }

    @Test
    void skal_stoppe_ved_100_kombinasjoner_og_planlegge_neste_task_selv_med_id_hull() {
        var rader = new ArrayList<ForespørselEntitet>();
        // 105 distinkte kombinasjoner, med store hull mellom id-ene for å simulere "lange id-hull"
        for (var i = 1; i <= 105; i++) {
            rader.add(opprettForespørsel(i * 100L, UUID.randomUUID(), "SAK-" + i, LocalDateTime.now()));
        }
        mockSide(0L, rader);

        when(forespørselTjeneste.finnÅpneForespørslerForFagsak(any())).thenAnswer(inv -> {
            Saksnummer saksnummer = inv.getArgument(0);
            return List.of(ForespørselDto.builder()
                .loepenr(1L)
                .uuid(UUID.randomUUID())
                .arbeidsgiver(Arbeidsgiver.fra(ORG_NUMMER))
                .status(ForespørselStatus.UNDER_BEHANDLING)
                .opprettetTidspunkt(LocalDateTime.now())
                .fagsystemSaksnummer(saksnummer)
                .build());
        });
        when(fpsakKlient.sjekkForespørselStatus(any())).thenAnswer(inv -> {
            List<FpsakKlient.ForespørselStatusRequest.Forespørsel> forespørsler = inv.getArgument(0);
            return forespørsler.stream()
                .map(f -> new FpsakKlient.ForespørselStatusResponse(f.fagsakSaksnummer(), f.orgnummer(),
                    FpsakKlient.ForespørselStatusResponse.Vurdering.UKJENT, FpsakKlient.ForespørselStatusResponse.Årsak.SAK_IKKE_FUNNET))
                .toList();
        });

        // dryRun default = true, vi tester kun paginering/maks-tak
        var prosessTaskData = ProsessTaskData.forProsessTask(ForespørselOppryddingTask.class);
        prosessTaskData.setProperty("fraId", "0");

        task.doTask(prosessTaskData);

        var requestCaptor = ArgumentCaptor.forClass(List.class);
        // Alle 105 forsøkte kombinasjonene har hvert sitt unike saksnummer, så fp-sak kalles én gang per saksnummer
        // (maks 100 saksnummer behandlet denne kjøringen pga. maks-taket).
        verify(fpsakKlient, times(100)).sjekkForespørselStatus(requestCaptor.capture());
        requestCaptor.getAllValues().forEach(kall -> assertThat(kall).hasSize(1));

        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskTjeneste).lagre(captor.capture());
        // Rad 100 har id 100*100=10000, rad 101 (som forårsaket bruddet) skal IKKE regnes som behandlet
        assertThat(captor.getValue().getPropertyValue("fraId")).isEqualTo("10000");
        assertThat(captor.getValue().getPropertyValue("dryRun")).isEqualTo("true");
    }

    @Test
    void skal_gjøre_ett_fpsak_kall_per_saksnummer_når_flere_saksnumre_finnes_i_samme_vindu() {
        var forespørselA = opprettForespørsel(1L, UUID.randomUUID(), "SAK-A", LocalDateTime.now());
        var forespørselB = opprettForespørsel(2L, UUID.randomUUID(), "SAK-B", LocalDateTime.now());
        mockSide(0L, List.of(forespørselA, forespørselB));

        when(forespørselTjeneste.finnÅpneForespørslerForFagsak(any())).thenReturn(List.of());
        when(fpsakKlient.sjekkForespørselStatus(any())).thenAnswer(inv -> {
            List<FpsakKlient.ForespørselStatusRequest.Forespørsel> forespørsler = inv.getArgument(0);
            return forespørsler.stream()
                .map(f -> new FpsakKlient.ForespørselStatusResponse(f.fagsakSaksnummer(), f.orgnummer(),
                    FpsakKlient.ForespørselStatusResponse.Vurdering.UKJENT, FpsakKlient.ForespørselStatusResponse.Årsak.SAK_IKKE_FUNNET))
                .toList();
        });

        // dryRun default = true, vi tester kun hvordan fp-sak kalles
        var prosessTaskData = ProsessTaskData.forProsessTask(ForespørselOppryddingTask.class);
        prosessTaskData.setProperty("fraId", "0");

        task.doTask(prosessTaskData);

        var requestCaptor = ArgumentCaptor.forClass(List.class);
        verify(fpsakKlient, times(2)).sjekkForespørselStatus(requestCaptor.capture());
        var saksnumreForespurt = requestCaptor.getAllValues().stream()
            .flatMap(List::stream)
            .map(o -> ((FpsakKlient.ForespørselStatusRequest.Forespørsel) o).fagsakSaksnummer())
            .toList();
        assertThat(saksnumreForespurt).containsExactlyInAnyOrder("SAK-A", "SAK-B");
        requestCaptor.getAllValues().forEach(kall -> assertThat(kall).hasSize(1));
    }

    @Test
    void skal_ikke_lukke_forespørsel_hvis_pessimistisk_lås_viser_annen_status_enn_snapshotet() {
        var uuid = UUID.randomUUID();
        var forespørsel = opprettForespørsel(1L, uuid, "SAK9", LocalDateTime.now());
        mockSide(0L, List.of(forespørsel));

        var dto = byggDto(1L, uuid, "SAK9", forespørsel.getOpprettetTidspunkt());
        when(forespørselTjeneste.finnÅpneForespørslerForFagsak(new Saksnummer("SAK9"))).thenReturn(List.of(dto));

        when(fpsakKlient.sjekkForespørselStatus(any())).thenReturn(
            List.of(new FpsakKlient.ForespørselStatusResponse("SAK9", ORG_NUMMER, FpsakKlient.ForespørselStatusResponse.Vurdering.TRENGS_IKKE,
                FpsakKlient.ForespørselStatusResponse.Årsak.SAK_AVSLUTTET)));

        // Den låste, ferske lesingen av raden viser en annen status enn snapshotet fra oppslaget
        settFelter(forespørsel, 1L, uuid, ForespørselStatus.FERDIG, forespørsel.getOpprettetTidspunkt());
        mockLåstEntitet(forespørsel);

        task.doTask(lagProsessTaskData(0L, false));

        verify(entityManager).find(ForespørselEntitet.class, 1L, LockModeType.PESSIMISTIC_WRITE);
        verify(forespørselBehandlingTjeneste, never()).settForespørselTilUtgåttForvaltning(any());
    }

    // Hjelpemetoder

    private void mockLåstEntitet(ForespørselEntitet entitet) {
        when(entityManager.find(ForespørselEntitet.class, entitet.getId(), LockModeType.PESSIMISTIC_WRITE)).thenReturn(entitet);
    }

    private void mockSide(long fraId, List<ForespørselEntitet> resultat) {
        when(entityManager.createQuery("from ForespørselEntitet where id > :fraId and status = :status order by id",
            ForespørselEntitet.class)).thenReturn(query);
        when(query.setParameter("fraId", fraId)).thenReturn(query);
        when(query.setParameter("status", ForespørselStatus.UNDER_BEHANDLING)).thenReturn(query);
        when(query.setMaxResults(500)).thenReturn(query);
        when(query.getResultList()).thenReturn(resultat);
    }

    private ProsessTaskData lagProsessTaskData(long fraId, boolean dryRun) {
        var prosessTaskData = ProsessTaskData.forProsessTask(ForespørselOppryddingTask.class);
        prosessTaskData.setProperty("fraId", String.valueOf(fraId));
        prosessTaskData.setProperty("dryRun", String.valueOf(dryRun));
        return prosessTaskData;
    }

    private ForespørselDto byggDto(long id, UUID uuid, String saksnummer, LocalDateTime opprettetTidspunkt) {
        return ForespørselDto.builder()
            .loepenr(id)
            .uuid(uuid)
            .arbeidsgiver(Arbeidsgiver.fra(ORG_NUMMER))
            .status(ForespørselStatus.UNDER_BEHANDLING)
            .opprettetTidspunkt(opprettetTidspunkt)
            .fagsystemSaksnummer(new Saksnummer(saksnummer))
            .build();
    }

    private ForespørselEntitet opprettForespørsel(long id, UUID uuid, String saksnummer, LocalDateTime opprettetTidspunkt) {
        var forespørsel = new ForespørselEntitet(ORG_NUMMER, LocalDate.of(2026, 4, 1), AktørIdEntitet.dummy(), Ytelsetype.FORELDREPENGER,
            saksnummer, FØRSTE_UTTAKSDATO, ForespørselType.BESTILT_AV_FAGSYSTEM);
        settFelter(forespørsel, id, uuid, ForespørselStatus.UNDER_BEHANDLING, opprettetTidspunkt);
        return forespørsel;
    }

    private ForespørselEntitet opprettForespørselUtenSaksnummer(long id) {
        var forespørsel = new ForespørselEntitet(ORG_NUMMER, null, AktørIdEntitet.dummy(), Ytelsetype.FORELDREPENGER, null, FØRSTE_UTTAKSDATO,
            ForespørselType.ARBEIDSGIVERINITIERT_UREGISTRERT);
        settFelter(forespørsel, id, UUID.randomUUID(), ForespørselStatus.UNDER_BEHANDLING, LocalDateTime.now());
        return forespørsel;
    }

    private void settFelter(ForespørselEntitet forespørsel, long id, UUID uuid, ForespørselStatus status, LocalDateTime opprettetTidspunkt) {
        try {
            var idField = ForespørselEntitet.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(forespørsel, id);

            var uuidField = ForespørselEntitet.class.getDeclaredField("uuid");
            uuidField.setAccessible(true);
            uuidField.set(forespørsel, uuid);

            var statusField = ForespørselEntitet.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(forespørsel, status);

            var opprettetTidspunktField = ForespørselEntitet.class.getDeclaredField("opprettetTidspunkt");
            opprettetTidspunktField.setAccessible(true);
            opprettetTidspunktField.set(forespørsel, opprettetTidspunkt);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
