package no.nav.familie.inntektsmelding.forespørsel.modell;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;

import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;

import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

import no.nav.vedtak.konfig.Tid;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.familie.inntektsmelding.database.JpaExtension;
import no.nav.familie.inntektsmelding.koder.ForespørselType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith(JpaExtension.class)
class ForespørselRepositoryTest extends EntityManagerAwareTest {

    private ForespørselRepository forespørselRepository;

    @BeforeEach
    void setUp() {
        this.forespørselRepository = new ForespørselRepository(getEntityManager());
    }

    @Test
    void skal_lagre_arbeidsgiverinitiert_forespørsel() {
        var uuid = forespørselRepository.lagreForespørsel(null,
            Ytelsetype.FORELDREPENGER,
            "9999999999999",
            "999999999",
            null,
            LocalDate.now(),
            ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT);

        var hentet = forespørselRepository.hentForespørsel(uuid).orElse(null);

        assertThat(hentet).isNotNull();
        assertThat(hentet.getSkjæringstidspunkt()).isEmpty();
        assertThat(hentet.getOrganisasjonsnummer()).isEqualTo("999999999");
        assertThat(hentet.getAktørId().getAktørId()).isEqualTo("9999999999999");
        assertThat(hentet.getYtelseType()).isEqualTo(Ytelsetype.FORELDREPENGER);
        assertThat(hentet.getFagsystemSaksnummer()).isEmpty();
        assertThat(hentet.getFørsteUttaksdato()).isEqualTo(LocalDate.now());
    }

    @Test
    void skal_lagre_forespørsel_bestilt_fra_fagsystem() {
        var uuid = forespørselRepository.lagreForespørsel(LocalDate.now(),
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
        var uuid = forespørselRepository.lagreForespørsel(LocalDate.now(),
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

    @Test
    void skal_søke_på_forespørsler_når_kun_orgnr_oppgitt() {
        var orgnr = "999999999";
        forespørselRepository.lagreForespørsel(LocalDate.now(),
            Ytelsetype.FORELDREPENGER,
            "9999999999999",
            orgnr,
            "123",
            LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);
        forespørselRepository.lagreForespørsel(LocalDate.now(),
            Ytelsetype.SVANGERSKAPSPENGER,
            "9999999999999",
            orgnr,
            "123",
            LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);

        var forespørsler = forespørselRepository.hentForespørslerFraFilter(new OrganisasjonsnummerDto(orgnr), null, null, null, null, null);

        assertThat(forespørsler).hasSize(2);
    }

    @Test
    void skal_søke_på_forespørsler_når_kun_orgnr_og_ytelse_oppgitt() {
        var orgnr = "999999999";
        forespørselRepository.lagreForespørsel(LocalDate.now(),
            Ytelsetype.FORELDREPENGER,
            "9999999999999",
            orgnr,
            "123",
            LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);

        forespørselRepository.lagreForespørsel(LocalDate.now(),
            Ytelsetype.SVANGERSKAPSPENGER,
            "9999999999999",
            orgnr,
            "123",
            LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);

        var forespørsler = forespørselRepository.hentForespørslerFraFilter(new OrganisasjonsnummerDto(orgnr), null, null, Ytelsetype.SVANGERSKAPSPENGER, null, null);

        assertThat(forespørsler).hasSize(1);
    }

    @Test
    void skal_søke_på_forespørsler_når_kun_orgnr_og_aktørId_er_oppgitt() {
        var orgnr = "999999999";
        forespørselRepository.lagreForespørsel(LocalDate.now(),
            Ytelsetype.FORELDREPENGER,
            "9999999999999",
            orgnr,
            "123",
            LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);

        forespørselRepository.lagreForespørsel(LocalDate.now(),
            Ytelsetype.SVANGERSKAPSPENGER,
            "8888888888888",
            orgnr,
            "123",
            LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);

        forespørselRepository.lagreForespørsel(LocalDate.now(),
            Ytelsetype.FORELDREPENGER,
            "8888888888888",
            orgnr,
            "123",
            LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);

        var forespørsler = forespørselRepository.hentForespørslerFraFilter(new OrganisasjonsnummerDto(orgnr), new AktørIdEntitet("8888888888888"), null, null, null, null);

        assertThat(forespørsler).hasSize(2);
    }

    @Test
    void skal_søke_på_intervall() {
        var orgnr = "999999999";
        forespørselRepository.lagreForespørsel(LocalDate.now(),
            Ytelsetype.FORELDREPENGER,
            "9999999999999",
            orgnr,
            "123",
            LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);

        forespørselRepository.lagreForespørsel(LocalDate.now(),
            Ytelsetype.SVANGERSKAPSPENGER,
            "8888888888888",
            orgnr,
            "123",
            LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);

        var forespørsler = forespørselRepository.hentForespørslerFraFilter(new OrganisasjonsnummerDto(orgnr), null, null, null,
            Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE);

        assertThat(forespørsler).hasSize(2);
    }

    @Test
    void skal_søke_på_intervall_med_0_resultat() {
        var orgnr = "999999999";
        forespørselRepository.lagreForespørsel(LocalDate.now(),
            Ytelsetype.FORELDREPENGER,
            "9999999999999",
            orgnr,
            "123",
            LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);

        forespørselRepository.lagreForespørsel(LocalDate.now(),
            Ytelsetype.SVANGERSKAPSPENGER,
            "8888888888888",
            orgnr,
            "123",
            LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);

        var forespørsler = forespørselRepository.hentForespørslerFraFilter(new OrganisasjonsnummerDto(orgnr), null, null, null,
            LocalDate.now().plusDays(7), Tid.TIDENES_ENDE);

        assertThat(forespørsler).isEmpty();
    }

    @Test
    void skal_søke_på_åpent_intervall() {
        var orgnr = "999999999";
        forespørselRepository.lagreForespørsel(LocalDate.now(),
            Ytelsetype.FORELDREPENGER,
            "9999999999999",
            orgnr,
            "123",
            LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);

        forespørselRepository.lagreForespørsel(LocalDate.now(),
            Ytelsetype.SVANGERSKAPSPENGER,
            "8888888888888",
            orgnr,
            "111",
            LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);

        forespørselRepository.lagreForespørsel(LocalDate.now(),
            Ytelsetype.FORELDREPENGER,
            "7777777777777",
            orgnr,
            "321",
            LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);

        var forespørsler = forespørselRepository.hentForespørslerFraFilter(new OrganisasjonsnummerDto(orgnr), null, null, null,
            null, LocalDate.now().plusDays(7));

        assertThat(forespørsler).hasSize(3);
    }

}
