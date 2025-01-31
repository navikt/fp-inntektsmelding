package no.nav.familie.inntektsmelding.forespørsel.modell;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import no.nav.familie.inntektsmelding.koder.ForespørselType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.familie.inntektsmelding.database.JpaExtension;
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
    void skal_teste_at_forespørsel_lagres_uten_første_uttak() {
        var uuid = forespørselRepository.lagreForespørsel(LocalDate.now(),
            Ytelsetype.FORELDREPENGER,
            "9999999999999",
            "999999999",
            "123",
            null, ForespørselType.BESTILT_AV_FAGSYSTEM);

        var hentet = forespørselRepository.hentForespørsel(uuid).orElse(null);

        assertThat(hentet).isNotNull();
        assertThat(hentet.getSkjæringstidspunkt()).isEqualTo(LocalDate.now());
        assertThat(hentet.getOrganisasjonsnummer()).isEqualTo("999999999");
        assertThat(hentet.getAktørId().getAktørId()).isEqualTo("9999999999999");
        assertThat(hentet.getYtelseType()).isEqualTo(Ytelsetype.FORELDREPENGER);
        assertThat(hentet.getFagsystemSaksnummer()).isEqualTo("123");
        assertThat(hentet.getFørsteUttaksdato()).isEmpty();
    }

    @Test
    void skal_teste_at_forespørsel_lagres_med_første_uttaksdato() {
        var uuid = forespørselRepository.lagreForespørsel(LocalDate.now(),
            Ytelsetype.FORELDREPENGER,
            "9999999999999",
            "999999999",
            "123",
            LocalDate.now(), ForespørselType.BESTILT_AV_FAGSYSTEM);

        var hentet = forespørselRepository.hentForespørsel(uuid).orElse(null);

        assertThat(hentet).isNotNull();
        assertThat(hentet.getSkjæringstidspunkt()).isEqualTo(LocalDate.now());
        assertThat(hentet.getOrganisasjonsnummer()).isEqualTo("999999999");
        assertThat(hentet.getAktørId().getAktørId()).isEqualTo("9999999999999");
        assertThat(hentet.getYtelseType()).isEqualTo(Ytelsetype.FORELDREPENGER);
        assertThat(hentet.getFagsystemSaksnummer()).isEqualTo("123");
        assertThat(hentet.getFørsteUttaksdato()).isPresent();
        assertThat(hentet.getFørsteUttaksdato()).contains(LocalDate.now());
    }
}
