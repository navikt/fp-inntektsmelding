package no.nav.foreldrepenger.inntektsmelding.imdialog.tjenester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.FellesMottakTjeneste;

import no.nav.foreldrepenger.inntektsmelding.typer.domene.Saksnummer;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.EndringsårsakType;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektKontrollResultat;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektKontrollTjeneste;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.fpsak.FpsakFagsak;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.fpsak.FpsakTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.inntektskomponent.Inntektsopplysninger;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ArbeidsgiverinitiertÅrsak;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.vedtak.konfig.Tid;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.kontekst.RequestKontekst;
import no.nav.vedtak.sikkerhet.oidc.config.OpenIDProvider;
import no.nav.vedtak.sikkerhet.oidc.token.OpenIDToken;
import no.nav.vedtak.sikkerhet.oidc.token.TokenString;

@ExtendWith(MockitoExtension.class)
class InntektsmeldingMottakTjenesteTest {
    private static final String INNMELDER_UID = "12324312345";

    @Mock
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    @Mock
    private FellesMottakTjeneste fellesMottakTjeneste;
    @Mock
    private FpsakTjeneste fpsakTjeneste;
    @Mock
    private InntektKontrollTjeneste inntektKontrollTjeneste;

    private InntektsmeldingMottakTjeneste inntektsmeldingMottakTjeneste;

    @BeforeAll
    static void beforeAll() {
        KontekstHolder.setKontekst(RequestKontekst.forRequest(INNMELDER_UID, "kompakt", IdentType.EksternBruker,
            new OpenIDToken(OpenIDProvider.TOKENX, new TokenString("token")), UUID.randomUUID(), Set.of()));
    }

    @AfterAll
    static void afterAll() {
        KontekstHolder.fjernKontekst();
    }

    @BeforeEach
    void setUp() {
        inntektsmeldingMottakTjeneste = new InntektsmeldingMottakTjeneste(forespørselBehandlingTjeneste, fellesMottakTjeneste, fpsakTjeneste,
            inntektKontrollTjeneste);
    }

    @Test
    void skal_ikke_godta_im_på_utgått_forespørrsel() {
        // Arrange
        var uuid = UUID.randomUUID();
        var forespørselDto = lagForespørselDto(ForespørselType.BESTILT_AV_FAGSYSTEM, ForespørselStatus.UTGÅTT, LocalDate.now());
        when(forespørselBehandlingTjeneste.hentForespørsel(uuid)).thenReturn(forespørselDto);

        var inntektsmeldingDto = lagInntektsmeldingDto(AktørId.fra("9999999999999"),
            Arbeidsgiver.fra("999999999"),
            LocalDate.now(),
            BigDecimal.valueOf(10000),
            List.of(),
            List.of(),
            List.of(),
            null,
            null);

        // Act
        var ex = assertThrows(IllegalStateException.class, () -> inntektsmeldingMottakTjeneste.mottaInntektsmelding(inntektsmeldingDto, uuid));

        // Assert
        assertThat(ex.getMessage()).contains("Kan ikke motta nye inntektsmeldinger på utgåtte forespørsler");
    }

    @Test
    void skal_kunne_motta_inntektsmelding_fra_arbeidgiverportal() {
        // Arrange
        var aktørId = AktørId.fra("9999999999999");
        var orgnr = "999999999";
        var startdato = LocalDate.now();
        var forespørselDto = lagForespørselDto(ForespørselType.BESTILT_AV_FAGSYSTEM, ForespørselStatus.UNDER_BEHANDLING,LocalDate.now());

        var im = lagInntektsmeldingDto(aktørId, Arbeidsgiver.fra(orgnr), startdato, BigDecimal.valueOf(100), List.of(),List.of(), List.of(), BigDecimal.valueOf(100), Tid.TIDENES_ENDE);

        when(forespørselBehandlingTjeneste.hentForespørsel(forespørselDto.uuid())).thenReturn(forespørselDto);
        when(inntektKontrollTjeneste.sjekkInntektMotAInntekt(any(), any())).thenReturn(
            new InntektKontrollResultat.Godkjent(lagInntektsopplysninger()));
        when(fellesMottakTjeneste.lagreImOgOpprettJournalførTask(any(), any())).thenReturn(im);

        // Act
        var responseDto = inntektsmeldingMottakTjeneste.mottaInntektsmelding(im, forespørselDto.uuid());

        // Assert
        verify(fellesMottakTjeneste, times(1)).ferdigstillOgOppdaterEksterneSystemer(forespørselDto, Optional.ofNullable(im.getInntektsmeldingUuid()));
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.refusjon()).hasSize(1);
    }

    @Test
    void skal_lagre_im_som_venter_vurdering_og_ikke_ferdigstille_når_ainntekt_har_nedetid() {
        // Arrange
        var aktørId = AktørId.fra("9999999999999");
        var orgnr = "999999999";
        var startdato = LocalDate.now();
        var forespørselDto = lagForespørselDto(ForespørselType.BESTILT_AV_FAGSYSTEM, ForespørselStatus.UNDER_BEHANDLING, LocalDate.now());

        var im = lagInntektsmeldingDto(aktørId, Arbeidsgiver.fra(orgnr), startdato, BigDecimal.valueOf(100), List.of(), List.of(), List.of(),
            BigDecimal.valueOf(100), Tid.TIDENES_ENDE);
        var lagretImVenterVurdering = InntektsmeldingDto.builder(im).medStatus(no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.InntektsmeldingStatus.VENTER_VURDERING).build();

        when(forespørselBehandlingTjeneste.hentForespørsel(forespørselDto.uuid())).thenReturn(forespørselDto);
        when(inntektKontrollTjeneste.sjekkInntektMotAInntekt(any(), any())).thenReturn(
            new InntektKontrollResultat.Nedetid("Inntektskomponenten har nedetid, ..."));
        when(fellesMottakTjeneste.lagreImOgOpprettTaskForEtterkontroll(any(), any())).thenReturn(lagretImVenterVurdering);

        // Act
        var responseDto = inntektsmeldingMottakTjeneste.mottaInntektsmelding(im, forespørselDto.uuid());

        // Assert
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.status()).isEqualTo(no.nav.foreldrepenger.inntektsmelding.typer.dto.InntektsmeldingStatusDto.VENTER_VURDERING);
        verify(fellesMottakTjeneste).lagreImOgOpprettTaskForEtterkontroll(any(), any());
        verify(fellesMottakTjeneste, never()).lagreImOgOpprettJournalførTask(any(), any());
        verify(fellesMottakTjeneste, never()).ferdigstillOgOppdaterEksterneSystemer(any(), any());
    }

    @Test
    void skal_kaste_exception_når_oppgitt_inntekt_avviker_fra_ainntekt() {
        // Arrange
        var aktørId = AktørId.fra("9999999999999");
        var orgnr = "999999999";
        var startdato = LocalDate.now();
        var forespørselDto = lagForespørselDto(ForespørselType.BESTILT_AV_FAGSYSTEM, ForespørselStatus.UNDER_BEHANDLING, LocalDate.now());

        var im = lagInntektsmeldingDto(aktørId, Arbeidsgiver.fra(orgnr), startdato, BigDecimal.valueOf(100), List.of(), List.of(), List.of(),
            BigDecimal.valueOf(100), Tid.TIDENES_ENDE);

        when(forespørselBehandlingTjeneste.hentForespørsel(forespørselDto.uuid())).thenReturn(forespørselDto);
        when(inntektKontrollTjeneste.sjekkInntektMotAInntekt(any(), any())).thenReturn(
            new InntektKontrollResultat.UlikInntekt("Inntekt i inntektsmelding er ulik inntekt fra A-inntekt, og ingen endringsårsak er oppgitt",
                lagInntektsopplysninger()));

        // Act & assert
        assertThrows(no.nav.foreldrepenger.inntektsmelding.server.exceptions.InntektAvvikerFraAInntektException.class,
            () -> inntektsmeldingMottakTjeneste.mottaInntektsmelding(im, forespørselDto.uuid()));

        verify(fellesMottakTjeneste, never()).lagreImOgOpprettJournalførTask(any(), any());
        verify(fellesMottakTjeneste, never()).lagreImOgOpprettTaskForEtterkontroll(any(), any());
        verify(fellesMottakTjeneste, never()).ferdigstillOgOppdaterEksterneSystemer(any(), any());
    }

    @Test
    void skal_kunne_motta_arbeidsgiverinitert_inntektsmelding() {
        // Arrange
        var ytelse = Ytelsetype.FORELDREPENGER;
        var aktørId = AktørId.fra("9999999999999");
        var orgnr = "999999999";
        var startdato = LocalDate.now();
        var forespørselDto = lagForespørselDto(ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT, ForespørselStatus.UNDER_BEHANDLING, startdato);

        var im = lagInntektsmeldingDto(aktørId, Arbeidsgiver.fra(orgnr),startdato, BigDecimal.valueOf(100), List.of(), List.of(), List.of(), BigDecimal.valueOf(100), Tid.TIDENES_ENDE);

        when(forespørselBehandlingTjeneste.opprettForespørselForArbeidsgiverInitiertIm(ytelse,
            aktørId,
            Arbeidsgiver.fra(orgnr),
            startdato,
            ArbeidsgiverinitiertÅrsak.NYANSATT,
            null,
            null)).thenReturn(forespørselDto);
        when(fellesMottakTjeneste.lagreImOgOpprettJournalførTask(any(), any())).thenReturn(im);

        // Act
        var responseDto = inntektsmeldingMottakTjeneste.mottaArbeidsgiverinitiertInntektsmelding(im, null, ArbeidsgiverinitiertÅrsak.NYANSATT);

        // Assert
        verify(forespørselBehandlingTjeneste, times(1)).ferdigstillForespørsel(forespørselDto.uuid());
        verify(forespørselBehandlingTjeneste, times(1)).opprettSakOgFerdigstillTasksIPortaler(forespørselDto, im.getInntektsmeldingUuid());
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.refusjon()).hasSize(1);
    }

    @Test
    void skal_kunne_motta_endring_av_arbeidsgiverinitert_inntektsmelding() {
        // Arrange
        var aktørId = AktørId.fra("9999999999999");
        var orgnr = "999999999";

        var eksisterendeForespørselDto = lagForespørselDto(ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT, ForespørselStatus.UNDER_BEHANDLING, LocalDate.now());

        var nyStartDato = LocalDate.now().plusWeeks(1);

        var im = lagInntektsmeldingDto(aktørId, Arbeidsgiver.fra(orgnr), nyStartDato, BigDecimal.valueOf(100), List.of(), List.of(), List.of(), BigDecimal.valueOf(100), Tid.TIDENES_ENDE);

        var forespørselMedNyDatoDto = lagForespørselDto(ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT, ForespørselStatus.UNDER_BEHANDLING, nyStartDato);

        when(forespørselBehandlingTjeneste.hentForespørsel(eksisterendeForespørselDto.uuid())).thenReturn(eksisterendeForespørselDto);
        when(forespørselBehandlingTjeneste.oppdaterFørsteUttaksdato(any(), any())).thenReturn(forespørselMedNyDatoDto);
        when(fellesMottakTjeneste.lagreImOgOpprettJournalførTask(any(), any())).thenReturn(im);

        // Act
        var responseDto = inntektsmeldingMottakTjeneste.mottaArbeidsgiverinitiertInntektsmelding(im, eksisterendeForespørselDto.uuid(), ArbeidsgiverinitiertÅrsak.NYANSATT);

        // Assert
        verify(forespørselBehandlingTjeneste, times(0)).ferdigstillForespørsel(any(), any(), any());
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.refusjon()).hasSize(1);
        assertThat(responseDto.startdato()).isEqualTo(nyStartDato);
    }

    @Test
    void skal_kunne_motta_arbeidsgiverinitert_uregistrert_inntektsmelding() {
        // Arrange
        var ytelse = Ytelsetype.FORELDREPENGER;
        var aktørId = AktørId.fra("9999999999999");
        var orgnr = "999999999";
        var startdato = LocalDate.now();
        var forespørselDto = lagForespørselDto(ForespørselType.ARBEIDSGIVERINITIERT_UREGISTRERT, ForespørselStatus.UNDER_BEHANDLING, startdato);

        var im =lagInntektsmeldingDto(aktørId, Arbeidsgiver.fra(orgnr), startdato, BigDecimal.valueOf(100), List.of(), List.of(), List.of(), BigDecimal.valueOf(100),Tid.TIDENES_ENDE);

        var skjæringstidspunkt = startdato.minusDays(2);
        var infoOmSak = new FpsakFagsak(FpsakFagsak.StatusSakInntektsmelding.ÅPEN_FOR_BEHANDLING, startdato, skjæringstidspunkt, new Saksnummer("12345"));
        when(fpsakTjeneste.henterInfoOmSakIFagsystem(aktørId, Ytelsetype.FORELDREPENGER)).thenReturn(List.of(infoOmSak));
        when(forespørselBehandlingTjeneste.opprettForespørselForArbeidsgiverInitiertIm(ytelse,
            aktørId,
            Arbeidsgiver.fra(orgnr),
            startdato,
            ArbeidsgiverinitiertÅrsak.UREGISTRERT,
            skjæringstidspunkt,
            new Saksnummer("12345"))).thenReturn(forespørselDto);

        when(fellesMottakTjeneste.lagreImOgOpprettJournalførTask(any(), any())).thenReturn(im);

        // Act
        var responseDto = inntektsmeldingMottakTjeneste.mottaArbeidsgiverinitiertInntektsmelding(im, null,
            ArbeidsgiverinitiertÅrsak.UREGISTRERT);

        // Assert
        verify(forespørselBehandlingTjeneste, times(1)).ferdigstillForespørsel(forespørselDto.uuid());
        verify(forespørselBehandlingTjeneste, times(1)).opprettSakOgFerdigstillTasksIPortaler(forespørselDto, im.getInntektsmeldingUuid());
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.refusjon()).hasSize(1);
        assertThat(responseDto.startdato()).isEqualTo(startdato);
    }

    @Test
    void skal_kunne_motta_endring_av_arbeidsgiverinitert_uregistrert_inntektsmelding() {
        // Arrange
        var uuid = UUID.randomUUID();
        var aktørId = AktørId.fra("9999999999999");
        var orgnr = "999999999";
        var startdato = LocalDate.now();
        var eksisterendeForespørselDto = lagForespørselDto(ForespørselType.ARBEIDSGIVERINITIERT_UREGISTRERT, ForespørselStatus.UNDER_BEHANDLING, startdato);

        var opphørsdato = LocalDate.now().plusMonths(5);
        var nyInntekt = BigDecimal.valueOf(200);

        when(forespørselBehandlingTjeneste.hentForespørsel(uuid)).thenReturn(eksisterendeForespørselDto);

        var endringsårsaker = List.of(new InntektsmeldingDto.Endringsårsak(EndringsårsakType.VARIG_LØNNSENDRING, null, null, null));

        var nyIm = lagInntektsmeldingDto(aktørId, Arbeidsgiver.fra(orgnr), startdato, nyInntekt, List.of(), List.of(), endringsårsaker, nyInntekt, opphørsdato.minusDays(1));

        when(fellesMottakTjeneste.lagreImOgOpprettJournalførTask(any(), any())).thenReturn(nyIm);

        // Act
        var responseDto = inntektsmeldingMottakTjeneste.mottaArbeidsgiverinitiertInntektsmelding(nyIm, uuid,
            ArbeidsgiverinitiertÅrsak.UREGISTRERT);

        // Assert
        verify(forespørselBehandlingTjeneste, times(0)).ferdigstillForespørsel(any(), any(), any());
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.refusjon()).hasSize(2);
        assertThat(responseDto.startdato()).isEqualTo(startdato);
        assertThat(responseDto.refusjon().getFirst().fom()).isEqualTo(startdato);
        assertThat(responseDto.refusjon().getFirst().beløp()).isEqualTo(BigDecimal.valueOf(200));
        assertThat(responseDto.refusjon().get(1).fom()).isEqualTo(opphørsdato);
        assertThat(responseDto.refusjon().get(1).beløp()).isEqualTo(BigDecimal.valueOf(0));
        assertThat(responseDto.endringAvInntektÅrsaker()).hasSize(1);
    }

    private static Inntektsopplysninger lagInntektsopplysninger() {
        return new Inntektsopplysninger(BigDecimal.valueOf(100), "999999999", List.of());
    }

    private static ForespørselDto lagForespørselDto(ForespørselType forespørselType, ForespørselStatus status, LocalDate startdato) {
        return ForespørselDto.builder()
            .uuid(UUID.randomUUID())
            .arbeidsgiver(Arbeidsgiver.fra("999999999"))
            .aktørId(AktørId.fra("9999999999999"))
            .ytelseType(Ytelsetype.FORELDREPENGER)
            .status(status)
            .forespørselType(forespørselType)
            .førsteUttaksdato(startdato)
            .build();
    }

    private static InntektsmeldingDto lagInntektsmeldingDto(AktørId aktørId, Arbeidsgiver arbeidsgiver, LocalDate startdato, BigDecimal inntekt,
                                                            List<InntektsmeldingDto.SøktRefusjon> søkteRefusjonsperioder,
                                                            List<InntektsmeldingDto.BortfaltNaturalytelse> bortfaltNaturalytelsePerioder,
                                                            List<InntektsmeldingDto.Endringsårsak> endringAvInntektÅrsaker,
                                                            BigDecimal månedRefusjon, LocalDate opphørsdatoRefusjon) {
        var builder = InntektsmeldingDto.builder()
            .medAktørId(aktørId)
            .medYtelse(Ytelsetype.FORELDREPENGER)
            .medArbeidsgiver(arbeidsgiver)
            .medKontaktperson(new InntektsmeldingDto.Kontaktperson("123", "Navn"))
            .medStartdato(startdato)
            .medInntekt(inntekt)
            .medSøkteRefusjonsperioder(søkteRefusjonsperioder)
            .medBortfaltNaturalytelsePerioder(bortfaltNaturalytelsePerioder)
            .medEndringAvInntektÅrsaker(endringAvInntektÅrsaker)
            .medStatus(no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.InntektsmeldingStatus.GODKJENT);
        if (månedRefusjon != null) {
            builder.medMånedRefusjon(månedRefusjon);
        }

        if (opphørsdatoRefusjon != null) {
            builder.medOpphørsdatoRefusjon(opphørsdatoRefusjon);
        }
        return builder.build();
    }

}
