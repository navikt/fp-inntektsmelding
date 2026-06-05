package no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.task.FerdigstillForespørselIEksterneSystemerTask;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.task.OppdaterPortalerMedEndretInntektsmeldingTask;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.task.OpprettForespørselIEksterneSystemerTask;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.task.SendBeskjedMedEksternVarslingTask;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.task.SettForespørselTilUtgåttIEksterneSystemerTask;
import no.nav.foreldrepenger.inntektsmelding.forvaltning.rest.InntektsmeldingForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.metrikker.MetrikkerTjeneste;
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
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;


/**
 * Okrestreringsklasse som håndterer alle endringer på en forespørsel, og synkroniserer dette på tvers av intern database og eksterne systemer.
 */
@ApplicationScoped
public class ForespørselBehandlingTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(ForespørselBehandlingTjeneste.class);

    private ForespørselTjeneste forespørselTjeneste;
    private PersonTjeneste personTjeneste;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    public ForespørselBehandlingTjeneste() {
        // CDI
    }

    @Inject
    public ForespørselBehandlingTjeneste(
        ForespørselTjeneste forespørselTjeneste,
        PersonTjeneste personTjeneste,
        ProsessTaskTjeneste prosessTaskTjeneste) {
        this.forespørselTjeneste = forespørselTjeneste;
        this.personTjeneste = personTjeneste;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    public ForespørselResultat håndterInnkommendeForespørsel(LocalDate skjæringstidspunkt,
                                                             Ytelsetype ytelsetype,
                                                             AktørId aktørId,
                                                             Arbeidsgiver arbeidsgiver,
                                                             Saksnummer fagsakSaksnummer,
                                                             LocalDate førsteUttaksdato) {
        // Henter fra database
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

        // Deaktiverer gammel forespørsel i database, fager og dialogporten
        settTidligereForespørslerForSaksnummerTilUtgått(fagsakSaksnummer, arbeidsgiver, aktørId);

        // Oppretter ny forespørsel database, fager og dialogporten
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

        // Oppdaterer status i lokal database
        forespørselTjeneste.ferdigstillForespørselByUuid(foresporselUuid);

        // Planlegger oppdatering av eksterne systemer som asynkron task
        var task = FerdigstillForespørselIEksterneSystemerTask.opprettTask(
            foresporselUuid, arbeidsgiver, aktorId, årsak, inntektsmeldingUuid, erFørstegangsinnsending);
        prosessTaskTjeneste.lagre(task);

        // Re-fetch to get updated status
        return forespørselTjeneste.hentForespørsel(foresporselUuid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel etter ferdigstilling"));
    }

    public void oppdaterPortalerMedEndretInntektsmelding(ForespørselDto forespørsel,
                                                         Optional<UUID> inntektsmeldingUuid,
                                                         Arbeidsgiver arbeidsgiver) {
        var task = OppdaterPortalerMedEndretInntektsmeldingTask.opprettTask(
            forespørsel.uuid(), arbeidsgiver, inntektsmeldingUuid);
        prosessTaskTjeneste.lagre(task);
    }

    public Optional<ForespørselDto> hentForespørsel(UUID forespørselUUID) {
        return forespørselTjeneste.hentForespørsel(forespørselUUID);
    }

    public List<ForespørselDto> finnForespørsler(AktørId aktørId, Ytelsetype ytelsetype, String orgnr) {
        return forespørselTjeneste.finnForespørsler(aktørId, ytelsetype, orgnr);
    }

    public List<ForespørselDto> finnForespørslerForAktørId(AktørId aktørId, Ytelsetype ytelsetype) {
        return forespørselTjeneste.finnForespørslerForAktørid(aktørId, ytelsetype);
    }

    public void settForespørselTilUtgått(ForespørselDto eksisterendeForespørsel, boolean skalOppdatereArbeidsgiverNotifikasjon) {
        LOG.info("Verdien for skalOppdatereArbeidsgiverNotifikasjon er: {}", skalOppdatereArbeidsgiverNotifikasjon);

        // Oppdaterer status i lokal database
        forespørselTjeneste.settForespørselTilUtgåttByUuid(eksisterendeForespørsel.uuid());

        // Planlegger oppdatering av eksterne systemer som asynkron task
        var task = SettForespørselTilUtgåttIEksterneSystemerTask.opprettTask(
            eksisterendeForespørsel.uuid(),
            skalOppdatereArbeidsgiverNotifikasjon);
        prosessTaskTjeneste.lagre(task);

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

        // Planlegger opprettelse i eksterne systemer som asynkron task
        var task = OpprettForespørselIEksterneSystemerTask.opprettTask(
            forespørselUuid, arbeidsgiver, aktørId, ytelsetype, førsteUttaksdato);
        prosessTaskTjeneste.lagre(task);
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

        // Planlegger opprettelse i eksterne systemer som asynkron task
        var task = OpprettForespørselIEksterneSystemerTask.opprettTask(
            uuid, arbeidsgiver, aktørId, ytelsetype, førsteFraværsdato);
        prosessTaskTjeneste.lagre(task);

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

        var task = SendBeskjedMedEksternVarslingTask.opprettTask(forespørsel.uuid(), arbeidsgiver);
        prosessTaskTjeneste.lagre(task);

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

        // Oppdaterer status i lokal database
        forespørselTjeneste.settForespørselTilUtgåttByUuid(forespørselUuid);

        // Planlegger oppdatering av eksterne systemer som asynkron task
        var task = SettForespørselTilUtgåttIEksterneSystemerTask.opprettTask(
            forespørselUuid,
            false);
        prosessTaskTjeneste.lagre(task);

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
        var forespørsel = sakerSomSkalSlettes.getFirst();
        // Oppdaterer status i lokal database
        forespørselTjeneste.settForespørselTilUtgåttByUuid(forespørsel.uuid());

        // Planlegger sletting i eksterne systemer
        var task = SettForespørselTilUtgåttIEksterneSystemerTask.opprettTask(
            forespørsel.uuid(),
            false);
        prosessTaskTjeneste.lagre(task);
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
