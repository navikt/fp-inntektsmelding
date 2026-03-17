package no.nav.foreldrepenger.inntektsmelding.integrasjoner.joark;

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

import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.organisasjon.Organisasjon;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Saksnummer;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.NaturalytelseType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
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
        var aktørIdSøker = new no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId(aktør);
        var naturalytelse = new InntektsmeldingDto.BortfaltNaturalytelse(LocalDate.of(2024, 6, 10),
            LocalDate.of(2024, 6, 30),
                NaturalytelseType.AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS, BigDecimal.valueOf(2000));
        var arbeidsgiverIdent = "999999999";
        var inntektsmelding = InntektsmeldingDto.builder()
            .medArbeidsgiver(new Arbeidsgiver(arbeidsgiverIdent))
            .medStartdato(LocalDate.of(2024, 6, 1))
            .medYtelse(Ytelsetype.FORELDREPENGER)
            .medOpphørsdatoRefusjon(Tid.TIDENES_ENDE)
            .medMånedRefusjon(BigDecimal.valueOf(35000))
            .medInntekt(BigDecimal.valueOf(35000))
            .medAktørId(aktørIdSøker)
            .medInnsendtTidspunkt(LocalDateTime.now())
            .medKontaktperson(new InntektsmeldingDto.Kontaktperson("Test Testen", "111111111"))
            .medBortfaltNaturalytelsePerioder(Collections.singletonList(naturalytelse))
            .build();

        var testBedrift = new Organisasjon("Test Bedrift", arbeidsgiverIdent);

        // Kan foreløpig ikke teste med spesifikk request i mock siden eksternreferanse genereres on the fly
        when(organisasjonTjeneste.finnOrganisasjon(Arbeidsgiver.fra(arbeidsgiverIdent))).thenReturn(testBedrift);
        when(klient.opprettJournalpost(any(), anyBoolean())).thenReturn(new OpprettJournalpostResponse("9999", false, Collections.emptyList()));

        // Act
        var fagsystemSaksnummer = Saksnummer.fra( "23423423");
        var journalpostId = joarkTjeneste.journalførInntektsmelding("XML", inntektsmelding, PDFSIGNATURE, fagsystemSaksnummer);

        // Assert
        assertThat(journalpostId).isEqualTo("9999");

        var argumentCaptor = ArgumentCaptor.forClass(OpprettJournalpostRequest.class);
        verify(klient).opprettJournalpost(argumentCaptor.capture(), eq(false));

        var opprettJournalpostRequest = argumentCaptor.getValue();
        assertThat(opprettJournalpostRequest.sak()).isNotNull();
        assertThat(opprettJournalpostRequest.sak().fagsakId()).isEqualTo(fagsystemSaksnummer.saksnummer());
        assertThat(opprettJournalpostRequest.sak().fagsaksystem()).isEqualTo(Fagsystem.FPSAK.getOffisiellKode());
        assertThat(opprettJournalpostRequest.sak().sakstype()).isEqualTo(Sak.Sakstype.FAGSAK);
        assertThat(opprettJournalpostRequest.bruker().id()).isEqualTo(aktør);
    }

    @Test
    void skal_teste_oversending_privapterson() {
        // Arrange
        var aktør = "1234567891234";
        var aktørIdSøker = new AktørId(aktør);
        var naturalytelse = new InntektsmeldingDto.BortfaltNaturalytelse(
            LocalDate.of(2024, 6, 10),
                LocalDate.of(2024, 6, 30),
            NaturalytelseType.AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS,
            BigDecimal.valueOf(2000));

        var aktørIdArbeidsgiver = "2222222222222";
        var inntektsmelding = InntektsmeldingDto.builder()
            .medArbeidsgiver(new Arbeidsgiver(aktørIdArbeidsgiver))
            .medStartdato(LocalDate.of(2024, 6, 1))
            .medYtelse(Ytelsetype.FORELDREPENGER)
            .medInntekt(BigDecimal.valueOf(35000))
            .medOpphørsdatoRefusjon(Tid.TIDENES_ENDE)
            .medMånedRefusjon(BigDecimal.valueOf(35000))
            .medAktørId(aktørIdSøker)
            .medInnsendtTidspunkt(LocalDateTime.now())
            .medKontaktperson(new InntektsmeldingDto.Kontaktperson("Test Testen", "111111111"))
            .medBortfaltNaturalytelsePerioder(Collections.singletonList(naturalytelse))
            .build();

        // Kan foreløpig ikke teste med spesifikk request i mock siden eksternreferanse genereres on the fly
        when(personTjeneste.hentPersonInfoFraAktørId(new no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId(aktørIdArbeidsgiver), Ytelsetype.FORELDREPENGER)).thenReturn(
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
