package no.nav.foreldrepenger.inntektsmelding.imdialog.tjenester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.lager.ForespørselEntitet;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDtoMapper;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;
import no.nav.foreldrepenger.inntektsmelding.imdialog.rest.SendInntektsmeldingRequestDto;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.fpsak.FpsakKlient;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.fpsak.FpsakTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.ArbeidsgiverDto;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.ArbeidsgiverinitiertÅrsakDto;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.EndringsårsakDto;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.YtelseTypeDto;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ArbeidsgiverinitiertÅrsak;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Endringsårsak;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.foreldrepenger.inntektsmelding.typer.lager.AktørIdEntitet;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
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
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    @Mock
    private ProsessTaskTjeneste prosessTaskTjeneste;
    @Mock
    private FpsakTjeneste fpsakTjeneste;

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
        inntektsmeldingMottakTjeneste = new InntektsmeldingMottakTjeneste(forespørselBehandlingTjeneste,
            inntektsmeldingTjeneste, prosessTaskTjeneste, fpsakTjeneste);
    }


    @Test
    void skal_ikke_godta_im_på_utgått_forespørrsel() {
        // Arrange
        var uuid = UUID.randomUUID();
        var forespørsel = new ForespørselEntitet("999999999",
            LocalDate.now(),
            new AktørIdEntitet("9999999999999"),
            Ytelsetype.FORELDREPENGER,
            "123",
            LocalDate.now(),
            ForespørselType.BESTILT_AV_FAGSYSTEM);
        forespørsel.setStatus(ForespørselStatus.UTGÅTT);
        when(forespørselBehandlingTjeneste.hentForespørsel(uuid)).thenReturn(Optional.of(ForespørselDtoMapper.mapFraEntitet(forespørsel)));
        var innsendingDto = new SendInntektsmeldingRequestDto(uuid,
            new AktørIdDto("9999999999999"),
            YtelseTypeDto.FORELDREPENGER,
            new OrganisasjonsnummerDto("999999999"),
            new SendInntektsmeldingRequestDto.KontaktpersonRequestDto("Navn", "123"),
            LocalDate.now(),
            BigDecimal.valueOf(10000),
            List.of(),
            List.of(),
            List.of(),
            null);

        // Act
        var ex = assertThrows(IllegalStateException.class, () -> inntektsmeldingMottakTjeneste.mottaInntektsmelding(innsendingDto));

        // Assert
        assertThat(ex.getMessage()).contains("Kan ikke motta nye inntektsmeldinger på utgåtte forespørsler");
    }

    @Test
    void skal_kunne_motta_arbeidsgiverinitert_inntektsmelding() {
        // Arrange
        var uuid = UUID.randomUUID();
        var ytelse = Ytelsetype.FORELDREPENGER;
        var aktørId = new AktørId("9999999999999");
        var orgnr = "999999999";
        var startdato = LocalDate.now();
        var forespørsel = new ForespørselEntitet(orgnr,
            startdato,
            new AktørIdEntitet(aktørId.getAktørId()),
            ytelse,
            "123",
            LocalDate.now(),
            ForespørselType.BESTILT_AV_FAGSYSTEM);

        var im = InntektsmeldingDto.builder()
            .medAktørId(new AktørId(aktørId.getAktørId()))
            .medKontaktperson(new InntektsmeldingDto.Kontaktperson("Test", "Test"))
            .medYtelse(Ytelsetype.FORELDREPENGER)
            .medInntekt(BigDecimal.valueOf(100))
            .medStartdato(startdato)
            .medMånedRefusjon(BigDecimal.valueOf(100))
            .medOpphørsdatoRefusjon(Tid.TIDENES_ENDE)
            .medArbeidsgiver(new Arbeidsgiver(orgnr))
            .medSøkteRefusjonsperioder(List.of())
            .medBortfaltNaturalytelsePerioder(List.of())
            .medEndringAvInntektÅrsaker(List.of())
            .build();

        when(forespørselBehandlingTjeneste.opprettForespørselForArbeidsgiverInitiertIm(ytelse,
            aktørId,
            Arbeidsgiver.fra(orgnr),
            startdato,
            ArbeidsgiverinitiertÅrsak.NYANSATT,
            null)).thenReturn(uuid);
        when(forespørselBehandlingTjeneste.hentForespørsel(uuid)).thenReturn(Optional.of(ForespørselDtoMapper.mapFraEntitet(forespørsel)));
        var innsendingDto = new SendInntektsmeldingRequestDto(null,
            new AktørIdDto("9999999999999"),
            YtelseTypeDto.FORELDREPENGER,
            new OrganisasjonsnummerDto(orgnr),
            new SendInntektsmeldingRequestDto.KontaktpersonRequestDto("Navn", "123"),
            LocalDate.now(),
            null,
            List.of(new SendInntektsmeldingRequestDto.Refusjon(startdato, BigDecimal.valueOf(100))),
            List.of(),
            List.of(),
            ArbeidsgiverinitiertÅrsakDto.NYANSATT);
        when(inntektsmeldingTjeneste.lagreInntektsmelding(any())).thenReturn(1L);
        when(inntektsmeldingTjeneste.hentInntektsmelding(1L)).thenReturn(im);

        // Act
        var responseDto = inntektsmeldingMottakTjeneste.mottaArbeidsgiverinitiertInntektsmelding(innsendingDto, ArbeidsgiverinitiertÅrsak.NYANSATT);

        // Assert
        verify(forespørselBehandlingTjeneste, times(1)).ferdigstillForespørsel(forespørsel.getUuid(), aktørId, Arbeidsgiver.fra(orgnr), startdato, LukkeÅrsak.ORDINÆR_INNSENDING,
                Optional.ofNullable(im.getInntektsmeldingUuid()));
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.refusjon()).hasSize(1);
    }

    @Test
    void skal_kunne_motta_endring_av_arbeidsgiverinitert_inntektsmelding() {
        // Arrange
        var uuid = UUID.randomUUID();
        var ytelse = Ytelsetype.FORELDREPENGER;
        var aktørId = new AktørId("9999999999999");
        var orgnr = "999999999";
        var startdato = LocalDate.now();
        var eksisterendeForespørsel = new ForespørselEntitet(orgnr,
            startdato,
            new AktørIdEntitet(aktørId.getAktørId()),
            ytelse,
            "123",
            LocalDate.now(),
            ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT);

        var nyStartDato = LocalDate.now().plusWeeks(1);

        var im = InntektsmeldingDto.builder()
            .medAktørId(new no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId(aktørId.getAktørId()))
            .medKontaktperson(new InntektsmeldingDto.Kontaktperson("Test", "Test"))
            .medYtelse(Ytelsetype.FORELDREPENGER)
            .medInntekt(BigDecimal.valueOf(100))
            .medStartdato(nyStartDato)
            .medMånedRefusjon(BigDecimal.valueOf(100))
            .medOpphørsdatoRefusjon(Tid.TIDENES_ENDE)
            .medArbeidsgiver(new Arbeidsgiver(orgnr))
            .medSøkteRefusjonsperioder(List.of())
            .medBortfaltNaturalytelsePerioder(List.of())
            .medEndringAvInntektÅrsaker(List.of())
            .build();


        var forespørselMedNyDato = new ForespørselEntitet(orgnr,
            nyStartDato,
            new AktørIdEntitet(aktørId.getAktørId()),
            ytelse,
            "123",
            LocalDate.now(),
            ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT);

        when(forespørselBehandlingTjeneste.hentForespørsel(uuid)).thenReturn(Optional.of(ForespørselDtoMapper.mapFraEntitet(eksisterendeForespørsel)));
        when(forespørselBehandlingTjeneste.oppdaterFørsteUttaksdato(any(), any())).thenReturn(ForespørselDtoMapper.mapFraEntitet(forespørselMedNyDato));

        var innsendingDto = new SendInntektsmeldingRequestDto(uuid,
            new AktørIdDto("9999999999999"),
            YtelseTypeDto.FORELDREPENGER,
            new OrganisasjonsnummerDto(orgnr),
            new SendInntektsmeldingRequestDto.KontaktpersonRequestDto("Navn", "123"),
            nyStartDato,
            null,
            List.of(new SendInntektsmeldingRequestDto.Refusjon(nyStartDato, BigDecimal.valueOf(100))),
            List.of(),
            List.of(),
            ArbeidsgiverinitiertÅrsakDto.NYANSATT);
        when(inntektsmeldingTjeneste.lagreInntektsmelding(any())).thenReturn(1L);
        when(inntektsmeldingTjeneste.hentInntektsmelding(1L)).thenReturn(im);

        // Act
        var responseDto = inntektsmeldingMottakTjeneste.mottaArbeidsgiverinitiertInntektsmelding(innsendingDto, ArbeidsgiverinitiertÅrsak.NYANSATT);

        // Assert
        verify(forespørselBehandlingTjeneste, times(0)).ferdigstillForespørsel(uuid, aktørId, Arbeidsgiver.fra(orgnr), startdato, LukkeÅrsak.ORDINÆR_INNSENDING,
            Optional.ofNullable(im.getInntektsmeldingUuid()));
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.refusjon()).hasSize(1);
        assertThat(responseDto.startdato()).isEqualTo(nyStartDato);
    }

    @Test
    void skal_kunne_motta_arbeidsgiverinitert_uregistrert_inntektsmelding() {
        // Arrange
        var uuid = UUID.randomUUID();
        var ytelse = Ytelsetype.FORELDREPENGER;
        var aktørId = new AktørId("9999999999999");
        var orgnr = "999999999";
        var startdato = LocalDate.now();
        var forespørsel = new ForespørselEntitet(orgnr,
            startdato,
            new AktørIdEntitet(aktørId.getAktørId()),
            ytelse,
            "123",
            LocalDate.now(),
            ForespørselType.BESTILT_AV_FAGSYSTEM);

        var im = InntektsmeldingDto.builder()
            .medAktørId(new no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId(aktørId.getAktørId()))
            .medKontaktperson(new InntektsmeldingDto.Kontaktperson("Test", "Test"))
            .medYtelse(Ytelsetype.FORELDREPENGER)
            .medInntekt(BigDecimal.valueOf(100))
            .medStartdato(startdato)
            .medMånedRefusjon(BigDecimal.valueOf(100))
            .medOpphørsdatoRefusjon(Tid.TIDENES_ENDE)
            .medArbeidsgiver(new Arbeidsgiver(orgnr))
            .medSøkteRefusjonsperioder(List.of())
            .medBortfaltNaturalytelsePerioder(List.of())
            .medEndringAvInntektÅrsaker(List.of())
            .build();

        var skjæringstidspunkt = startdato.minusDays(2);
        var infoOmSak = new FpsakKlient.InfoOmSakInntektsmeldingResponse(FpsakKlient.StatusSakInntektsmelding.ÅPEN_FOR_BEHANDLING,startdato,skjæringstidspunkt);
        when(fpsakTjeneste.henterInfoOmSakIFagsystem(aktørId, Ytelsetype.FORELDREPENGER)).thenReturn(infoOmSak);
        when(forespørselBehandlingTjeneste.opprettForespørselForArbeidsgiverInitiertIm(ytelse,
            aktørId,
            Arbeidsgiver.fra(orgnr),
            startdato,
            ArbeidsgiverinitiertÅrsak.UREGISTRERT,
            skjæringstidspunkt)).thenReturn(uuid);
        when(forespørselBehandlingTjeneste.hentForespørsel(uuid)).thenReturn(Optional.of(ForespørselDtoMapper.mapFraEntitet(forespørsel)));
        var inntekt = BigDecimal.valueOf(100);
        var innsendingDto = new SendInntektsmeldingRequestDto(null,
            new AktørIdDto("9999999999999"),
            YtelseTypeDto.FORELDREPENGER,
            new OrganisasjonsnummerDto(orgnr),
            new SendInntektsmeldingRequestDto.KontaktpersonRequestDto("Navn", "123"),
            LocalDate.now(),
            inntekt,
            List.of(new SendInntektsmeldingRequestDto.Refusjon(startdato, inntekt)),
            List.of(),
            List.of(),
            ArbeidsgiverinitiertÅrsakDto.UREGISTRERT);
        when(inntektsmeldingTjeneste.lagreInntektsmelding(any())).thenReturn(1L);
        when(inntektsmeldingTjeneste.hentInntektsmelding(1L)).thenReturn(im);

        // Act
        var responseDto = inntektsmeldingMottakTjeneste.mottaArbeidsgiverinitiertInntektsmelding(innsendingDto, ArbeidsgiverinitiertÅrsak.UREGISTRERT);

        // Assert
        verify(forespørselBehandlingTjeneste, times(1)).ferdigstillForespørsel(forespørsel.getUuid(), aktørId, Arbeidsgiver.fra(orgnr), startdato, LukkeÅrsak.ORDINÆR_INNSENDING,
            Optional.ofNullable(im.getInntektsmeldingUuid()));
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.refusjon()).hasSize(1);
        assertThat(responseDto.startdato()).isEqualTo(startdato);
    }

    @Test
    void skal_kunne_motta_endring_av_arbeidsgiverinitert_uregistrert_inntektsmelding() {
        // Arrange
        var uuid = UUID.randomUUID();
        var ytelse = Ytelsetype.FORELDREPENGER;
        var aktørId = new AktørId("9999999999999");
        var orgnr = "999999999";
        var startdato = LocalDate.now();
        var eksisterendeForespørsel = new ForespørselEntitet(orgnr,
            startdato,
            new AktørIdEntitet(aktørId.getAktørId()),
            ytelse,
            "123",
            LocalDate.now(),
            ForespørselType.ARBEIDSGIVERINITIERT_UREGISTRERT);

        var opphørsdato = LocalDate.now().plusMonths(5);
        var nyInntekt = BigDecimal.valueOf(200);

        var im = InntektsmeldingDto.builder()
            .medAktørId(new no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId(aktørId.getAktørId()))
            .medKontaktperson(new InntektsmeldingDto.Kontaktperson("Test", "Test"))
            .medYtelse(Ytelsetype.FORELDREPENGER)
            .medInntekt(nyInntekt)
            .medStartdato(startdato)
            .medMånedRefusjon(BigDecimal.valueOf(200))
            .medEndringAvInntektÅrsaker(List.of(new InntektsmeldingDto.Endringsårsaker(Endringsårsak.VARIG_LØNNSENDRING, null, null, null)))
            .medOpphørsdatoRefusjon(opphørsdato.minusDays(1))
            .medArbeidsgiver(new Arbeidsgiver(orgnr))
            .medSøkteRefusjonsperioder(List.of())
            .medBortfaltNaturalytelsePerioder(List.of())
            .build();

        when(forespørselBehandlingTjeneste.hentForespørsel(uuid)).thenReturn(Optional.of(ForespørselDtoMapper.mapFraEntitet(eksisterendeForespørsel)));

        var endringsårsak = List.of(new SendInntektsmeldingRequestDto.EndringsårsakerRequestDto(EndringsårsakDto.VARIG_LØNNSENDRING, null, null, null));

        var innsendingDto = new SendInntektsmeldingRequestDto(uuid,
            new AktørIdDto("9999999999999"),
            YtelseTypeDto.FORELDREPENGER,
            new OrganisasjonsnummerDto(orgnr),
            new SendInntektsmeldingRequestDto.KontaktpersonRequestDto("Navn", "123"),
            startdato,
            nyInntekt,
            List.of(new SendInntektsmeldingRequestDto.Refusjon(startdato, BigDecimal.valueOf(200)),
                new SendInntektsmeldingRequestDto.Refusjon(opphørsdato, BigDecimal.ZERO)),
            List.of(),
            endringsårsak,
            ArbeidsgiverinitiertÅrsakDto.UREGISTRERT);
        when(inntektsmeldingTjeneste.lagreInntektsmelding(any())).thenReturn(1L);
        when(inntektsmeldingTjeneste.hentInntektsmelding(1L)).thenReturn(im);

        // Act
        var responseDto = inntektsmeldingMottakTjeneste.mottaArbeidsgiverinitiertInntektsmelding(innsendingDto, ArbeidsgiverinitiertÅrsak.UREGISTRERT);

        // Assert
        verify(forespørselBehandlingTjeneste, times(0)).ferdigstillForespørsel(uuid, aktørId, Arbeidsgiver.fra(orgnr), startdato, LukkeÅrsak.ORDINÆR_INNSENDING,
            Optional.ofNullable(im.getInntektsmeldingUuid()));
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.refusjon()).hasSize(2);
        assertThat(responseDto.startdato()).isEqualTo(startdato);
        assertThat(responseDto.refusjon().getFirst().fom()).isEqualTo(startdato);
        assertThat(responseDto.refusjon().getFirst().beløp()).isEqualTo(BigDecimal.valueOf(200));
        assertThat(responseDto.refusjon().get(1).fom()).isEqualTo(opphørsdato);
        assertThat(responseDto.refusjon().get(1).beløp()).isEqualTo(BigDecimal.valueOf(0));
        assertThat(responseDto.endringAvInntektÅrsaker()).isEqualTo(endringsårsak);
    }
}
