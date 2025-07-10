package no.nav.familie.inntektsmelding.forespørsel.tjenester;

import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.familie.inntektsmelding.integrasjoner.altinn.DialogportenKlient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forvaltning.rest.InntektsmeldingForespørselDto;
import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.MinSideArbeidsgiverTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.ArbeidsgiverinitiertÅrsak;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.koder.ForespørselType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.metrikker.MetrikkerTjeneste;
import no.nav.familie.inntektsmelding.typer.dto.ForespørselResultat;
import no.nav.familie.inntektsmelding.typer.dto.NyBeskjedResultat;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.foreldrepenger.konfig.Environment;

/**
 * Okrestreringsklasse som håndterer alle endringer på en forespørsel, og synkroniserer dette på tvers av intern database og eksterne systemer.
 */
@ApplicationScoped
public class ForespørselBehandlingTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(ForespørselBehandlingTjeneste.class);

    private static final no.nav.foreldrepenger.konfig.Environment ENV = Environment.current();

    private ForespørselTjeneste forespørselTjeneste;
    private MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste;
    private PersonTjeneste personTjeneste;
    private OrganisasjonTjeneste organisasjonTjeneste;
    private String inntektsmeldingSkjemaLenke;
    private DialogportenKlient dialogportenKlient;

    public ForespørselBehandlingTjeneste() {
        // CDI
    }

    @Inject
    public ForespørselBehandlingTjeneste(ForespørselTjeneste forespørselTjeneste,
                                         MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste,
                                         PersonTjeneste personTjeneste,
                                         OrganisasjonTjeneste organisasjonTjeneste, DialogportenKlient dialogportenKlient) {
        this.forespørselTjeneste = forespørselTjeneste;
        this.minSideArbeidsgiverTjeneste = minSideArbeidsgiverTjeneste;
        this.personTjeneste = personTjeneste;
        this.organisasjonTjeneste = organisasjonTjeneste;
        this.dialogportenKlient = dialogportenKlient;
        this.inntektsmeldingSkjemaLenke = ENV.getProperty("inntektsmelding.skjema.lenke", "https://arbeidsgiver.intern.dev.nav.no/fp-im-dialog");
    }

    public ForespørselResultat håndterInnkommendeForespørsel(LocalDate skjæringstidspunkt,
                                                             Ytelsetype ytelsetype,
                                                             AktørIdEntitet aktørId,
                                                             OrganisasjonsnummerDto organisasjonsnummer,
                                                             SaksnummerDto fagsakSaksnummer,
                                                             LocalDate førsteUttaksdato) {
        var finnesForespørsel = forespørselTjeneste.finnGjeldendeForespørsel(skjæringstidspunkt,
            ytelsetype,
            aktørId,
            organisasjonsnummer,
            fagsakSaksnummer,
            førsteUttaksdato);

        if (finnesForespørsel.isPresent()) {
            LOG.info("Finnes allerede forespørsel for saksnummer: {} med orgnummer: {} med skjæringstidspunkt: {} og første uttaksdato: {}",
                fagsakSaksnummer,
                organisasjonsnummer,
                skjæringstidspunkt,
                førsteUttaksdato);
            return ForespørselResultat.IKKE_OPPRETTET_FINNES_ALLEREDE;
        }

        settFerdigeForespørslerForTidligereStpTilUtgått(skjæringstidspunkt, fagsakSaksnummer, organisasjonsnummer);
        opprettForespørsel(ytelsetype, aktørId, fagsakSaksnummer, organisasjonsnummer, skjæringstidspunkt, førsteUttaksdato);

        return ForespørselResultat.FORESPØRSEL_OPPRETTET;
    }

    private void settFerdigeForespørslerForTidligereStpTilUtgått(LocalDate skjæringstidspunktFraRequest,
                                                                 SaksnummerDto fagsakSaksnummer,
                                                                 OrganisasjonsnummerDto organisasjonsnummerFraRequest) {
        LOG.info("ForespørselBehandlingTjenesteImpl: settFerdigeForespørslerForTidligereStpTilUtgått for saksnummer: {}, orgnummer: {} med stp: {}",
            fagsakSaksnummer,
            organisasjonsnummerFraRequest,
            skjæringstidspunktFraRequest);

        //Vi sjekker kun mot FERDIGE forespørsler da fpsak allerede har lukket forespørsler som er UNDER_BEHANDLING
        forespørselTjeneste.finnForespørslerForFagsak(fagsakSaksnummer).stream()
            .filter(forespørselEntitet -> organisasjonsnummerFraRequest.orgnr().equals(forespørselEntitet.getOrganisasjonsnummer()))
            .filter(forespørselEntitet -> !skjæringstidspunktFraRequest.equals(forespørselEntitet.getSkjæringstidspunkt().orElse(null)))
            .filter(forespørselEntitet -> ForespørselStatus.FERDIG.equals(forespørselEntitet.getStatus()))
            .forEach(forespørselEntitet -> settForespørselTilUtgått(forespørselEntitet, false));
    }

    public ForespørselEntitet ferdigstillForespørsel(UUID foresporselUuid,
                                                     AktørIdEntitet aktorId,
                                                     OrganisasjonsnummerDto organisasjonsnummerDto,
                                                     LocalDate startdato,
                                                     LukkeÅrsak årsak) {
        var foresporsel = forespørselTjeneste.hentForespørsel(foresporselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel for inntektsmelding, ugyldig tilstand"));

        validerAktør(foresporsel, aktorId);
        validerOrganisasjon(foresporsel, organisasjonsnummerDto);
        validerStartdato(foresporsel, startdato);


        // Arbeidsgiverinitierte forespørsler har ingen oppgave
        foresporsel.getOppgaveId().ifPresent(oppgaveId -> minSideArbeidsgiverTjeneste.oppgaveUtført(oppgaveId, OffsetDateTime.now()));

        var erArbeidsgiverInitiertInntektsmelding = foresporsel.getOppgaveId().isEmpty();
        minSideArbeidsgiverTjeneste.ferdigstillSak(foresporsel.getArbeidsgiverNotifikasjonSakId(), erArbeidsgiverInitiertInntektsmelding
        ); // Oppdaterer status i arbeidsgiver-notifikasjon
        minSideArbeidsgiverTjeneste.oppdaterSakTilleggsinformasjon(foresporsel.getArbeidsgiverNotifikasjonSakId(),
            ForespørselTekster.lagTilleggsInformasjon(årsak));
        forespørselTjeneste.ferdigstillForespørsel(foresporsel.getArbeidsgiverNotifikasjonSakId()); // Oppdaterer status i forespørsel
        return foresporsel;
    }

    public Optional<ForespørselEntitet> hentForespørsel(UUID forespørselUUID) {
        return forespørselTjeneste.hentForespørsel(forespørselUUID);
    }

    public List<ForespørselEntitet> finnForespørsler(AktørIdEntitet aktørId, Ytelsetype ytelsetype, String orgnr) {
        return forespørselTjeneste.finnForespørsler(aktørId, ytelsetype, orgnr);
    }

    public List<ForespørselEntitet> finnForespørslerForAktørId(AktørIdEntitet aktørId, Ytelsetype ytelsetype) {
        return forespørselTjeneste.finnForespørslerForAktørid(aktørId, ytelsetype);
    }

    public void settForespørselTilUtgått(ForespørselEntitet eksisterendeForespørsel, boolean skalOppdatereArbeidsgiverNotifikasjon) {
        if (skalOppdatereArbeidsgiverNotifikasjon) {
            eksisterendeForespørsel.getOppgaveId().ifPresent( oppgaveId -> minSideArbeidsgiverTjeneste.oppgaveUtgått(oppgaveId, OffsetDateTime.now()));
            minSideArbeidsgiverTjeneste.ferdigstillSak(eksisterendeForespørsel.getArbeidsgiverNotifikasjonSakId(), false); // Oppdaterer status i arbeidsgiver-notifikasjon
        }

        minSideArbeidsgiverTjeneste.oppdaterSakTilleggsinformasjon(eksisterendeForespørsel.getArbeidsgiverNotifikasjonSakId(),
            ForespørselTekster.lagTilleggsInformasjon(LukkeÅrsak.UTGÅTT));
        forespørselTjeneste.settForespørselTilUtgått(eksisterendeForespørsel.getArbeidsgiverNotifikasjonSakId());

        var msg = String.format("Setter forespørsel til utgått, orgnr: %s, stp: %s, saksnr: %s, ytelse: %s",
            eksisterendeForespørsel.getOrganisasjonsnummer(),
            eksisterendeForespørsel.getSkjæringstidspunkt().orElse(null),
            eksisterendeForespørsel.getFagsystemSaksnummer().orElse(null),
            eksisterendeForespørsel.getYtelseType());
        LOG.info(msg);
    }

    public void opprettForespørsel(Ytelsetype ytelsetype,
                                   AktørIdEntitet aktørId,
                                   SaksnummerDto fagsakSaksnummer,
                                   OrganisasjonsnummerDto organisasjonsnummer,
                                   LocalDate skjæringstidspunkt,
                                   LocalDate førsteUttaksdato) {
        var msg = String.format("Oppretter forespørsel, orgnr: %s, stp: %s, saksnr: %s, ytelse: %s",
            organisasjonsnummer,
            skjæringstidspunkt,
            fagsakSaksnummer.saksnr(),
            ytelsetype);
        LOG.info(msg);

        // Oppretter forespørsel i lokal database
        var forespørselUuid = forespørselTjeneste.opprettForespørsel(skjæringstidspunkt,
            ytelsetype,
            aktørId,
            organisasjonsnummer,
            fagsakSaksnummer,
            førsteUttaksdato);

        opprettForespørselMinSideArbeidsgiver(forespørselUuid, organisasjonsnummer, aktørId, ytelsetype);

        if (ENV.isDev()) {
            opprettForespørselDialogporten(forespørselUuid, organisasjonsnummer, aktørId, ytelsetype);
        }
    }

    private void opprettForespørselMinSideArbeidsgiver(UUID forespørselUuid,  OrganisasjonsnummerDto orgnummer, AktørIdEntitet aktørIdEntitet, Ytelsetype ytelsetype) {
        var organisasjon = organisasjonTjeneste.finnOrganisasjon(orgnummer.orgnr());

        var person = personTjeneste.hentPersonInfoFraAktørId(aktørIdEntitet, ytelsetype);

        var merkelapp = ForespørselTekster.finnMerkelapp(ytelsetype);
        var skjemaUri = URI.create(inntektsmeldingSkjemaLenke + "/" + forespørselUuid);
        var arbeidsgiverNotifikasjonSakId = minSideArbeidsgiverTjeneste.opprettSak(forespørselUuid.toString(),
            merkelapp,
            orgnummer.orgnr(),
            ForespørselTekster.lagSaksTittel(person.mapFulltNavn(), person.fødselsdato()),
            skjemaUri);

        forespørselTjeneste.setArbeidsgiverNotifikasjonSakId(forespørselUuid, arbeidsgiverNotifikasjonSakId);

        String oppgaveId;
        try {
            oppgaveId = minSideArbeidsgiverTjeneste.opprettOppgave(forespørselUuid.toString(),
                merkelapp,
                forespørselUuid.toString(),
                orgnummer.orgnr(),
                ForespørselTekster.lagOppgaveTekst(ytelsetype),
                ForespørselTekster.lagVarselTekst(ytelsetype, organisasjon),
                ForespørselTekster.lagPåminnelseTekst(ytelsetype, organisasjon),
                skjemaUri);
        } catch (Exception e) {
            //Manuell rollback er nødvendig fordi sak og oppgave går i to forskjellige kall
            minSideArbeidsgiverTjeneste.slettSak(arbeidsgiverNotifikasjonSakId);
            throw e;
        }
        forespørselTjeneste.setOppgaveId(forespørselUuid, oppgaveId);
    }

    private void opprettForespørselDialogporten(UUID forespørselUuid, OrganisasjonsnummerDto orgnummer, AktørIdEntitet aktørIdEntitet, Ytelsetype ytelsetype) {
        var person = personTjeneste.hentPersonInfoFraAktørId(aktørIdEntitet, ytelsetype);
        dialogportenKlient.opprettDialog(forespørselUuid, orgnummer, ForespørselTekster.lagSaksTittel(person.mapFulltNavn(), person.fødselsdato()));
    }

    public UUID opprettForespørselForArbeidsgiverInitiertIm(Ytelsetype ytelsetype,
                                                            AktørIdEntitet aktørId,
                                                            OrganisasjonsnummerDto organisasjonsnummer,
                                                            LocalDate førsteFraværsdato,
                                                            ArbeidsgiverinitiertÅrsak arbeidsgiverinitiertÅrsak,
                                                            LocalDate skjæringstidspunkt) {
        var msg = String.format("Oppretter forespørsel for arbeidsgiverinitiert, orgnr: %s, ytelse: %s",
            organisasjonsnummer,
            ytelsetype);
        LOG.info(msg);
        var forespørselType = utledForespørselType(arbeidsgiverinitiertÅrsak);
        var uuid = forespørselTjeneste.opprettForespørselArbeidsgiverinitiert(ytelsetype,
            aktørId,
            organisasjonsnummer,
            førsteFraværsdato,
            forespørselType,
            skjæringstidspunkt);

        var person = personTjeneste.hentPersonInfoFraAktørId(aktørId, ytelsetype);
        var merkelapp = ForespørselTekster.finnMerkelapp(ytelsetype);
        var skjemaUri = URI.create(inntektsmeldingSkjemaLenke + "/" + uuid);
        var fagerSakId = minSideArbeidsgiverTjeneste.opprettSak(uuid.toString(),
            merkelapp,
            organisasjonsnummer.orgnr(),
            ForespørselTekster.lagSaksTittel(person.mapFulltNavn(), person.fødselsdato()),
            skjemaUri
        );

        forespørselTjeneste.setArbeidsgiverNotifikasjonSakId(uuid, fagerSakId);

        return uuid;
    }

    private ForespørselType utledForespørselType(ArbeidsgiverinitiertÅrsak arbeidsgiverinitiertÅrsak) {
        return switch (arbeidsgiverinitiertÅrsak) {
            case UREGISTRERT -> ForespørselType.ARBEIDSGIVERINITIERT_UREGISTRERT;
            case NYANSATT -> ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT;
        };
    }

    public NyBeskjedResultat opprettNyBeskjedMedEksternVarsling(SaksnummerDto fagsakSaksnummer,
                                                                OrganisasjonsnummerDto organisasjonsnummer) {
        var forespørsel = forespørselTjeneste.finnÅpenForespørslelForFagsak(fagsakSaksnummer, organisasjonsnummer)
            .orElse(null);

        if (forespørsel == null) {
            return NyBeskjedResultat.FORESPØRSEL_FINNES_IKKE;
        }

        var msg = String.format("Oppretter ny beskjed med ekstern varsling, orgnr: %s, stp: %s, saksnr: %s, ytelse: %s",
            organisasjonsnummer,
            forespørsel.getSkjæringstidspunkt().orElse(null),
            fagsakSaksnummer.saksnr(),
            forespørsel.getYtelseType());
        LOG.info(msg);
        var merkelapp = ForespørselTekster.finnMerkelapp(forespørsel.getYtelseType());
        var forespørselUuid = forespørsel.getUuid();
        var skjemaUri = URI.create(inntektsmeldingSkjemaLenke + "/" + forespørselUuid);
        var organisasjon = organisasjonTjeneste.finnOrganisasjon(organisasjonsnummer.orgnr());
        var person = personTjeneste.hentPersonInfoFraAktørId(forespørsel.getAktørId(), forespørsel.getYtelseType());
        var varselTekst = ForespørselTekster.lagVarselFraSaksbehandlerTekst(forespørsel.getYtelseType(), organisasjon);
        var beskjedTekst = ForespørselTekster.lagBeskjedFraSaksbehandlerTekst(forespørsel.getYtelseType(), person.mapFulltNavn());

        minSideArbeidsgiverTjeneste.opprettNyBeskjedMedEksternVarsling(forespørselUuid.toString(),
            merkelapp,
            forespørselUuid.toString(),
            organisasjonsnummer.orgnr(),
            beskjedTekst,
            varselTekst,
            skjemaUri);

        return NyBeskjedResultat.NY_BESKJED_SENDT;
    }

    public void lukkForespørsel(SaksnummerDto fagsakSaksnummer, OrganisasjonsnummerDto orgnummerDto, LocalDate skjæringstidspunkt) {
        var forespørsler = hentÅpneForespørslerForFagsak(fagsakSaksnummer, orgnummerDto, skjæringstidspunkt);

        // Alle inntektsmeldinger sendt inn via arbeidsgiverportal blir lukket umiddelbart etter innsending fra #InntektsmeldingTjeneste,
        // så forespørsler som enda er åpne her blir løst ved innsending fra andre systemer
        forespørsler.forEach(f -> {
            var lukketForespørsel = ferdigstillForespørsel(f.getUuid(),
                f.getAktørId(),
                new OrganisasjonsnummerDto(f.getOrganisasjonsnummer()),
                f.getFørsteUttaksdato(),
                LukkeÅrsak.EKSTERN_INNSENDING);
            MetrikkerTjeneste.loggForespørselLukkEkstern(lukketForespørsel);
        });
    }

    public void settForespørselTilUtgått(SaksnummerDto fagsakSaksnummer, OrganisasjonsnummerDto orgnummerDto, LocalDate skjæringstidspunkt) {
        var forespørsler = hentÅpneForespørslerForFagsak(fagsakSaksnummer, orgnummerDto, skjæringstidspunkt);

        forespørsler.forEach(it -> settForespørselTilUtgått(it, true));
    }

    private List<ForespørselEntitet> hentÅpneForespørslerForFagsak(SaksnummerDto fagsakSaksnummer,
                                                                   OrganisasjonsnummerDto orgnummerDto,
                                                                   LocalDate skjæringstidspunkt) {
        return forespørselTjeneste.finnÅpneForespørslerForFagsak(fagsakSaksnummer).stream()
            .filter(f -> orgnummerDto == null || orgnummerDto.orgnr().equals(f.getOrganisasjonsnummer()))
            .filter(f -> skjæringstidspunkt == null || skjæringstidspunkt.equals(f.getSkjæringstidspunkt().orElse(null)))
            .toList();
    }

    public void slettForespørsel(SaksnummerDto fagsakSaksnummer, OrganisasjonsnummerDto orgnummerDto, LocalDate skjæringstidspunkt) {
        var sakerSomSkalSlettes = forespørselTjeneste.finnForespørslerForFagsak(fagsakSaksnummer).stream()
            .filter(f -> skjæringstidspunkt == null || skjæringstidspunkt.equals(f.getSkjæringstidspunkt().orElse(null)))
            .filter(f -> orgnummerDto == null || f.getOrganisasjonsnummer().equals(orgnummerDto.orgnr()))
            .filter(f -> f.getStatus().equals(ForespørselStatus.UNDER_BEHANDLING))
            .toList();

        if (sakerSomSkalSlettes.size() != 1) {
            var msg = String.format("Fant ikke akkurat 1 sak som skulle slettes. Fant istedet %s saker ", sakerSomSkalSlettes.size());
            throw new IllegalStateException(msg);
        }
        var agPortalSakId = sakerSomSkalSlettes.getFirst().getArbeidsgiverNotifikasjonSakId();
        minSideArbeidsgiverTjeneste.slettSak(agPortalSakId);
        forespørselTjeneste.settForespørselTilUtgått(agPortalSakId);
    }

    public List<InntektsmeldingForespørselDto> finnForespørslerForFagsak(SaksnummerDto fagsakSaksnummer) {
        return forespørselTjeneste.finnForespørslerForFagsak(fagsakSaksnummer).stream().map(forespoersel ->
                new InntektsmeldingForespørselDto(
                    forespoersel.getUuid(),
                    forespoersel.getSkjæringstidspunkt().orElse(null),
                    forespoersel.getOrganisasjonsnummer(),
                    forespoersel.getAktørId().getAktørId(),
                    forespoersel.getYtelseType().toString(),
                    forespoersel.getFørsteUttaksdato()))
            .toList();
    }

    private void validerStartdato(ForespørselEntitet forespørsel, LocalDate startdato) {
        if (!forespørsel.getFørsteUttaksdato().equals(startdato)) {
            throw new IllegalStateException("Startdato var ikke like");
        }
    }

    private void validerOrganisasjon(ForespørselEntitet forespørsel, OrganisasjonsnummerDto orgnummer) {
        if (!forespørsel.getOrganisasjonsnummer().equals(orgnummer.orgnr())) {
            throw new IllegalStateException("Organisasjonsnummer var ikke like");
        }
    }

    private void validerAktør(ForespørselEntitet forespørsel, AktørIdEntitet aktorId) {
        if (!forespørsel.getAktørId().equals(aktorId)) {
            throw new IllegalStateException("AktørId for bruker var ikke like");
        }
    }

    public ForespørselEntitet setFørsteUttaksdato(ForespørselEntitet forespørselEnitet, LocalDate startdato) {
        return forespørselTjeneste.setFørsteUttaksdato(forespørselEnitet, startdato);
    }
}
