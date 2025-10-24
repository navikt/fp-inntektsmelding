package no.nav.familie.inntektsmelding.forespørsel.tjenester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import no.nav.familie.inntektsmelding.integrasjoner.altinn.DialogportenKlient;
import no.nav.familie.inntektsmelding.koder.ArbeidsgiverinitiertÅrsak;
import no.nav.familie.inntektsmelding.koder.ForespørselType;

import org.jboss.jdeparser.FormatPreferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.database.JpaExtension;
import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselRepository;
import no.nav.familie.inntektsmelding.forvaltning.rest.InntektsmeldingForespørselDto;
import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.MinSideArbeidsgiverTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.Merkelapp;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.Organisasjon;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.ForespørselResultat;
import no.nav.familie.inntektsmelding.typer.dto.NyBeskjedResultat;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith({JpaExtension.class, MockitoExtension.class})
class ForespørselBehandlingTjenesteTest extends EntityManagerAwareTest {

    private static final String BRREG_ORGNUMMER = "974760673";
    private static final String AKTØR_ID = "1234567891234";
    private static final String SAK_ID = "1";
    private static final String OPPGAVE_ID = "2";
    private static final String SAK_ID_2 = "3";
    private static final String SAKSNUMMMER = "FAGSAK_SAKEN";
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now().minusYears(1);
    private static final LocalDate FØRSTE_UTTAKSDATO = LocalDate.now().minusYears(1).plusDays(1);
    private static final Ytelsetype YTELSETYPE = Ytelsetype.FORELDREPENGER;

    @Mock
    private MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste;
    @Mock
    private PersonTjeneste personTjeneste;
    @Mock
    private OrganisasjonTjeneste organisasjonTjeneste;
    @Mock
    private DialogportenKlient dialogportenKlient;

    private ForespørselRepository forespørselRepository;
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    @BeforeEach
    void setUp() {
        this.forespørselRepository = new ForespørselRepository(getEntityManager());
        this.forespørselBehandlingTjeneste = new ForespørselBehandlingTjeneste(new ForespørselTjeneste(forespørselRepository),
            minSideArbeidsgiverTjeneste,
            personTjeneste,
            organisasjonTjeneste,
            dialogportenKlient);
    }

    @Test
    void skal_opprette_forespørsel_og_sette_sak_og_oppgave() {
        mockInfoForOpprettelse(AKTØR_ID, YTELSETYPE, BRREG_ORGNUMMER, SAK_ID, OPPGAVE_ID);
        when(organisasjonTjeneste.finnOrganisasjon(BRREG_ORGNUMMER)).thenReturn(new Organisasjon("test org", BRREG_ORGNUMMER));

        var resultat = forespørselBehandlingTjeneste.håndterInnkommendeForespørsel(SKJÆRINGSTIDSPUNKT,
            YTELSETYPE,
            new AktørIdEntitet(AKTØR_ID),
            new OrganisasjonsnummerDto(BRREG_ORGNUMMER),
            new SaksnummerDto(SAKSNUMMMER),
            SKJÆRINGSTIDSPUNKT
        );

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsler(new SaksnummerDto(SAKSNUMMMER));

        assertThat(resultat).isEqualTo(ForespørselResultat.FORESPØRSEL_OPPRETTET);
        assertThat(lagret).hasSize(1);
        assertThat(lagret.getFirst().getArbeidsgiverNotifikasjonSakId()).isEqualTo(SAK_ID);
        assertThat(lagret.getFirst().getOppgaveId()).isEqualTo(Optional.of(OPPGAVE_ID));
    }

    @Test
    void eksisterende_forespørsel_på_samme_stp_skal_gi_nei() {
        forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER, SKJÆRINGSTIDSPUNKT,
            ForespørselType.BESTILT_AV_FAGSYSTEM);

        getEntityManager().clear();

        var resultat = forespørselBehandlingTjeneste.håndterInnkommendeForespørsel(SKJÆRINGSTIDSPUNKT,
            YTELSETYPE,
            new AktørIdEntitet(AKTØR_ID),
            new OrganisasjonsnummerDto(BRREG_ORGNUMMER),
            new SaksnummerDto(SAKSNUMMMER),
            SKJÆRINGSTIDSPUNKT
        );

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsler(new SaksnummerDto(SAKSNUMMMER));
        assertThat(resultat).isEqualTo(ForespørselResultat.IKKE_OPPRETTET_FINNES_ALLEREDE);
        assertThat(lagret).hasSize(1);
    }

    @Test
    void skal_ikke_opprette_forespørsel_når_finnes_allerede_for_stp_og_første_uttaksdato() {
        mockInfoForOpprettelse(AKTØR_ID, YTELSETYPE, BRREG_ORGNUMMER, SAK_ID, OPPGAVE_ID);
        when(organisasjonTjeneste.finnOrganisasjon(BRREG_ORGNUMMER)).thenReturn(new Organisasjon("test org", BRREG_ORGNUMMER));

        forespørselBehandlingTjeneste.håndterInnkommendeForespørsel(SKJÆRINGSTIDSPUNKT,
            YTELSETYPE,
            new AktørIdEntitet(AKTØR_ID),
            new OrganisasjonsnummerDto(BRREG_ORGNUMMER),
            new SaksnummerDto(SAKSNUMMMER),
            FØRSTE_UTTAKSDATO
        );

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsler(new SaksnummerDto(SAKSNUMMMER)).getFirst();
        var fpEntitet = forespørselBehandlingTjeneste.ferdigstillForespørsel(lagret.getUuid(),
            lagret.getAktørId(),
            new OrganisasjonsnummerDto(lagret.getOrganisasjonsnummer()),
            lagret.getFørsteUttaksdato(),
            LukkeÅrsak.EKSTERN_INNSENDING, Optional.empty());

        assertThat(fpEntitet.getStatus()).isEqualTo(ForespørselStatus.FERDIG);

        var resultat2 = forespørselBehandlingTjeneste.håndterInnkommendeForespørsel(SKJÆRINGSTIDSPUNKT,
            YTELSETYPE,
            new AktørIdEntitet(AKTØR_ID),
            new OrganisasjonsnummerDto(BRREG_ORGNUMMER),
            new SaksnummerDto(SAKSNUMMMER),
            FØRSTE_UTTAKSDATO
        );

        assertThat(resultat2).isEqualTo(ForespørselResultat.IKKE_OPPRETTET_FINNES_ALLEREDE);
    }

    @Test
    void skal_opprette_forespørsel_når_finnes_allerede_for_samme_stp_og_ulik_uttaksdato() {
        mockInfoForOpprettelse(AKTØR_ID, YTELSETYPE, BRREG_ORGNUMMER, SAK_ID, OPPGAVE_ID);
        when(organisasjonTjeneste.finnOrganisasjon(BRREG_ORGNUMMER)).thenReturn(new Organisasjon("test org", BRREG_ORGNUMMER));

        forespørselBehandlingTjeneste.håndterInnkommendeForespørsel(SKJÆRINGSTIDSPUNKT,
            YTELSETYPE,
            new AktørIdEntitet(AKTØR_ID),
            new OrganisasjonsnummerDto(BRREG_ORGNUMMER),
            new SaksnummerDto(SAKSNUMMMER),
            FØRSTE_UTTAKSDATO
        );

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsler(new SaksnummerDto(SAKSNUMMMER)).getFirst();

        var fpEntitet = forespørselBehandlingTjeneste.ferdigstillForespørsel(lagret.getUuid(),
            lagret.getAktørId(),
            new OrganisasjonsnummerDto(lagret.getOrganisasjonsnummer()),
            lagret.getFørsteUttaksdato(),
            LukkeÅrsak.EKSTERN_INNSENDING, Optional.empty());

        assertThat(fpEntitet.getStatus()).isEqualTo(ForespørselStatus.FERDIG);

        var resultat2 = forespørselBehandlingTjeneste.håndterInnkommendeForespørsel(SKJÆRINGSTIDSPUNKT,
            YTELSETYPE,
            new AktørIdEntitet(AKTØR_ID),
            new OrganisasjonsnummerDto(BRREG_ORGNUMMER),
            new SaksnummerDto(SAKSNUMMMER),
            FØRSTE_UTTAKSDATO.plusDays(1)
        );

        assertThat(resultat2).isEqualTo(ForespørselResultat.FORESPØRSEL_OPPRETTET);
    }

    @Test
    void skal_opprette_ny_forespørsel_med_nytt_stp_og_sette_mottatte_forespørsel_med_tidligere_stp_til_utgått() {
        mockInfoForOpprettelse(AKTØR_ID, YTELSETYPE, BRREG_ORGNUMMER, SAK_ID, OPPGAVE_ID);
        when(organisasjonTjeneste.finnOrganisasjon(BRREG_ORGNUMMER)).thenReturn(new Organisasjon("test org", BRREG_ORGNUMMER));
        var saksnummerDto = new SaksnummerDto(SAKSNUMMMER);

        forespørselBehandlingTjeneste.håndterInnkommendeForespørsel(SKJÆRINGSTIDSPUNKT,
            YTELSETYPE,
            new AktørIdEntitet(AKTØR_ID),
            new OrganisasjonsnummerDto(BRREG_ORGNUMMER),
            saksnummerDto,
            FØRSTE_UTTAKSDATO
        );

        var lagret = forespørselRepository.hentForespørsler(saksnummerDto).getFirst();

        var fpEntitet = forespørselBehandlingTjeneste.ferdigstillForespørsel(lagret.getUuid(),
            lagret.getAktørId(),
            new OrganisasjonsnummerDto(lagret.getOrganisasjonsnummer()),
            lagret.getFørsteUttaksdato(),
            LukkeÅrsak.ORDINÆR_INNSENDING, Optional.empty());

        assertThat(fpEntitet.getStatus()).isEqualTo(ForespørselStatus.FERDIG);

        var resultat2 = forespørselBehandlingTjeneste.håndterInnkommendeForespørsel(SKJÆRINGSTIDSPUNKT.plusMonths(2),
            YTELSETYPE,
            new AktørIdEntitet(AKTØR_ID),
            new OrganisasjonsnummerDto(BRREG_ORGNUMMER),
            new SaksnummerDto(SAKSNUMMMER),
            FØRSTE_UTTAKSDATO
        );

        var forrigeForespørsel = forespørselRepository.hentForespørsel(lagret.getUuid());

        clearHibernateCache();
        assertThat(forrigeForespørsel.map(ForespørselEntitet::getStatus)).isEqualTo(Optional.of(ForespørselStatus.UTGÅTT));
        assertThat(resultat2).isEqualTo(ForespørselResultat.FORESPØRSEL_OPPRETTET);
    }

    @Test
    void skal_opprette_opprette_arbeidsgiverinitiert_forespørsel_uten_oppgave() {
        mockInfoForOpprettelse(AKTØR_ID, YTELSETYPE, BRREG_ORGNUMMER, SAK_ID, OPPGAVE_ID);

        var uuid = forespørselBehandlingTjeneste.opprettForespørselForArbeidsgiverInitiertIm(YTELSETYPE,
            new AktørIdEntitet(AKTØR_ID),
            new OrganisasjonsnummerDto(BRREG_ORGNUMMER),
            FØRSTE_UTTAKSDATO, ArbeidsgiverinitiertÅrsak.NYANSATT,
            null);

        var lagret = forespørselRepository.hentForespørsel(uuid).orElseThrow();

        clearHibernateCache();
        assertThat(lagret.getStatus()).isEqualTo(ForespørselStatus.UNDER_BEHANDLING);
        assertThat(lagret.getOppgaveId()).isEmpty();
        assertThat(lagret.getFørsteUttaksdato()).isEqualTo(FØRSTE_UTTAKSDATO);
    }

    @Test
    void skal_opprette_opprette_arbeidsgiverinitiert_forespørsel_med_skjæringstidspunkt() {
        mockInfoForOpprettelse(AKTØR_ID, YTELSETYPE, BRREG_ORGNUMMER, SAK_ID, OPPGAVE_ID);
        var forventetSkjæringstidspunkt = FØRSTE_UTTAKSDATO.minusDays(1);
        var uuid = forespørselBehandlingTjeneste.opprettForespørselForArbeidsgiverInitiertIm(YTELSETYPE,
            new AktørIdEntitet(AKTØR_ID),
            new OrganisasjonsnummerDto(BRREG_ORGNUMMER),
            FØRSTE_UTTAKSDATO,
            ArbeidsgiverinitiertÅrsak.UREGISTRERT,
            forventetSkjæringstidspunkt);

        var lagret = forespørselRepository.hentForespørsel(uuid).orElseThrow();

        clearHibernateCache();
        assertThat(lagret.getStatus()).isEqualTo(ForespørselStatus.UNDER_BEHANDLING);
        assertThat(lagret.getOppgaveId()).isEmpty();
        assertThat(lagret.getFørsteUttaksdato()).isEqualTo(FØRSTE_UTTAKSDATO);
        assertThat(lagret.getSkjæringstidspunkt()).isEqualTo(Optional.of(forventetSkjæringstidspunkt));
    }

    @Test
    void skal_ferdigstille_forespørsel() {
        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER,
            SKJÆRINGSTIDSPUNKT, ForespørselType.BESTILT_AV_FAGSYSTEM);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);

        forespørselBehandlingTjeneste.ferdigstillForespørsel(forespørselUuid,
            new AktørIdEntitet(AKTØR_ID),
            new OrganisasjonsnummerDto(BRREG_ORGNUMMER),
            SKJÆRINGSTIDSPUNKT,
            LukkeÅrsak.EKSTERN_INNSENDING, Optional.empty());

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsel(forespørselUuid);
        assertThat(lagret.map(ForespørselEntitet::getStatus)).isEqualTo(Optional.of(ForespørselStatus.FERDIG));
    }

    @Test
    void skal_ferdigstille_forespørsel_ulik_stp_og_startdato() {
        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER,
            FØRSTE_UTTAKSDATO, ForespørselType.BESTILT_AV_FAGSYSTEM);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);

        forespørselBehandlingTjeneste.ferdigstillForespørsel(forespørselUuid,
            new AktørIdEntitet(AKTØR_ID),
            new OrganisasjonsnummerDto(BRREG_ORGNUMMER),
            FØRSTE_UTTAKSDATO,
            LukkeÅrsak.EKSTERN_INNSENDING, Optional.empty());

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsel(forespørselUuid);
        assertThat(lagret.map(ForespørselEntitet::getStatus)).isEqualTo(Optional.of(ForespørselStatus.FERDIG));
    }

    @Test
    void skal_sette_alle_forespørspørsler_for_sak_til_ferdig() {
        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER,
            FØRSTE_UTTAKSDATO, ForespørselType.BESTILT_AV_FAGSYSTEM);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);
        var forespørselUuid2 = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT.plusDays(2),
            YTELSETYPE,
            AKTØR_ID,
            BRREG_ORGNUMMER,
            SAKSNUMMMER,
            FØRSTE_UTTAKSDATO.plusDays(1), ForespørselType.BESTILT_AV_FAGSYSTEM);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid2, "2");

        forespørselBehandlingTjeneste.lukkForespørsel(new SaksnummerDto(SAKSNUMMMER), new OrganisasjonsnummerDto(BRREG_ORGNUMMER), null);

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsel(forespørselUuid);
        assertThat(lagret.map(ForespørselEntitet::getStatus)).isEqualTo(Optional.of(ForespørselStatus.FERDIG));
        var lagret2 = forespørselRepository.hentForespørsel(forespørselUuid2);
        assertThat(lagret2.map(ForespørselEntitet::getStatus)).isEqualTo(Optional.of(ForespørselStatus.FERDIG));
    }

    @Test
    void skal_sette_alle_forespørspørsler_for_sak_til_utgått() {
        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER,
            FØRSTE_UTTAKSDATO, ForespørselType.BESTILT_AV_FAGSYSTEM);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);
        var forespørselUuid2 = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT.plusDays(2),
            YTELSETYPE,
            AKTØR_ID,
            BRREG_ORGNUMMER,
            SAKSNUMMMER,
            FØRSTE_UTTAKSDATO, ForespørselType.BESTILT_AV_FAGSYSTEM);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid2, "2");

        forespørselBehandlingTjeneste.settForespørselTilUtgått(new SaksnummerDto(SAKSNUMMMER), null, null);

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsel(forespørselUuid);
        assertThat(lagret.map(ForespørselEntitet::getStatus)).isEqualTo(Optional.of(ForespørselStatus.UTGÅTT));
        var lagret2 = forespørselRepository.hentForespørsel(forespørselUuid2);
        assertThat(lagret2.map(ForespørselEntitet::getStatus)).isEqualTo(Optional.of(ForespørselStatus.UTGÅTT));
    }

    @Test
    void skal_lukke_forespørsel_for_sak_med_gitt_stp() {
        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER,
            SKJÆRINGSTIDSPUNKT, ForespørselType.BESTILT_AV_FAGSYSTEM);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);

        var forespørselUuid2 = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT.plusDays(2),
            YTELSETYPE,
            AKTØR_ID,
            BRREG_ORGNUMMER,
            SAKSNUMMMER,
            SKJÆRINGSTIDSPUNKT, ForespørselType.BESTILT_AV_FAGSYSTEM);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, "2");

        forespørselBehandlingTjeneste.lukkForespørsel(new SaksnummerDto(SAKSNUMMMER),
            new OrganisasjonsnummerDto(BRREG_ORGNUMMER),
            SKJÆRINGSTIDSPUNKT);

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsel(forespørselUuid);
        assertThat(lagret.map(ForespørselEntitet::getStatus)).isEqualTo(Optional.of(ForespørselStatus.FERDIG));
        var lagret2 = forespørselRepository.hentForespørsel(forespørselUuid2);
        assertThat(lagret2.map(ForespørselEntitet::getStatus)).isEqualTo(Optional.of(ForespørselStatus.UNDER_BEHANDLING));
    }

    @Test
    void skal_slette_oppgave_gitt_saksnummer_og_orgnr() {
        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER,
            SKJÆRINGSTIDSPUNKT, ForespørselType.BESTILT_AV_FAGSYSTEM);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);

        when(minSideArbeidsgiverTjeneste.slettSak(SAK_ID)).thenReturn(SAK_ID);

        forespørselBehandlingTjeneste.slettForespørsel(new SaksnummerDto(SAKSNUMMMER), new OrganisasjonsnummerDto(BRREG_ORGNUMMER), null);

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsel(forespørselUuid);
        assertThat(lagret.map(ForespørselEntitet::getStatus)).isEqualTo(Optional.of(ForespørselStatus.UTGÅTT));
        verify(minSideArbeidsgiverTjeneste, Mockito.times(1)).slettSak(SAK_ID);
    }

    @Test
    void skal_slette_oppgave_gitt_saksnummer() {
        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT, YTELSETYPE, AKTØR_ID, BRREG_ORGNUMMER, SAKSNUMMMER,
            SKJÆRINGSTIDSPUNKT, ForespørselType.BESTILT_AV_FAGSYSTEM);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);

        when(minSideArbeidsgiverTjeneste.slettSak(SAK_ID)).thenReturn(SAK_ID);

        forespørselBehandlingTjeneste.slettForespørsel(new SaksnummerDto(SAKSNUMMMER), null, null);

        clearHibernateCache();

        var lagret = forespørselRepository.hentForespørsel(forespørselUuid);
        assertThat(lagret.map(ForespørselEntitet::getStatus)).isEqualTo(Optional.of(ForespørselStatus.UTGÅTT));
        verify(minSideArbeidsgiverTjeneste, Mockito.times(1)).slettSak(SAK_ID);
    }

    @Test
    void skal_opprette_ny_beskjed() {
        String varseltekst = "TEST A/S - orgnr 974760673: Vi har ennå ikke mottatt inntektsmelding. For at vi skal kunne behandle søknaden om foreldrepenger, må inntektsmeldingen sendes inn så raskt som mulig.";
        String beskjedtekst = "Vi har ennå ikke mottatt inntektsmelding for Navn Navnesen. For at vi skal kunne behandle søknaden om foreldrepenger, må inntektsmeldingen sendes inn så raskt som mulig.";
        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT,
            Ytelsetype.FORELDREPENGER,
            AKTØR_ID,
            BRREG_ORGNUMMER,
            SAKSNUMMMER,
            SKJÆRINGSTIDSPUNKT, ForespørselType.BESTILT_AV_FAGSYSTEM);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);
        var uri = URI.create(String.format("https://arbeidsgiver.nav.no/fp-im-dialog/%s", forespørselUuid.toString()));

        var personInfo = new PersonInfo("Navn",
            null,
            "Navnesen",
            new PersonIdent("01019100000"),
            new AktørIdEntitet(AKTØR_ID),
            LocalDate.of(1991, 1, 1).minusYears(30),
            null,
            null);

        when(organisasjonTjeneste.finnOrganisasjon(BRREG_ORGNUMMER)).thenReturn(new Organisasjon("Test A/S", BRREG_ORGNUMMER));
        when(minSideArbeidsgiverTjeneste.opprettNyBeskjedMedEksternVarsling(forespørselUuid.toString(),
            Merkelapp.INNTEKTSMELDING_FP,
            forespørselUuid.toString(),
            BRREG_ORGNUMMER,
            beskjedtekst,
            varseltekst,
            uri)).thenReturn("beskjedId");
        when(personTjeneste.hentPersonInfoFraAktørId(new AktørIdEntitet(AKTØR_ID), Ytelsetype.FORELDREPENGER)).thenReturn(personInfo);

        var resultat = forespørselBehandlingTjeneste.opprettNyBeskjedMedEksternVarsling(new SaksnummerDto(SAKSNUMMMER),
            new OrganisasjonsnummerDto(BRREG_ORGNUMMER));

        clearHibernateCache();

        assertThat(resultat).isEqualTo(NyBeskjedResultat.NY_BESKJED_SENDT);
        verify(minSideArbeidsgiverTjeneste, Mockito.times(1)).opprettNyBeskjedMedEksternVarsling(forespørselUuid.toString(),
            Merkelapp.INNTEKTSMELDING_FP,
            forespørselUuid.toString(),
            BRREG_ORGNUMMER,
            beskjedtekst,
            varseltekst,
            uri);
    }

    @Test
    void skal_gi_riktig_resultat_om_det_ikke_finnes_en_åpen_forespørsel() {
        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT,
            Ytelsetype.FORELDREPENGER,
            AKTØR_ID,
            BRREG_ORGNUMMER,
            SAKSNUMMMER,
            SKJÆRINGSTIDSPUNKT, ForespørselType.BESTILT_AV_FAGSYSTEM);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);
        forespørselRepository.ferdigstillForespørsel(SAK_ID);

        var resultat = forespørselBehandlingTjeneste.opprettNyBeskjedMedEksternVarsling(new SaksnummerDto(SAKSNUMMMER),
            new OrganisasjonsnummerDto(BRREG_ORGNUMMER));
        clearHibernateCache();
        assertThat(resultat).isEqualTo(NyBeskjedResultat.FORESPØRSEL_FINNES_IKKE);
    }


    @Test
    void skal_oppdatere_førsteUttaksdto_for_arbeidsgiverinitert() {
        var forespørselUuid = forespørselRepository.lagreForespørsel(SKJÆRINGSTIDSPUNKT,
            Ytelsetype.FORELDREPENGER,
            AKTØR_ID,
            BRREG_ORGNUMMER,
            SAKSNUMMMER,
            FØRSTE_UTTAKSDATO,
            ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT);
        forespørselRepository.oppdaterArbeidsgiverNotifikasjonSakId(forespørselUuid, SAK_ID);
        forespørselRepository.ferdigstillForespørsel(SAK_ID);

        var forespørsel = forespørselRepository.hentForespørsel(forespørselUuid).orElseThrow();
        var nyFørsteUttaksdato = FØRSTE_UTTAKSDATO.plusWeeks(1);

        var resultat = forespørselBehandlingTjeneste.setFørsteUttaksdato(forespørsel, nyFørsteUttaksdato);

        clearHibernateCache();

        var oppdatertForespørsel = forespørselRepository.hentForespørsel(forespørselUuid).orElseThrow();

        assertThat(resultat.getAktørId()).isEqualTo(oppdatertForespørsel.getAktørId());
        assertThat(resultat.getFørsteUttaksdato()).isEqualTo(nyFørsteUttaksdato);
        assertThat(resultat.getId()).isEqualTo(oppdatertForespørsel.getId());
        assertThat(resultat.getUuid()).isEqualTo(oppdatertForespørsel.getUuid());
        assertThat(resultat.getArbeidsgiverNotifikasjonSakId()).isEqualTo(oppdatertForespørsel.getArbeidsgiverNotifikasjonSakId());
        assertThat(resultat.getStatus()).isEqualTo(oppdatertForespørsel.getStatus());
        assertThat(resultat.getOppgaveId()).isEqualTo(oppdatertForespørsel.getOppgaveId());
        assertThat(resultat.getOrganisasjonsnummer()).isEqualTo(oppdatertForespørsel.getOrganisasjonsnummer());
        assertThat(resultat.getSkjæringstidspunkt()).isEqualTo(oppdatertForespørsel.getSkjæringstidspunkt());
        assertThat(resultat.getYtelseType()).isEqualTo(oppdatertForespørsel.getYtelseType());
        assertThat(resultat.getFagsystemSaksnummer()).isEqualTo(oppdatertForespørsel.getFagsystemSaksnummer());
        assertThat(resultat.getForespørselType()).isEqualTo(oppdatertForespørsel.getForespørselType());
    }
    @Test
    void skal_returnere_liste_av_inntektsmeldingdto_for_forespørsler() {

        var forespørsel1sak1 = new ForespørselEntitet(BRREG_ORGNUMMER,
            LocalDate.of(2025, 1, 1),
            new AktørIdEntitet(AKTØR_ID),
            Ytelsetype.FORELDREPENGER,
            SAK_ID,
            LocalDate.of(2025, 1, 1), ForespørselType.BESTILT_AV_FAGSYSTEM);
        var forespørsel1sak2 = new ForespørselEntitet(BRREG_ORGNUMMER,
            LocalDate.of(2025, 2, 1),
            new AktørIdEntitet(AKTØR_ID),
            Ytelsetype.FORELDREPENGER,
            SAK_ID_2,
            LocalDate.of(2025, 2, 1), ForespørselType.BESTILT_AV_FAGSYSTEM);
        var forespørsel2sak1 = new ForespørselEntitet(BRREG_ORGNUMMER,
            LocalDate.of(2025, 3, 1),
            new AktørIdEntitet(AKTØR_ID),
            Ytelsetype.FORELDREPENGER,
            SAK_ID,
            LocalDate.of(2025, 3, 1), ForespørselType.BESTILT_AV_FAGSYSTEM);
        var forespørsel2sak2 = new ForespørselEntitet(BRREG_ORGNUMMER,
            LocalDate.of(2025, 4, 1),
            new AktørIdEntitet(AKTØR_ID),
            Ytelsetype.FORELDREPENGER,
            SAK_ID_2,
            LocalDate.of(2025, 4, 1), ForespørselType.BESTILT_AV_FAGSYSTEM);

        getEntityManager().persist(forespørsel1sak1);
        getEntityManager().persist(forespørsel1sak2);
        getEntityManager().persist(forespørsel2sak1);
        getEntityManager().persist(forespørsel2sak2);
        getEntityManager().flush();

        List<InntektsmeldingForespørselDto> inntektsmeldingForespørselDtos = forespørselBehandlingTjeneste.finnForespørslerForFagsak(new SaksnummerDto(
            SAK_ID));

        assertThat(inntektsmeldingForespørselDtos).hasSize(2);
        var dto1 = inntektsmeldingForespørselDtos.stream()
            .filter(forespørsel -> forespørsel.skjæringstidspunkt().equals(forespørsel1sak1.getSkjæringstidspunkt().orElse(null)))
            .findAny()
            .orElseThrow();
        var dto2 = inntektsmeldingForespørselDtos.stream()
            .filter(forespørsel -> forespørsel.skjæringstidspunkt().equals(forespørsel2sak1.getSkjæringstidspunkt().orElse(null)))
            .findAny()
            .orElseThrow();

        assertThat(dto1.aktørid()).isEqualTo(forespørsel1sak1.getAktørId().getAktørId());
        assertThat(dto1.skjæringstidspunkt()).isEqualTo(forespørsel1sak1.getSkjæringstidspunkt().orElse(null));
        assertThat(dto1.ytelsetype()).isEqualTo(forespørsel1sak1.getYtelseType().toString());
        assertThat(dto1.uuid()).isEqualTo(forespørsel1sak1.getUuid());
        assertThat(dto1.arbeidsgiverident()).isEqualTo(forespørsel1sak1.getOrganisasjonsnummer());

        assertThat(dto2.aktørid()).isEqualTo(forespørsel2sak1.getAktørId().getAktørId());
        assertThat(dto2.skjæringstidspunkt()).isEqualTo(forespørsel2sak1.getSkjæringstidspunkt().orElse(null));
        assertThat(dto2.ytelsetype()).isEqualTo(forespørsel2sak1.getYtelseType().toString());
        assertThat(dto2.uuid()).isEqualTo(forespørsel2sak1.getUuid());
        assertThat(dto2.arbeidsgiverident()).isEqualTo(forespørsel2sak1.getOrganisasjonsnummer());

    }

    private void clearHibernateCache() {
        // Fjerne hibernate cachen før assertions skal evalueres - hibernate ignorerer alle updates som er markert med updatable = false ved skriving mot databasen
        // men objektene i cachen blir oppdatert helt greit likevel.
        // På denne måten evaluerer vi faktisk tilstanden som blir til slutt lagret i databasen.
        getEntityManager().clear();
    }

    private void mockInfoForOpprettelse(String aktørId, Ytelsetype ytelsetype, String brregOrgnummer, String sakId, String oppgaveId) {
        var personInfo = new PersonInfo("Navn",
            null,
            "Navnesen",
            new PersonIdent("01019100000"),
            new AktørIdEntitet(aktørId),
            LocalDate.of(1991, 1, 1).minusYears(30),
            null,
            null);
        var sakTittel = ForespørselTekster.lagSaksTittel(personInfo.mapFulltNavn(), personInfo.fødselsdato());

        lenient().when(personTjeneste.hentPersonInfoFraAktørId(new AktørIdEntitet(aktørId), ytelsetype)).thenReturn(personInfo);
        lenient().when(minSideArbeidsgiverTjeneste.opprettOppgave(any(), any(), any(), eq(brregOrgnummer), any(), any(), any(), any()))
            .thenReturn(oppgaveId);
        lenient().when(minSideArbeidsgiverTjeneste.opprettSak(any(), any(), eq(brregOrgnummer), eq(sakTittel), any())).thenReturn(sakId);
    }
}
