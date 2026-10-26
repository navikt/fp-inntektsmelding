package no.nav.foreldrepenger.inntektsmelding.forespørsel.lager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.tuple;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import no.nav.foreldrepenger.inntektsmelding.database.JpaExtension;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDtoMapper;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.foreldrepenger.inntektsmelding.typer.lager.AktørIdEntitet;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;
import no.nav.vedtak.konfig.Tid;

@ExtendWith(JpaExtension.class)
class ForespørselRepositoryTest extends EntityManagerAwareTest {

    private ForespørselRepository forespørselRepository;

    @BeforeEach
    void setUp() {
        this.forespørselRepository = new ForespørselRepository(getEntityManager());
    }

    private UUID lagreForespørsel(LocalDate skjæringstidspunkt, Ytelsetype ytelsetype, String aktørId, String orgnr, String saksnummer,
                                  LocalDate førsteUttaksdato, ForespørselType type) {
        return forespørselRepository.lagreForespørsel(
            new ForespørselEntitet(orgnr, skjæringstidspunkt, new AktørIdEntitet(aktørId), ytelsetype, saksnummer, førsteUttaksdato, type));
    }

    @Test
    void skal_tildele_stigende_løpenummer_bare_ved_første_lagring() {
        var førsteUuid = lagreForespørsel(LocalDate.now(), Ytelsetype.FORELDREPENGER, "9999999999999",
            "999999999", "123", LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);
        var første = forespørselRepository.hentForespørsel(førsteUuid).orElseThrow();
        var løpenummer = første.getLøpenummer();
        assertThat(løpenummer).isNotNull();

        forespørselRepository.lagreForespørsel(første);
        assertThat(første.getLøpenummer()).isEqualTo(løpenummer);

        var andreUuid = lagreForespørsel(LocalDate.now(), Ytelsetype.FORELDREPENGER, "9999999999999",
            "999999999", "124", LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);
        getEntityManager().clear();

        assertThat(forespørselRepository.hentForespørsel(førsteUuid).orElseThrow().getLøpenummer()).isEqualTo(løpenummer);
        assertThat(forespørselRepository.hentForespørsel(andreUuid).orElseThrow().getLøpenummer()).isEqualTo(løpenummer + 1);
    }

    @Test
    void skal_beholde_null_løpenummer_ved_lagring_av_historisk_forespørsel() {
        var historisk = new ForespørselEntitet("999999999", LocalDate.now(), new AktørIdEntitet("9999999999999"),
            Ytelsetype.FORELDREPENGER, "123", LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);
        getEntityManager().persist(historisk);
        getEntityManager().flush();
        getEntityManager().clear();

        var hentet = forespørselRepository.hentForespørsel(historisk.getUuid()).orElseThrow();
        assertThat(hentet.getLøpenummer()).isNull();
        forespørselRepository.lagreForespørsel(hentet);
        assertThat(hentet.getLøpenummer()).isNull();
        getEntityManager().clear();

        assertThat(forespørselRepository.hentForespørsel(historisk.getUuid()).orElseThrow().getLøpenummer()).isNull();
    }

    @Test
    void skal_avvise_endring_av_tildelt_løpenummer() {
        var forespørsel = new ForespørselEntitet("999999999", LocalDate.now(), new AktørIdEntitet("9999999999999"),
            Ytelsetype.FORELDREPENGER, "123", LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);
        forespørsel.setLøpenummer(150000L);
        forespørsel.setLøpenummer(150000L);

        assertThatIllegalStateException().isThrownBy(() -> forespørsel.setLøpenummer(150001L));
        assertThat(forespørsel.getLøpenummer()).isEqualTo(150000L);
    }

    @Test
    void skal_lagre_arbeidsgiverinitiert_forespørsel() {
        var uuid = lagreForespørsel(null,
            Ytelsetype.FORELDREPENGER,
            "9999999999999",
            "999999999",
            "123",
            LocalDate.now(),
            ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT);

        var hentet = forespørselRepository.hentForespørsel(uuid).orElse(null);

        assertThat(hentet).isNotNull();
        assertThat(hentet.getSkjæringstidspunkt()).isEmpty();
        assertThat(hentet.getOrganisasjonsnummer()).isEqualTo("999999999");
        assertThat(hentet.getAktørId().getAktørId()).isEqualTo("9999999999999");
        assertThat(hentet.getYtelseType()).isEqualTo(Ytelsetype.FORELDREPENGER);
        assertThat(hentet.getFagsystemSaksnummer()).contains("123");
        assertThat(hentet.getFørsteUttaksdato()).isEqualTo(LocalDate.now());
    }

    @Test
    void skal_lagre_forespørsel_bestilt_fra_fagsystem() {
        var uuid = lagreForespørsel(LocalDate.now(),
            Ytelsetype.FORELDREPENGER,
            "9999999999999",
            "999999999",
            "123",
            LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);

        var hentet = forespørselRepository.hentForespørsel(uuid).orElse(null);

        assertThat(hentet).isNotNull();
        assertThat(hentet.getSkjæringstidspunkt().orElse(null)).isEqualTo(LocalDate.now());
        assertThat(hentet.getOrganisasjonsnummer()).isEqualTo("999999999");
        assertThat(hentet.getAktørId().getAktørId()).isEqualTo("9999999999999");
        assertThat(hentet.getYtelseType()).isEqualTo(Ytelsetype.FORELDREPENGER);
        assertThat(hentet.getFagsystemSaksnummer().orElseThrow()).isEqualTo("123");
        assertThat(hentet.getFørsteUttaksdato()).isEqualTo(LocalDate.now());
    }

    @Test
    void skal_lagre_forespørsel_og_oppdatere_med_dialogporten_uuid() {
        var uuid = lagreForespørsel(LocalDate.now(),
            Ytelsetype.FORELDREPENGER,
            "9999999999999",
            "999999999",
            "123",
            LocalDate.now(),
            ForespørselType.BESTILT_AV_FAGSYSTEM);
        // UUID versjon 7, som dialogporten gir oss. Kan ikke generere med java.util da den er på versjon 4
        var dialogportenUuid = UUID.fromString("0198cbab-47ec-7aca-82ad-5dc7d4a823d4");
        forespørselRepository.oppdaterDialogportenUuid(uuid, dialogportenUuid);

        var hentet = forespørselRepository.hentForespørsel(uuid).orElse(null);

        assertThat(hentet).isNotNull();
        assertThat(hentet.getSkjæringstidspunkt().orElse(null)).isEqualTo(LocalDate.now());
        assertThat(hentet.getOrganisasjonsnummer()).isEqualTo("999999999");
        assertThat(hentet.getAktørId().getAktørId()).isEqualTo("9999999999999");
        assertThat(hentet.getYtelseType()).isEqualTo(Ytelsetype.FORELDREPENGER);
        assertThat(hentet.getFagsystemSaksnummer().orElse(null)).isEqualTo("123");
        assertThat(hentet.getFørsteUttaksdato()).isEqualTo(LocalDate.now());
        assertThat(hentet.getDialogportenUuid().orElse(null)).isEqualTo(dialogportenUuid);
    }

    @ParameterizedTest
    @CsvSource({
        "2026-09-08, 2026-08-01",
        "2026-09-01, 2026-08-08",
        "2026-09-08, 2026-08-08"
    })
    void skal_lagre_tidligere_datoer_i_historikk_ved_datoendring(LocalDate nyFørsteUttaksdato, LocalDate nyttSkjæringstidspunkt) {
        var tidligereSkjæringstidspunkt = LocalDate.of(2026, 8, 1);
        var tidligereFørsteUttaksdato = LocalDate.of(2026, 9, 1);
        var uuid = lagreForespørsel(tidligereSkjæringstidspunkt, Ytelsetype.FORELDREPENGER,
            "9999999999999", "999999999", "123", tidligereFørsteUttaksdato, ForespørselType.BESTILT_AV_FAGSYSTEM);

        forespørselRepository.oppdaterUttaksdatoOgSkjæringstidspunkt(uuid, nyFørsteUttaksdato, nyttSkjæringstidspunkt);
        getEntityManager().flush();
        getEntityManager().clear();

        var hentet = forespørselRepository.hentForespørsel(uuid).orElseThrow();
        assertThat(hentet.getFørsteUttaksdato()).isEqualTo(nyFørsteUttaksdato);
        assertThat(hentet.getSkjæringstidspunkt()).contains(nyttSkjæringstidspunkt);
        assertThat(hentet.getEndringer()).singleElement().satisfies(endring -> {
            assertThat(endring.getId()).isPositive();
            assertThat(endring.getForespørsel().getUuid()).isEqualTo(uuid);
            assertThat(endring.getFørsteUttaksdato()).contains(tidligereFørsteUttaksdato);
            assertThat(endring.getSkjæringstidspunkt()).contains(tidligereSkjæringstidspunkt);
            assertThat(endring.getOpprettetTidspunkt()).isNotNull();
        });
    }

    @Test
    void skal_beholde_tidligere_historikk_ved_flere_datoendringer() {
        var opprinneligSkjæringstidspunkt = LocalDate.of(2026, 8, 1);
        var opprinneligFørsteUttaksdato = LocalDate.of(2026, 9, 1);
        var uuid = lagreForespørsel(opprinneligSkjæringstidspunkt, Ytelsetype.FORELDREPENGER,
            "9999999999999", "999999999", "123", opprinneligFørsteUttaksdato, ForespørselType.BESTILT_AV_FAGSYSTEM);
        var mellomliggendeSkjæringstidspunkt = opprinneligSkjæringstidspunkt.plusWeeks(1);
        var mellomliggendeFørsteUttaksdato = opprinneligFørsteUttaksdato.plusWeeks(1);

        forespørselRepository.oppdaterUttaksdatoOgSkjæringstidspunkt(uuid, mellomliggendeFørsteUttaksdato, mellomliggendeSkjæringstidspunkt);
        getEntityManager().flush();
        getEntityManager().clear();

        var etterFørsteEndring = forespørselRepository.hentForespørsel(uuid).orElseThrow();
        assertThat(etterFørsteEndring.getEndringer()).hasSize(1);
        var førsteHistorikkId = etterFørsteEndring.getEndringer().getFirst().getId();
        var sisteFørsteUttaksdato = mellomliggendeFørsteUttaksdato.plusWeeks(1);
        var sisteSkjæringstidspunkt = mellomliggendeSkjæringstidspunkt.plusWeeks(1);

        forespørselRepository.oppdaterUttaksdatoOgSkjæringstidspunkt(uuid, sisteFørsteUttaksdato, sisteSkjæringstidspunkt);
        getEntityManager().flush();
        getEntityManager().clear();

        var hentet = forespørselRepository.hentForespørsel(uuid).orElseThrow();
        assertThat(hentet.getFørsteUttaksdato()).isEqualTo(sisteFørsteUttaksdato);
        assertThat(hentet.getSkjæringstidspunkt()).contains(sisteSkjæringstidspunkt);
        assertThat(hentet.getEndringer()).hasSize(2)
            .extracting(endring -> endring.getSkjæringstidspunkt().orElseThrow(),
                endring -> endring.getFørsteUttaksdato().orElseThrow())
            .containsExactlyInAnyOrder(
                tuple(opprinneligSkjæringstidspunkt, opprinneligFørsteUttaksdato),
                tuple(mellomliggendeSkjæringstidspunkt, mellomliggendeFørsteUttaksdato));
        assertThat(hentet.getEndringer()).extracting(ForespørselEndringHistorikkEntitet::getId)
            .contains(førsteHistorikkId)
            .doesNotContainNull()
            .doesNotHaveDuplicates();
    }

    @Test
    void skal_lagre_historikk_uten_tidligere_skjæringstidspunkt() {
        var tidligereFørsteUttaksdato = LocalDate.of(2026, 9, 1);
        var uuid = lagreForespørsel(null, Ytelsetype.FORELDREPENGER,
            "9999999999999", "999999999", "123", tidligereFørsteUttaksdato, ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT);
        var nyFørsteUttaksdato = tidligereFørsteUttaksdato.plusWeeks(1);
        var nyttSkjæringstidspunkt = LocalDate.of(2026, 8, 1);

        forespørselRepository.oppdaterUttaksdatoOgSkjæringstidspunkt(uuid, nyFørsteUttaksdato, nyttSkjæringstidspunkt);
        getEntityManager().flush();
        getEntityManager().clear();

        var hentet = forespørselRepository.hentForespørsel(uuid).orElseThrow();
        assertThat(hentet.getFørsteUttaksdato()).isEqualTo(nyFørsteUttaksdato);
        assertThat(hentet.getSkjæringstidspunkt()).contains(nyttSkjæringstidspunkt);
        assertThat(hentet.getEndringer()).singleElement().satisfies(endring -> {
            assertThat(endring.getId()).isPositive();
            assertThat(endring.getForespørsel().getUuid()).isEqualTo(uuid);
            assertThat(endring.getFørsteUttaksdato()).contains(tidligereFørsteUttaksdato);
            assertThat(endring.getSkjæringstidspunkt()).isEmpty();
            assertThat(endring.getOpprettetTidspunkt()).isNotNull();
        });
    }

    @Test
    void skal_søke_på_forespørsler_når_kun_orgnr_oppgitt() {
        var orgnr = "999999999";
        lagreForespørsel(LocalDate.now(),
            Ytelsetype.FORELDREPENGER,
            "9999999999999",
            orgnr,
            "123",
            LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);
        lagreForespørsel(LocalDate.now(),
            Ytelsetype.SVANGERSKAPSPENGER,
            "9999999999999",
            orgnr,
            "123",
            LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);

        var forespørsler = forespørselRepository.hentForespørslerFraFilter(orgnr, null, null, null, null, null, null);

        assertThat(forespørsler).hasSize(2);
    }

    @Test
    void skal_søke_på_forespørsler_når_kun_orgnr_og_ytelse_oppgitt() {
        var orgnr = "999999999";
        lagreForespørsel(LocalDate.now(),
            Ytelsetype.FORELDREPENGER,
            "9999999999999",
            orgnr,
            "123",
            LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);

        lagreForespørsel(LocalDate.now(),
            Ytelsetype.SVANGERSKAPSPENGER,
            "9999999999999",
            orgnr,
            "123",
            LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);

        var forespørsler = forespørselRepository.hentForespørslerFraFilter(orgnr, null, null, Ytelsetype.SVANGERSKAPSPENGER, null, null, null);

        assertThat(forespørsler).hasSize(1);
    }

    @Test
    void skal_søke_på_forespørsler_når_kun_orgnr_og_aktørId_er_oppgitt() {
        var orgnr = "999999999";
        lagreForespørsel(LocalDate.now(),
            Ytelsetype.FORELDREPENGER,
            "9999999999999",
            orgnr,
            "123",
            LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);

        lagreForespørsel(LocalDate.now(),
            Ytelsetype.SVANGERSKAPSPENGER,
            "8888888888888",
            orgnr,
            "123",
            LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);

        lagreForespørsel(LocalDate.now(),
            Ytelsetype.FORELDREPENGER,
            "8888888888888",
            orgnr,
            "123",
            LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);

        var forespørsler = forespørselRepository.hentForespørslerFraFilter(orgnr, new AktørIdEntitet("8888888888888"), null, null, null, null, null);

        assertThat(forespørsler).hasSize(2);
    }

    @Test
    void skal_søke_på_intervall() {
        var orgnr = "999999999";
        lagreForespørsel(LocalDate.now(),
            Ytelsetype.FORELDREPENGER,
            "9999999999999",
            orgnr,
            "123",
            LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);

        lagreForespørsel(LocalDate.now(),
            Ytelsetype.SVANGERSKAPSPENGER,
            "8888888888888",
            orgnr,
            "123",
            LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);

        var forespørsler = forespørselRepository.hentForespørslerFraFilter(orgnr, null, null, null,
            Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE, null);

        assertThat(forespørsler).hasSize(2);
    }

    @Test
    void skal_søke_på_intervall_med_0_resultat() {
        var orgnr = "999999999";
        lagreForespørsel(LocalDate.now(),
            Ytelsetype.FORELDREPENGER,
            "9999999999999",
            orgnr,
            "123",
            LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);

        lagreForespørsel(LocalDate.now(),
            Ytelsetype.SVANGERSKAPSPENGER,
            "8888888888888",
            orgnr,
            "123",
            LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);

        var forespørsler = forespørselRepository.hentForespørslerFraFilter(orgnr, null, null, null,
            LocalDate.now().plusDays(7), Tid.TIDENES_ENDE, null);

        assertThat(forespørsler).isEmpty();
    }

    @Test
    void skal_søke_på_åpent_intervall() {
        var orgnr = "999999999";
        lagreForespørsel(LocalDate.now(),
            Ytelsetype.FORELDREPENGER,
            "9999999999999",
            orgnr,
            "123",
            LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);

        lagreForespørsel(LocalDate.now(),
            Ytelsetype.SVANGERSKAPSPENGER,
            "8888888888888",
            orgnr,
            "111",
            LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);

        lagreForespørsel(LocalDate.now(),
            Ytelsetype.FORELDREPENGER,
            "7777777777777",
            orgnr,
            "321",
            LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);

        var forespørsler = forespørselRepository.hentForespørslerFraFilter(orgnr, null, null, null,
            null, LocalDate.now().plusDays(7), null);

        assertThat(forespørsler).hasSize(3);
    }

    @Test
    void skal_søke_på_fraLoepenr() {
        var orgnr = "999999999";
        lagreForespørsel(LocalDate.now(),
            Ytelsetype.FORELDREPENGER,
            "9999999999999",
            orgnr,
            "123",
            LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);

        lagreForespørsel(LocalDate.now(),
            Ytelsetype.SVANGERSKAPSPENGER,
            "8888888888888",
            orgnr,
            "111",
            LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);

        lagreForespørsel(LocalDate.now(),
            Ytelsetype.FORELDREPENGER,
            "7777777777777",
            orgnr,
            "321",
            LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);

        var lavesteLøpenummer = forespørselRepository.hentForespørslerFraFilter(orgnr, null, null, null, null, null, null).stream()
            .map(ForespørselEntitet::getLøpenummer)
            .min(Long::compareTo)
            .orElseThrow();

        var forespørsler = forespørselRepository.hentForespørslerFraFilter(orgnr, null, null, null,
            null, LocalDate.now().plusDays(7), lavesteLøpenummer);

        assertThat(forespørsler).hasSize(2);
        assertThat(forespørsler).allSatisfy(fs -> assertThat(fs.getLøpenummer()).isGreaterThan(lavesteLøpenummer));
    }

    @Test
    void skal_bruke_samme_løpenummer_i_svar_filter_og_sortering_før_og_etter_etterfylling() {
        var orgnr = "999999999";
        var historisk = new ForespørselEntitet(orgnr, LocalDate.now(), new AktørIdEntitet("9999999999999"),
            Ytelsetype.FORELDREPENGER, "123", LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);
        getEntityManager().persist(historisk);
        getEntityManager().flush();
        var lavesteNyeLøpenummer = historisk.getId() + 150000;

        var første = new ForespørselEntitet(orgnr, LocalDate.now(), new AktørIdEntitet("9999999999999"),
            Ytelsetype.FORELDREPENGER, "124", LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);
        første.setLøpenummer(lavesteNyeLøpenummer + 1);
        forespørselRepository.lagreForespørsel(første);
        var andre = new ForespørselEntitet(orgnr, LocalDate.now(), new AktørIdEntitet("9999999999999"),
            Ytelsetype.FORELDREPENGER, "125", LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);
        andre.setLøpenummer(lavesteNyeLøpenummer);
        forespørselRepository.lagreForespørsel(andre);
        assertThat(første.getId()).isLessThan(andre.getId());

        oppdaterOpprettetTidspunkt(første.getUuid(), LocalDateTime.now().minusHours(1));
        oppdaterOpprettetTidspunkt(andre.getUuid(), LocalDateTime.now().minusHours(2));
        getEntityManager().clear();

        var førEtterfylling = forespørselRepository.hentForespørslerFraFilter(orgnr, null, null, null, null, null, null).stream()
            .map(ForespørselDtoMapper::mapFraEntitet)
            .toList();
        assertThat(førEtterfylling).extracting(ForespørselDto::loepenr)
            .containsExactly(historisk.getId(), lavesteNyeLøpenummer, lavesteNyeLøpenummer + 1);
        assertThat(førEtterfylling).extracting(ForespørselDto::uuid)
            .containsExactly(historisk.getUuid(), andre.getUuid(), første.getUuid());
        assertThat(forespørselRepository.hentForespørslerFraFilter(orgnr, null, null, null, null, null, historisk.getId() - 1))
            .extracting(ForespørselEntitet::getId).containsExactly(historisk.getId(), andre.getId(), første.getId());
        assertThat(forespørselRepository.hentForespørslerFraFilter(orgnr, null, null, null, null, null, lavesteNyeLøpenummer))
            .extracting(ForespørselEntitet::getId).containsExactly(første.getId());

        var oppdatert = getEntityManager().createQuery("""
            update ForespørselEntitet f set f.løpenummer = f.id where f.id = :id and f.løpenummer is null
            """).setParameter("id", historisk.getId()).executeUpdate();
        assertThat(oppdatert).isEqualTo(1);
        getEntityManager().clear();

        var etterEtterfylling = forespørselRepository.hentForespørslerFraFilter(orgnr, null, null, null, null, null, null).stream()
            .map(ForespørselDtoMapper::mapFraEntitet)
            .toList();
        assertThat(etterEtterfylling).containsExactlyElementsOf(førEtterfylling);
        assertThat(forespørselRepository.hentForespørslerFraFilter(orgnr, null, null, null, null, null, historisk.getId()))
            .extracting(ForespørselEntitet::getId).containsExactly(andre.getId(), første.getId());
    }

    @Test
    void skal_paginere_blandede_løpenummer_uten_tap_eller_duplikater() {
        var orgnr = "999999999";
        getEntityManager().createNativeQuery("""
            insert into forespoersel (id, uuid, orgnr, bruker_aktoer_id, ytelse_type, loepe_nummer)
            select 10000 + n, gen_random_uuid(), :orgnr, '9999999999999', 'FORELDREPENGER',
                case when n % 4 = 0 then 10000 + n when n % 2 = 0 then null else 151002 - n end
            from generate_series(1, 1002) as n
            """).setParameter("orgnr", orgnr).executeUpdate();
        getEntityManager().clear();

        var førsteSide = forespørselRepository.hentForespørslerFraFilter(orgnr, null, null, null, null, null, null).stream()
            .map(ForespørselDtoMapper::mapFraEntitet)
            .toList();
        assertThat(førsteSide).hasSize(1000);
        var andreSide = forespørselRepository.hentForespørslerFraFilter(orgnr, null, null, null, null, null, førsteSide.getLast().loepenr()).stream()
            .map(ForespørselDtoMapper::mapFraEntitet)
            .toList();
        assertThat(andreSide).extracting(ForespørselDto::loepenr).containsExactly(150999L, 151001L);
        assertThat(Stream.concat(førsteSide.stream(), andreSide.stream()).map(ForespørselDto::loepenr).toList())
            .hasSize(1002).doesNotHaveDuplicates().isSorted();
        assertThat(forespørselRepository.hentForespørslerFraFilter(orgnr, null, null, null, null, null, andreSide.getLast().loepenr())).isEmpty();
    }

    private void oppdaterOpprettetTidspunkt(UUID uuid, LocalDateTime opprettetTidspunkt) {
        getEntityManager().createNativeQuery("UPDATE FORESPOERSEL SET OPPRETTET_TID = :opprettetTidspunkt WHERE UUID = :uuid")
            .setParameter("opprettetTidspunkt", Timestamp.valueOf(opprettetTidspunkt))
            .setParameter("uuid", uuid)
            .executeUpdate();
    }

}
