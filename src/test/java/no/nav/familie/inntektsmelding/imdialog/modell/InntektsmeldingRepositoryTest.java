package no.nav.familie.inntektsmelding.imdialog.modell;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.familie.inntektsmelding.database.JpaExtension;
import no.nav.familie.inntektsmelding.koder.Endringsårsak;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;
import no.nav.vedtak.konfig.Tid;

@ExtendWith(JpaExtension.class)
class InntektsmeldingRepositoryTest extends EntityManagerAwareTest {

    private InntektsmeldingRepository inntektsmeldingRepository;

    @BeforeEach
    void setUp() {
        this.inntektsmeldingRepository = new InntektsmeldingRepository(getEntityManager());
    }

    @Test
    void skal_lagre_inntektsmelding() {
        // Arrange
        var førLagring = InntektsmeldingEntitet.builder()
            .medAktørId(new AktørIdEntitet("9999999999999"))
            .medKontaktperson(new KontaktpersonEntitet("Testy test", "999999999"))
            .medYtelsetype(Ytelsetype.FORELDREPENGER)
            .medMånedInntekt(BigDecimal.valueOf(4000))
            .medStartDato(LocalDate.now())
            .medArbeidsgiverIdent("999999999")
            .build();

        // Act
        inntektsmeldingRepository.lagreInntektsmelding(førLagring);
        var etterLagring = inntektsmeldingRepository.hentSisteInntektsmelding(førLagring.getAktørId(), førLagring.getArbeidsgiverIdent(),
            førLagring.getStartDato(), førLagring.getYtelsetype());

        // Assert
        assertThat(etterLagring).isPresent();
        assertThat(etterLagring.get().getKontaktperson().getTelefonnummer()).isEqualTo(førLagring.getKontaktperson().getTelefonnummer());
        assertThat(etterLagring.get().getKontaktperson().getNavn()).isEqualTo(førLagring.getKontaktperson().getNavn());
        assertThat(etterLagring.get().getMånedInntekt()).isEqualByComparingTo(førLagring.getMånedInntekt());
        assertThat(etterLagring.get().getArbeidsgiverIdent()).isEqualTo(førLagring.getArbeidsgiverIdent());
        assertThat(etterLagring.get().getAktørId()).isEqualTo(førLagring.getAktørId());
        assertThat(etterLagring.get().getStartDato()).isEqualTo(førLagring.getStartDato());
        assertThat(etterLagring.get().getYtelsetype()).isEqualTo(førLagring.getYtelsetype());
    }

    @Test
    void skal_lagre_inntektsmelding_med_refusjon() {
        // Arrange
        var startdato = LocalDate.now();
        var førLagring = InntektsmeldingEntitet.builder()
            .medAktørId(new AktørIdEntitet("9999999999999"))
            .medKontaktperson(new KontaktpersonEntitet("Testy test", "999999999"))
            .medYtelsetype(Ytelsetype.FORELDREPENGER)
            .medMånedInntekt(BigDecimal.valueOf(4000))
            .medMånedRefusjon(BigDecimal.valueOf(4000))
            .medRefusjonOpphørsdato(Tid.TIDENES_ENDE)
            .medStartDato(startdato)
            .medRefusjonsendringer(Collections.singletonList(new RefusjonsendringEntitet(startdato.plusDays(3), BigDecimal.valueOf(2000))))
            .medArbeidsgiverIdent("999999999")
            .build();

        // Act
        inntektsmeldingRepository.lagreInntektsmelding(førLagring);
        var etterLagring = inntektsmeldingRepository.hentSisteInntektsmelding(førLagring.getAktørId(), førLagring.getArbeidsgiverIdent(),
            førLagring.getStartDato(), førLagring.getYtelsetype());

        // Assert
        assertThat(etterLagring).isPresent();
        assertThat(etterLagring.get().getKontaktperson().getTelefonnummer()).isEqualTo(førLagring.getKontaktperson().getTelefonnummer());
        assertThat(etterLagring.get().getKontaktperson().getNavn()).isEqualTo(førLagring.getKontaktperson().getNavn());
        assertThat(etterLagring.get().getMånedInntekt()).isEqualByComparingTo(førLagring.getMånedInntekt());
        assertThat(etterLagring.get().getArbeidsgiverIdent()).isEqualTo(førLagring.getArbeidsgiverIdent());
        assertThat(etterLagring.get().getAktørId()).isEqualTo(førLagring.getAktørId());
        assertThat(etterLagring.get().getStartDato()).isEqualTo(førLagring.getStartDato());
        assertThat(etterLagring.get().getYtelsetype()).isEqualTo(førLagring.getYtelsetype());
        assertThat(etterLagring.get().getOpphørsdatoRefusjon()).isEqualTo(førLagring.getOpphørsdatoRefusjon());
        assertThat(etterLagring.get().getMånedRefusjon()).isEqualByComparingTo(førLagring.getMånedRefusjon());
        assertThat(etterLagring.get().getRefusjonsendringer()).hasSize(1);
        assertThat(etterLagring.get().getRefusjonsendringer().getFirst().getFom()).isEqualTo(førLagring.getRefusjonsendringer().getFirst().getFom());
        assertThat(etterLagring.get().getRefusjonsendringer().getFirst().getRefusjonPrMnd()).isEqualByComparingTo(førLagring.getRefusjonsendringer()
            .getFirst()
            .getRefusjonPrMnd());
    }

    @Test
    void skal_lagre_inntektsmelding_med_endringsårsaker() {
        // Arrange
        var endring = EndringsårsakEntitet.builder()
            .medÅrsak(Endringsårsak.TARIFFENDRING)
            .medFom(LocalDate.now())
            .medBleKjentFra(LocalDate.now().plusDays(10))
            .build();
        var førLagring = InntektsmeldingEntitet.builder()
            .medAktørId(new AktørIdEntitet("9999999999999"))
            .medKontaktperson(new KontaktpersonEntitet("Testy test", "999999999"))
            .medYtelsetype(Ytelsetype.FORELDREPENGER)
            .medMånedInntekt(BigDecimal.valueOf(4000))
            .medMånedRefusjon(BigDecimal.valueOf(4000))
            .medRefusjonOpphørsdato(Tid.TIDENES_ENDE)
            .medStartDato(LocalDate.now())
            .medEndringsårsaker(Collections.singletonList(endring))
            .medArbeidsgiverIdent("999999999")
            .build();

        // Act
        inntektsmeldingRepository.lagreInntektsmelding(førLagring);
        var etterLagring = inntektsmeldingRepository.hentSisteInntektsmelding(førLagring.getAktørId(), førLagring.getArbeidsgiverIdent(),
            førLagring.getStartDato(), førLagring.getYtelsetype());

        // Assert
        assertThat(etterLagring).isPresent();
        assertThat(etterLagring.get().getKontaktperson().getTelefonnummer()).isEqualTo(førLagring.getKontaktperson().getTelefonnummer());
        assertThat(etterLagring.get().getKontaktperson().getNavn()).isEqualTo(førLagring.getKontaktperson().getNavn());
        assertThat(etterLagring.get().getMånedInntekt()).isEqualByComparingTo(førLagring.getMånedInntekt());
        assertThat(etterLagring.get().getArbeidsgiverIdent()).isEqualTo(førLagring.getArbeidsgiverIdent());
        assertThat(etterLagring.get().getAktørId()).isEqualTo(førLagring.getAktørId());
        assertThat(etterLagring.get().getStartDato()).isEqualTo(førLagring.getStartDato());
        assertThat(etterLagring.get().getYtelsetype()).isEqualTo(førLagring.getYtelsetype());
        assertThat(etterLagring.get().getOpphørsdatoRefusjon()).isEqualTo(førLagring.getOpphørsdatoRefusjon());
        assertThat(etterLagring.get().getMånedRefusjon()).isEqualByComparingTo(førLagring.getMånedRefusjon());
        assertThat(etterLagring.get().getEndringsårsaker()).hasSize(1);
        assertThat(etterLagring.get().getEndringsårsaker().getFirst().getÅrsak()).isEqualTo(Endringsårsak.TARIFFENDRING);
        assertThat(etterLagring.get().getEndringsårsaker().getFirst().getFom().orElse(null)).isEqualTo(LocalDate.now());
        assertThat(etterLagring.get().getEndringsårsaker().getFirst().getBleKjentFom().orElse(null)).isEqualTo(LocalDate.now().plusDays(10));
    }


    @Test
    void skal_hente_siste_inntektsmelding() {
        // Arrange
        var aktørId = new AktørIdEntitet("9999999999999");
        var arbeidsgiverIdent = "999999999";
        var startDato = LocalDate.now();

        var im1 = InntektsmeldingEntitet.builder()
            .medAktørId(aktørId)
            .medKontaktperson(new KontaktpersonEntitet("Første", "999999999"))
            .medYtelsetype(Ytelsetype.FORELDREPENGER)
            .medMånedInntekt(BigDecimal.valueOf(4000))
            .medMånedRefusjon(BigDecimal.valueOf(4000))
            .medRefusjonOpphørsdato(Tid.TIDENES_ENDE)
            .medStartDato(startDato)
            .medArbeidsgiverIdent(arbeidsgiverIdent)
            .medOpprettetTidspunkt(LocalDateTime.now().plusDays(1))
            .build();

        var im2 = InntektsmeldingEntitet.builder()
            .medAktørId(aktørId)
            .medKontaktperson(new KontaktpersonEntitet("Andre", "999999999"))
            .medYtelsetype(Ytelsetype.FORELDREPENGER)
            .medMånedInntekt(BigDecimal.valueOf(4000))
            .medMånedRefusjon(BigDecimal.valueOf(4000))
            .medRefusjonOpphørsdato(Tid.TIDENES_ENDE)
            .medStartDato(startDato)
            .medRefusjonsendringer(Collections.singletonList(new RefusjonsendringEntitet(startDato.plusDays(10), BigDecimal.valueOf(2000))))
            .medArbeidsgiverIdent(arbeidsgiverIdent)
            .medOpprettetTidspunkt(LocalDateTime.now().plusDays(2))
            .build();

        // Act
        inntektsmeldingRepository.lagreInntektsmelding(im1);
        inntektsmeldingRepository.lagreInntektsmelding(im2);

        var etterLagring = inntektsmeldingRepository.hentSisteInntektsmelding(aktørId, arbeidsgiverIdent, startDato, Ytelsetype.FORELDREPENGER);

        // Assert
        assertThat(etterLagring).isPresent();
        assertThat(etterLagring.get().getKontaktperson().getNavn()).isEqualTo(im2.getKontaktperson().getNavn());
    }

    @Test
    void skal_hente_alle_im_for_forespørsel() {
        // Arrange
        var aktørId = new AktørIdEntitet("9999999999999");
        var arbeidsgiverIdent = "999999999";
        var startDato = LocalDate.now();

        var im1 = InntektsmeldingEntitet.builder()
            .medAktørId(aktørId)
            .medKontaktperson(new KontaktpersonEntitet("Første", "999999999"))
            .medYtelsetype(Ytelsetype.FORELDREPENGER)
            .medMånedInntekt(BigDecimal.valueOf(4000))
            .medMånedRefusjon(BigDecimal.valueOf(4000))
            .medRefusjonOpphørsdato(Tid.TIDENES_ENDE)
            .medStartDato(startDato)
            .medRefusjonsendringer(Collections.singletonList(new RefusjonsendringEntitet(startDato.plusDays(10), BigDecimal.valueOf(2000))))
            .medArbeidsgiverIdent(arbeidsgiverIdent)
            .medOpprettetTidspunkt(LocalDateTime.now().plusDays(1))
            .build();

        var im2 = InntektsmeldingEntitet.builder()
            .medAktørId(new AktørIdEntitet("1234567891111"))
            .medKontaktperson(new KontaktpersonEntitet("Andre", "999999999"))
            .medYtelsetype(Ytelsetype.FORELDREPENGER)
            .medMånedInntekt(BigDecimal.valueOf(4000))
            .medMånedRefusjon(BigDecimal.valueOf(4000))
            .medRefusjonOpphørsdato(Tid.TIDENES_ENDE)
            .medStartDato(startDato)
            .medRefusjonsendringer(Collections.singletonList(new RefusjonsendringEntitet(startDato.plusDays(10), BigDecimal.valueOf(2000))))
            .medArbeidsgiverIdent(arbeidsgiverIdent)
            .medOpprettetTidspunkt(LocalDateTime.now().plusDays(2))
            .build();

        var im3 = InntektsmeldingEntitet.builder()
            .medAktørId(aktørId)
            .medKontaktperson(new KontaktpersonEntitet("Tredje", "999999999"))
            .medYtelsetype(Ytelsetype.FORELDREPENGER)
            .medMånedInntekt(BigDecimal.valueOf(4000))
            .medMånedRefusjon(BigDecimal.valueOf(4000))
            .medRefusjonOpphørsdato(Tid.TIDENES_ENDE)
            .medStartDato(startDato)
            .medRefusjonsendringer(Collections.singletonList(new RefusjonsendringEntitet(startDato.plusDays(10), BigDecimal.valueOf(2000))))
            .medArbeidsgiverIdent(arbeidsgiverIdent)
            .medOpprettetTidspunkt(LocalDateTime.now().plusDays(3))
            .build();

        var im4 = InntektsmeldingEntitet.builder()
            .medAktørId(aktørId)
            .medKontaktperson(new KontaktpersonEntitet("Fjerde", "999999999"))
            .medYtelsetype(Ytelsetype.SVANGERSKAPSPENGER)
            .medMånedInntekt(BigDecimal.valueOf(4000))
            .medMånedRefusjon(BigDecimal.valueOf(4000))
            .medRefusjonOpphørsdato(Tid.TIDENES_ENDE)
            .medStartDato(startDato)
            .medRefusjonsendringer(Collections.singletonList(new RefusjonsendringEntitet(startDato.plusDays(10), BigDecimal.valueOf(2000))))
            .medArbeidsgiverIdent(arbeidsgiverIdent)
            .medOpprettetTidspunkt(LocalDateTime.now().plusDays(4))
            .build();

        // Act
        inntektsmeldingRepository.lagreInntektsmelding(im1);
        inntektsmeldingRepository.lagreInntektsmelding(im2);
        inntektsmeldingRepository.lagreInntektsmelding(im3);
        inntektsmeldingRepository.lagreInntektsmelding(im4);

        var etterLagring = inntektsmeldingRepository.hentInntektsmeldinger(aktørId, arbeidsgiverIdent, startDato, Ytelsetype.FORELDREPENGER);

        // Assert
        assertThat(etterLagring).hasSize(2);
        assertThat(etterLagring.get(0).getKontaktperson().getNavn()).isEqualTo(im3.getKontaktperson().getNavn());
        assertThat(etterLagring.get(1).getKontaktperson().getNavn()).isEqualTo(im1.getKontaktperson().getNavn());
    }

    @Test
    void skal_hente_inntektsmelding_fra_uuid() {
        // Arrange
        var førLagring = InntektsmeldingEntitet.builder()
            .medAktørId(new AktørIdEntitet("9999999999999"))
            .medKontaktperson(new KontaktpersonEntitet("Testy test", "999999999"))
            .medYtelsetype(Ytelsetype.FORELDREPENGER)
            .medMånedInntekt(BigDecimal.valueOf(4000))
            .medStartDato(LocalDate.now())
            .medArbeidsgiverIdent("999999999")
            .build();

        var uuid = førLagring.getUuid().orElseThrow();
        // Act
        inntektsmeldingRepository.lagreInntektsmelding(førLagring);
        var etterLagring = inntektsmeldingRepository.hentInntektsmelding(uuid);

        // Assert
        assertThat(etterLagring).isPresent();
    }

}
