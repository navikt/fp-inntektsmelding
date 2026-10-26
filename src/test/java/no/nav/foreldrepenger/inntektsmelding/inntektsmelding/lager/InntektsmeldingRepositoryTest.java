package no.nav.foreldrepenger.inntektsmelding.inntektsmelding.lager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.inntektsmelding.database.JpaExtension;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.lager.ForespørselEntitet;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.lager.ForespørselRepository;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDtoMapper;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.EndringsårsakType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Kildesystem;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.foreldrepenger.inntektsmelding.typer.lager.AktørIdEntitet;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;
import no.nav.vedtak.konfig.Tid;

@ExtendWith(JpaExtension.class)
class InntektsmeldingRepositoryTest extends EntityManagerAwareTest {

    private InntektsmeldingRepository inntektsmeldingRepository;
    private ForespørselRepository forespørselRepository;

    @BeforeEach
    void setUp() {
        this.forespørselRepository = new ForespørselRepository(getEntityManager());
        this.inntektsmeldingRepository = new InntektsmeldingRepository(getEntityManager());
    }

    @Test
    void skal_tildele_stigende_løpenummer_bare_ved_første_lagring() {
        var første = lagInntektsmeldingEntitet("9999999999999", "999999999", Ytelsetype.FORELDREPENGER, LocalDateTime.now());
        var id = inntektsmeldingRepository.lagreInntektsmelding(første);
        var løpenummer = første.getLøpenummer();
        assertThat(løpenummer).isNotNull();
        assertThat(id).isEqualTo(første.getId());

        inntektsmeldingRepository.lagreInntektsmelding(første);
        assertThat(første.getLøpenummer()).isEqualTo(løpenummer);

        var andre = lagInntektsmeldingEntitet("9999999999999", "999999999", Ytelsetype.FORELDREPENGER, LocalDateTime.now());
        inntektsmeldingRepository.lagreInntektsmelding(andre);
        getEntityManager().clear();

        assertThat(inntektsmeldingRepository.finnInntektsmelding(første.getUuid()).orElseThrow().getLøpenummer()).isEqualTo(løpenummer);
        assertThat(inntektsmeldingRepository.finnInntektsmelding(andre.getUuid()).orElseThrow().getLøpenummer()).isEqualTo(løpenummer + 1);
    }

    @Test
    void skal_beholde_null_løpenummer_ved_lagring_av_historisk_inntektsmelding() {
        var historisk = lagInntektsmeldingEntitet("9999999999999", "999999999", Ytelsetype.FORELDREPENGER, LocalDateTime.now());
        getEntityManager().persist(historisk);
        getEntityManager().flush();
        getEntityManager().clear();

        var hentet = inntektsmeldingRepository.finnInntektsmelding(historisk.getUuid()).orElseThrow();
        assertThat(hentet.getLøpenummer()).isNull();
        inntektsmeldingRepository.lagreInntektsmelding(hentet);
        assertThat(hentet.getLøpenummer()).isNull();
        getEntityManager().clear();

        assertThat(inntektsmeldingRepository.finnInntektsmelding(historisk.getUuid()).orElseThrow().getLøpenummer()).isNull();
    }

    @Test
    void skal_avvise_endring_av_tildelt_løpenummer() {
        var inntektsmelding = lagInntektsmeldingEntitet("9999999999999", "999999999", Ytelsetype.FORELDREPENGER, LocalDateTime.now());
        inntektsmelding.setLøpenummer(150000L);
        inntektsmelding.setLøpenummer(150000L);

        assertThatIllegalStateException().isThrownBy(() -> inntektsmelding.setLøpenummer(150001L));
        assertThat(inntektsmelding.getLøpenummer()).isEqualTo(150000L);
    }

    @Test
    void skal_lagre_inntektsmelding() {
        // Arrange
        var forespørselEntitet = lagreForespørsel("9999999999999", "999999999", Ytelsetype.FORELDREPENGER);
        var førLagring = InntektsmeldingEntitet.builder()
            .medAktørId(new AktørIdEntitet("9999999999999"))
            .medKontaktperson(new KontaktpersonEntitet("Testy test", "999999999"))
            .medYtelsetype(Ytelsetype.FORELDREPENGER)
            .medForespørsel(forespørselEntitet)
            .medKildesystem(Kildesystem.ARBEIDSGIVERPORTAL)
            .medMånedInntekt(BigDecimal.valueOf(4000))
            .medStartDato(LocalDate.now())
            .medArbeidsgiverIdent("999999999")
            .build();

        // Act
        inntektsmeldingRepository.lagreInntektsmelding(førLagring);
        var etterLagring = inntektsmeldingRepository.finnInntektsmelding(førLagring.getUuid());

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
        var forespørselEntitet = lagreForespørsel("9999999999999", "999999999", Ytelsetype.FORELDREPENGER);
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
            .medForespørsel(forespørselEntitet)
            .medKildesystem(Kildesystem.ARBEIDSGIVERPORTAL)
            .build();

        // Act
        inntektsmeldingRepository.lagreInntektsmelding(førLagring);
        var etterLagring = inntektsmeldingRepository.finnInntektsmelding(førLagring.getUuid());

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
        var forespørselEntitet = lagreForespørsel("9999999999999", "999999999", Ytelsetype.FORELDREPENGER);
        var endring = EndringsårsakEntitet.builder()
            .medÅrsak(EndringsårsakType.TARIFFENDRING)
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
            .medForespørsel(forespørselEntitet)
            .medKildesystem(Kildesystem.ARBEIDSGIVERPORTAL)
            .build();

        // Act
        inntektsmeldingRepository.lagreInntektsmelding(førLagring);
        var etterLagring = inntektsmeldingRepository.finnInntektsmelding(førLagring.getUuid());

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
        assertThat(etterLagring.get().getEndringsårsaker().getFirst().getÅrsak()).isEqualTo(EndringsårsakType.TARIFFENDRING);
        assertThat(etterLagring.get().getEndringsårsaker().getFirst().getFom().orElse(null)).isEqualTo(LocalDate.now());
        assertThat(etterLagring.get().getEndringsårsaker().getFirst().getBleKjentFom().orElse(null)).isEqualTo(LocalDate.now().plusDays(10));
    }


    @Test
    void skal_hente_siste_inntektsmelding() {
        // Arrange
        var aktørId = new AktørIdEntitet("9999999999999");
        var arbeidsgiverIdent = "999999999";
        var startDato = LocalDate.now();
        var forespørselEntitet = lagreForespørsel("9999999999999", arbeidsgiverIdent, Ytelsetype.FORELDREPENGER);

        var im1 = InntektsmeldingEntitet.builder()
            .medAktørId(aktørId)
            .medKontaktperson(new KontaktpersonEntitet("Første", "999999999"))
            .medYtelsetype(Ytelsetype.FORELDREPENGER)
            .medMånedInntekt(BigDecimal.valueOf(4000))
            .medMånedRefusjon(BigDecimal.valueOf(4000))
            .medRefusjonOpphørsdato(Tid.TIDENES_ENDE)
            .medStartDato(startDato)
            .medKildesystem(Kildesystem.ARBEIDSGIVERPORTAL)
            .medArbeidsgiverIdent(arbeidsgiverIdent)
            .medOpprettetTidspunkt(LocalDateTime.now().plusDays(1))
            .medForespørsel(forespørselEntitet)
            .build();

        var im2 = InntektsmeldingEntitet.builder()
            .medAktørId(aktørId)
            .medKontaktperson(new KontaktpersonEntitet("Andre", "999999999"))
            .medYtelsetype(Ytelsetype.FORELDREPENGER)
            .medMånedInntekt(BigDecimal.valueOf(4000))
            .medMånedRefusjon(BigDecimal.valueOf(4000))
            .medRefusjonOpphørsdato(Tid.TIDENES_ENDE)
            .medStartDato(startDato)
            .medKildesystem(Kildesystem.ARBEIDSGIVERPORTAL)
            .medForespørsel(forespørselEntitet)
            .medRefusjonsendringer(Collections.singletonList(new RefusjonsendringEntitet(startDato.plusDays(10), BigDecimal.valueOf(2000))))
            .medArbeidsgiverIdent(arbeidsgiverIdent)
            .medOpprettetTidspunkt(LocalDateTime.now().plusDays(2))
            .build();

        // Act
        inntektsmeldingRepository.lagreInntektsmelding(im1);
        inntektsmeldingRepository.lagreInntektsmelding(im2);

        var etterLagring = inntektsmeldingRepository.hentInntektsmeldingerSortertNyesteFørst(aktørId, arbeidsgiverIdent, startDato, Ytelsetype.FORELDREPENGER).getFirst();

        // Assert
        assertThat(etterLagring).isNotNull();
        assertThat(etterLagring.getKontaktperson().getNavn()).isEqualTo(im2.getKontaktperson().getNavn());
    }

    @Test
    void skal_hente_alle_im_for_forespørsel() {
        // Arrange
        var aktørId = new AktørIdEntitet("9999999999999");
        var arbeidsgiverIdent = "999999999";
        var startDato = LocalDate.now();
        var forespørselEntitet = lagreForespørsel("9999999999999", arbeidsgiverIdent, Ytelsetype.FORELDREPENGER);

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
            .medKildesystem(Kildesystem.ARBEIDSGIVERPORTAL)
            .medForespørsel(forespørselEntitet)
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
            .medKildesystem(Kildesystem.ARBEIDSGIVERPORTAL)
            .medForespørsel(forespørselEntitet)
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
            .medKildesystem(Kildesystem.ARBEIDSGIVERPORTAL)
            .medForespørsel(forespørselEntitet)
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
            .medKildesystem(Kildesystem.ARBEIDSGIVERPORTAL)
            .medForespørsel(forespørselEntitet)
            .medOpprettetTidspunkt(LocalDateTime.now().plusDays(4))
            .build();

        // Act
        inntektsmeldingRepository.lagreInntektsmelding(im1);
        inntektsmeldingRepository.lagreInntektsmelding(im2);
        inntektsmeldingRepository.lagreInntektsmelding(im3);
        inntektsmeldingRepository.lagreInntektsmelding(im4);

        var etterLagring = inntektsmeldingRepository.hentInntektsmeldingerSortertNyesteFørst(aktørId, arbeidsgiverIdent, startDato, Ytelsetype.FORELDREPENGER);

        // Assert
        assertThat(etterLagring).hasSize(2);
        assertThat(etterLagring.get(0).getKontaktperson().getNavn()).isEqualTo(im3.getKontaktperson().getNavn());
        assertThat(etterLagring.get(1).getKontaktperson().getNavn()).isEqualTo(im1.getKontaktperson().getNavn());
    }

    @Test
    void skal_hente_inntektsmelding_fra_uuid() {
        // Arrange
        var forespørselEntitet = lagreForespørsel("9999999999999", "999999999", Ytelsetype.FORELDREPENGER);
        var førLagring = InntektsmeldingEntitet.builder()
            .medAktørId(new AktørIdEntitet("9999999999999"))
            .medKontaktperson(new KontaktpersonEntitet("Testy test", "999999999"))
            .medYtelsetype(Ytelsetype.FORELDREPENGER)
            .medMånedInntekt(BigDecimal.valueOf(4000))
            .medStartDato(LocalDate.now())
            .medArbeidsgiverIdent("999999999")
            .medForespørsel(forespørselEntitet)
            .medKildesystem(Kildesystem.ARBEIDSGIVERPORTAL)
            .build();

        var uuid = førLagring.getUuid();
        // Act
        inntektsmeldingRepository.lagreInntektsmelding(førLagring);
        var etterLagring = inntektsmeldingRepository.finnInntektsmelding(uuid);

        // Assert
        assertThat(etterLagring).isPresent();
    }

    @Test
    void skal_hente_inntektsmelding_for_forespørsel() {
        // Arrange
        var forespørsel = lagreForespørsel("9999999999999", "999999999", Ytelsetype.FORELDREPENGER);
        var annenForespørsel = lagreForespørsel("8888888888888", "888888888", Ytelsetype.FORELDREPENGER);
        var forventetInntektsmelding = InntektsmeldingEntitet.builder()
            .medAktørId(new AktørIdEntitet("9999999999999"))
            .medKontaktperson(new KontaktpersonEntitet("Testy test", "999999999"))
            .medYtelsetype(Ytelsetype.FORELDREPENGER)
            .medMånedInntekt(BigDecimal.valueOf(4000))
            .medStartDato(LocalDate.now())
            .medArbeidsgiverIdent("999999999")
            .medForespørsel(forespørsel)
            .medKildesystem(Kildesystem.ARBEIDSGIVERPORTAL)
            .build();
        var annenInntektsmelding = InntektsmeldingEntitet.builder()
            .medAktørId(new AktørIdEntitet("8888888888888"))
            .medKontaktperson(new KontaktpersonEntitet("Annen test", "888888888"))
            .medYtelsetype(Ytelsetype.FORELDREPENGER)
            .medMånedInntekt(BigDecimal.valueOf(5000))
            .medStartDato(LocalDate.now())
            .medArbeidsgiverIdent("888888888")
            .medForespørsel(annenForespørsel)
            .medKildesystem(Kildesystem.ARBEIDSGIVERPORTAL)
            .build();
        var forventetUuid = forventetInntektsmelding.getUuid();
        inntektsmeldingRepository.lagreInntektsmelding(forventetInntektsmelding);
        inntektsmeldingRepository.lagreInntektsmelding(annenInntektsmelding);
        getEntityManager().clear();

        // Act
        var resultat = inntektsmeldingRepository.hentInntektsmeldingerForForespørsel(forespørsel);

        // Assert
        assertThat(resultat).hasSize(1);
        assertThat(resultat.getFirst().getUuid()).isEqualTo(forventetUuid);
        assertThat(resultat.getFirst().getForespørsel().orElseThrow().getId()).isEqualTo(forespørsel.getId());
    }

    @Test
    void skal_filtrere_kun_på_orgnr() {
        var orgnr = "999999999";
        var annetOrgnr = "888888888";
        lagreInntektsmelding("9999999999999", orgnr, Ytelsetype.FORELDREPENGER);
        lagreInntektsmelding("9999999999999", orgnr, Ytelsetype.SVANGERSKAPSPENGER);
        lagreInntektsmelding("9999999999999", annetOrgnr, Ytelsetype.FORELDREPENGER);

        var resultat = inntektsmeldingRepository.hentInntektsmeldingerFraFilter(orgnr, null, null, null, null, null, null);

        assertThat(resultat).hasSize(2);
    }

    @Test
    void skal_filtrere_på_orgnr_og_ytelsetype() {
        var orgnr = "999999999";
        lagreInntektsmelding("9999999999999", orgnr, Ytelsetype.FORELDREPENGER);
        lagreInntektsmelding("9999999999999", orgnr, Ytelsetype.SVANGERSKAPSPENGER);

        var resultat = inntektsmeldingRepository.hentInntektsmeldingerFraFilter(orgnr, null, Ytelsetype.SVANGERSKAPSPENGER, null, null, null, null);

        assertThat(resultat).hasSize(1);
        assertThat(resultat.getFirst().getYtelsetype()).isEqualTo(Ytelsetype.SVANGERSKAPSPENGER);
    }

    @Test
    void skal_filtrere_på_orgnr_og_aktørId() {
        var orgnr = "999999999";
        lagreInntektsmelding("9999999999999", orgnr, Ytelsetype.FORELDREPENGER);
        lagreInntektsmelding("8888888888888", orgnr, Ytelsetype.FORELDREPENGER);
        lagreInntektsmelding("8888888888888", orgnr, Ytelsetype.SVANGERSKAPSPENGER);

        var resultat = inntektsmeldingRepository.hentInntektsmeldingerFraFilter(orgnr, new AktørIdEntitet("8888888888888"), null, null, null, null,
            null);

        assertThat(resultat).hasSize(2);
    }

    @Test
    void skal_filtrere_på_datointerval() {
        var orgnr = "999999999";
        lagreInntektsmelding("9999999999999", orgnr, Ytelsetype.FORELDREPENGER);
        lagreInntektsmelding("9999999999999", orgnr, Ytelsetype.SVANGERSKAPSPENGER);

        var resultat = inntektsmeldingRepository.hentInntektsmeldingerFraFilter(orgnr, null, null,
            Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE, null, null);

        assertThat(resultat).hasSize(2);
    }

    @Test
    void skal_returnere_tomt_når_datointerval_ikke_treffer() {
        var orgnr = "999999999";
        lagreInntektsmelding("9999999999999", orgnr, Ytelsetype.FORELDREPENGER);

        var resultat = inntektsmeldingRepository.hentInntektsmeldingerFraFilter(orgnr, null, null,
            LocalDate.now().plusDays(7), Tid.TIDENES_ENDE, null, null);

        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_filtrere_med_kun_fom_dato() {
        var orgnr = "999999999";
        lagreInntektsmelding("9999999999999", orgnr, Ytelsetype.FORELDREPENGER);

        var resultat = inntektsmeldingRepository.hentInntektsmeldingerFraFilter(orgnr, null, null,
            Tid.TIDENES_BEGYNNELSE, null, null, null);

        assertThat(resultat).hasSize(1);
    }
    @Test
    void skal_filtrere_på_loepenr_større_enn_fraLoepenr() {
        var orgnr = "999999999";
        lagreInntektsmelding("9999999999999", orgnr, Ytelsetype.FORELDREPENGER);
        lagreInntektsmelding("8888888888888", orgnr, Ytelsetype.FORELDREPENGER);
        lagreInntektsmelding("8888888888888", orgnr, Ytelsetype.SVANGERSKAPSPENGER);

        var lavesteLøpenummer = inntektsmeldingRepository.hentInntektsmeldingerFraFilter(orgnr, null, null, null, null, null, null).stream()
            .map(InntektsmeldingEntitet::getLøpenummer)
            .min(Long::compareTo)
            .orElseThrow();


        var resultat = inntektsmeldingRepository.hentInntektsmeldingerFraFilter(orgnr, new AktørIdEntitet("8888888888888"), null, null, null, lavesteLøpenummer,
            null);

        assertThat(resultat).hasSize(2);
        assertThat(resultat).allSatisfy(im -> assertThat(im.getLøpenummer()).isGreaterThan(lavesteLøpenummer));
    }

    @Test
    void skal_bruke_samme_løpenummer_i_svar_filter_og_sortering_før_og_etter_etterfylling() {
        var orgnr = "999999999";
        var opprettetTidspunkt = LocalDateTime.now();
        var historisk = lagInntektsmeldingEntitet("9999999999999", orgnr, Ytelsetype.FORELDREPENGER, opprettetTidspunkt);
        getEntityManager().persist(historisk);
        getEntityManager().flush();
        var lavesteNyeLøpenummer = historisk.getId() + 150000;

        var første = lagInntektsmeldingEntitet("9999999999999", orgnr, Ytelsetype.FORELDREPENGER, opprettetTidspunkt.minusDays(1));
        første.setLøpenummer(lavesteNyeLøpenummer + 1);
        inntektsmeldingRepository.lagreInntektsmelding(første);
        var andre = lagInntektsmeldingEntitet("9999999999999", orgnr, Ytelsetype.FORELDREPENGER, opprettetTidspunkt.minusDays(2));
        andre.setLøpenummer(lavesteNyeLøpenummer);
        inntektsmeldingRepository.lagreInntektsmelding(andre);
        assertThat(første.getId()).isLessThan(andre.getId());
        getEntityManager().clear();

        var førEtterfylling = inntektsmeldingRepository.hentInntektsmeldingerFraFilter(orgnr, null, null, null, null, null, null).stream()
            .map(InntektsmeldingDtoMapper::mapFraEntitet)
            .toList();
        assertThat(førEtterfylling).extracting(InntektsmeldingDto::getId, InntektsmeldingDto::getLoepeNummer)
            .containsExactly(tuple(historisk.getId(), historisk.getId()), tuple(andre.getId(), lavesteNyeLøpenummer),
                tuple(første.getId(), lavesteNyeLøpenummer + 1));
        assertThat(inntektsmeldingRepository.hentInntektsmeldingerFraFilter(orgnr, null, null, null, null, historisk.getId() - 1, null))
            .extracting(InntektsmeldingEntitet::getId).containsExactly(historisk.getId(), andre.getId(), første.getId());
        assertThat(inntektsmeldingRepository.hentInntektsmeldingerFraFilter(orgnr, null, null, null, null, lavesteNyeLøpenummer, null))
            .extracting(InntektsmeldingEntitet::getId).containsExactly(første.getId());

        var oppdatert = getEntityManager().createQuery("""
            update InntektsmeldingEntitet im set im.løpenummer = im.id where im.id = :id and im.løpenummer is null
            """).setParameter("id", historisk.getId()).executeUpdate();
        assertThat(oppdatert).isEqualTo(1);
        getEntityManager().clear();

        var etterEtterfylling = inntektsmeldingRepository.hentInntektsmeldingerFraFilter(orgnr, null, null, null, null, null, null).stream()
            .map(InntektsmeldingDtoMapper::mapFraEntitet)
            .toList();
        assertThat(etterEtterfylling).extracting(InntektsmeldingDto::getId, InntektsmeldingDto::getLoepeNummer)
            .containsExactlyElementsOf(førEtterfylling.stream().map(im -> tuple(im.getId(), im.getLoepeNummer())).toList());
        assertThat(inntektsmeldingRepository.hentInntektsmeldingerFraFilter(orgnr, null, null, null, null, historisk.getId(), null))
            .extracting(InntektsmeldingEntitet::getId).containsExactly(andre.getId(), første.getId());
    }

    @Test
    void skal_paginere_blandede_løpenummer_uten_tap_eller_duplikater() {
        var orgnr = "999999999";
        getEntityManager().createNativeQuery("""
            insert into inntektsmelding
                (id, uuid, arbeidsgiver_ident, bruker_aktoer_id, ytelse_type, start_dato_permisjon, maaned_inntekt, kildesystem, status, loepe_nummer)
            select 10000 + n, gen_random_uuid(), :orgnr, '9999999999999', 'FORELDREPENGER', current_date, 4000, 'ARBEIDSGIVERPORTAL', 'GODKJENT',
                case when n % 4 = 0 then 10000 + n when n % 2 = 0 then null else 151002 - n end
            from generate_series(1, 1002) as n
            """).setParameter("orgnr", orgnr).executeUpdate();
        getEntityManager().clear();

        var førsteSide = inntektsmeldingRepository.hentInntektsmeldingerFraFilter(orgnr, null, null, null, null, null, null);
        assertThat(førsteSide).hasSize(1000);
        var andreSide = inntektsmeldingRepository.hentInntektsmeldingerFraFilter(orgnr, null, null, null, null, førsteSide.getLast().getLøpenummer(), null);
        assertThat(andreSide).extracting(InntektsmeldingEntitet::getLøpenummer).containsExactly(150999L, 151001L);
        assertThat(Stream.concat(førsteSide.stream(), andreSide.stream())
            .map(im -> im.getLøpenummer() != null ? im.getLøpenummer() : im.getId()).toList())
            .hasSize(1002).doesNotHaveDuplicates().isSorted();
        assertThat(inntektsmeldingRepository.hentInntektsmeldingerFraFilter(orgnr, null, null, null, null, andreSide.getLast().getLøpenummer(), null))
            .isEmpty();
    }

    @Test
    void skal_filtrere_bort_inntektsmeldinger_overstyrt_av_saksbehandler_med_kildesystem_fpsak() {
        var orgnr = "999999999";
        lagreInntektsmelding("9999999999999", orgnr, Ytelsetype.FORELDREPENGER);
        lagreOverstyrtInntektsmeldingUtenForespørsel("9999999999999", orgnr, Ytelsetype.FORELDREPENGER);

        var resultat = inntektsmeldingRepository.hentInntektsmeldingerFraFilter(orgnr, null, null, null, null, null, null);

        assertThat(resultat).hasSize(1);
        assertThat(resultat.getFirst().getKildesystem()).isNotEqualTo(Kildesystem.FPSAK);
    }

    private void lagreInntektsmelding(String aktørId, String orgnr, Ytelsetype ytelsetype) {
        var entitet = InntektsmeldingEntitet.builder()
            .medAktørId(new AktørIdEntitet(aktørId))
            .medKontaktperson(new KontaktpersonEntitet("Test", "999999999"))
            .medYtelsetype(ytelsetype)
            .medMånedInntekt(BigDecimal.valueOf(4000))
            .medStartDato(LocalDate.now())
            .medArbeidsgiverIdent(orgnr)
            .medKildesystem(Kildesystem.ARBEIDSGIVERPORTAL)
            .medForespørsel(lagreForespørsel(aktørId, orgnr, ytelsetype))
            .build();
        inntektsmeldingRepository.lagreInntektsmelding(entitet);
    }

    private InntektsmeldingEntitet lagInntektsmeldingEntitet(String aktørId, String orgnr, Ytelsetype ytelsetype, LocalDateTime opprettetTidspunkt) {
        return InntektsmeldingEntitet.builder()
            .medAktørId(new AktørIdEntitet(aktørId))
            .medKontaktperson(new KontaktpersonEntitet("Test", "999999999"))
            .medYtelsetype(ytelsetype)
            .medMånedInntekt(BigDecimal.valueOf(4000))
            .medStartDato(LocalDate.now())
            .medArbeidsgiverIdent(orgnr)
            .medForespørsel(lagreForespørsel(aktørId, orgnr, ytelsetype))
            .medKildesystem(Kildesystem.ARBEIDSGIVERPORTAL)
            .medOpprettetTidspunkt(opprettetTidspunkt)
            .build();
    }

    private ForespørselEntitet lagreForespørsel(String aktørId, String orgnr, Ytelsetype ytelsetype) {
        var forespørselEntitet = new ForespørselEntitet(orgnr,
            LocalDate.now(),
            new AktørIdEntitet(aktørId),
            ytelsetype,
            "123",
            LocalDate.now(),
            ForespørselType.BESTILT_AV_FAGSYSTEM);
        forespørselRepository.lagreForespørsel(forespørselEntitet);
        return forespørselEntitet;
    }

    private void lagreOverstyrtInntektsmeldingUtenForespørsel(String aktørId, String orgnr, Ytelsetype ytelsetype) {
        var entitet = InntektsmeldingEntitet.builder()
            .medAktørId(new AktørIdEntitet(aktørId))
            .medYtelsetype(ytelsetype)
            .medMånedInntekt(BigDecimal.valueOf(4000))
            .medStartDato(LocalDate.now())
            .medArbeidsgiverIdent(orgnr)
            .medKildesystem(Kildesystem.FPSAK)
            .medOpprettetAv("Z999999")
            .build();
        inntektsmeldingRepository.lagreInntektsmelding(entitet);
    }

}
