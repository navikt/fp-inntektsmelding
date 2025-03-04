package no.nav.familie.inntektsmelding.imdialog.tjenester;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.imdialog.rest.InntektsmeldingDialogDto;
import no.nav.familie.inntektsmelding.integrasjoner.aareg.Arbeidsforhold;
import no.nav.familie.inntektsmelding.integrasjoner.aareg.ArbeidstakerTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.InntektTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.Inntektsopplysninger;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.Organisasjon;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.MånedslønnStatus;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.kontekst.RequestKontekst;
import no.nav.vedtak.sikkerhet.oidc.config.OpenIDProvider;
import no.nav.vedtak.sikkerhet.oidc.token.OpenIDToken;
import no.nav.vedtak.sikkerhet.oidc.token.TokenString;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrunnlagDtoTjenesteTest {
    private static final String INNMELDER_UID = "12324312345";

    @Mock
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    @Mock
    private PersonTjeneste personTjeneste;
    @Mock
    private OrganisasjonTjeneste organisasjonTjeneste;
    @Mock
    private InntektTjeneste inntektTjeneste;
    @Mock
    private ArbeidstakerTjeneste arbeidstakerTjeneste;

    private GrunnlagDtoTjeneste grunnlagDtoTjeneste;

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
        grunnlagDtoTjeneste = new GrunnlagDtoTjeneste(forespørselBehandlingTjeneste, personTjeneste,
            organisasjonTjeneste, inntektTjeneste, arbeidstakerTjeneste);
    }

    @Test
    void skal_lage_dto() {
        // Arrange
        var uuid = UUID.randomUUID();
        var forespørsel = new ForespørselEntitet("999999999",
            LocalDate.now(),
            new AktørIdEntitet("9999999999999"),
            Ytelsetype.FORELDREPENGER,
            "123",
            LocalDate.now(),
            ForespørselType.BESTILT_AV_FAGSYSTEM);
        when(forespørselBehandlingTjeneste.hentForespørsel(uuid)).thenReturn(Optional.of(forespørsel));
        when(organisasjonTjeneste.finnOrganisasjon(forespørsel.getOrganisasjonsnummer())).thenReturn(
            new Organisasjon("Bedriften", forespørsel.getOrganisasjonsnummer()));
        when(personTjeneste.hentPersonInfoFraAktørId(forespørsel.getAktørId(), forespørsel.getYtelseType())).thenReturn(
            new PersonInfo("Navn", null, "Navnesen", new PersonIdent("12121212122"), forespørsel.getAktørId(), LocalDate.now(), null, null));
        var innsenderNavn = "Ine";
        var innsenderEtternavn = "Sender";
        var innsenderTelefonnummer = "+4711111111";
        when(personTjeneste.hentPersonFraIdent(PersonIdent.fra(INNMELDER_UID), forespørsel.getYtelseType())).thenReturn(
            new PersonInfo(innsenderNavn, null, innsenderEtternavn, new PersonIdent(INNMELDER_UID), null, LocalDate.now(), innsenderTelefonnummer, null));
        var inntekt1 = new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(52000), YearMonth.of(2024, 3), MånedslønnStatus.BRUKT_I_GJENNOMSNITT);
        var inntekt2 = new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(52000), YearMonth.of(2024, 4), MånedslønnStatus.BRUKT_I_GJENNOMSNITT);
        var inntekt3 = new Inntektsopplysninger.InntektMåned(BigDecimal.valueOf(52000), YearMonth.of(2024, 5), MånedslønnStatus.BRUKT_I_GJENNOMSNITT);
        when(inntektTjeneste.hentInntekt(forespørsel.getAktørId(), LocalDate.now(), LocalDate.now(),
            forespørsel.getOrganisasjonsnummer())).thenReturn(new Inntektsopplysninger(BigDecimal.valueOf(52000),
            forespørsel.getOrganisasjonsnummer(),
            List.of(inntekt1, inntekt2, inntekt3)));

        // Act
        var imDialogDto = grunnlagDtoTjeneste.lagDialogDto(uuid);

        // Assert
        assertThat(imDialogDto.skjæringstidspunkt()).isEqualTo(forespørsel.getSkjæringstidspunkt().orElse(null));
        assertThat(imDialogDto.ytelse()).isEqualTo(YtelseTypeDto.FORELDREPENGER);

        assertThat(imDialogDto.person().aktørId()).isEqualTo(forespørsel.getAktørId().getAktørId());
        assertThat(imDialogDto.person().fornavn()).isEqualTo("Navn");
        assertThat(imDialogDto.person().etternavn()).isEqualTo("Navnesen");

        assertThat(imDialogDto.arbeidsgiver().organisasjonNavn()).isEqualTo("Bedriften");
        assertThat(imDialogDto.arbeidsgiver().organisasjonNummer()).isEqualTo(forespørsel.getOrganisasjonsnummer());

        assertThat(imDialogDto.førsteUttaksdato()).isEqualTo(LocalDate.now());

        assertThat(imDialogDto.innsender().fornavn()).isEqualTo(innsenderNavn);
        assertThat(imDialogDto.innsender().etternavn()).isEqualTo(innsenderEtternavn);
        assertThat(imDialogDto.innsender().mellomnavn()).isNull();
        assertThat(imDialogDto.innsender().telefon()).isEqualTo(innsenderTelefonnummer);

        assertThat(imDialogDto.inntektsopplysninger().månedsinntekter()).hasSize(3);
        assertThat(imDialogDto.inntektsopplysninger().gjennomsnittLønn()).isEqualByComparingTo(BigDecimal.valueOf(52_000));
        assertThat(imDialogDto.inntektsopplysninger().månedsinntekter()).contains(
            new InntektsmeldingDialogDto.InntektsopplysningerDto.MånedsinntektDto(LocalDate.of(2024, 3, 1),
                LocalDate.of(2024, 3, 31),
                BigDecimal.valueOf(52_000),
                MånedslønnStatus.BRUKT_I_GJENNOMSNITT));
        assertThat(imDialogDto.inntektsopplysninger().månedsinntekter()).contains(
            new InntektsmeldingDialogDto.InntektsopplysningerDto.MånedsinntektDto(LocalDate.of(2024, 4, 1),
                LocalDate.of(2024, 4, 30),
                BigDecimal.valueOf(52_000),
                MånedslønnStatus.BRUKT_I_GJENNOMSNITT));
        assertThat(imDialogDto.inntektsopplysninger().månedsinntekter()).contains(
            new InntektsmeldingDialogDto.InntektsopplysningerDto.MånedsinntektDto(LocalDate.of(2024, 5, 1),
                LocalDate.of(2024, 5, 31),
                BigDecimal.valueOf(52_000),
                MånedslønnStatus.BRUKT_I_GJENNOMSNITT));
    }

    @Test
    void skal_lage_dto_med_første_uttaksdato() {
        // Arrange
        var uuid = UUID.randomUUID();
        var forespørsel = new ForespørselEntitet("999999999",
            LocalDate.now(),
            new AktørIdEntitet("9999999999999"),
            Ytelsetype.FORELDREPENGER,
            "123",
            LocalDate.now().plusDays(10), ForespørselType.BESTILT_AV_FAGSYSTEM);
        when(forespørselBehandlingTjeneste.hentForespørsel(uuid)).thenReturn(Optional.of(forespørsel));
        when(organisasjonTjeneste.finnOrganisasjon(forespørsel.getOrganisasjonsnummer())).thenReturn(
            new Organisasjon("Bedriften", forespørsel.getOrganisasjonsnummer()));
        when(personTjeneste.hentPersonInfoFraAktørId(forespørsel.getAktørId(), forespørsel.getYtelseType())).thenReturn(
            new PersonInfo("Navn", null, "Navnesen", new PersonIdent("12121212122"), forespørsel.getAktørId(), LocalDate.now(), null, null));
        var innsenderNavn = "Ine";
        var innsenderEtternavn = "Sender";
        var innsenderTelefonnummer = "+4711111111";
        when(personTjeneste.hentPersonFraIdent(PersonIdent.fra(INNMELDER_UID), forespørsel.getYtelseType())).thenReturn(
            new PersonInfo(innsenderNavn, null, innsenderEtternavn, new PersonIdent(INNMELDER_UID), null, LocalDate.now(), innsenderTelefonnummer, null));
        when(inntektTjeneste.hentInntekt(forespørsel.getAktørId(), LocalDate.now(), LocalDate.now(),
            forespørsel.getOrganisasjonsnummer())).thenReturn(new Inntektsopplysninger(BigDecimal.valueOf(52000),
            forespørsel.getOrganisasjonsnummer(),
            List.of()));

        // Act
        var imDialogDto = grunnlagDtoTjeneste.lagDialogDto(uuid);

        // Assert
        assertThat(imDialogDto.skjæringstidspunkt()).isEqualTo(forespørsel.getSkjæringstidspunkt().orElse(null));
        assertThat(imDialogDto.ytelse()).isEqualTo(YtelseTypeDto.FORELDREPENGER);

        assertThat(imDialogDto.person().aktørId()).isEqualTo(forespørsel.getAktørId().getAktørId());
        assertThat(imDialogDto.person().fornavn()).isEqualTo("Navn");
        assertThat(imDialogDto.person().etternavn()).isEqualTo("Navnesen");

        assertThat(imDialogDto.arbeidsgiver().organisasjonNavn()).isEqualTo("Bedriften");
        assertThat(imDialogDto.arbeidsgiver().organisasjonNummer()).isEqualTo(forespørsel.getOrganisasjonsnummer());

        assertThat(imDialogDto.førsteUttaksdato()).isEqualTo(LocalDate.now().plusDays(10));

        assertThat(imDialogDto.innsender().fornavn()).isEqualTo(innsenderNavn);
        assertThat(imDialogDto.innsender().etternavn()).isEqualTo(innsenderEtternavn);
        assertThat(imDialogDto.innsender().mellomnavn()).isNull();
        assertThat(imDialogDto.innsender().telefon()).isEqualTo(innsenderTelefonnummer);
    }

    @Test
    void skal_hente_arbeidsforhold_gitt_fnr() {
        // Arrange
        var fnr = new PersonIdent("11111111111");
        var førsteFraværsdag = LocalDate.now();
        var aktørId = new AktørIdEntitet("9999999999999");
        var personInfo = new PersonInfo("Navn", null, "Navnesen", fnr, aktørId, LocalDate.now(), null, null);
        var orgnr = "999999999";
        when(arbeidstakerTjeneste.finnArbeidsforholdInnsenderHarTilgangTil(fnr, førsteFraværsdag)).thenReturn(List.of(new Arbeidsforhold(orgnr,
            "ARB-001")));
        when(organisasjonTjeneste.finnOrganisasjon(orgnr)).thenReturn(new Organisasjon("Bedriften", orgnr));
        // Act
        var response = grunnlagDtoTjeneste.finnArbeidsforholdForFnr(personInfo, førsteFraværsdag).orElse(null);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.fornavn()).isEqualTo("Navn");
        assertThat(response.etternavn()).isEqualTo("Navnesen");
        assertThat(response.arbeidsforhold()).hasSize(1);
        assertThat(response.arbeidsforhold().stream().toList().getFirst().organisasjonsnavn()).isEqualTo("Bedriften");
        assertThat(response.arbeidsforhold().stream().toList().getFirst().organisasjonsnummer()).isEqualTo(orgnr);
        assertThat(response.kjønn()).isNull();
    }

    @Test
    void skal_ikke_få_lov_å_sende_inn_hvis_ingen_eksisterende_forespørsler_for_personen_finnes() {
        // Arrange
        var fødselsnummer = new PersonIdent("11111111111");
        var ytelsetype = Ytelsetype.FORELDREPENGER;
        var eksForespørselDato = LocalDate.now().minusYears(4);
        var førsteFraværsdag = LocalDate.now();
        var organisasjonsnummer = new OrganisasjonsnummerDto("999999999");
        var aktørId = new AktørIdEntitet("9999999999999");
        var forespørsel = new ForespørselEntitet("999999998",
            eksForespørselDato,
            aktørId,
            ytelsetype,
            "123",
            eksForespørselDato, ForespørselType.BESTILT_AV_FAGSYSTEM);
        var personInfo = new PersonInfo("Navn", null, "Navnesen", fødselsnummer, aktørId, LocalDate.now(), null, PersonInfo.Kjønn.MANN);
        when(personTjeneste.hentPersonFraIdent(fødselsnummer, ytelsetype)).thenReturn(personInfo);
        when(forespørselBehandlingTjeneste.finnForespørslerForAktørId(aktørId, ytelsetype)).thenReturn(List.of(forespørsel));

        var ex = assertThrows(FunksjonellException.class, () -> grunnlagDtoTjeneste.lagArbeidsgiverinitiertDialogDto(fødselsnummer,
            ytelsetype,
            førsteFraværsdag,
            organisasjonsnummer));

        assertThat(ex.getMessage()).contains("INGEN_SAK_FUNNET");
    }

    @Test
    void skal_ikke_få_lov_å_sende_inn_hvis_ingen_eksisterende_forespørsler_før_fraværsdato_finnes() {
        // Arrange
        var fødselsnummer = new PersonIdent("11111111111");
        var ytelsetype = Ytelsetype.FORELDREPENGER;
        var eksForespørselDato = LocalDate.now().plusDays(10);
        var førsteFraværsdag = LocalDate.now();
        var organisasjonsnummer = new OrganisasjonsnummerDto("999999999");
        var aktørId = new AktørIdEntitet("9999999999999");
        var forespørsel = new ForespørselEntitet("999999998",
            eksForespørselDato,
            aktørId,
            ytelsetype,
            "123",
            eksForespørselDato, ForespørselType.BESTILT_AV_FAGSYSTEM);
        var personInfo = new PersonInfo("Navn", null, "Navnesen", fødselsnummer, aktørId, LocalDate.now(), null, PersonInfo.Kjønn.MANN);
        when(personTjeneste.hentPersonFraIdent(fødselsnummer, ytelsetype)).thenReturn(personInfo);
        when(forespørselBehandlingTjeneste.finnForespørslerForAktørId(aktørId, ytelsetype)).thenReturn(List.of(forespørsel));

        var ex = assertThrows(FunksjonellException.class, () -> grunnlagDtoTjeneste.lagArbeidsgiverinitiertDialogDto(fødselsnummer,
            ytelsetype,
            førsteFraværsdag,
            organisasjonsnummer));

        assertThat(ex.getMessage()).contains("INGEN_SAK_FUNNET");
    }

    @Test
    void skal_gi_arbeidsgiverinitiertdto_hvis_ingen_matchende_forespørsler_finnes() {
        // Arrange
        var fødselsnummer = new PersonIdent("11111111111");
        var ytelsetype = Ytelsetype.FORELDREPENGER;
        var eksForespørselDato = LocalDate.now().minusYears(1);
        var førsteFraværsdag = LocalDate.now();
        var organisasjonsnummer = new OrganisasjonsnummerDto("999999999");
        var aktørId = new AktørIdEntitet("9999999999999");
        var forespørsel = new ForespørselEntitet("999999998",
            eksForespørselDato,
            aktørId,
            ytelsetype,
            "123",
            eksForespørselDato, ForespørselType.BESTILT_AV_FAGSYSTEM);
        var personInfo = new PersonInfo("Navn", null, "Navnesen", fødselsnummer, aktørId, LocalDate.now(), null, PersonInfo.Kjønn.MANN);
        when(personTjeneste.hentPersonFraIdent(fødselsnummer, ytelsetype)).thenReturn(personInfo);
        when(personTjeneste.hentPersonFraIdent(PersonIdent.fra(INNMELDER_UID), ytelsetype)).thenReturn(
            new PersonInfo("Ine", null, "Sender", new PersonIdent(INNMELDER_UID), null, LocalDate.now(), "+4711111111", null));
        when(forespørselBehandlingTjeneste.finnForespørslerForAktørId(aktørId, ytelsetype)).thenReturn(List.of(forespørsel));
        when(organisasjonTjeneste.finnOrganisasjon(organisasjonsnummer.orgnr())).thenReturn(new Organisasjon("Bedriften",
            organisasjonsnummer.orgnr()));
        when(inntektTjeneste.hentInntekt(aktørId,
            førsteFraværsdag,
            LocalDate.now(),
            organisasjonsnummer.orgnr())).thenReturn(new Inntektsopplysninger(BigDecimal.valueOf(52000), organisasjonsnummer.orgnr(), List.of()));
        // Act
        var imDialogDto = grunnlagDtoTjeneste.lagArbeidsgiverinitiertDialogDto(fødselsnummer,
            ytelsetype,
            førsteFraværsdag,
            organisasjonsnummer);

        // Assert
        assertThat(imDialogDto.person().aktørId()).isEqualTo(aktørId.getAktørId());
        assertThat(imDialogDto.person().fornavn()).isEqualTo("Navn");
        assertThat(imDialogDto.person().etternavn()).isEqualTo("Navnesen");
        assertThat(imDialogDto.arbeidsgiver().organisasjonNavn()).isEqualTo("Bedriften");
        assertThat(imDialogDto.arbeidsgiver().organisasjonNummer()).isEqualTo(organisasjonsnummer.orgnr());
        assertThat(imDialogDto.førsteUttaksdato()).isEqualTo(førsteFraværsdag);
        assertThat(imDialogDto.inntektsopplysninger().gjennomsnittLønn()).isEqualByComparingTo(BigDecimal.valueOf(52000));
        assertThat(imDialogDto.forespørselUuid()).isNull();
    }

    @Test
    void skal_gi_opplysningerDto_hvis_matchende_forespørsel_finnes() {
        // Arrange
        var fødselsnummer = new PersonIdent("11111111111");
        var ytelsetype = Ytelsetype.FORELDREPENGER;
        var førsteFraværsdag = LocalDate.now();
        var eksFpFørsteUttaksdato = LocalDate.now().minusDays(2);
        var organisasjonsnummer = new OrganisasjonsnummerDto("999999999");
        var aktørId = new AktørIdEntitet("9999999999999");
        var forespørsel = new ForespørselEntitet("999999999", eksFpFørsteUttaksdato, aktørId, ytelsetype, "123", eksFpFørsteUttaksdato, ForespørselType.BESTILT_AV_FAGSYSTEM);
        var personInfo = new PersonInfo("Navn", null, "Navnesen", fødselsnummer, aktørId, LocalDate.now(), null, null);
        when(personTjeneste.hentPersonFraIdent(fødselsnummer, ytelsetype)).thenReturn(personInfo);
        when(personTjeneste.hentPersonInfoFraAktørId(aktørId, ytelsetype)).thenReturn(personInfo);
        when(personTjeneste.hentPersonFraIdent(PersonIdent.fra(INNMELDER_UID), ytelsetype)).thenReturn(
            new PersonInfo("Ine", null, "Sender", new PersonIdent(INNMELDER_UID), null, LocalDate.now(), "+4711111111", PersonInfo.Kjønn.MANN));
        when(forespørselBehandlingTjeneste.finnForespørslerForAktørId(aktørId, ytelsetype)).thenReturn(List.of(forespørsel));
        when(forespørselBehandlingTjeneste.hentForespørsel(forespørsel.getUuid())).thenReturn(Optional.of(forespørsel));
        when(organisasjonTjeneste.finnOrganisasjon(organisasjonsnummer.orgnr())).thenReturn(new Organisasjon("Bedriften",
            organisasjonsnummer.orgnr()));
        when(inntektTjeneste.hentInntekt(aktørId,
            eksFpFørsteUttaksdato,
            LocalDate.now(),
            organisasjonsnummer.orgnr())).thenReturn(new Inntektsopplysninger(BigDecimal.valueOf(52000), organisasjonsnummer.orgnr(), List.of()));
        // Act
        var imDialogDto = grunnlagDtoTjeneste.lagArbeidsgiverinitiertDialogDto(fødselsnummer, ytelsetype, førsteFraværsdag, organisasjonsnummer);

        // Assert
        assertThat(imDialogDto.person().aktørId()).isEqualTo(aktørId.getAktørId());
        assertThat(imDialogDto.person().fornavn()).isEqualTo("Navn");
        assertThat(imDialogDto.person().etternavn()).isEqualTo("Navnesen");
        assertThat(imDialogDto.arbeidsgiver().organisasjonNavn()).isEqualTo("Bedriften");
        assertThat(imDialogDto.arbeidsgiver().organisasjonNummer()).isEqualTo(organisasjonsnummer.orgnr());
        assertThat(imDialogDto.førsteUttaksdato()).isEqualTo(eksFpFørsteUttaksdato);
        assertThat(imDialogDto.inntektsopplysninger().gjennomsnittLønn()).isEqualByComparingTo(BigDecimal.valueOf(52000));
        assertThat(imDialogDto.forespørselUuid()).isEqualTo(forespørsel.getUuid());
    }

}
