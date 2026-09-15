package no.nav.foreldrepenger.inntektsmelding.inntektsmelding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.inntektskomponent.InntektTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.inntektskomponent.Inntektsopplysninger;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.MånedslønnStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.EndringsårsakType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.InntektsmeldingStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Kildesystem;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.NaturalytelseType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.konfig.Tid;

@ExtendWith(MockitoExtension.class)
class InntektKontrollTjenesteTest {
    private static final String ORGNR = "999999999";
    private static final String AKTØR_ID = "1234567891011";

    private InntektKontrollTjeneste inntektKontrollTjeneste;

    @Mock
    private InntektTjeneste inntektTjeneste;
    @Mock
    private PersonTjeneste personTjeneste;
    @Mock
    private FellesGrunnlagTjeneste fellesGrunnlagTjeneste;
    @Mock
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    @Mock
    private FellesMottakTjeneste fellesMottakTjeneste;
    @Mock
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    @BeforeEach
    void setup() {
        inntektKontrollTjeneste = new InntektKontrollTjeneste(
            inntektTjeneste, personTjeneste, fellesGrunnlagTjeneste, inntektsmeldingTjeneste, fellesMottakTjeneste, forespørselBehandlingTjeneste);
    }

    @Test
    void sjekkInntektMotAInntekt_skal_returnere_godkjent_når_inntekt_er_lik() {
        var forespørselDto = lagForespørselDtoMedSkjæringstidspunkt(UUID.randomUUID(), ForespørselStatus.UNDER_BEHANDLING);
        var inntektsmelding = lagInntektsmeldingDto(forespørselDto.førsteUttaksdato(), false);
        var inntektsopplysninger = lagInntektsopplysninger(BigDecimal.valueOf(45000));

        when(personTjeneste.hentPersonInfoFraAktørId(any(), any())).thenReturn(lagPersonInfo());
        when(fellesGrunnlagTjeneste.harJobbetHeleBeregningsperioden(any(), any(), any())).thenReturn(false);
        when(inntektTjeneste.hentInntekt(any(), any(), any(), any(), eq(false))).thenReturn(inntektsopplysninger);

        var resultat = inntektKontrollTjeneste.sjekkInntektMotAInntekt(forespørselDto, inntektsmelding);

        assertThat(resultat).isInstanceOf(InntektKontrollResultat.Godkjent.class);
        assertThat(((InntektKontrollResultat.Godkjent) resultat).inntektFraAInntekt()).isEqualTo(inntektsopplysninger);
    }

    @Test
    void sjekkInntektMotAInntekt_skal_returnere_godkjent_når_avvik_er_innenfor_akseptert_grense() {
        var forespørselDto = lagForespørselDtoMedSkjæringstidspunkt(UUID.randomUUID(), ForespørselStatus.UNDER_BEHANDLING);
        var inntektsmelding = lagInntektsmeldingDto(forespørselDto.førsteUttaksdato(), false);
        // inntekt=45000, gjennomsnitt=45050 -> diff=50 -> ikke > 50 -> godkjent
        var inntektsopplysninger = lagInntektsopplysninger(BigDecimal.valueOf(45050));

        when(personTjeneste.hentPersonInfoFraAktørId(any(), any())).thenReturn(lagPersonInfo());
        when(fellesGrunnlagTjeneste.harJobbetHeleBeregningsperioden(any(), any(), any())).thenReturn(false);
        when(inntektTjeneste.hentInntekt(any(), any(), any(), any(), eq(false))).thenReturn(inntektsopplysninger);

        var resultat = inntektKontrollTjeneste.sjekkInntektMotAInntekt(forespørselDto, inntektsmelding);

        assertThat(resultat).isInstanceOf(InntektKontrollResultat.Godkjent.class);
    }

    @Test
    void sjekkInntektMotAInntekt_skal_returnere_godkjent_når_avvik_har_endringsårsak() {
        var forespørselDto = lagForespørselDtoMedSkjæringstidspunkt(UUID.randomUUID(), ForespørselStatus.UNDER_BEHANDLING);
        var inntektsmelding = lagInntektsmeldingDto(forespørselDto.førsteUttaksdato(), true);
        // stort avvik, men endringsårsak er oppgitt -> godkjent
        var inntektsopplysninger = lagInntektsopplysninger(BigDecimal.valueOf(46000));

        when(personTjeneste.hentPersonInfoFraAktørId(any(), any())).thenReturn(lagPersonInfo());
        when(fellesGrunnlagTjeneste.harJobbetHeleBeregningsperioden(any(), any(), any())).thenReturn(false);
        when(inntektTjeneste.hentInntekt(any(), any(), any(), any(), eq(false))).thenReturn(inntektsopplysninger);

        var resultat = inntektKontrollTjeneste.sjekkInntektMotAInntekt(forespørselDto, inntektsmelding);

        assertThat(resultat).isInstanceOf(InntektKontrollResultat.Godkjent.class);
    }

    @Test
    void sjekkInntektMotAInntekt_skal_returnere_ulik_inntekt_når_avvik_er_utenfor_grense_uten_årsak() {
        var forespørselDto = lagForespørselDtoMedSkjæringstidspunkt(UUID.randomUUID(), ForespørselStatus.UNDER_BEHANDLING);
        var inntektsmelding = lagInntektsmeldingDto(forespørselDto.førsteUttaksdato(), false);
        // inntekt=45000, gjennomsnitt=45051 -> diff=51 -> avvist
        var inntektsopplysninger = lagInntektsopplysninger(BigDecimal.valueOf(45051));

        when(personTjeneste.hentPersonInfoFraAktørId(any(), any())).thenReturn(lagPersonInfo());
        when(fellesGrunnlagTjeneste.harJobbetHeleBeregningsperioden(any(), any(), any())).thenReturn(false);
        when(inntektTjeneste.hentInntekt(any(), any(), any(), any(), eq(false))).thenReturn(inntektsopplysninger);

        var resultat = inntektKontrollTjeneste.sjekkInntektMotAInntekt(forespørselDto, inntektsmelding);

        assertThat(resultat).isInstanceOf(InntektKontrollResultat.UlikInntekt.class);
        var ulikInntekt = (InntektKontrollResultat.UlikInntekt) resultat;
        assertThat(ulikInntekt.feilmelding()).contains("Inntekt i inntektsmelding er ulik inntekt fra A-inntekt");
        assertThat(ulikInntekt.inntektFraAInntekt()).isEqualTo(inntektsopplysninger);
    }

    @Test
    void sjekkInntektMotAInntekt_skal_returnere_nedetid_når_ainntekt_har_nedetid() {
        var forespørselDto = lagForespørselDtoMedSkjæringstidspunkt(UUID.randomUUID(), ForespørselStatus.UNDER_BEHANDLING);
        var inntektsmelding = lagInntektsmeldingDto(forespørselDto.førsteUttaksdato(), false);
        var inntektsopplysninger = new Inntektsopplysninger(BigDecimal.ZERO, ORGNR, List.of(
            new Inntektsopplysninger.InntektMåned(BigDecimal.ZERO, YearMonth.of(2026, Month.JANUARY), MånedslønnStatus.NEDETID_AINNTEKT)));

        when(personTjeneste.hentPersonInfoFraAktørId(any(), any())).thenReturn(lagPersonInfo());
        when(fellesGrunnlagTjeneste.harJobbetHeleBeregningsperioden(any(), any(), any())).thenReturn(false);
        when(inntektTjeneste.hentInntekt(any(), any(), any(), any(), eq(false))).thenReturn(inntektsopplysninger);

        var resultat = inntektKontrollTjeneste.sjekkInntektMotAInntekt(forespørselDto, inntektsmelding);

        assertThat(resultat).isInstanceOf(InntektKontrollResultat.Nedetid.class);
        assertThat(((InntektKontrollResultat.Nedetid) resultat).melding()).contains("nedetid");
    }

    @Test
    void sjekkInntektMotAInntekt_skal_kaste_feil_når_ainntekt_returnerer_null() {
        var forespørselDto = lagForespørselDtoMedSkjæringstidspunkt(UUID.randomUUID(), ForespørselStatus.UNDER_BEHANDLING);
        var inntektsmelding = lagInntektsmeldingDto(forespørselDto.førsteUttaksdato(), false);

        when(personTjeneste.hentPersonInfoFraAktørId(any(), any())).thenReturn(lagPersonInfo());
        when(fellesGrunnlagTjeneste.harJobbetHeleBeregningsperioden(any(), any(), any())).thenReturn(false);
        when(inntektTjeneste.hentInntekt(any(), any(), any(), any(), eq(false))).thenReturn(null);

        assertThrows(IllegalStateException.class,
            () -> inntektKontrollTjeneste.sjekkInntektMotAInntekt(forespørselDto, inntektsmelding));
    }

    @Test
    void kontrollerInntektsmeldingEtterNedetid_skal_kaste_exception_når_det_fortsatt_er_nedetid() {
        var inntektsmeldingId = 123L;
        var imUuid = UUID.randomUUID();
        var forespørselDto = lagForespørselDtoMedSkjæringstidspunkt(UUID.randomUUID(), ForespørselStatus.UNDER_BEHANDLING);
        var inntektsmelding = lagInntektsmeldingDtoMedForespørsel(imUuid, forespørselDto, false);
        var inntektsopplysninger = new Inntektsopplysninger(BigDecimal.ZERO, ORGNR, List.of(
            new Inntektsopplysninger.InntektMåned(BigDecimal.ZERO, YearMonth.of(2026, Month.JANUARY), MånedslønnStatus.NEDETID_AINNTEKT)));

        when(inntektsmeldingTjeneste.hentInntektsmelding(inntektsmeldingId)).thenReturn(inntektsmelding);
        when(personTjeneste.hentPersonInfoFraAktørId(any(), any())).thenReturn(lagPersonInfo());
        when(fellesGrunnlagTjeneste.harJobbetHeleBeregningsperioden(any(), any(), any())).thenReturn(false);
        when(inntektTjeneste.hentInntekt(any(), any(), any(), any(), eq(false))).thenReturn(inntektsopplysninger);

        assertThrows(TekniskException.class,
            () -> inntektKontrollTjeneste.kontrollerInntektsmeldingEtterNedetid(inntektsmeldingId));

        verify(fellesMottakTjeneste, never()).opprettTaskForSendTilJoark(any(), any());
        verify(fellesMottakTjeneste, never()).ferdigstillOgOppdaterEksterneSystemer(any(), any());
    }

    @Test
    void kontrollerInntektsmeldingEtterNedetid_skal_ikke_gjøre_noe_når_inntektsmelding_er_utdatert() {
        var inntektsmeldingId = 123L;
        var imUuid = UUID.randomUUID();
        var forespørselDto = lagForespørselDtoMedSkjæringstidspunkt(UUID.randomUUID(), ForespørselStatus.UNDER_BEHANDLING);
        var utdatertInntektsmelding = InntektsmeldingDto.builder(lagInntektsmeldingDtoMedForespørsel(imUuid, forespørselDto, false))
            .medStatus(InntektsmeldingStatus.UTDATERT)
            .build();

        when(inntektsmeldingTjeneste.hentInntektsmelding(inntektsmeldingId)).thenReturn(utdatertInntektsmelding);

        inntektKontrollTjeneste.kontrollerInntektsmeldingEtterNedetid(inntektsmeldingId);

        verify(inntektsmeldingTjeneste, never()).oppdatertStatusTilInntektsmelding(any(), any());
        verify(fellesMottakTjeneste, never()).opprettTaskForSendTilJoark(any(), any());
        verify(fellesMottakTjeneste, never()).ferdigstillOgOppdaterEksterneSystemer(any(), any());
        verify(personTjeneste, never()).hentPersonInfoFraAktørId(any(), any());
    }

    @Test
    void kontrollerInntektsmeldingEtterNedetid_skal_ferdigstille_når_inntekt_er_gyldig() {
        var inntektsmeldingId = 123L;
        var imUuid = UUID.randomUUID();
        var forespørselDto = lagForespørselDtoMedSkjæringstidspunkt(UUID.randomUUID(), ForespørselStatus.UNDER_BEHANDLING);
        var inntektsmelding = lagInntektsmeldingDtoMedForespørsel(imUuid, forespørselDto, false);
        var inntektsopplysninger = lagInntektsopplysninger(BigDecimal.valueOf(45000));

        when(inntektsmeldingTjeneste.hentInntektsmelding(inntektsmeldingId)).thenReturn(inntektsmelding);
        when(personTjeneste.hentPersonInfoFraAktørId(any(), any())).thenReturn(lagPersonInfo());
        when(fellesGrunnlagTjeneste.harJobbetHeleBeregningsperioden(any(), any(), any())).thenReturn(false);
        when(inntektTjeneste.hentInntekt(any(), any(), any(), any(), eq(false))).thenReturn(inntektsopplysninger);

        inntektKontrollTjeneste.kontrollerInntektsmeldingEtterNedetid(inntektsmeldingId);

        verify(inntektsmeldingTjeneste).oppdatertStatusTilInntektsmelding(imUuid, InntektsmeldingStatus.GODKJENT);
        verify(fellesMottakTjeneste).opprettTaskForSendTilJoark(inntektsmeldingId, forespørselDto);
        verify(fellesMottakTjeneste).ferdigstillOgOppdaterEksterneSystemer(forespørselDto, Optional.of(imUuid));
    }

    @Test
    void kontrollerInntektsmeldingEtterNedetid_skal_ikke_ferdigstille_når_inntekt_er_ugyldig() {
        var inntektsmeldingId = 123L;
        var imUuid = UUID.randomUUID();
        var forespørselDto = lagForespørselDtoMedSkjæringstidspunkt(UUID.randomUUID(), ForespørselStatus.UNDER_BEHANDLING);
        var inntektsmelding = lagInntektsmeldingDtoMedForespørsel(imUuid, forespørselDto, false);
        var inntektsopplysninger = lagInntektsopplysninger(BigDecimal.valueOf(46000));

        when(inntektsmeldingTjeneste.hentInntektsmelding(inntektsmeldingId)).thenReturn(inntektsmelding);
        when(personTjeneste.hentPersonInfoFraAktørId(any(), any())).thenReturn(lagPersonInfo());
        when(fellesGrunnlagTjeneste.harJobbetHeleBeregningsperioden(any(), any(), any())).thenReturn(false);
        when(inntektTjeneste.hentInntekt(any(), any(), any(), any(), eq(false))).thenReturn(inntektsopplysninger);

        inntektKontrollTjeneste.kontrollerInntektsmeldingEtterNedetid(inntektsmeldingId);

        verify(fellesMottakTjeneste, never()).opprettTaskForSendTilJoark(any(), any());
        verify(fellesMottakTjeneste, never()).ferdigstillOgOppdaterEksterneSystemer(any(), any());
    }

    @Test
    void kontrollerInntektsmeldingEtterNedetid_skal_sette_status_GODKJENT_og_ferdigstille() {
        var inntektsmeldingId = 123L;
        var imUuid = UUID.randomUUID();
        var forespørselDto = lagForespørselDtoMedSkjæringstidspunkt(UUID.randomUUID(), ForespørselStatus.UNDER_BEHANDLING);
        var inntektsmelding = lagInntektsmeldingDtoMedForespørsel(imUuid, forespørselDto, false);
        var inntektsopplysninger = lagInntektsopplysninger(BigDecimal.valueOf(45000));

        when(inntektsmeldingTjeneste.hentInntektsmelding(inntektsmeldingId)).thenReturn(inntektsmelding);
        when(personTjeneste.hentPersonInfoFraAktørId(any(), any())).thenReturn(lagPersonInfo());
        when(fellesGrunnlagTjeneste.harJobbetHeleBeregningsperioden(any(), any(), any())).thenReturn(false);
        when(inntektTjeneste.hentInntekt(any(), any(), any(), any(), eq(false))).thenReturn(inntektsopplysninger);

        inntektKontrollTjeneste.kontrollerInntektsmeldingEtterNedetid(inntektsmeldingId);

        verify(inntektsmeldingTjeneste).oppdatertStatusTilInntektsmelding(imUuid, InntektsmeldingStatus.GODKJENT);
        verify(fellesMottakTjeneste).opprettTaskForSendTilJoark(inntektsmeldingId, forespørselDto);
        verify(fellesMottakTjeneste).ferdigstillOgOppdaterEksterneSystemer(forespørselDto, Optional.of(imUuid));
    }

    @Test
    void kontrollerInntektsmeldingEtterNedetid_skal_sette_status_AVVIST_og_sende_melding_til_arbeidsgiver() {
        var inntektsmeldingId = 123L;
        var imUuid = UUID.randomUUID();
        var forespørselDto = lagForespørselDtoMedSkjæringstidspunkt(UUID.randomUUID(), ForespørselStatus.UNDER_BEHANDLING);
        // inntekt=45000, gjennomsnitt=46000 -> avvik=1000 > 50 og ingen årsak -> AVVIST
        var inntektsmelding = lagInntektsmeldingDtoMedForespørsel(imUuid, forespørselDto, false);
        var inntektsopplysninger = lagInntektsopplysninger(BigDecimal.valueOf(46000));

        when(inntektsmeldingTjeneste.hentInntektsmelding(inntektsmeldingId)).thenReturn(inntektsmelding);
        when(personTjeneste.hentPersonInfoFraAktørId(any(), any())).thenReturn(lagPersonInfo());
        when(fellesGrunnlagTjeneste.harJobbetHeleBeregningsperioden(any(), any(), any())).thenReturn(false);
        when(inntektTjeneste.hentInntekt(any(), any(), any(), any(), eq(false))).thenReturn(inntektsopplysninger);

        inntektKontrollTjeneste.kontrollerInntektsmeldingEtterNedetid(inntektsmeldingId);

        verify(inntektsmeldingTjeneste).oppdatertStatusTilInntektsmelding(imUuid, InntektsmeldingStatus.AVVIST);
        verify(forespørselBehandlingTjeneste).sendMeldingOmAvvistInntektsmelding(eq(forespørselDto), any());
        verify(fellesMottakTjeneste, never()).opprettTaskForSendTilJoark(any(), any());
        verify(fellesMottakTjeneste, never()).ferdigstillOgOppdaterEksterneSystemer(any(), any());
    }

    private static Inntektsopplysninger lagInntektsopplysninger(BigDecimal gjennomsnitt) {
        return new Inntektsopplysninger(gjennomsnitt, ORGNR, List.of(
            new Inntektsopplysninger.InntektMåned(gjennomsnitt, YearMonth.of(2026, Month.JANUARY), MånedslønnStatus.BRUKT_I_GJENNOMSNITT)));
    }

    private static ForespørselDto lagForespørselDtoMedSkjæringstidspunkt(UUID uuid, ForespørselStatus status) {
        var startdato = LocalDate.of(2026, Month.JANUARY, 10);
        return ForespørselDto.builder()
            .uuid(uuid)
            .arbeidsgiver(Arbeidsgiver.fra(ORGNR))
            .aktørId(AktørId.fra(AKTØR_ID))
            .ytelseType(Ytelsetype.FORELDREPENGER)
            .status(status)
            .forespørselType(ForespørselType.BESTILT_AV_FAGSYSTEM)
            .skjæringstidspunkt(startdato)
            .førsteUttaksdato(startdato)
            .build();
    }

    private static InntektsmeldingDto lagInntektsmeldingDto(LocalDate startdato, boolean skalHaEndringsårsak) {
        var builder = InntektsmeldingDto.builder()
            .medAktørId(AktørId.fra(AKTØR_ID))
            .medArbeidsgiver(Arbeidsgiver.fra(ORGNR))
            .medStartdato(startdato)
            .medYtelse(Ytelsetype.FORELDREPENGER)
            .medKontaktperson(new InntektsmeldingDto.Kontaktperson("12345678", "Kontakt Person"))
            .medInntekt(BigDecimal.valueOf(45000))
            .medMånedRefusjon(BigDecimal.valueOf(45000))
            .medOpphørsdatoRefusjon(startdato.plusDays(9))
            .medSøkteRefusjonsperioder(List.of(new InntektsmeldingDto.SøktRefusjon(startdato.plusDays(5), BigDecimal.valueOf(9000))))
            .medBortfaltNaturalytelsePerioder(List.of(
                new InntektsmeldingDto.BortfaltNaturalytelse(startdato.plusDays(2), Tid.TIDENES_ENDE, NaturalytelseType.BIL, BigDecimal.valueOf(1200))
            ))
            .medKildesystem(Kildesystem.LØNN_OG_PERSONAL_SYSTEM)
            .medAvsenderSystem(new InntektsmeldingDto.AvsenderSystem("test-lps", "1.0.0"))
            .medStatus(InntektsmeldingStatus.GODKJENT);
        if (skalHaEndringsårsak) {
            builder.medEndringAvInntektÅrsaker(List.of(
                new InntektsmeldingDto.Endringsårsak(EndringsårsakType.TARIFFENDRING, null, null, startdato.plusDays(1))
            ));
        }
        return builder.build();
    }

    private static InntektsmeldingDto lagInntektsmeldingDtoMedForespørsel(UUID imUuid, ForespørselDto forespørselDto, boolean skalHaEndringsårsak) {
        var startdato = forespørselDto.førsteUttaksdato();
        var builder = InntektsmeldingDto.builder()
            .medId(123L)
            .medAktørId(AktørId.fra(AKTØR_ID))
            .medArbeidsgiver(Arbeidsgiver.fra(ORGNR))
            .medStartdato(startdato)
            .medYtelse(Ytelsetype.FORELDREPENGER)
            .medKontaktperson(new InntektsmeldingDto.Kontaktperson("12345678", "Kontakt Person"))
            .medInntekt(BigDecimal.valueOf(45000))
            .medMånedRefusjon(BigDecimal.valueOf(45000))
            .medOpphørsdatoRefusjon(startdato.plusDays(9))
            .medSøkteRefusjonsperioder(List.of(new InntektsmeldingDto.SøktRefusjon(startdato.plusDays(5), BigDecimal.valueOf(9000))))
            .medBortfaltNaturalytelsePerioder(List.of(
                new InntektsmeldingDto.BortfaltNaturalytelse(startdato.plusDays(2), Tid.TIDENES_ENDE, NaturalytelseType.BIL, BigDecimal.valueOf(1200))
            ))
            .medKildesystem(Kildesystem.LØNN_OG_PERSONAL_SYSTEM)
            .medAvsenderSystem(new InntektsmeldingDto.AvsenderSystem("test-lps", "1.0.0"))
            .medForespørsel(forespørselDto);
        if (skalHaEndringsårsak) {
            builder.medEndringAvInntektÅrsaker(List.of(
                new InntektsmeldingDto.Endringsårsak(EndringsårsakType.TARIFFENDRING, null, null, startdato.plusDays(1))
            ));
        }
        if (imUuid != null) {
            builder.medInntektsmeldingUuid(imUuid);
        }
        return builder.build();
    }

    private static PersonInfo lagPersonInfo() {
        return new PersonInfo("Ola", null, "Nordmann",
            new PersonIdent("12345678901"),
            AktørId.fra(AKTØR_ID),
            LocalDate.of(1990, Month.JANUARY, 1), null, null);
    }
}
