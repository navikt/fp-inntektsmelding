package no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester;

import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.task.FerdigstillSakArbeidsgiverportalenTask;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.task.FerdigstillSakDialogportenTask;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.task.UtgåSakArbeidsgiverportalenTask;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.task.UtgåSakDialogportenTask;

import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.task.OpprettSakArbeidsgiverportalenTask;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.task.OpprettSakDialogportenTask;
import no.nav.foreldrepenger.inntektsmelding.forvaltning.rest.InntektsmeldingForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.imdialog.rest.kvittering.PdfDokumentRest;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.altinn.DialogportenKlient;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.MinSideArbeidsgiverTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.metrikker.MetrikkerTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Fødselsnummer;
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
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;


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
    private ProsessTaskTjeneste prosessTaskTjeneste;

    public ForespørselBehandlingTjeneste() {
        // CDI
    }

    @Inject
    public ForespørselBehandlingTjeneste(
        @KonfigVerdi(value = "inntektsmelding.skjema.lenke", defaultVerdi = "https://arbeidsgiver.nav.no/fp-im-dialog")
        String inntektsmeldingSkjemaLenke,
        ForespørselTjeneste forespørselTjeneste,
        MinSideArbeidsgiverTjeneste minSideArbeidsgiverTjeneste,
        PersonTjeneste personTjeneste,
        OrganisasjonTjeneste organisasjonTjeneste,
        DialogportenKlient dialogportenKlient,
        ProsessTaskTjeneste prosessTaskTjeneste) {
        this.inntektsmeldingSkjemaLenke = inntektsmeldingSkjemaLenke;
        this.forespørselTjeneste = forespørselTjeneste;
        this.minSideArbeidsgiverTjeneste = minSideArbeidsgiverTjeneste;
        this.personTjeneste = personTjeneste;
        this.organisasjonTjeneste = organisasjonTjeneste;
        this.dialogportenKlient = dialogportenKlient;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
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
            .forEach(this::settForespørselTilUtgått);
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

        // Oppdaterer status i forespørsel
        forespørselTjeneste.ferdigstillForespørsel(forespørsel.arbeidsgiverNotifikasjonSakId());

        var ferdigstillIArbeidsgiverportalTask = FerdigstillSakArbeidsgiverportalenTask.lagTask(foresporselUuid, årsak, inntektsmeldingUuid);
        prosessTaskTjeneste.lagre(ferdigstillIArbeidsgiverportalTask);

        var ferdigstillDialogportenTask = FerdigstillSakDialogportenTask.lagTask(foresporselUuid, årsak, inntektsmeldingUuid);
        prosessTaskTjeneste.lagre(ferdigstillDialogportenTask);

        // Re-fetch to get updated status
        return forespørselTjeneste.hentForespørsel(foresporselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel etter ferdigstilling"));
    }

    public void oppdaterPortalerMedEndretInntektsmelding(ForespørselDto forespørsel,
                                                         Optional<UUID> inntektsmeldingUuid,
                                                         Arbeidsgiver arbeidsgiver) {
        // Oppdater status i arbeidsgiverportalen
        inntektsmeldingUuid.ifPresent(imUuid -> {
            var merkelapp = ForespørselTekster.finnMerkelapp(forespørsel.ytelseType());
            var beskjedTekst = ForespørselTekster.lagBeskjedOmOppdatertInntektsmelding();
            String url = new StringBuilder(inntektsmeldingSkjemaLenke)
                .append("/server/api")
                .append(PdfDokumentRest.INNTEKTSMELDING_FULL_PATH)
                .append("/")
                .append(imUuid).toString();
            minSideArbeidsgiverTjeneste.sendNyBeskjedMedKvittering(forespørsel.uuid().toString(),
                merkelapp,
                forespørsel.uuid().toString(),
                arbeidsgiver.orgnr(),
                beskjedTekst,
                URI.create(url));
        });

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

    public List<ForespørselDto> finnForespørslerForAktørId(AktørId aktørId, Ytelsetype ytelsetype) {
        return forespørselTjeneste.finnForespørslerForAktørid(aktørId, ytelsetype);
    }

    public void settForespørselTilUtgått(ForespørselDto forespørselDto) {
        var msg = String.format("Setter forespørsel til utgått, orgnr: %s, stp: %s, saksnummer: %s",
            forespørselDto.arbeidsgiver(),
            forespørselDto.skjæringstidspunkt(),
            forespørselDto.fagsystemSaksnummer());
        LOG.info(msg);

        forespørselTjeneste.settForespørselTilUtgått(forespørselDto.arbeidsgiverNotifikasjonSakId());

        var settUtgåttArbeidsgiverportalenTask = UtgåSakArbeidsgiverportalenTask.lagTask(forespørselDto.uuid());
        prosessTaskTjeneste.lagre(settUtgåttArbeidsgiverportalenTask);

        var settUtgåttDialogpportenTask = UtgåSakDialogportenTask.lagTask(forespørselDto.uuid());
        prosessTaskTjeneste.lagre(settUtgåttDialogpportenTask);
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

        // Oppdater arbeidsgiverportalen
        var arbeidsgiverportalenTask = OpprettSakArbeidsgiverportalenTask.lagTask(forespørselUuid);
        // Oppdater dialogporten
        var dialogportenTask = OpprettSakDialogportenTask.lagTask(forespørselUuid);
        var ptGruppe = new ProsessTaskGruppe();
        ptGruppe.addNesteSekvensiell(arbeidsgiverportalenTask);
        ptGruppe.addNesteSekvensiell(dialogportenTask);

        prosessTaskTjeneste.lagre(ptGruppe);
    }

    private String lagSaksTittelForDialogporten(AktørId aktørId, Ytelsetype ytelsetype) {
        var person = personTjeneste.hentPersonInfoFraAktørId(aktørId, ytelsetype);
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
        var forespørselUuid = forespørselTjeneste.opprettForespørselArbeidsgiverinitiert(ytelsetype,
            aktørId,
            arbeidsgiver,
            førsteFraværsdato,
            forespørselType,
            skjæringstidspunkt);

        // Oppdater arbeidsgiverportalen
        var arbeidsgiverportalenTask = OpprettSakArbeidsgiverportalenTask.lagTask(forespørselUuid);
        prosessTaskTjeneste.lagre(arbeidsgiverportalenTask);

        // Oppdater dialogporten
        var dialogportenTask = OpprettSakDialogportenTask.lagTask(forespørselUuid);
        prosessTaskTjeneste.lagre(dialogportenTask);

        return forespørselUuid;
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

        forespørsler.forEach(this::settForespørselTilUtgått);
    }

    private List<ForespørselDto> hentÅpneForespørslerForFagsak(Saksnummer fagsakSaksnummer,
                                                               Arbeidsgiver arbeidsgiver,
                                                               LocalDate skjæringstidspunkt) {
        return forespørselTjeneste.finnÅpneForespørslerForFagsak(fagsakSaksnummer).stream()
            .filter(f -> arbeidsgiver == null || arbeidsgiver.orgnr().equals(f.arbeidsgiver().orgnr()))
            .filter(f -> skjæringstidspunkt == null || skjæringstidspunkt.equals(f.skjæringstidspunkt()))
            .toList();
    }

    public void settForespørselTilUtgåttForvaltning(UUID forespørselUuid) {
        var forespørselDto = hentForespørsel(forespørselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel med forespørselUuid: " + forespørselUuid));

        // Oppdaterer status til utgått på saken og oppgaven i arbeidsgiverportalen / dialogporten
        minSideArbeidsgiverTjeneste.oppdaterSakTilleggsinformasjon(forespørselDto.arbeidsgiverNotifikasjonSakId(),
            ForespørselTekster.lagTilleggsInformasjon(LukkeÅrsak.UTGÅTT, forespørselDto.førsteUttaksdato()));
        minSideArbeidsgiverTjeneste.oppgaveUtgått(forespørselDto.oppgaveId(), OffsetDateTime.now());

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
                                                 Fødselsnummer fødselsnummer,
                                                 ForespørselStatusDto status,
                                                 YtelseTypeDto ytelseType,
                                                 LocalDate fom,
                                                 LocalDate tom) {
        var aktørId = fødselsnummer == null ? null : personTjeneste.finnAktørIdForIdent(new PersonIdent(fødselsnummer.fnr()))
            .orElseThrow(() -> new IllegalStateException("Finner ikke aktørId"));
        return forespørselTjeneste.hentForespørsler(arbeidsgiver,
            aktørId,
            status == null ? null : KodeverkMapper.mapForespørselStatus(status),
            ytelseType == null ? null : KodeverkMapper.mapYtelsetype(ytelseType),
            fom,
            tom);
    }
}
