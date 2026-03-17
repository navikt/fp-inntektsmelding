package no.nav.foreldrepenger.inntektsmelding.inntektsmelding.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Kildesystem;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.NaturalytelseType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
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
        var aktørIdSøker = new no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId("1234567891234");
        var fnrSøker = new PersonIdent("11111111111");

        var inntektsmelding = InntektsmeldingDto.builder()
            .medAktørId(aktørIdSøker)
            .medArbeidsgiver(new Arbeidsgiver("999999999"))
            .medStartdato(LocalDate.of(2024, 6, 1))
            .medYtelse(Ytelsetype.FORELDREPENGER)
            .medInntekt(BigDecimal.valueOf(35000))
            .medMånedRefusjon(BigDecimal.valueOf(35000))
            .medOpphørsdatoRefusjon(Tid.TIDENES_ENDE)
            .medInnsendtTidspunkt(opprettetTidspunkt)
            .medKontaktperson(new InntektsmeldingDto.Kontaktperson("111111111", "Test Testen"))
            .medKildesystem(Kildesystem.ARBEIDSGIVERPORTAL)
            .medSøkteRefusjonsperioder(List.of())
            .medBortfaltNaturalytelsePerioder(Collections.singletonList(
                new InntektsmeldingDto.BortfaltNaturalytelse(
                    LocalDate.of(2024, 6, 10), Tid.TIDENES_ENDE,
                    NaturalytelseType.AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS,
                    BigDecimal.valueOf(2000))))
            .build();

        when(personTjeneste.finnPersonIdentForAktørId(new no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId(aktørIdSøker.getAktørId()))).thenReturn(fnrSøker);

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
        var aktørIdSøker = new AktørId("1234567891234");
        var fnrSøker = new PersonIdent("11111111111");

        var inntektsmelding = InntektsmeldingDto.builder()
            .medAktørId(aktørIdSøker)
            .medArbeidsgiver(new Arbeidsgiver("999999999"))
            .medStartdato(LocalDate.of(2024, 6, 1))
            .medYtelse(Ytelsetype.SVANGERSKAPSPENGER)
            .medInntekt(BigDecimal.valueOf(35000))
            .medMånedRefusjon(BigDecimal.valueOf(35000))
            .medOpphørsdatoRefusjon(Tid.TIDENES_ENDE)
            .medInnsendtTidspunkt(opprettetTidspunkt)
            .medKontaktperson(new InntektsmeldingDto.Kontaktperson("111111111", "Test Testen"))
            .medKildesystem(Kildesystem.ARBEIDSGIVERPORTAL)
            .medSøkteRefusjonsperioder(List.of())
            .medBortfaltNaturalytelsePerioder(Collections.singletonList(
                new InntektsmeldingDto.BortfaltNaturalytelse(
                    LocalDate.of(2024, 6, 10), Tid.TIDENES_ENDE,
                    NaturalytelseType.AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS,
                    BigDecimal.valueOf(2000))))
            .build();

        when(personTjeneste.finnPersonIdentForAktørId(new no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId(aktørIdSøker.getAktørId()))).thenReturn(fnrSøker);

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
