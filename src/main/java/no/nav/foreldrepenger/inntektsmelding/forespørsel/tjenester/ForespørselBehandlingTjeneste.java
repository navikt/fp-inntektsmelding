package no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester;

import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forvaltning.rest.InntektsmeldingForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.altinn.DialogportenKlient;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.MinSideArbeidsgiverTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.metrikker.MetrikkerTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Saksnummer;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.ForespørselResultat;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.ForespørselStatusDto;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.NyBeskjedResultat;
import no.nav.foreldrepenger.inntektsmelding.typer.dto.YtelseTypeDto;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ArbeidsgiverinitiertÅrsak;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.ForespørselType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.konfig.KonfigVerdi;

/**
 * Okrestreringsklasse som håndterer alle endringer på en forespørsel, og synkroniserer dette på tvers av intern database og eksterne systemer.
 */
@ApplicationScoped
public class ForespørselBehandlingTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(ForespørselBehandlingTjeneste.class);
    private static final no.nav.foreldrepenger.konfig.Environment ENV = Environment.current();

    private String inntektsmeldingSkjemaLenke;

    private ForespørselTjeneste forespørselTjeneste;
    private MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste;
    private PersonTjeneste personTjeneste;
    private OrganisasjonTjeneste organisasjonTjeneste;
    private DialogportenKlient dialogportenKlient;

    public ForespørselBehandlingTjeneste() {
        // CDI
    }

    @Inject
    public ForespørselBehandlingTjeneste(@KonfigVerdi(value = "inntektsmelding.skjema.lenke", defaultVerdi = "https://arbeidsgiver.nav.no/fp-im-dialog") String inntektsmeldingSkjemaLenke,
                                         ForespørselTjeneste forespørselTjeneste,
                                         MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste,
                                         PersonTjeneste personTjeneste,
                                         OrganisasjonTjeneste organisasjonTjeneste,
                                         DialogportenKlient dialogportenKlient) {
        this.inntektsmeldingSkjemaLenke = inntektsmeldingSkjemaLenke;
        this.forespørselTjeneste = forespørselTjeneste;
        this.minSideArbeidsgiverTjeneste = minSideArbeidsgiverTjeneste;
        this.personTjeneste = personTjeneste;
        this.organisasjonTjeneste = organisasjonTjeneste;
        this.dialogportenKlient = dialogportenKlient;
    }

    public ForespørselResultat håndterInnkommendeForespørsel(LocalDate skjæringstidspunkt,
                                                             Ytelsetype ytelsetype,
                                                             AktørId aktørId,
                                                             Arbeidsgiver arbeidsgiver,
                                                             Saksnummer fagsakSaksnummer,
                                                             LocalDate førsteUttaksdato) {
        var finnesForespørsel = forespørselTjeneste.finnGjeldendeForespørsel(skjæringstidspunkt,
            ytelsetype,
            aktørId,
            arbeidsgiver,
            fagsakSaksnummer,
            førsteUttaksdato);

        if (finnesForespørsel.isPresent()) {
            LOG.info("Finnes allerede forespørsel for saksnummer: {} med orgnummer: {} med skjæringstidspunkt: {} og første uttaksdato: {}",
                fagsakSaksnummer,
                arbeidsgiver,
                skjæringstidspunkt,
                førsteUttaksdato);
            return ForespørselResultat.IKKE_OPPRETTET_FINNES_ALLEREDE;
        }

        settTidligereForespørslerForSaksnummerTilUtgått(fagsakSaksnummer, arbeidsgiver, aktørId);
        opprettForespørsel(ytelsetype, aktørId, fagsakSaksnummer, arbeidsgiver, skjæringstidspunkt, førsteUttaksdato);

        return ForespørselResultat.FORESPØRSEL_OPPRETTET;
    }

    // Vi skal aldri ha mer enn en forespørsel til under_behandling eller ferdig for samme sak med samme orgnummer og aktørid
    private void settTidligereForespørslerForSaksnummerTilUtgått(Saksnummer fagsakSaksnummer,
                                                                 Arbeidsgiver arbeidsgiver,
                                                                 AktørId aktørId) {
        LOG.info("ForespørselBehandlingTjenesteImpl: settTidligereForespørslerForSaksnummerTilUtgått for saksnummer: {}, orgnummer: {}, aktørId: {}  ",
            fagsakSaksnummer,
            arbeidsgiver,
            aktørId);

        forespørselTjeneste.finnForespørslerForFagsak(fagsakSaksnummer).stream()
            .filter(forespørsel -> forespørsel.aktørId().equals(aktørId))
            .filter(forespørselDto -> arbeidsgiver.orgnr().equals(forespørselDto.arbeidsgiver().orgnr()))
            .filter(forespørselDto -> !ForespørselStatus.UTGÅTT.name().equals(forespørselDto.status().name()))
            .forEach(forespørselDto -> settForespørselTilUtgått(forespørselDto, false));
    }

    public ForespørselDto ferdigstillForespørsel(UUID foresporselUuid,
                                                 AktørId aktorId,
                                                 Arbeidsgiver arbeidsgiver,
                                                 LocalDate startdato,
                                                 LukkeÅrsak årsak,
                                                 // inntektsmeldingUuid er optional fordi vi ikke har inntektsmeldingen lagret hvis den er innsendt via Altinn / LPS'er
                                                 Optional<UUID> inntektsmeldingUuid) {
        var forespørsel = forespørselTjeneste.hentForespørsel(foresporselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel for inntektsmelding, ugyldig tilstand"));
        validerAktør(forespørsel, aktorId);
        validerOrganisasjon(forespørsel, arbeidsgiver);
        validerStartdato(forespørsel, startdato);

        var erFørstegangsinnsending = ForespørselStatus.UNDER_BEHANDLING.equals(forespørsel.status());

        // Arbeidsgiverinitierte forespørsler har ingen oppgave
        if (forespørsel.oppgaveId() != null) {
            minSideArbeidsgiverTjeneste.oppgaveUtført(forespørsel.oppgaveId(), OffsetDateTime.now());
        }

        var erArbeidsgiverInitiertInntektsmelding = forespørsel.oppgaveId() == null;
        minSideArbeidsgiverTjeneste.ferdigstillSak(forespørsel.arbeidsgiverNotifikasjonSakId(), erArbeidsgiverInitiertInntektsmelding);

        // Oppdaterer status i arbeidsgiver-notifikasjon
        minSideArbeidsgiverTjeneste.oppdaterSakTilleggsinformasjon(forespørsel.arbeidsgiverNotifikasjonSakId(),
            ForespørselTekster.lagTilleggsInformasjon(årsak, forespørsel.førsteUttaksdato()));

        // Oppdaterer status i forespørsel
        forespørselTjeneste.ferdigstillForespørsel(forespørsel.arbeidsgiverNotifikasjonSakId());

        if (!Environment.current().isProd()) {
            inntektsmeldingUuid.ifPresent(imUuid -> {
                var merkelapp = ForespørselTekster.finnMerkelapp(forespørsel.ytelseType());
                var beskjedTekst = erFørstegangsinnsending ? ForespørselTekster.lagBeskjedOmKvitteringFørsteInnsendingTekst() : ForespørselTekster.lagBeskjedOmOppdatertInntektsmelding();
                var kvitteringUrl = URI.create(inntektsmeldingSkjemaLenke + "/server/api/ekstern/innsendt/inntektsmelding/" +  imUuid);
                minSideArbeidsgiverTjeneste.sendNyBeskjedMedKvittering(foresporselUuid.toString(), merkelapp, foresporselUuid.toString(), arbeidsgiver.orgnr(), beskjedTekst, kvitteringUrl);
            });
        }
        // Oppdaterer status i altinn dialogporten
        if (forespørsel.dialogportenUuid() != null) {
            if (ENV.isDev()) {
                try {
                    dialogportenKlient.ferdigstillDialog(forespørsel.dialogportenUuid(),
                        arbeidsgiver,
                        lagSaksTittelForDialogporten(aktorId, forespørsel.ytelseType()),
                        forespørsel.ytelseType(),
                        forespørsel.førsteUttaksdato(),
                        inntektsmeldingUuid,
                        årsak);
                } catch (Exception e) {
                    // Ikke alle organisasjoner som brukes av Dolly finnes i Tenor, som Altinn bruker for å slå opp bedrifter i test. Må derfor tåle å feile for enkelte kall i dev
                    LOG.warn("Feil ved kall til dialogporten: ", e);
                }
            }
        }
        // Re-fetch to get updated status
        return forespørselTjeneste.hentForespørsel(foresporselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel etter ferdigstilling"));
    }

    public void oppdaterPortalerMedEndretInntektsmelding(ForespørselDto forespørsel,
                                                         Optional<UUID> inntektsmeldingUuid,
                                                         Arbeidsgiver arbeidsgiver) {
        // Oppdater status i arbeidsgiverportalen
        if (!Environment.current().isProd()) {
            inntektsmeldingUuid.ifPresent(imUuid -> {
                var merkelapp = ForespørselTekster.finnMerkelapp(forespørsel.ytelseType());
                var beskjedTekst = ForespørselTekster.lagBeskjedOmOppdatertInntektsmelding();
                var kvitteringUrl = URI.create(inntektsmeldingSkjemaLenke + "/server/api/ekstern/kvittering/inntektsmelding/" +  imUuid);
                minSideArbeidsgiverTjeneste.sendNyBeskjedMedKvittering(forespørsel.toString(), merkelapp, forespørsel.toString(), arbeidsgiver.orgnr(), beskjedTekst, kvitteringUrl);
            });
        }

        // Oppdater status i altinn dialogporten
        var dialogUuid = forespørsel.dialogportenUuid();
        if (dialogUuid != null) {
            dialogportenKlient.oppdaterDialogMedEndretInntektsmelding(dialogUuid,
                arbeidsgiver,
                inntektsmeldingUuid);
        }
    }

    public Optional<ForespørselDto> hentForespørsel(UUID forespørselUUID) {
        return forespørselTjeneste.hentForespørsel(forespørselUUID);
    }

    //TODO: kun test som bruker - fjern?
    public List<ForespørselDto> finnForespørsler(AktørId aktørId, Ytelsetype ytelsetype, String orgnr) {
        return forespørselTjeneste.finnForespørsler(aktørId, ytelsetype, orgnr);
    }

    public List<ForespørselDto> finnForespørslerForAktørId(AktørId aktørId, Ytelsetype ytelsetype) {
        return forespørselTjeneste.finnForespørslerForAktørid(aktørId, ytelsetype);
    }

    public void settForespørselTilUtgått(ForespørselDto eksisterendeForespørsel, boolean skalOppdatereArbeidsgiverNotifikasjon) {
        LOG.info("Verdien for skalOppdatereArbeidsgiverNotifikasjon er: {}", skalOppdatereArbeidsgiverNotifikasjon);

        if (skalOppdatereArbeidsgiverNotifikasjon) {
            Optional.ofNullable(eksisterendeForespørsel.oppgaveId()).ifPresent( oppgaveId -> minSideArbeidsgiverTjeneste.oppgaveUtgått(oppgaveId, OffsetDateTime.now()));
            // Oppdaterer status i arbeidsgiver-notifikasjon
            minSideArbeidsgiverTjeneste.ferdigstillSak(eksisterendeForespørsel.arbeidsgiverNotifikasjonSakId(), false);
        }

        // Oppdaterer status til utgått på saken i arbeidsgiverportalen
        minSideArbeidsgiverTjeneste.oppdaterSakTilleggsinformasjon(eksisterendeForespørsel.arbeidsgiverNotifikasjonSakId(),
            ForespørselTekster.lagTilleggsInformasjon(LukkeÅrsak.UTGÅTT, eksisterendeForespørsel.førsteUttaksdato()));
        forespørselTjeneste.settForespørselTilUtgått(eksisterendeForespørsel.arbeidsgiverNotifikasjonSakId());
        //oppdaterer status til not applicable i altinn dialogporten
        Optional.ofNullable(eksisterendeForespørsel.dialogportenUuid()).ifPresent(dialogUuid ->
            dialogportenKlient.settDialogTilUtgått(dialogUuid, lagSaksTittelForDialogporten(eksisterendeForespørsel.aktørId(), eksisterendeForespørsel.ytelseType())));

        var msg = String.format("Setter forespørsel til utgått, orgnr: %s, stp: %s, saksnummer: %s, ytelse: %s",
            eksisterendeForespørsel.arbeidsgiver(),
            eksisterendeForespørsel.skjæringstidspunkt(),
            eksisterendeForespørsel.fagsystemSaksnummer(),
            eksisterendeForespørsel.ytelseType());
        LOG.info(msg);
    }

    public void opprettForespørsel(Ytelsetype ytelsetype,
                                   AktørId aktørId,
                                   Saksnummer fagsakSaksnummer,
                                   Arbeidsgiver arbeidsgiver,
                                   LocalDate skjæringstidspunkt,
                                   LocalDate førsteUttaksdato) {
        var msg = String.format("Oppretter forespørsel, orgnr: %s, stp: %s, saksnummer: %s, ytelse: %s",
            arbeidsgiver,
            skjæringstidspunkt,
            fagsakSaksnummer.saksnummer(),
            ytelsetype);
        LOG.info(msg);

        // Oppretter forespørsel i lokal database
        var forespørselUuid = forespørselTjeneste.opprettForespørsel(skjæringstidspunkt,
            ytelsetype,
            aktørId,
            arbeidsgiver,
            fagsakSaksnummer,
            førsteUttaksdato);

        opprettForespørselMinSideArbeidsgiver(forespørselUuid, arbeidsgiver, aktørId, ytelsetype, førsteUttaksdato);

        if (ENV.isDev()) {
            try {
                opprettForespørselDialogporten(forespørselUuid, arbeidsgiver, aktørId, ytelsetype, førsteUttaksdato);
            } catch (Exception e) {
                // Ikke alle organisasjoner som brukes av Dolly finnes i Tenor, som Altinn bruker for å slå opp bedrifter i test. Må derfor tåle å feile for enkelte kall i dev
                LOG.warn("Feil ved kall til dialogporten: ", e);
            }
        }
    }

    private void opprettForespørselMinSideArbeidsgiver(UUID forespørselUuid, Arbeidsgiver arbeidsgiver, AktørId aktørId, Ytelsetype ytelsetype,
                                                       LocalDate førsteUttaksdato) {
        var organisasjon = organisasjonTjeneste.finnOrganisasjon(arbeidsgiver);

        var person = personTjeneste.hentPersonInfoFraAktørId(aktørId, ytelsetype);

        var merkelapp = ForespørselTekster.finnMerkelapp(ytelsetype);
        var skjemaUri = URI.create(inntektsmeldingSkjemaLenke + "/" + forespørselUuid);
        var arbeidsgiverNotifikasjonSakId = minSideArbeidsgiverTjeneste.opprettSak(forespørselUuid.toString(),
            merkelapp,
            arbeidsgiver.orgnr(),
            ForespørselTekster.lagSaksTittel(person.mapFulltNavn(), person.fødselsdato()),
            skjemaUri);

        var tilleggsinformasjon = ForespørselTekster.lagTilleggsInformasjon(LukkeÅrsak.ORDINÆR_INNSENDING, førsteUttaksdato);
        minSideArbeidsgiverTjeneste.oppdaterSakTilleggsinformasjon(arbeidsgiverNotifikasjonSakId, tilleggsinformasjon);

        forespørselTjeneste.setArbeidsgiverNotifikasjonSakId(forespørselUuid, arbeidsgiverNotifikasjonSakId);

        String oppgaveId;
        try {
            oppgaveId = minSideArbeidsgiverTjeneste.opprettOppgave(forespørselUuid.toString(),
                merkelapp,
                forespørselUuid.toString(),
                arbeidsgiver.orgnr(),
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

    private void opprettForespørselDialogporten(UUID forespørselUuid,
                                                Arbeidsgiver arbeidsgiver,
                                                AktørId aktørId,
                                                Ytelsetype ytelsetype,
                                                LocalDate førsteUttaksdato) {
        var person = personTjeneste.hentPersonInfoFraAktørId(aktørId, ytelsetype);
        var saksTittelDialog = lagSaksTittelForDialogporten(person);

        var dialogPortenUuid = dialogportenKlient.opprettDialog(forespørselUuid,
            arbeidsgiver, saksTittelDialog, førsteUttaksdato, ytelsetype, person.fødselsnummer());

        var vasketDialogUuid = dialogPortenUuid.replace("\"", "");
        LOG.info("Mottok UUID {} fra dialogporten", vasketDialogUuid);
        forespørselTjeneste.setDialogportenUuid(forespørselUuid, UUID.fromString(vasketDialogUuid));
    }

    private String lagSaksTittelForDialogporten(AktørId aktørId, Ytelsetype ytelsetype) {
        var person = personTjeneste.hentPersonInfoFraAktørId(aktørId, ytelsetype);
        return ForespørselTekster.lagSaksTittel(person.mapFulltNavn(), person.fødselsdato());
    }

    private String lagSaksTittelForDialogporten(PersonInfo person) {
        return ForespørselTekster.lagSaksTittel(person.mapFulltNavn(), person.fødselsdato());
    }

    public UUID opprettForespørselForArbeidsgiverInitiertIm(Ytelsetype ytelsetype,
                                                            AktørId aktørId,
                                                            Arbeidsgiver arbeidsgiver,
                                                            LocalDate førsteFraværsdato,
                                                            ArbeidsgiverinitiertÅrsak arbeidsgiverinitiertÅrsak,
                                                            LocalDate skjæringstidspunkt) {
        var msg = String.format("Oppretter forespørsel for arbeidsgiverinitiert, orgnr: %s, ytelse: %s",
            arbeidsgiver,
            ytelsetype);
        LOG.info(msg);
        var forespørselType = utledForespørselType(arbeidsgiverinitiertÅrsak);
        var uuid = forespørselTjeneste.opprettForespørselArbeidsgiverinitiert(ytelsetype,
            aktørId,
            arbeidsgiver,
            førsteFraværsdato,
            forespørselType,
            skjæringstidspunkt);

        var person = personTjeneste.hentPersonInfoFraAktørId(aktørId, ytelsetype);
        var merkelapp = ForespørselTekster.finnMerkelapp(ytelsetype);
        var skjemaUri = URI.create(inntektsmeldingSkjemaLenke + "/" + uuid);
        var fagerSakId = minSideArbeidsgiverTjeneste.opprettSak(uuid.toString(),
            merkelapp,
            arbeidsgiver.orgnr(),
            ForespørselTekster.lagSaksTittel(person.mapFulltNavn(), person.fødselsdato()),
            skjemaUri
        );

        var tilleggsinformasjon = ForespørselTekster.lagTilleggsInformasjon(LukkeÅrsak.ORDINÆR_INNSENDING, førsteFraværsdato);
        minSideArbeidsgiverTjeneste.oppdaterSakTilleggsinformasjon(fagerSakId, tilleggsinformasjon);

        forespørselTjeneste.setArbeidsgiverNotifikasjonSakId(uuid, fagerSakId);

        if (ENV.isDev()) {
            try {
                opprettForespørselDialogporten(uuid, arbeidsgiver, aktørId, ytelsetype, førsteFraværsdato);
            } catch (Exception e) {
                // Ikke alle organisasjoner som brukes av Dolly finnes i Tenor, som Altinn bruker for å slå opp bedrifter i test. Må derfor tåle å feile for enkelte kall i dev
                LOG.warn("Feil ved kall til dialogporten: ", e);
            }
        }

        return uuid;
    }

    private ForespørselType utledForespørselType(ArbeidsgiverinitiertÅrsak arbeidsgiverinitiertÅrsak) {
        return switch (arbeidsgiverinitiertÅrsak) {
            case UREGISTRERT -> ForespørselType.ARBEIDSGIVERINITIERT_UREGISTRERT;
            case NYANSATT -> ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT;
        };
    }

    public NyBeskjedResultat opprettNyBeskjedMedEksternVarsling(Saksnummer fagsakSaksnummer,
                                                                Arbeidsgiver arbeidsgiver) {
        var forespørsel = forespørselTjeneste.finnÅpenForespørslelForFagsak(fagsakSaksnummer, arbeidsgiver)
            .orElse(null);

        if (forespørsel == null) {
            return NyBeskjedResultat.FORESPØRSEL_FINNES_IKKE;
        }

        var msg = String.format("Oppretter ny beskjed med ekstern varsling, orgnr: %s, stp: %s, saksnummer: %s, ytelse: %s",
            arbeidsgiver,
            forespørsel.skjæringstidspunkt(),
            fagsakSaksnummer,
            forespørsel.ytelseType());
        LOG.info(msg);
        var merkelapp = ForespørselTekster.finnMerkelapp(forespørsel.ytelseType());
        var forespørselUuid = forespørsel.uuid();
        var skjemaUri = URI.create(inntektsmeldingSkjemaLenke + "/" + forespørselUuid);
        var organisasjon = organisasjonTjeneste.finnOrganisasjon(arbeidsgiver);
        var person = personTjeneste.hentPersonInfoFraAktørId(forespørsel.aktørId(), forespørsel.ytelseType());
        var varselTekst = ForespørselTekster.lagVarselFraSaksbehandlerTekst(forespørsel.ytelseType(), organisasjon);
        var beskjedTekst = ForespørselTekster.lagBeskjedFraSaksbehandlerTekst(forespørsel.ytelseType(), person.mapFulltNavn());

        minSideArbeidsgiverTjeneste.sendNyBeskjedMedEksternVarsling(forespørselUuid.toString(),
            merkelapp,
            forespørselUuid.toString(),
            arbeidsgiver.orgnr(),
            beskjedTekst,
            varselTekst,
            skjemaUri);

        return NyBeskjedResultat.NY_BESKJED_SENDT;
    }

    public void lukkForespørsel(Saksnummer fagsakSaksnummer, Arbeidsgiver arbeidsgiver, LocalDate skjæringstidspunkt) {
        var forespørsler = hentÅpneForespørslerForFagsak(fagsakSaksnummer, arbeidsgiver, skjæringstidspunkt);

        // Alle inntektsmeldinger sendt inn via arbeidsgiverportal blir lukket umiddelbart etter innsending fra #InntektsmeldingTjeneste,
        // så forespørsler som enda er åpne her blir løst ved innsending fra andre systemer
        forespørsler.forEach(f -> {
            var lukketForespørsel = ferdigstillForespørsel(f.uuid(),
                f.aktørId(),
                f.arbeidsgiver(),
                f.førsteUttaksdato(),
                LukkeÅrsak.EKSTERN_INNSENDING, Optional.empty());
            MetrikkerTjeneste.loggForespørselLukkEkstern(lukketForespørsel);
        });
    }

    public void settForespørselTilUtgått(Saksnummer fagsakSaksnummer, Arbeidsgiver arbeidsgiver, LocalDate skjæringstidspunkt) {
        var forespørsler = hentÅpneForespørslerForFagsak(fagsakSaksnummer, arbeidsgiver, skjæringstidspunkt);

        forespørsler.forEach(it -> settForespørselTilUtgått(it, true));
    }

    private List<ForespørselDto> hentÅpneForespørslerForFagsak(Saksnummer fagsakSaksnummer,
                                                               Arbeidsgiver arbeidsgiver,
                                                               LocalDate skjæringstidspunkt) {
        return forespørselTjeneste.finnÅpneForespørslerForFagsak(fagsakSaksnummer).stream()
            .filter(f -> arbeidsgiver == null || arbeidsgiver.orgnr().equals(f.arbeidsgiver().orgnr()))
            .filter(f -> skjæringstidspunkt == null || skjæringstidspunkt.equals(f.skjæringstidspunkt()))
            .toList();
    }

    public void settForespørselTilUtgått(UUID forespørselUuid) {
        var forespørselDto = hentForespørsel(forespørselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel med forespørselUuid: " + forespørselUuid));

        // Oppdaterer status til utgått på saken i arbeidsgiverportalen
        minSideArbeidsgiverTjeneste.oppdaterSakTilleggsinformasjon(forespørselDto.arbeidsgiverNotifikasjonSakId(),
            ForespørselTekster.lagTilleggsInformasjon(LukkeÅrsak.UTGÅTT, forespørselDto.førsteUttaksdato()));
        forespørselTjeneste.settForespørselTilUtgått(forespørselDto.arbeidsgiverNotifikasjonSakId());
        //oppdaterer status til not applicable i altinn dialogporten
        if (forespørselDto.dialogportenUuid() != null) {
            dialogportenKlient.settDialogTilUtgått(forespørselDto.dialogportenUuid(),
                lagSaksTittelForDialogporten(forespørselDto.aktørId(), forespørselDto.ytelseType()));
        }

        var msg = String.format("Setter forespørsel til utgått, orgnr: %s, stp: %s, saksnummer: %s, ytelse: %s",
            forespørselDto.arbeidsgiver().orgnr(),
            forespørselDto.skjæringstidspunkt(),
            forespørselDto.fagsystemSaksnummer().saksnummer(),
            forespørselDto.ytelseType());
        LOG.info(msg);
    }

    public void slettForespørsel(Saksnummer fagsakSaksnummer, Arbeidsgiver arbeidsgiver, LocalDate skjæringstidspunkt) {
        var sakerSomSkalSlettes = forespørselTjeneste.finnForespørslerForFagsak(fagsakSaksnummer).stream()
            .filter(f -> skjæringstidspunkt == null || skjæringstidspunkt.equals(f.skjæringstidspunkt()))
            .filter(f -> arbeidsgiver == null || f.arbeidsgiver().equals(arbeidsgiver))
            .filter(f -> f.status().equals(ForespørselStatus.UNDER_BEHANDLING))
            .toList();

        if (sakerSomSkalSlettes.size() != 1) {
            var msg = String.format("Fant ikke akkurat 1 sak som skulle slettes. Fant istedet %s saker ", sakerSomSkalSlettes.size());
            throw new IllegalStateException(msg);
        }
        var agPortalSakId = sakerSomSkalSlettes.getFirst().arbeidsgiverNotifikasjonSakId();
        minSideArbeidsgiverTjeneste.slettSak(agPortalSakId);
        forespørselTjeneste.settForespørselTilUtgått(agPortalSakId);
    }

    public List<InntektsmeldingForespørselDto> finnForespørslerForFagsak(Saksnummer fagsakSaksnummer) {
        return forespørselTjeneste.finnForespørslerForFagsak(fagsakSaksnummer).stream().map(forespoersel ->
                new InntektsmeldingForespørselDto(
                    forespoersel.uuid(),
                    forespoersel.skjæringstidspunkt(),
                    forespoersel.arbeidsgiver().orgnr(),
                    forespoersel.aktørId().getAktørId(),
                    forespoersel.ytelseType().toString(),
                    forespoersel.førsteUttaksdato()))
            .toList();
    }

    private void validerStartdato(ForespørselDto forespørsel, LocalDate startdato) {
        if (!forespørsel.førsteUttaksdato().equals(startdato)) {
            throw new IllegalStateException("Startdato var ikke like");
        }
    }

    private void validerOrganisasjon(ForespørselDto forespørsel, Arbeidsgiver arbeidsgiver) {
        if (!forespørsel.arbeidsgiver().equals(arbeidsgiver)) {
            throw new IllegalStateException("Organisasjonsnummer var ikke like");
        }
    }

    private void validerAktør(ForespørselDto forespørsel, AktørId aktorId) {
        if (!forespørsel.aktørId().equals(aktorId)) {
            throw new IllegalStateException("AktørId for bruker var ikke like");
        }
    }

    public ForespørselDto oppdaterFørsteUttaksdato(ForespørselDto forespørselEnitet, LocalDate startdato) {
        return forespørselTjeneste.setFørsteUttaksdato(forespørselEnitet.uuid(), startdato);
    }

    public List<ForespørselDto> hentForespørsler(Arbeidsgiver arbeidsgiver,
                                                 String fnr,
                                                 ForespørselStatusDto status,
                                                 YtelseTypeDto ytelseType,
                                                 LocalDate fom,
                                                 LocalDate tom) {
        var aktørId = fnr == null ? null : personTjeneste.finnAktørIdForIdent(new PersonIdent(fnr))
            .orElseThrow(() -> new IllegalStateException("Finner ikke aktørId"));
        return forespørselTjeneste.hentForespørsler(arbeidsgiver,
            aktørId,
            status == null ? null : KodeverkMapper.mapForespørselStatus(status),
            ytelseType == null ? null : KodeverkMapper.mapYtelsetype(ytelseType),
            fom,
            tom);
    }
}
