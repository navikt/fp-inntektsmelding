package no.nav.foreldrepenger.inntektsmelding.integrasjoner.dokgen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.dokgen.v1.FpDokgenRequest;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.dokgen.v1.FpDokgenRestKlient;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.organisasjon.Organisasjon;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;

class DokumentGeneratorTjenesteTest {

    private static final String ORGNR = "999999999";
    private static final String AKTØR_ID = "1234567891011";
    private static final AktørId AKTØR_ID_OBJ = new AktørId(AKTØR_ID);
    private static final LocalDate STARTDATO = LocalDate.of(2026, 1, 10);

    private FpDokgenRestKlient dokgenKlient;
    private PersonTjeneste personTjeneste;
    private OrganisasjonTjeneste organisasjonTjeneste;
    private DokumentGeneratorTjeneste tjeneste;

    @BeforeEach
    void setUp() {
        dokgenKlient = mock(FpDokgenRestKlient.class);
        personTjeneste = mock(PersonTjeneste.class);
        organisasjonTjeneste = mock(OrganisasjonTjeneste.class);
        tjeneste = new DokumentGeneratorTjeneste(dokgenKlient, personTjeneste, organisasjonTjeneste);
    }

    @Test
    void skal_generere_pdf_for_vanlig_inntektsmelding_med_organisasjon() {
        var imUuid = UUID.randomUUID();
        var forespørselUuid = UUID.randomUUID();
        var personInfo = lagPersonInfo();
        var pdfBytes = new byte[]{1, 2, 3, 4};

        when(personTjeneste.hentPersonInfoFraAktørId(AKTØR_ID_OBJ, Ytelsetype.FORELDREPENGER)).thenReturn(personInfo);
        when(organisasjonTjeneste.finnOrganisasjon(Arbeidsgiver.fra(ORGNR))).thenReturn(new Organisasjon("Test AS", ORGNR));
        when(dokgenKlient.genererPdf(any(FpDokgenRequest.class))).thenReturn(pdfBytes);

        var inntektsmelding = lagInntektsmeldingBuilder(imUuid, forespørselUuid, ForespørselType.BESTILT_AV_FAGSYSTEM).build();

        var resultat = tjeneste.mapDataOgGenererPdf(inntektsmelding);

        assertThat(resultat).isEqualTo(pdfBytes);
    }

    private PersonInfo lagPersonInfo() {
        return new PersonInfo("Test", "Tester", "Testesen", new PersonIdent("11111111111"), AKTØR_ID_OBJ, LocalDate.of(1990, 5, 15), null, null);
    }

    private InntektsmeldingDto.Builder lagInntektsmeldingBuilder(UUID imUuid, UUID forespørselUuid, ForespørselType forespørselType) {
        var forespørsel = ForespørselDto.builder()
            .uuid(forespørselUuid)
            .arbeidsgiver(Arbeidsgiver.fra(ORGNR))
            .aktørId(AKTØR_ID_OBJ)
            .ytelseType(Ytelsetype.FORELDREPENGER)
            .status(ForespørselStatus.UNDER_BEHANDLING)
            .forespørselType(forespørselType)
            .skjæringstidspunkt(STARTDATO)
            .førsteUttaksdato(STARTDATO)
            .build();

        return InntektsmeldingDto.builder()
            .medInntektsmeldingUuid(imUuid)
            .medAktørId(AKTØR_ID_OBJ)
            .medYtelse(Ytelsetype.FORELDREPENGER)
            .medArbeidsgiver(Arbeidsgiver.fra(ORGNR))
            .medKontaktperson(new InntektsmeldingDto.Kontaktperson("99999999", "Kontaktesen"))
            .medStartdato(STARTDATO)
            .medInntekt(BigDecimal.valueOf(40000))
            .medInnsendtTidspunkt(LocalDateTime.now())
            .medMånedRefusjon(BigDecimal.valueOf(35000))
            .medSøkteRefusjonsperioder(List.of())
            .medBortfaltNaturalytelsePerioder(List.of())
            .medEndringAvInntektÅrsaker(List.of())
            .medForespørsel(forespørsel);
    }
}
