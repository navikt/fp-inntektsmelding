package no.nav.familie.inntektsmelding.integrasjoner.aareg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import no.nav.familie.inntektsmelding.integrasjoner.aareg.dto.AnsettelsesperiodeDto;

import no.nav.familie.inntektsmelding.integrasjoner.aareg.dto.PeriodeDto;

import no.nav.vedtak.konfig.Tid;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.integrasjoner.aareg.dto.ArbeidsforholdDto;
import no.nav.familie.inntektsmelding.integrasjoner.aareg.dto.OpplysningspliktigArbeidsgiverDto;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;

@ExtendWith(MockitoExtension.class)
class ArbeidsforholdTjenesteTest {

    @Mock
    private AaregRestKlient aaregRestKlient;

    private ArbeidsforholdTjeneste arbeidsforholdTjeneste;

    private static final PersonIdent PERSON_IDENT = PersonIdent.fra("12345678901");

    @BeforeEach
    void setUp() {
        arbeidsforholdTjeneste = new ArbeidsforholdTjeneste(aaregRestKlient);
    }

    @Test
    void skalReturnereTomListeNårAaregReturnerNull() {
        var nå = LocalDate.now();
        when(aaregRestKlient.finnArbeidsforholdForArbeidstaker(PERSON_IDENT.getIdent(), nå))
            .thenReturn(null);

        var resultat = arbeidsforholdTjeneste.hentArbeidsforhold(PERSON_IDENT, nå);

        assertThat(resultat).isEmpty();
    }

    @Test
    void skalReturnereTomListeNårAaregReturnerTomListe() {
        var nå = LocalDate.now();
        when(aaregRestKlient.finnArbeidsforholdForArbeidstaker(PERSON_IDENT.getIdent(), nå))
            .thenReturn(Collections.emptyList());

        var resultat = arbeidsforholdTjeneste.hentArbeidsforhold(PERSON_IDENT, nå);

        assertThat(resultat).isEmpty();
    }

    @Test
    void skalMappeArbeidsforholdKorrekt() {
        var arbeidsforhold = new ArbeidsforholdDto(
            "abc123",
            123L,
            new OpplysningspliktigArbeidsgiverDto(
                OpplysningspliktigArbeidsgiverDto.Type.ORGANISASJON,
                "999999999",
                "000000000",
                "Arbeidsgiver AS"
            ),
            null,
            null,
            null,
            "type"
        );

        var nå = LocalDate.now();

        when(aaregRestKlient.finnArbeidsforholdForArbeidstaker(PERSON_IDENT.getIdent(), nå))
            .thenReturn(List.of(arbeidsforhold));

        var resultat = arbeidsforholdTjeneste.hentArbeidsforhold(PERSON_IDENT, nå);

        assertThat(resultat)
            .hasSize(1)
            .first()
            .satisfies(dto -> {
                assertThat(dto.ansettelsesperiode()).isNull();
                assertThat(dto.organisasjonsnummer()).isEqualTo("999999999");
            });
    }

    @Test
    void skalMappeFlereArbeidsforholdKorrekt() {
        var ansettelsesperiode = new AnsettelsesperiodeDto(new PeriodeDto(LocalDate.now().minusYears(1), LocalDate.now().plusMonths(6)));
        var arbeidsforhold1 = new ArbeidsforholdDto(
            "arbeidsforhold id 1",
            123L,
            new OpplysningspliktigArbeidsgiverDto(
                OpplysningspliktigArbeidsgiverDto.Type.ORGANISASJON,
                "000000001",
                "100000001",
                "Eino Arbeidsgiver AS"
            ),
            ansettelsesperiode,
            null,
            null,
            "type"
        );
        var ansettelsesperiode2 = new AnsettelsesperiodeDto(new PeriodeDto(LocalDate.now().minusYears(2), Tid.TIDENES_ENDE));
        var arbeidsforhold2 = new ArbeidsforholdDto(
            "arbeidsforhold id 2",
            123L,
            new OpplysningspliktigArbeidsgiverDto(
                OpplysningspliktigArbeidsgiverDto.Type.ORGANISASJON,
                "000000002",
                "100000002",
                "André Arbeidsgiver AS"
            ),
            ansettelsesperiode2,
            null,
            null,
            "type"
        );
        var nå = LocalDate.now();


        when(aaregRestKlient.finnArbeidsforholdForArbeidstaker(PERSON_IDENT.getIdent(), nå))
            .thenReturn(List.of(arbeidsforhold1, arbeidsforhold2));

        var resultat = arbeidsforholdTjeneste.hentArbeidsforhold(PERSON_IDENT, nå);

        var forventetAnsettelsesperiode = new Arbeidsforhold.Ansettelsesperiode( ansettelsesperiode.periode().fom(),
            ansettelsesperiode.periode().tom());
        var forventetAnsettelsesperiode2 = new Arbeidsforhold.Ansettelsesperiode( ansettelsesperiode2.periode().fom(),
            ansettelsesperiode2.periode().tom());

        assertThat(resultat).hasSize(2);

        assertThat(resultat.getFirst().ansettelsesperiode()).isEqualTo(forventetAnsettelsesperiode);
        assertThat(resultat.getFirst().organisasjonsnummer()).isEqualTo("000000001");
        assertThat(resultat.get(1).ansettelsesperiode()).isEqualTo(forventetAnsettelsesperiode2);
        assertThat(resultat.get(1).organisasjonsnummer()).isEqualTo("000000002");
    }
}
