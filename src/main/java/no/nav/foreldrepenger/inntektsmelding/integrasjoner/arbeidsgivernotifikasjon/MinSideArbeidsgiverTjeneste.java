package no.nav.foreldrepenger.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselTekster;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;
import no.nav.foreldrepenger.inntektsmelding.imdialog.rest.kvittering.PdfDokumentRest;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.altinn.AltinnRessurser;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.konfig.KonfigVerdi;

@ApplicationScoped
public class MinSideArbeidsgiverTjeneste {
    private static final Environment ENV = Environment.current();
    static final String SAK_STATUS_TEKST = "";
    static final String SAK_STATUS_TEKST_ARBEIDSGIVERINITIERT = "Mottatt - Se kvittering eller korriger inntektsmelding";
    static final Sendevindu VARSEL_SENDEVINDU = Sendevindu.LOEPENDE;
    static final int PÅMINNELSE_ETTER_DAGER;
    static final String ALTINN_INNTEKTSMELDING_RESSURS = AltinnRessurser.ALTINN_TRE_INNTEKTSMELDING_RESSURS;

    static {
        PÅMINNELSE_ETTER_DAGER = ENV.getProperty("paaminnelse.etter.dager", int.class, 14);
    }

    private MinSideArbeidsgiverKlient minSideArbeidsgiverKlient;
    private PersonTjeneste personTjeneste;
    private OrganisasjonTjeneste organisasjonTjeneste;
    private String inntektsmeldingSkjemaLenke;

    public MinSideArbeidsgiverTjeneste() {
        // CDI
    }

    @Inject
    public MinSideArbeidsgiverTjeneste(MinSideArbeidsgiverKlient minSideArbeidsgiverKlient,
                                       PersonTjeneste personTjeneste,
                                       OrganisasjonTjeneste organisasjonTjeneste,
                                       @KonfigVerdi(value = "inntektsmelding.skjema.lenke", defaultVerdi = "https://arbeidsgiver.nav.no/fp-im-dialog")
                                       String inntektsmeldingSkjemaLenke) {
        this.minSideArbeidsgiverKlient = minSideArbeidsgiverKlient;
        this.personTjeneste = personTjeneste;
        this.organisasjonTjeneste = organisasjonTjeneste;
        this.inntektsmeldingSkjemaLenke = inntektsmeldingSkjemaLenke;
    }

    public record OpprettSakResultat(String arbeidsgiverNotifikasjonSakId, String oppgaveId) {}

    public OpprettSakResultat opprettSakOgOppgave(ForespørselDto forespørsel) {
        var arbeidsgiver = forespørsel.arbeidsgiver();
        var organisasjon = organisasjonTjeneste.finnOrganisasjon(arbeidsgiver);
        var person = personTjeneste.hentPersonInfoFraAktørId(forespørsel.aktørId(), forespørsel.ytelseType());

        var merkelapp = ForespørselTekster.finnMerkelapp(forespørsel.ytelseType());
        var skjemaUri = URI.create(inntektsmeldingSkjemaLenke + "/" + forespørsel.uuid());
        var saksTittel = ForespørselTekster.lagSaksTittel(person.mapFulltNavn(), person.fødselsdato());

        var sakId = opprettSak(forespørsel.uuid().toString(), merkelapp, arbeidsgiver.orgnr(), saksTittel, skjemaUri);

        var tilleggsinformasjon = ForespørselTekster.lagTilleggsInformasjon(LukkeÅrsak.ORDINÆR_INNSENDING, forespørsel.førsteUttaksdato());
        oppdaterSakTilleggsinformasjon(sakId, tilleggsinformasjon);

        String oppgaveId;
        try {
            oppgaveId = opprettOppgave(forespørsel.uuid().toString(),
                merkelapp,
                forespørsel.uuid().toString(),
                arbeidsgiver.orgnr(),
                ForespørselTekster.lagOppgaveTekst(forespørsel.ytelseType()),
                ForespørselTekster.lagVarselTekst(forespørsel.ytelseType(), organisasjon),
                ForespørselTekster.lagPåminnelseTekst(forespørsel.ytelseType(), organisasjon),
                skjemaUri);
        } catch (Exception e) {
            slettSak(sakId);
            throw e;
        }

        return new OpprettSakResultat(sakId, oppgaveId);
    }

    public String opprettSakUtenOppgave(ForespørselDto forespørsel) {
        var person = personTjeneste.hentPersonInfoFraAktørId(forespørsel.aktørId(), forespørsel.ytelseType());

        var merkelapp = ForespørselTekster.finnMerkelapp(forespørsel.ytelseType());
        var skjemaUri = URI.create(inntektsmeldingSkjemaLenke + "/" + forespørsel.uuid());
        var saksTittel = ForespørselTekster.lagSaksTittel(person.mapFulltNavn(), person.fødselsdato());

        var sakId = opprettSak(forespørsel.uuid().toString(), merkelapp, forespørsel.arbeidsgiver().orgnr(), saksTittel, skjemaUri);

        var tilleggsinformasjon = ForespørselTekster.lagTilleggsInformasjon(LukkeÅrsak.ORDINÆR_INNSENDING, forespørsel.førsteUttaksdato());
        oppdaterSakTilleggsinformasjon(sakId, tilleggsinformasjon);

        return sakId;
    }

    public void ferdigstillSak(ForespørselDto forespørsel, LukkeÅrsak årsak, Optional<UUID> inntektsmeldingUuid, boolean erFørstegangsinnsending) {
        // Arbeidsgiverinitierte forespørsler har ingen oppgave
        if (forespørsel.oppgaveId() != null) {
            oppgaveUtført(forespørsel.oppgaveId(), OffsetDateTime.now());
        }

        var erArbeidsgiverInitiertInntektsmelding = forespørsel.oppgaveId() == null;
        ferdigstillSak(forespørsel.arbeidsgiverNotifikasjonSakId(), erArbeidsgiverInitiertInntektsmelding);

        oppdaterSakTilleggsinformasjon(forespørsel.arbeidsgiverNotifikasjonSakId(),
            ForespørselTekster.lagTilleggsInformasjon(årsak, forespørsel.førsteUttaksdato()));

        inntektsmeldingUuid.ifPresent(imUuid -> {
            var merkelapp = ForespørselTekster.finnMerkelapp(forespørsel.ytelseType());
            var beskjedTekst = erFørstegangsinnsending
                ? ForespørselTekster.lagBeskjedOmKvitteringFørsteInnsendingTekst()
                : ForespørselTekster.lagBeskjedOmOppdatertInntektsmelding();
            var url = lagKvitteringUrl(imUuid);
            sendNyBeskjedMedKvittering(forespørsel.uuid().toString(),
                merkelapp,
                forespørsel.uuid().toString(),
                forespørsel.arbeidsgiver().orgnr(),
                beskjedTekst,
                URI.create(url));
        });
    }

    public void settSakTilUtgått(ForespørselDto forespørsel) {
        Optional.ofNullable(forespørsel.oppgaveId())
            .ifPresent(oppgaveId -> oppgaveUtgått(oppgaveId, OffsetDateTime.now()));

        oppdaterSakTilleggsinformasjon(forespørsel.arbeidsgiverNotifikasjonSakId(),
            ForespørselTekster.lagTilleggsInformasjon(LukkeÅrsak.UTGÅTT, forespørsel.førsteUttaksdato()));
    }

    public void sendNyBeskjedMedEksternVarsling(ForespørselDto forespørsel) {
        var arbeidsgiver = forespørsel.arbeidsgiver();
        var organisasjon = organisasjonTjeneste.finnOrganisasjon(arbeidsgiver);
        var person = personTjeneste.hentPersonInfoFraAktørId(forespørsel.aktørId(), forespørsel.ytelseType());

        var merkelapp = ForespørselTekster.finnMerkelapp(forespørsel.ytelseType());
        var skjemaUri = URI.create(inntektsmeldingSkjemaLenke + "/" + forespørsel.uuid());
        var varselTekst = ForespørselTekster.lagVarselFraSaksbehandlerTekst(forespørsel.ytelseType(), organisasjon);
        var beskjedTekst = ForespørselTekster.lagBeskjedFraSaksbehandlerTekst(forespørsel.ytelseType(), person.mapFulltNavn());

        sendNyBeskjedMedEksternVarsling(forespørsel.uuid().toString(),
            merkelapp,
            forespørsel.uuid().toString(),
            arbeidsgiver.orgnr(),
            beskjedTekst,
            varselTekst,
            skjemaUri);
    }

    public void sendBeskjedOmOppdatertInntektsmelding(ForespørselDto forespørsel, UUID inntektsmeldingUuid) {
        var merkelapp = ForespørselTekster.finnMerkelapp(forespørsel.ytelseType());
        var beskjedTekst = ForespørselTekster.lagBeskjedOmOppdatertInntektsmelding();
        var url = lagKvitteringUrl(inntektsmeldingUuid);
        sendNyBeskjedMedKvittering(forespørsel.uuid().toString(),
            merkelapp,
            forespørsel.uuid().toString(),
            forespørsel.arbeidsgiver().orgnr(),
            beskjedTekst,
            URI.create(url));
    }

    private String lagKvitteringUrl(UUID inntektsmeldingUuid) {
        return inntektsmeldingSkjemaLenke + "/server/api" + PdfDokumentRest.INNTEKTSMELDING_FULL_PATH + "/" + inntektsmeldingUuid;
    }

    public String opprettSak(String grupperingsid, Merkelapp merkelapp, String virksomhetsnummer, String saksTittel, URI lenke) {
        var request = NySakMutationRequest.builder()
            .setGrupperingsid(grupperingsid)
            .setTittel(saksTittel)
            .setVirksomhetsnummer(virksomhetsnummer)
            .setMerkelapp(merkelapp.getBeskrivelse())
            .setLenke(lenke.toString())
            .setInitiellStatus(SaksStatus.UNDER_BEHANDLING)
            .setOverstyrStatustekstMed(SAK_STATUS_TEKST)
            .setMottakere(List.of(lagAltinnMottakerInput()));

        var projection = new NySakResultatResponseProjection().typename()
            .onNySakVellykket(new NySakVellykketResponseProjection().id())
            .onUgyldigMerkelapp(new UgyldigMerkelappResponseProjection().feilmelding())
            .onUgyldigMottaker(new UgyldigMottakerResponseProjection().feilmelding())
            .onDuplikatGrupperingsid(new DuplikatGrupperingsidResponseProjection().feilmelding())
            .onDuplikatGrupperingsidEtterDelete(new DuplikatGrupperingsidEtterDeleteResponseProjection().feilmelding())
            .onUkjentProdusent(new UkjentProdusentResponseProjection().feilmelding())
            .onUkjentRolle(new UkjentRolleResponseProjection().feilmelding());

        return minSideArbeidsgiverKlient.opprettSak(request.build(), projection);
    }

    public String oppdaterSakTilleggsinformasjon(String id, String tilleggsinformasjon) {
        var request = TilleggsinformasjonSakMutationRequest.builder().setId(id).setTilleggsinformasjon(tilleggsinformasjon).build();

        var projection = new TilleggsinformasjonSakResultatResponseProjection().typename()
            .onTilleggsinformasjonSakVellykket(new TilleggsinformasjonSakVellykketResponseProjection().id())
            .onSakFinnesIkke(new SakFinnesIkkeResponseProjection().feilmelding())
            .onKonflikt(new KonfliktResponseProjection().feilmelding())
            .onUgyldigMerkelapp(new UgyldigMerkelappResponseProjection().feilmelding())
            .onUkjentProdusent(new UkjentProdusentResponseProjection().feilmelding());

        return minSideArbeidsgiverKlient.oppdaterSakTilleggsinformasjon(request, projection);
    }

    public String ferdigstillSak(String id, boolean arbeidsgiverInitiert) {
        var requestBuilder = NyStatusSakMutationRequest.builder()
            .setId(id)
            .setNyStatus(SaksStatus.FERDIG);

        if (arbeidsgiverInitiert) {
            requestBuilder.setOverstyrStatustekstMed(SAK_STATUS_TEKST_ARBEIDSGIVERINITIERT);
        } else {
            requestBuilder.setOverstyrStatustekstMed(SAK_STATUS_TEKST);
        }

        var projection = new NyStatusSakResultatResponseProjection().typename()
            .onNyStatusSakVellykket(new NyStatusSakVellykketResponseProjection().id())
            .onUgyldigMerkelapp(new UgyldigMerkelappResponseProjection().feilmelding())
            .onKonflikt(new KonfliktResponseProjection().feilmelding())
            .onUkjentProdusent(new UkjentProdusentResponseProjection().feilmelding())
            .onSakFinnesIkke(new SakFinnesIkkeResponseProjection().feilmelding());

        return minSideArbeidsgiverKlient.oppdaterSakStatus(requestBuilder.build(), projection);
    }

    public String opprettOppgave(String grupperingsid, Merkelapp merkelapp, String eksternId, String virksomhetsnummer,
                                 String oppgaveTekst, String varselTekst, String påminnelseTekst, URI lenke) {
        var request = NyOppgaveMutationRequest.builder()
            .setNyOppgave(NyOppgaveInput.builder()
                .setMottaker(lagAltinnMottakerInput())
                .setNotifikasjon(NotifikasjonInput.builder()
                    .setMerkelapp(merkelapp.getBeskrivelse())
                    .setTekst(oppgaveTekst)
                    .setLenke(lenke.toString())
                    .build())
                .setMetadata(MetadataInput.builder()
                    .setVirksomhetsnummer(virksomhetsnummer)
                    .setEksternId(eksternId)
                    .setGrupperingsid(grupperingsid)
                    .build())
                .setEksterneVarsler(List.of(lagEksternVarselAltinn(varselTekst, 15)))
                .setPaaminnelse(PaaminnelseInput.builder()
                    .setTidspunkt(PaaminnelseTidspunktInput.builder().setEtterOpprettelse(Duration.ofDays(PÅMINNELSE_ETTER_DAGER).toString()).build())
                    .setEksterneVarsler(List.of(lagPåminnelseVarselAltinn(påminnelseTekst)))
                    .build())
                .build())
            .build();

        var projection = new NyOppgaveResultatResponseProjection().typename()
            .onNyOppgaveVellykket(new NyOppgaveVellykketResponseProjection().id())
            .onUgyldigMerkelapp(new UgyldigMerkelappResponseProjection().feilmelding())
            .onUgyldigMottaker(new UgyldigMottakerResponseProjection().feilmelding())
            .onDuplikatEksternIdOgMerkelapp(new DuplikatEksternIdOgMerkelappResponseProjection().feilmelding())
            .onUkjentProdusent(new UkjentProdusentResponseProjection().feilmelding())
            .onUkjentRolle(new UkjentRolleResponseProjection().feilmelding())
            .onUgyldigPaaminnelseTidspunkt(new UgyldigPaaminnelseTidspunktResponseProjection().feilmelding());

        return minSideArbeidsgiverKlient.opprettOppgave(request, projection);
    }

    public String oppgaveUtført(String oppgaveId, OffsetDateTime utførtTidspunkt) {
        var request = OppgaveUtfoertMutationRequest.builder()
            .setId(oppgaveId)
            .setUtfoertTidspunkt(utførtTidspunkt.format(DateTimeFormatter.ISO_DATE_TIME))
            .build();

        var projection = new OppgaveUtfoertResultatResponseProjection().typename()
            .onOppgaveUtfoertVellykket(new OppgaveUtfoertVellykketResponseProjection().id())
            .onUgyldigMerkelapp(new UgyldigMerkelappResponseProjection().feilmelding())
            .onNotifikasjonFinnesIkke(new NotifikasjonFinnesIkkeResponseProjection().feilmelding())
            .onUkjentProdusent(new UkjentProdusentResponseProjection().feilmelding());

        return minSideArbeidsgiverKlient.oppgaveUtført(request, projection);
    }

    public String oppgaveUtgått(String oppgaveId, OffsetDateTime utgåttTidspunkt) {
        var request = OppgaveUtgaattMutationRequest.builder()
            .setId(oppgaveId)
            .setUtgaattTidspunkt(utgåttTidspunkt.format(DateTimeFormatter.ISO_DATE_TIME))
            .build();

        var projection = new OppgaveUtgaattResultatResponseProjection().typename()
            .onOppgaveUtgaattVellykket(new OppgaveUtgaattVellykketResponseProjection().id())
            .onUgyldigMerkelapp(new UgyldigMerkelappResponseProjection().feilmelding())
            .onNotifikasjonFinnesIkke(new NotifikasjonFinnesIkkeResponseProjection().feilmelding())
            .onUkjentProdusent(new UkjentProdusentResponseProjection().feilmelding());

        return minSideArbeidsgiverKlient.oppgaveUtgått(request, projection);
    }

    public String slettSak(String id) {
        var request = HardDeleteSakMutationRequest.builder().setId(id).build();
        var projection = new HardDeleteSakResultatResponseProjection().typename()
            .onHardDeleteSakVellykket(new HardDeleteSakVellykketResponseProjection().id())
            .onUgyldigMerkelapp(new UgyldigMerkelappResponseProjection().feilmelding())
            .onUkjentProdusent(new UkjentProdusentResponseProjection().feilmelding())
            .onSakFinnesIkke(new SakFinnesIkkeResponseProjection().feilmelding());
        return minSideArbeidsgiverKlient.slettSak(request, projection);
    }

    public String sendNyBeskjedMedKvittering(String grupperingsid, Merkelapp merkelapp, String eksternId,
                                             String virksomhetsnummer, String beskjedTekst, URI kvitteringLenke) {
        return sendNyBeskjed(grupperingsid, merkelapp, virksomhetsnummer, beskjedTekst, Optional.empty(), kvitteringLenke);
    }

    public String sendNyBeskjedMedEksternVarsling(String grupperingsid, Merkelapp merkelapp, String eksternId,
                                                  String virksomhetsnummer, String beskjedTekst, String varselTekst, URI lenke) {
        return sendNyBeskjed(grupperingsid, merkelapp, virksomhetsnummer, beskjedTekst, Optional.of(varselTekst), lenke);
    }

    private String sendNyBeskjed(String grupperingsid,
                                 Merkelapp beskjedMerkelapp,
                                 String organisasjonsnummer,
                                 String beskjedTekst,
                                 Optional<String> varselTekst,
                                 URI oppgaveLenke) {
        var beskjedInput = NyBeskjedInput.builder()
            .setNotifikasjon(NotifikasjonInput.builder()
                .setMerkelapp(beskjedMerkelapp.getBeskrivelse())
                .setTekst(beskjedTekst)
                .setLenke(oppgaveLenke.toString())
                .build())
            .setMottaker(lagAltinnMottakerInput())
            .setMetadata(MetadataInput.builder()
                .setVirksomhetsnummer(organisasjonsnummer)
                .setEksternId(UUID.randomUUID().toString())
                .setGrupperingsid(grupperingsid)
                .build())
            .setEksterneVarsler(varselTekst.map(tekst -> List.of(lagEksternVarselAltinn(tekst, 0))).orElse(List.of()))
            .build();
        var beskjedRequest = new NyBeskjedMutationRequest();
        beskjedRequest.setNyBeskjed(beskjedInput);
        var projection = new NyBeskjedResultatResponseProjection().typename()
            .onNyBeskjedVellykket(new NyBeskjedVellykketResponseProjection().id())
            .onUgyldigMerkelapp(new UgyldigMerkelappResponseProjection().feilmelding())
            .onUkjentProdusent(new UkjentProdusentResponseProjection().feilmelding())
            .onDuplikatEksternIdOgMerkelapp(new DuplikatEksternIdOgMerkelappResponseProjection().feilmelding())
            .onUgyldigMerkelapp(new UgyldigMerkelappResponseProjection().feilmelding())
            .onUgyldigMottaker(new UgyldigMottakerResponseProjection().feilmelding())
            .onUkjentRolle(new UkjentRolleResponseProjection().feilmelding());
        return minSideArbeidsgiverKlient.opprettBeskjedOgVarsling(beskjedRequest, projection);
    }

    private static MottakerInput lagAltinnMottakerInput() {
        return MottakerInput.builder()
            .setAltinnRessurs(AltinnRessursMottakerInput.builder().setRessursId(ALTINN_INNTEKTSMELDING_RESSURS).build())
            .build();
    }

    private static EksterntVarselInput lagEksternVarselAltinn(String varselTekst, Integer minutterForsinkelse) {
        var tittel = "Nav trenger inntektsmelding";
        return EksterntVarselInput.builder()
            .setAltinnressurs(EksterntVarselAltinnressursInput.builder()
                .setEpostTittel(tittel)
                .setEpostHtmlBody(varselTekst)
                .setSmsTekst("%s. %s".formatted(tittel, varselTekst))
                .setMottaker(lagAltinnRessursMottakerInput())
                .setSendetidspunkt(SendetidspunktInput.builder()
                    .setTidspunkt(LocalDateTime.now().plusMinutes(minutterForsinkelse).toString())
                    .build())
                .build())
            .build();
    }

    private static PaaminnelseEksterntVarselInput lagPåminnelseVarselAltinn(String påminnelseTekst) {
        var tittel = "Påminnelse: Nav trenger inntektsmelding";
        return PaaminnelseEksterntVarselInput.builder()
            .setAltinnressurs(PaaminnelseEksterntVarselAltinnressursInput.builder()
                .setEpostTittel(tittel)
                .setEpostHtmlBody(påminnelseTekst)
                .setSmsTekst("%s. %s".formatted(tittel, påminnelseTekst))
                .setMottaker(lagAltinnRessursMottakerInput())
                .setSendevindu(VARSEL_SENDEVINDU)
                .build())
            .build();
    }

    private static AltinnRessursMottakerInput lagAltinnRessursMottakerInput() {
        return AltinnRessursMottakerInput.builder().setRessursId(ALTINN_INNTEKTSMELDING_RESSURS).build();
    }
}
