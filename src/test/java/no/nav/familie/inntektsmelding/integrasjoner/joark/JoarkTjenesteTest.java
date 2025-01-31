package no.nav.familie.inntektsmelding.integrasjoner.joark;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.imdialog.modell.BortaltNaturalytelseEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.KontaktpersonEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.Organisasjon;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.NaturalytelseType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.felles.integrasjon.dokarkiv.dto.OpprettJournalpostRequest;
import no.nav.vedtak.felles.integrasjon.dokarkiv.dto.OpprettJournalpostResponse;
import no.nav.vedtak.felles.integrasjon.dokarkiv.dto.Sak;
import no.nav.vedtak.konfig.Tid;

@ExtendWith(MockitoExtension.class)
class JoarkTjenesteTest {

    @Mock
    private PersonTjeneste personTjeneste;

    @Mock
    private OrganisasjonTjeneste organisasjonTjeneste;

    @Mock
    private JoarkKlient klient;

    private static final byte[] PDFSIGNATURE = {0x25, 0x50, 0x44, 0x46, 0x2d};

    private JoarkTjeneste joarkTjeneste;

    @BeforeEach
    void setup() {
        joarkTjeneste = new JoarkTjeneste(klient, organisasjonTjeneste, personTjeneste);
    }

    @Test
    void skal_teste_oversending_organisasjon() {
        // Arrange
        var aktør = "1234567891234";
        var aktørIdSøker = new AktørIdEntitet(aktør);
        var naturalytelse = BortaltNaturalytelseEntitet.builder()
            .medPeriode(LocalDate.of(2024, 6, 10), LocalDate.of(2024, 6, 30))
            .medType(NaturalytelseType.AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS)
            .medMånedBeløp(BigDecimal.valueOf(2000))
            .build();
        var arbeidsgiverIdent = "999999999";
        var inntektsmelding = InntektsmeldingEntitet.builder()
            .medArbeidsgiverIdent(arbeidsgiverIdent)
            .medStartDato(LocalDate.of(2024, 6, 1))
            .medYtelsetype(Ytelsetype.FORELDREPENGER)
            .medRefusjonOpphørsdato(Tid.TIDENES_ENDE)
            .medMånedRefusjon(BigDecimal.valueOf(35000))
            .medMånedInntekt(BigDecimal.valueOf(35000))
            .medAktørId(aktørIdSøker)
            .medOpprettetTidspunkt(LocalDateTime.now())
            .medKontaktperson(new KontaktpersonEntitet("Test Testen", "111111111"))
            .medBortfaltNaturalytelser(Collections.singletonList(naturalytelse))
            .build();

        var testBedrift = new Organisasjon("Test Bedrift", arbeidsgiverIdent);

        // Kan foreløpig ikke teste med spesifikk request i mock siden eksternreferanse genereres on the fly
        when(organisasjonTjeneste.finnOrganisasjon(arbeidsgiverIdent)).thenReturn(testBedrift);
        when(klient.opprettJournalpost(any(), anyBoolean())).thenReturn(new OpprettJournalpostResponse("9999", false, Collections.emptyList()));

        // Act
        var fagsystemSaksnummer = "23423423";
        var journalpostId = joarkTjeneste.journalførInntektsmelding("XML", inntektsmelding, PDFSIGNATURE, fagsystemSaksnummer);

        // Assert
        assertThat(journalpostId).isEqualTo("9999");

        var argumentCaptor = ArgumentCaptor.forClass(OpprettJournalpostRequest.class);
        verify(klient).opprettJournalpost(argumentCaptor.capture(), eq(false));

        var opprettJournalpostRequest = argumentCaptor.getValue();
        assertThat(opprettJournalpostRequest.sak()).isNotNull();
        assertThat(opprettJournalpostRequest.sak().fagsakId()).isEqualTo(fagsystemSaksnummer);
        assertThat(opprettJournalpostRequest.sak().fagsaksystem()).isEqualTo(Fagsystem.FPSAK.getOffisiellKode());
        assertThat(opprettJournalpostRequest.sak().sakstype()).isEqualTo(Sak.Sakstype.FAGSAK);
        assertThat(opprettJournalpostRequest.bruker().id()).isEqualTo(aktør);
    }

    @Test
    void skal_teste_oversending_privapterson() {
        // Arrange
        var aktør = "1234567891234";
        var aktørIdSøker = new AktørIdEntitet(aktør);
        var naturalytelse = BortaltNaturalytelseEntitet.builder()
            .medPeriode(LocalDate.of(2024, 6, 10), LocalDate.of(2024, 6, 30))
            .medType(NaturalytelseType.AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS)
            .medMånedBeløp(BigDecimal.valueOf(2000))
            .build();
        var aktørIdArbeidsgiver = "2222222222222";
        var inntektsmelding = InntektsmeldingEntitet.builder()
            .medArbeidsgiverIdent(aktørIdArbeidsgiver)
            .medStartDato(LocalDate.of(2024, 6, 1))
            .medYtelsetype(Ytelsetype.FORELDREPENGER)
            .medMånedInntekt(BigDecimal.valueOf(35000))
            .medRefusjonOpphørsdato(Tid.TIDENES_ENDE)
            .medMånedRefusjon(BigDecimal.valueOf(35000))
            .medAktørId(aktørIdSøker)
            .medOpprettetTidspunkt(LocalDateTime.now())
            .medKontaktperson(new KontaktpersonEntitet("Test Testen", "111111111"))
            .medBortfaltNaturalytelser(Collections.singletonList(naturalytelse))
            .build();

        // Kan foreløpig ikke teste med spesifikk request i mock siden eksternreferanse genereres on the fly
        when(personTjeneste.hentPersonInfoFraAktørId(new AktørIdEntitet(aktørIdArbeidsgiver), Ytelsetype.FORELDREPENGER)).thenReturn(
            new PersonInfo("Navn", null, "Navnesen", new PersonIdent("9999999999999"), aktørIdSøker, LocalDate.now(), null, null));
        when(klient.opprettJournalpost(any(), anyBoolean())).thenReturn(new OpprettJournalpostResponse("9999", false, Collections.emptyList()));
        // Act
        var journalpostId = joarkTjeneste.journalførInntektsmelding("XML", inntektsmelding, PDFSIGNATURE, null);

        // Assert
        assertThat(journalpostId).isEqualTo("9999");

        var argumentCaptor = ArgumentCaptor.forClass(OpprettJournalpostRequest.class);
        verify(klient).opprettJournalpost(argumentCaptor.capture(), eq(false));

        var opprettJournalpostRequest = argumentCaptor.getValue();
        assertThat(opprettJournalpostRequest.sak()).isNull();
        assertThat(opprettJournalpostRequest.bruker().id()).isEqualTo(aktør);

    }
}
