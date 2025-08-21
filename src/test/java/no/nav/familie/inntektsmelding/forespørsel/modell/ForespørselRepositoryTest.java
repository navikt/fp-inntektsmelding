package no.nav.familie.inntektsmelding.forespørsel.modell;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;

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
    void skal_lagre_arbeidsgiverinitiert_forespørsel_og_oppdatere_med_dialogporten_uuid() {
        var uuid = forespørselRepository.lagreForespørsel(null,
            Ytelsetype.FORELDREPENGER,
            "9999999999999",
            "999999999",
            null,
            LocalDate.now(),
            ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT);
        // UUID versjon 7, som dialogporten gir oss. Kan ikke generere med java.util da den er på versjon 4
        var dialogportenUuid = UUID.fromString("0198cbab-47ec-7aca-82ad-5dc7d4a823d4");
        forespørselRepository.oppdaterDialogportenUuid(uuid, dialogportenUuid);

        var hentet = forespørselRepository.hentForespørsel(uuid).orElse(null);

        assertThat(hentet).isNotNull();
        assertThat(hentet.getSkjæringstidspunkt()).isEmpty();
        assertThat(hentet.getOrganisasjonsnummer()).isEqualTo("999999999");
        assertThat(hentet.getAktørId().getAktørId()).isEqualTo("9999999999999");
        assertThat(hentet.getYtelseType()).isEqualTo(Ytelsetype.FORELDREPENGER);
        assertThat(hentet.getFagsystemSaksnummer()).isEmpty();
        assertThat(hentet.getFørsteUttaksdato()).isEqualTo(LocalDate.now());
        assertThat(hentet.getDialogportenUuid().orElse(null)).isEqualTo(dialogportenUuid);
    }
}
