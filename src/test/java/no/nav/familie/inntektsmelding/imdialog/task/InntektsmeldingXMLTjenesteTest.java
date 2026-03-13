package no.nav.familie.inntektsmelding.imdialog.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.imdialog.modell.BortaltNaturalytelseEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.KontaktpersonEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.NaturalytelseType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.konfig.Tid;

@ExtendWith(MockitoExtension.class)
class InntektsmeldingXMLTjenesteTest {

    @Mock
    private PersonTjeneste personTjeneste;

    private InntektsmeldingXMLTjeneste inntektsmeldingXMLTjeneste;

    @BeforeEach
    void setUp() {
        inntektsmeldingXMLTjeneste = new InntektsmeldingXMLTjeneste(personTjeneste);
    }

    @Test
    void skal_teste_xml_generering() {
        // Arrange
        var opprettetTidspunkt = LocalDateTime.of(2024, 6, 30, 12, 12, 30);
        var naturalytelse = BortaltNaturalytelseEntitet.builder()
            .medPeriode(LocalDate.of(2024, 6, 10), Tid.TIDENES_ENDE)
            .medType(NaturalytelseType.AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS)
            .medMånedBeløp(BigDecimal.valueOf(2000))
            .build();
        var aktørIdSøker = new AktørIdEntitet("1234567891234");
        var fnrSøker = new PersonIdent("11111111111");
        var inntektsmelding = InntektsmeldingEntitet.builder()
            .medArbeidsgiverIdent("999999999")
            .medStartDato(LocalDate.of(2024, 6, 1))
            .medYtelsetype(Ytelsetype.FORELDREPENGER)
            .medMånedInntekt(BigDecimal.valueOf(35000))
            .medAktørId(aktørIdSøker)
            .medMånedRefusjon(BigDecimal.valueOf(35000))
            .medRefusjonOpphørsdato(Tid.TIDENES_ENDE)
            .medOpprettetTidspunkt(opprettetTidspunkt)
            .medKontaktperson(new KontaktpersonEntitet("Test Testen", "111111111"))
            .medBortfaltNaturalytelser(Collections.singletonList(naturalytelse))
            .build();

        when(personTjeneste.finnPersonIdentForAktørId(new AktørId(aktørIdSøker.getAktørId()))).thenReturn(fnrSøker);

        // Act
        var xml = inntektsmeldingXMLTjeneste.lagXMLAvInntektsmelding(inntektsmelding);

        // Assert
        // Kjører replace slik at vi kan lagre XML i mer lesbart format under
        assertThat(xml).isEqualTo(forventetXmlFp().replaceAll("[\r\n\t]", ""));
    }

    @Test
    void skal_teste_xml_generering_svp() {
        // Arrange
        var opprettetTidspunkt = LocalDateTime.of(2024, 6, 30, 12, 12, 30);
        var naturalytelse = BortaltNaturalytelseEntitet.builder()
            .medPeriode(LocalDate.of(2024, 6, 10), Tid.TIDENES_ENDE)
            .medType(NaturalytelseType.AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS)
            .medMånedBeløp(BigDecimal.valueOf(2000))
            .build();
        var aktørIdSøker = new AktørIdEntitet("1234567891234");
        var fnrSøker = new PersonIdent("11111111111");
        var inntektsmelding = InntektsmeldingEntitet.builder()
            .medArbeidsgiverIdent("999999999")
            .medStartDato(LocalDate.of(2024, 6, 1))
            .medYtelsetype(Ytelsetype.SVANGERSKAPSPENGER)
            .medMånedInntekt(BigDecimal.valueOf(35000))
            .medAktørId(aktørIdSøker)
            .medMånedRefusjon(BigDecimal.valueOf(35000))
            .medRefusjonOpphørsdato(Tid.TIDENES_ENDE)
            .medOpprettetTidspunkt(opprettetTidspunkt)
            .medKontaktperson(new KontaktpersonEntitet("Test Testen", "111111111"))
            .medBortfaltNaturalytelser(Collections.singletonList(naturalytelse))
            .build();

        when(personTjeneste.finnPersonIdentForAktørId(new AktørId(aktørIdSøker.getAktørId()))).thenReturn(fnrSøker);

        // Act
        var xml = inntektsmeldingXMLTjeneste.lagXMLAvInntektsmelding(inntektsmelding);

        // Assert
        // Kjører replace slik at vi kan lagre XML i mer lesbart format under
        assertThat(xml).isEqualTo(forventetXmlSVP().replaceAll("[\r\n\t]", ""));
    }

    private String forventetXmlFp() {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <melding xmlns="http://seres.no/xsd/NAV/Inntektsmelding_M/20181211">
            	<Skjemainnhold>
            		<ytelse>Foreldrepenger</ytelse>
            		<aarsakTilInnsending>Ny</aarsakTilInnsending>
            		<arbeidsgiver>
            			<virksomhetsnummer>999999999</virksomhetsnummer>
            			<kontaktinformasjon>
            				<kontaktinformasjonNavn>Test Testen</kontaktinformasjonNavn>
            				<telefonnummer>111111111</telefonnummer>
            			</kontaktinformasjon>
            		</arbeidsgiver>
            		<arbeidstakerFnr>11111111111</arbeidstakerFnr>
            		<naerRelasjon>false</naerRelasjon>
            		<arbeidsforhold>
            			<foersteFravaersdag>2024-06-01</foersteFravaersdag>
            			<beregnetInntekt>
            				<beloep>35000</beloep>
            			</beregnetInntekt>
            		</arbeidsforhold>
            		<refusjon>
            			<refusjonsbeloepPrMnd>35000</refusjonsbeloepPrMnd>
            			<refusjonsopphoersdato>9999-12-31</refusjonsopphoersdato>
            			<endringIRefusjonListe/>
            		</refusjon>
            		<startdatoForeldrepengeperiode>2024-06-01</startdatoForeldrepengeperiode>
            		<opphoerAvNaturalytelseListe>
            			<opphoerAvNaturalytelse>
            				<naturalytelseType>aksjerGrunnfondsbevisTilUnderkurs</naturalytelseType>
            				<fom>2024-06-10</fom>
            				<beloepPrMnd>2000</beloepPrMnd>
            			</opphoerAvNaturalytelse>
            		</opphoerAvNaturalytelseListe>
            		<gjenopptakelseNaturalytelseListe/>
            		<avsendersystem>
            			<systemnavn>NAV_NO</systemnavn>
            			<systemversjon>1.0</systemversjon>
            			<innsendingstidspunkt>2024-06-30T12:12:30</innsendingstidspunkt>
            		</avsendersystem>
            	</Skjemainnhold>
            </melding>
            """;
    }
    private String forventetXmlSVP() {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <melding xmlns="http://seres.no/xsd/NAV/Inntektsmelding_M/20181211">
            	<Skjemainnhold>
            		<ytelse>Svangerskapspenger</ytelse>
            		<aarsakTilInnsending>Ny</aarsakTilInnsending>
            		<arbeidsgiver>
            			<virksomhetsnummer>999999999</virksomhetsnummer>
            			<kontaktinformasjon>
            				<kontaktinformasjonNavn>Test Testen</kontaktinformasjonNavn>
            				<telefonnummer>111111111</telefonnummer>
            			</kontaktinformasjon>
            		</arbeidsgiver>
            		<arbeidstakerFnr>11111111111</arbeidstakerFnr>
            		<naerRelasjon>false</naerRelasjon>
            		<arbeidsforhold>
            			<foersteFravaersdag>2024-06-01</foersteFravaersdag>
            			<beregnetInntekt>
            				<beloep>35000</beloep>
            			</beregnetInntekt>
            		</arbeidsforhold>
            		<refusjon>
            			<refusjonsbeloepPrMnd>35000</refusjonsbeloepPrMnd>
            			<refusjonsopphoersdato>9999-12-31</refusjonsopphoersdato>
            			<endringIRefusjonListe/>
            		</refusjon>
            		<opphoerAvNaturalytelseListe>
            			<opphoerAvNaturalytelse>
            				<naturalytelseType>aksjerGrunnfondsbevisTilUnderkurs</naturalytelseType>
            				<fom>2024-06-10</fom>
            				<beloepPrMnd>2000</beloepPrMnd>
            			</opphoerAvNaturalytelse>
            		</opphoerAvNaturalytelseListe>
            		<gjenopptakelseNaturalytelseListe/>
            		<avsendersystem>
            			<systemnavn>NAV_NO</systemnavn>
            			<systemversjon>1.0</systemversjon>
            			<innsendingstidspunkt>2024-06-30T12:12:30</innsendingstidspunkt>
            		</avsendersystem>
            	</Skjemainnhold>
            </melding>
            """;
    }

}
