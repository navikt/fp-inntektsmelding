package no.nav.foreldrepenger.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon;

import static no.nav.foreldrepenger.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.MinSideArbeidsgiverTjeneste.ALTINN_INNTEKTSMELDING_RESSURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTekster;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;
import no.nav.foreldrepenger.inntektsmelding.imdialog.rest.kvittering.PdfDokumentRest;
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

@ExtendWith(MockitoExtension.class)
class MinSideArbeidsgiverTjenesteTjenesteTest {

    private static final String INNTEKTSMELDING_SKJEMA_LENKE = "https://arbeidsgiver.nav.no/fp-im-dialog";
    private static final String ORGNR = "974760673";
    private static final String AKTØR_ID = "1234567890123";

    @Mock
    MinSideArbeidsgiverKlient klient;
    @Mock
    PersonTjeneste personTjeneste;
    @Mock
    OrganisasjonTjeneste organisasjonTjeneste;

    private MinSideArbeidsgiverTjeneste tjeneste;

    @BeforeEach
    void setUp() {
        tjeneste = new MinSideArbeidsgiverTjeneste(klient, personTjeneste, organisasjonTjeneste, INNTEKTSMELDING_SKJEMA_LENKE);
    }

    private static ForespørselDto lagForespørsel(UUID uuid, String oppgaveId, String sakId, LocalDate førsteUttaksdato) {
        return ForespørselDto.builder()
            .uuid(uuid)
            .arbeidsgiver(Arbeidsgiver.fra(ORGNR))
            .aktørId(new AktørId(AKTØR_ID))
            .ytelseType(Ytelsetype.FORELDREPENGER)
            .status(ForespørselStatus.UNDER_BEHANDLING)
            .forespørselType(ForespørselType.BESTILT_AV_FAGSYSTEM)
            .førsteUttaksdato(førsteUttaksdato)
            .arbeidsgiverNotifikasjonSakId(sakId)
            .oppgaveId(oppgaveId)
            .build();
    }

    private static PersonInfo lagPersonInfo() {
        return new PersonInfo("Navn",
            null,
            "Navnesen",
            new PersonIdent("01019100000"),
            new AktørId(AKTØR_ID),
            LocalDate.of(1991, 1, 1),
            null,
            null);
    }

    @Test
    void opprett_sak() {
        var uuid = UUID.randomUUID();
        var førsteUttaksdato = LocalDate.of(2024, 6, 1);
        var forespørsel = lagForespørsel(uuid, null, null, førsteUttaksdato);
        var personInfo = lagPersonInfo();
        when(personTjeneste.hentPersonInfoFraAktørId(forespørsel.aktørId(), Ytelsetype.FORELDREPENGER)).thenReturn(personInfo);
        when(klient.opprettSak(any(), any())).thenReturn("sak-1");

        tjeneste.opprettSak(forespørsel);

        var requestCaptor = ArgumentCaptor.forClass(NySakMutationRequest.class);
        verify(klient).opprettSak(requestCaptor.capture(), any(NySakResultatResponseProjection.class));

        var input = requestCaptor.getValue().getInput();
        assertThat(input).containsOnlyKeys("grupperingsid",
            "initiellStatus",
            "lenke",
            "merkelapp",
            "tittel",
            "virksomhetsnummer",
            "mottakere",
            "overstyrStatustekstMed",
            "nesteSteg",
            "tidspunkt",
            "tilleggsinformasjon",
            "hardDelete")
            .containsEntry("grupperingsid", uuid.toString())
            .containsEntry("initiellStatus", SaksStatus.UNDER_BEHANDLING)
            .containsEntry("lenke", INNTEKTSMELDING_SKJEMA_LENKE + "/" + uuid)
            .containsEntry("merkelapp", Merkelapp.INNTEKTSMELDING_FP.getBeskrivelse())
            .containsEntry("tittel", ForespørselTekster.lagSaksTittel(personInfo.mapFulltNavn(), personInfo.fødselsdato()))
            .containsEntry("virksomhetsnummer", ORGNR)
            .containsEntry("overstyrStatustekstMed", "")
            .containsEntry("tilleggsinformasjon", ForespørselTekster.lagTilleggsInformasjonOrdinær(førsteUttaksdato));
        assertThat(input.get("mottakere")).isNotNull();
    }

    @Test
    void opprett_oppgave() {
        var uuid = UUID.randomUUID();
        var forespørsel = lagForespørsel(uuid, null, null, LocalDate.now());
        var organisasjon = new Organisasjon("Test A/S", ORGNR);
        when(organisasjonTjeneste.finnOrganisasjon(forespørsel.arbeidsgiver())).thenReturn(organisasjon);
        when(klient.opprettOppgave(any(), any())).thenReturn("oppgave-1");

        tjeneste.opprettOppgave(forespørsel);

        var requestCaptor = ArgumentCaptor.forClass(NyOppgaveMutationRequest.class);
        verify(klient).opprettOppgave(requestCaptor.capture(), any(NyOppgaveResultatResponseProjection.class));

        var input = requestCaptor.getValue().getInput();
        assertThat(input).isNotNull().hasSize(1).containsKey("nyOppgave");
        var nyOppgave = (NyOppgaveInput) input.get("nyOppgave");

        assertThat(nyOppgave.getMottaker()).isNotNull();
        assertThat(nyOppgave.getMottaker().getAltinnRessurs().getRessursId()).isEqualTo(ALTINN_INNTEKTSMELDING_RESSURS);
        assertThat(nyOppgave.getMetadata().getEksternId()).isEqualTo(uuid.toString());
        assertThat(nyOppgave.getMetadata().getGrupperingsid()).isEqualTo(uuid.toString());
        assertThat(nyOppgave.getMetadata().getVirksomhetsnummer()).isEqualTo(ORGNR);
        assertThat(nyOppgave.getMetadata().getOpprettetTidspunkt()).isNull();

        assertThat(nyOppgave.getNotifikasjon().getTekst()).isEqualTo(ForespørselTekster.lagOppgaveTekst(Ytelsetype.FORELDREPENGER));
        assertThat(nyOppgave.getNotifikasjon().getLenke()).isEqualTo(INNTEKTSMELDING_SKJEMA_LENKE + "/" + uuid);
        assertThat(nyOppgave.getNotifikasjon().getMerkelapp()).isEqualTo(Merkelapp.INNTEKTSMELDING_FP.getBeskrivelse());

        assertThat(nyOppgave.getEksterneVarsler()).hasSize(1);
        assertThat(nyOppgave.getEksterneVarsler().getFirst().getAltinnressurs().getEpostTittel()).isEqualTo("Nav trenger inntektsmelding");
        assertThat(nyOppgave.getEksterneVarsler().getFirst().getAltinnressurs().getEpostHtmlBody())
            .isEqualTo(ForespørselTekster.lagVarselTekst(Ytelsetype.FORELDREPENGER, organisasjon));

        assertThat(nyOppgave.getPaaminnelse().getEksterneVarsler()).hasSize(1);
        assertThat(nyOppgave.getPaaminnelse().getEksterneVarsler().getFirst().getAltinnressurs().getEpostHtmlBody())
            .isEqualTo(ForespørselTekster.lagPåminnelseTekst(Ytelsetype.FORELDREPENGER, organisasjon));

        assertThat(nyOppgave.getFrist()).isNull();
        assertThat(nyOppgave.getMottakere()).isEmpty();
    }

    @Test
    void lukk_oppgave() {
        var expectedId = "TestId";
        var expectedTidspunkt = OffsetDateTime.now();

        var requestCaptor = ArgumentCaptor.forClass(OppgaveUtfoertMutationRequest.class);

        tjeneste.oppgaveUtført(expectedId, expectedTidspunkt);

        verify(klient).oppgaveUtført(requestCaptor.capture(), any(OppgaveUtfoertResultatResponseProjection.class));

        var input = requestCaptor.getValue().getInput();

        assertThat(input).containsOnlyKeys("id", "utfoertTidspunkt", "hardDelete", "nyLenke")
            .containsEntry("id", expectedId)
            .containsEntry("utfoertTidspunkt", expectedTidspunkt.format(DateTimeFormatter.ISO_DATE_TIME));
        assertThat(input.get("hardDelete")).isNull();
        assertThat(input.get("nyLenke")).isNull();
    }


    @Test
    void ferdigstill_sak() {
        var expectedId = "TestId";

        var requestCaptor = ArgumentCaptor.forClass(NyStatusSakMutationRequest.class);

        tjeneste.ferdigstillSak(expectedId, false);

        verify(klient).oppdaterSakStatus(requestCaptor.capture(), any(NyStatusSakResultatResponseProjection.class));

        var request = requestCaptor.getValue();

        var input = request.getInput();

        assertThat(input).containsOnlyKeys("id", "overstyrStatustekstMed", "nyStatus", "idempotencyKey", "hardDelete", "tidspunkt", "nyLenkeTilSak")
            .containsEntry("id", expectedId)
            .containsEntry("nyStatus", SaksStatus.FERDIG)
            .containsEntry("overstyrStatustekstMed", "");

        assertThat(input.get("idempotencyKey")).isNull();
        assertThat(input.get("hardDelete")).isNull();
        assertThat(input.get("tidspunkt")).isNull();
        assertThat(input.get("nyLenkeTilSak")).isNull();
    }

    @Test
    void ferdigstill_arbeidsgiverinitiert_sak() {
        var expectedId = "TestId";

        var requestCaptor = ArgumentCaptor.forClass(NyStatusSakMutationRequest.class);

        tjeneste.ferdigstillSak(expectedId, true);

        verify(klient).oppdaterSakStatus(requestCaptor.capture(), any(NyStatusSakResultatResponseProjection.class));

        var request = requestCaptor.getValue();

        var input = request.getInput();

        assertThat(input).containsOnlyKeys("id", "overstyrStatustekstMed", "nyStatus", "idempotencyKey", "hardDelete", "tidspunkt", "nyLenkeTilSak")
            .containsEntry("id", expectedId)
            .containsEntry("nyStatus", SaksStatus.FERDIG)
            .containsEntry("overstyrStatustekstMed", MinSideArbeidsgiverTjeneste.SAK_STATUS_TEKST_ARBEIDSGIVERINITIERT);

        assertThat(input.get("idempotencyKey")).isNull();
        assertThat(input.get("hardDelete")).isNull();
        assertThat(input.get("tidspunkt")).isNull();
        assertThat(input.get("nyLenkeTilSak")).isNull();
    }

    @Test
    void oppdater_Tillegsinformasjon() {
        var expectedId = "TestId";
        var expectedTilleggsinformasjon = "Saksbehandler har gått videre uten din inntektsmelding";

        var requestCaptor = ArgumentCaptor.forClass(TilleggsinformasjonSakMutationRequest.class);

        tjeneste.oppdaterSakTilleggsinformasjon(expectedId, expectedTilleggsinformasjon);

        verify(klient).oppdaterSakTilleggsinformasjon(requestCaptor.capture(), any(TilleggsinformasjonSakResultatResponseProjection.class));

        var request = requestCaptor.getValue();

        var input = request.getInput();

        assertThat(input).containsOnlyKeys("id", "idempotencyKey", "tilleggsinformasjon")
            .containsEntry("id", expectedId)
            .containsEntry("tilleggsinformasjon", expectedTilleggsinformasjon)
            .containsEntry("idempotencyKey", null);
    }

    @Test
    void opprett_oppgave_for_forespørsel_skal_generere_riktige_tekster_og_lenker() {
        var uuid = UUID.randomUUID();
        var forespørsel = lagForespørsel(uuid, null, null, LocalDate.now().minusYears(1).plusDays(1));
        var organisasjon = new Organisasjon("Test A/S", ORGNR);

        when(organisasjonTjeneste.finnOrganisasjon(forespørsel.arbeidsgiver())).thenReturn(organisasjon);
        when(klient.opprettOppgave(any(), any())).thenReturn("oppgave-1");

        var oppgaveId = tjeneste.opprettOppgave(forespørsel);

        assertThat(oppgaveId).isEqualTo("oppgave-1");

        var forventetSkjemaUri = URI.create(INNTEKTSMELDING_SKJEMA_LENKE + "/" + uuid);

        var oppgaveCaptor = ArgumentCaptor.forClass(NyOppgaveMutationRequest.class);
        verify(klient).opprettOppgave(oppgaveCaptor.capture(), any(NyOppgaveResultatResponseProjection.class));
        var nyOppgave = (NyOppgaveInput) oppgaveCaptor.getValue().getInput().get("nyOppgave");
        assertThat(nyOppgave.getMetadata().getGrupperingsid()).isEqualTo(uuid.toString());
        assertThat(nyOppgave.getMetadata().getEksternId()).isEqualTo(uuid.toString());
        assertThat(nyOppgave.getMetadata().getVirksomhetsnummer()).isEqualTo(ORGNR);
        assertThat(nyOppgave.getNotifikasjon().getTekst()).isEqualTo(ForespørselTekster.lagOppgaveTekst(Ytelsetype.FORELDREPENGER));
        assertThat(nyOppgave.getNotifikasjon().getLenke()).isEqualTo(forventetSkjemaUri.toString());
        assertThat(nyOppgave.getEksterneVarsler().getFirst().getAltinnressurs().getEpostHtmlBody())
            .isEqualTo(ForespørselTekster.lagVarselTekst(Ytelsetype.FORELDREPENGER, organisasjon));
        assertThat(nyOppgave.getPaaminnelse().getEksterneVarsler().getFirst().getAltinnressurs().getEpostHtmlBody())
            .isEqualTo(ForespørselTekster.lagPåminnelseTekst(Ytelsetype.FORELDREPENGER, organisasjon));
    }

    @Test
    void opprett_sak_skal_ikke_opprette_oppgave() {
        var uuid = UUID.randomUUID();
        var førsteUttaksdato = LocalDate.now().minusYears(1).plusDays(1);
        var forespørsel = lagForespørsel(uuid, null, null, førsteUttaksdato);
        when(personTjeneste.hentPersonInfoFraAktørId(forespørsel.aktørId(), Ytelsetype.FORELDREPENGER)).thenReturn(lagPersonInfo());
        when(klient.opprettSak(any(), any())).thenReturn("sak-1");

        var sakId = tjeneste.opprettSak(forespørsel);

        assertThat(sakId).isEqualTo("sak-1");
        verify(klient, never()).opprettOppgave(any(), any());
    }

    @Test
    void ferdigstill_sak_ved_førstegangsinnsending_skal_utføre_oppgave_og_sende_kvittering_med_riktig_tekst() {
        var uuid = UUID.randomUUID();
        var imUuid = UUID.randomUUID();
        var førsteUttaksdato = LocalDate.now().minusYears(1).plusDays(1);
        var forespørsel = lagForespørsel(uuid, "oppgave-1", "sak-1", førsteUttaksdato);

        tjeneste.ferdigstillSak(forespørsel, LukkeÅrsak.EKSTERN_INNSENDING, Optional.of(imUuid), true);

        var oppgaveUtførtCaptor = ArgumentCaptor.forClass(OppgaveUtfoertMutationRequest.class);
        verify(klient).oppgaveUtført(oppgaveUtførtCaptor.capture(), any(OppgaveUtfoertResultatResponseProjection.class));
        assertThat(oppgaveUtførtCaptor.getValue().getInput()).containsEntry("id", "oppgave-1");

        var statusCaptor = ArgumentCaptor.forClass(NyStatusSakMutationRequest.class);
        verify(klient).oppdaterSakStatus(statusCaptor.capture(), any(NyStatusSakResultatResponseProjection.class));
        assertThat(statusCaptor.getValue().getInput())
            .containsEntry("id", "sak-1")
            .containsEntry("overstyrStatustekstMed", MinSideArbeidsgiverTjeneste.SAK_STATUS_TEKST);

        var tilleggsinfoCaptor = ArgumentCaptor.forClass(TilleggsinformasjonSakMutationRequest.class);
        verify(klient).oppdaterSakTilleggsinformasjon(tilleggsinfoCaptor.capture(), any(TilleggsinformasjonSakResultatResponseProjection.class));
        assertThat(tilleggsinfoCaptor.getValue().getInput())
            .containsEntry("id", "sak-1")
            .containsEntry("tilleggsinformasjon", ForespørselTekster.lagTilleggsInformasjon(LukkeÅrsak.EKSTERN_INNSENDING, førsteUttaksdato));

        var beskjedCaptor = ArgumentCaptor.forClass(NyBeskjedMutationRequest.class);
        verify(klient).opprettBeskjedOgVarsling(beskjedCaptor.capture(), any(NyBeskjedResultatResponseProjection.class));
        var nyBeskjed = (NyBeskjedInput) beskjedCaptor.getValue().getInput().get("nyBeskjed");
        assertThat(nyBeskjed.getNotifikasjon().getTekst()).isEqualTo(ForespørselTekster.lagBeskjedOmKvitteringFørsteInnsendingTekst());
        assertThat(nyBeskjed.getNotifikasjon().getLenke())
            .isEqualTo(INNTEKTSMELDING_SKJEMA_LENKE + "/server/api" + PdfDokumentRest.INNTEKTSMELDING_FULL_PATH + "/" + imUuid);
        assertThat(nyBeskjed.getMetadata().getGrupperingsid()).isEqualTo(uuid.toString());
        assertThat(nyBeskjed.getMetadata().getVirksomhetsnummer()).isEqualTo(ORGNR);
    }

    @Test
    void ferdigstill_sak_ved_oppdatert_innsending_skal_bruke_oppdatert_tekst() {
        var uuid = UUID.randomUUID();
        var imUuid = UUID.randomUUID();
        var forespørsel = lagForespørsel(uuid, "oppgave-1", "sak-1", LocalDate.now());

        tjeneste.ferdigstillSak(forespørsel, LukkeÅrsak.EKSTERN_INNSENDING, Optional.of(imUuid), false);

        var beskjedCaptor = ArgumentCaptor.forClass(NyBeskjedMutationRequest.class);
        verify(klient).opprettBeskjedOgVarsling(beskjedCaptor.capture(), any(NyBeskjedResultatResponseProjection.class));
        var nyBeskjed = (NyBeskjedInput) beskjedCaptor.getValue().getInput().get("nyBeskjed");
        assertThat(nyBeskjed.getNotifikasjon().getTekst()).isEqualTo(ForespørselTekster.lagBeskjedOmOppdatertInntektsmelding());
    }

    @Test
    void ferdigstill_sak_for_arbeidsgiverinitiert_forespørsel_skal_ikke_utføre_oppgave_og_bruke_riktig_statustekst() {
        var uuid = UUID.randomUUID();
        var forespørsel = lagForespørsel(uuid, null, "sak-1", LocalDate.now());

        tjeneste.ferdigstillSak(forespørsel, LukkeÅrsak.ORDINÆR_INNSENDING, Optional.empty(), true);

        verify(klient, never()).oppgaveUtført(any(), any());
        verify(klient, never()).opprettBeskjedOgVarsling(any(), any());

        var statusCaptor = ArgumentCaptor.forClass(NyStatusSakMutationRequest.class);
        verify(klient).oppdaterSakStatus(statusCaptor.capture(), any(NyStatusSakResultatResponseProjection.class));
        assertThat(statusCaptor.getValue().getInput())
            .containsEntry("id", "sak-1")
            .containsEntry("overstyrStatustekstMed", MinSideArbeidsgiverTjeneste.SAK_STATUS_TEKST_ARBEIDSGIVERINITIERT);
    }

    @Test
    void sett_sak_til_utgått_med_oppgave_skal_sette_oppgave_utgått_og_oppdatere_tilleggsinformasjon() {
        var uuid = UUID.randomUUID();
        var førsteUttaksdato = LocalDate.now().minusYears(1).plusDays(1);
        var forespørsel = lagForespørsel(uuid, "oppgave-1", "sak-1", førsteUttaksdato);

        tjeneste.settSakTilUtgått(forespørsel);

        var oppgaveUtgåttCaptor = ArgumentCaptor.forClass(OppgaveUtgaattMutationRequest.class);
        verify(klient).oppgaveUtgått(oppgaveUtgåttCaptor.capture(), any(OppgaveUtgaattResultatResponseProjection.class));
        assertThat(oppgaveUtgåttCaptor.getValue().getInput()).containsEntry("id", "oppgave-1");

        var tilleggsinfoCaptor = ArgumentCaptor.forClass(TilleggsinformasjonSakMutationRequest.class);
        verify(klient).oppdaterSakTilleggsinformasjon(tilleggsinfoCaptor.capture(), any(TilleggsinformasjonSakResultatResponseProjection.class));
        assertThat(tilleggsinfoCaptor.getValue().getInput())
            .containsEntry("id", "sak-1")
            .containsEntry("tilleggsinformasjon", ForespørselTekster.lagTilleggsInformasjon(LukkeÅrsak.UTGÅTT, førsteUttaksdato));
    }

    @Test
    void sett_sak_til_utgått_uten_oppgave_skal_ikke_kalle_oppgave_utgått() {
        var uuid = UUID.randomUUID();
        var forespørsel = lagForespørsel(uuid, null, "sak-1", LocalDate.now());

        tjeneste.settSakTilUtgått(forespørsel);

        verify(klient, never()).oppgaveUtgått(any(), any());
        verify(klient).oppdaterSakTilleggsinformasjon(any(TilleggsinformasjonSakMutationRequest.class), any(TilleggsinformasjonSakResultatResponseProjection.class));
    }

    @Test
    void send_ny_beskjed_med_ekstern_varsling_skal_generere_riktige_tekster() {
        var uuid = UUID.randomUUID();
        var forespørsel = lagForespørsel(uuid, null, "sak-1", LocalDate.now());
        var organisasjon = new Organisasjon("Test A/S", ORGNR);
        var personInfo = lagPersonInfo();

        when(organisasjonTjeneste.finnOrganisasjon(forespørsel.arbeidsgiver())).thenReturn(organisasjon);
        when(personTjeneste.hentPersonInfoFraAktørId(forespørsel.aktørId(), Ytelsetype.FORELDREPENGER)).thenReturn(personInfo);

        tjeneste.sendNyBeskjedMedEksternVarsling(forespørsel);

        var beskjedCaptor = ArgumentCaptor.forClass(NyBeskjedMutationRequest.class);
        verify(klient).opprettBeskjedOgVarsling(beskjedCaptor.capture(), any(NyBeskjedResultatResponseProjection.class));
        var nyBeskjed = (NyBeskjedInput) beskjedCaptor.getValue().getInput().get("nyBeskjed");

        assertThat(nyBeskjed.getNotifikasjon().getTekst())
            .isEqualTo(ForespørselTekster.lagBeskjedFraSaksbehandlerTekst(Ytelsetype.FORELDREPENGER, personInfo.mapFulltNavn()));
        assertThat(nyBeskjed.getNotifikasjon().getLenke()).isEqualTo(INNTEKTSMELDING_SKJEMA_LENKE + "/" + uuid);
        assertThat(nyBeskjed.getEksterneVarsler()).hasSize(1);
        assertThat(nyBeskjed.getEksterneVarsler().getFirst().getAltinnressurs().getEpostHtmlBody())
            .isEqualTo(ForespørselTekster.lagVarselFraSaksbehandlerTekst(Ytelsetype.FORELDREPENGER, organisasjon));
    }

    @Test
    void send_ny_beskjed_om_avvist_inntektsmelding_skal_sende_riktig_feiltekst_og_lenke() {
        var uuid = UUID.randomUUID();
        var forespørsel = lagForespørsel(uuid, null, "sak-1", LocalDate.now());
        var feiltekst = "Inntektsmeldingen ble avvist fordi den mangler påkrevde opplysninger";

        tjeneste.sendNyBeskjedOmAvvistInntektsmelding(forespørsel, feiltekst);

        var beskjedCaptor = ArgumentCaptor.forClass(NyBeskjedMutationRequest.class);
        verify(klient).opprettBeskjedOgVarsling(beskjedCaptor.capture(), any(NyBeskjedResultatResponseProjection.class));
        var nyBeskjed = (NyBeskjedInput) beskjedCaptor.getValue().getInput().get("nyBeskjed");

        assertThat(nyBeskjed.getNotifikasjon().getTekst()).isEqualTo(feiltekst);
        assertThat(nyBeskjed.getNotifikasjon().getLenke()).isEqualTo(INNTEKTSMELDING_SKJEMA_LENKE + "/" + uuid);
        assertThat(nyBeskjed.getMetadata().getGrupperingsid()).isEqualTo(uuid.toString());
        assertThat(nyBeskjed.getMetadata().getVirksomhetsnummer()).isEqualTo(ORGNR);
        assertThat(nyBeskjed.getEksterneVarsler()).isEmpty();
    }

    @Test
    void send_beskjed_om_oppdatert_inntektsmelding_skal_generere_riktig_kvitteringslenke() {
        var uuid = UUID.randomUUID();
        var imUuid = UUID.randomUUID();
        var forespørsel = lagForespørsel(uuid, null, "sak-1", LocalDate.now());

        tjeneste.sendBeskjedOmOppdatertInntektsmelding(forespørsel, imUuid);

        var beskjedCaptor = ArgumentCaptor.forClass(NyBeskjedMutationRequest.class);
        verify(klient).opprettBeskjedOgVarsling(beskjedCaptor.capture(), any(NyBeskjedResultatResponseProjection.class));
        var nyBeskjed = (NyBeskjedInput) beskjedCaptor.getValue().getInput().get("nyBeskjed");

        assertThat(nyBeskjed.getNotifikasjon().getTekst()).isEqualTo(ForespørselTekster.lagBeskjedOmOppdatertInntektsmelding());
        assertThat(nyBeskjed.getNotifikasjon().getLenke())
            .isEqualTo(INNTEKTSMELDING_SKJEMA_LENKE + "/server/api" + PdfDokumentRest.INNTEKTSMELDING_FULL_PATH + "/" + imUuid);
    }
}
