package no.nav.foreldrepenger.inntektsmelding.integrasjoner.joark;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

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
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Saksnummer;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.vedtak.felles.integrasjon.dokarkiv.dto.OpprettJournalpostRequest;
import no.nav.vedtak.felles.integrasjon.dokarkiv.dto.OpprettJournalpostResponse;
import no.nav.vedtak.felles.integrasjon.dokarkiv.dto.Sak;

@ExtendWith(MockitoExtension.class)
class JoarkTjenesteTest {

    @Mock
    private OrganisasjonTjeneste organisasjonTjeneste;

    @Mock
    private JoarkKlient klient;

    private static final byte[] PDFSIGNATURE = {0x25, 0x50, 0x44, 0x46, 0x2d};

    private JoarkTjeneste joarkTjeneste;

    @BeforeEach
    void setup() {
        joarkTjeneste = new JoarkTjeneste(klient, organisasjonTjeneste);
    }

    @Test
    void skal_teste_oversending_organisasjon() {
        // Arrange
        var aktør = "1234567891234";
        var aktørIdSøker = new no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId(aktør);
        var arbeidsgiverIdent = "999999999";
        var inntektsmelding = InntektsmeldingDto.builder()
            .medArbeidsgiver(new Arbeidsgiver(arbeidsgiverIdent))
            .medYtelse(Ytelsetype.FORELDREPENGER)
            .medAktørId(aktørIdSøker)
            .medInnsendtTidspunkt(LocalDateTime.now())
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
    void skal_teste_at_saknummer_ikke_er_satt_om_fagsystemSaksnummer_er_null() {
        // Arrange
        var aktør = "1234567891234";
        var aktørIdSøker = new AktørId(aktør);
        var arbeidsgiver = Arbeidsgiver.fra("999999999");

        var inntektsmeldingUuid = UUID.randomUUID();
        var innsendtTidspunkt = LocalDateTime.now();
        var inntektsmelding = InntektsmeldingDto.builder()
            .medInntektsmeldingUuid(inntektsmeldingUuid)
            .medArbeidsgiver(arbeidsgiver)
            .medYtelse(Ytelsetype.FORELDREPENGER)
            .medAktørId(aktørIdSøker)
            .medInnsendtTidspunkt(innsendtTidspunkt)
            .build();

        var testBedrift = new Organisasjon("Test Bedrift", arbeidsgiver.toString());
        // Kan foreløpig ikke teste med spesifikk request i mock siden eksternreferanse genereres on the fly
        when(organisasjonTjeneste.finnOrganisasjon(arbeidsgiver)).thenReturn(testBedrift);
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
        assertThat(opprettJournalpostRequest.datoMottatt()).isEqualTo(innsendtTidspunkt.toLocalDate().atStartOfDay());
        assertThat(opprettJournalpostRequest.eksternReferanseId()).isNotEmpty().isEqualTo(inntektsmeldingUuid.toString());
    }

    @Test
    void skal_teste_at_saknummer_ikke_er_satt_om_fagsystemSaksnummer_er_empty() {
        // Arrange
        var aktør = "1234567891234";
        var aktørIdSøker = new AktørId(aktør);
        var arbeidsgiver = Arbeidsgiver.fra("999999999");

        var inntektsmeldingUuid = UUID.randomUUID();
        var inntektsmelding = InntektsmeldingDto.builder()
            .medInntektsmeldingUuid(inntektsmeldingUuid)
            .medArbeidsgiver(arbeidsgiver)
            .medYtelse(Ytelsetype.FORELDREPENGER)
            .medAktørId(aktørIdSøker)
            .medInnsendtTidspunkt(LocalDateTime.now())
            .build();

        var testBedrift = new Organisasjon("Test Bedrift", arbeidsgiver.toString());
        // Kan foreløpig ikke teste med spesifikk request i mock siden eksternreferanse genereres on the fly
        when(organisasjonTjeneste.finnOrganisasjon(arbeidsgiver)).thenReturn(testBedrift);
        when(klient.opprettJournalpost(any(), anyBoolean())).thenReturn(new OpprettJournalpostResponse("9999", false, Collections.emptyList()));

        // Act
        var journalpostId = joarkTjeneste.journalførInntektsmelding("XML", inntektsmelding, PDFSIGNATURE, Saksnummer.fra(""));

        // Assert
        assertThat(journalpostId).isEqualTo("9999");

        var argumentCaptor = ArgumentCaptor.forClass(OpprettJournalpostRequest.class);
        verify(klient).opprettJournalpost(argumentCaptor.capture(), eq(false));

        var opprettJournalpostRequest = argumentCaptor.getValue();
        assertThat(opprettJournalpostRequest.sak()).isNull();
        assertThat(opprettJournalpostRequest.eksternReferanseId()).isNotEmpty().isEqualTo(inntektsmeldingUuid.toString());
        assertThat(opprettJournalpostRequest.bruker().id()).isEqualTo(aktør);
        assertThat(opprettJournalpostRequest.tema()).isEqualTo(JoarkTjeneste.TEMA_FOR);
        assertThat(opprettJournalpostRequest.kanal()).isEqualTo(JoarkTjeneste.KANAL);
    }

    @Test
    void skal_teste_at_eksternreferanse_blir_generert_om_den_ikke_er_satt_på_inntektsmeldingen() {
        // Arrange
        var aktør = "1234567891234";
        var aktørIdSøker = new AktørId(aktør);
        var arbeidsgiver = Arbeidsgiver.fra("999999999");

        var inntektsmelding = InntektsmeldingDto.builder()
            .medArbeidsgiver(arbeidsgiver)
            .medYtelse(Ytelsetype.FORELDREPENGER)
            .medAktørId(aktørIdSøker)
            .medInnsendtTidspunkt(LocalDateTime.now())
            .build();

        var testBedrift = new Organisasjon("Test Bedrift", arbeidsgiver.toString());
        // Kan foreløpig ikke teste med spesifikk request i mock siden eksternreferanse genereres on the fly
        when(organisasjonTjeneste.finnOrganisasjon(arbeidsgiver)).thenReturn(testBedrift);
        when(klient.opprettJournalpost(any(), anyBoolean())).thenReturn(new OpprettJournalpostResponse("9999", false, Collections.emptyList()));

        // Act
        var journalpostId = joarkTjeneste.journalførInntektsmelding("XML", inntektsmelding, PDFSIGNATURE, Saksnummer.fra(""));

        // Assert
        assertThat(journalpostId).isEqualTo("9999");

        var argumentCaptor = ArgumentCaptor.forClass(OpprettJournalpostRequest.class);
        verify(klient).opprettJournalpost(argumentCaptor.capture(), eq(false));

        var opprettJournalpostRequest = argumentCaptor.getValue();
        assertThat(opprettJournalpostRequest.eksternReferanseId()).isNotEmpty();
        assertThat(opprettJournalpostRequest.bruker().id()).isEqualTo(aktør);
    }
}

