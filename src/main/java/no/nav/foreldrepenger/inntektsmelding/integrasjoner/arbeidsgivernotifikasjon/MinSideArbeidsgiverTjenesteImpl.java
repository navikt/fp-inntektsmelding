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
import no.nav.foreldrepenger.inntektsmelding.imdialog.rest.kvittering.PdfDokumentRest;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.altinn.AltinnRessurser;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.konfig.KonfigVerdi;

import org.jspecify.annotations.NonNull;

@ApplicationScoped
class MinSideArbeidsgiverTjenesteImpl implements MinSideArbeidsgiverTjeneste {
    private static final Environment ENV = Environment.current();
    static final String SAK_STATUS_TEKST = "";
    static final String SAK_STATUS_TEKST_ARBEIDSGIVERINITIERT = "Mottatt - Se kvittering eller korriger inntektsmelding";
    static final Sendevindu VARSEL_SENDEVINDU = Sendevindu.LOEPENDE;
    static final int PÅMINNELSE_ETTER_DAGER;
    static final String ALTINN_INNTEKTSMELDING_RESSURS = AltinnRessurser.ALTINN_TRE_INNTEKTSMELDING_RESSURS;

    private PersonTjeneste personTjeneste;
    private OrganisasjonTjeneste organisasjonTjeneste;
    private String inntektsmeldingSkjemaLenke;

    static {
        PÅMINNELSE_ETTER_DAGER = ENV.getProperty("paaminnelse.etter.dager", int.class, 14);
    }

    private final MinSideArbeidsgiverKlient minSideArbeidsgiverKlient;

    @Inject
    public MinSideArbeidsgiverTjenesteImpl(PersonTjeneste personTjeneste,
                                           OrganisasjonTjeneste organisasjonTjeneste,
                                           @KonfigVerdi(value = "inntektsmelding.skjema.lenke", defaultVerdi = "https://arbeidsgiver.nav.no/fp-im-dialog")
                                           String inntektsmeldingSkjemaLenke,
                                           MinSideArbeidsgiverKlient minSideArbeidsgiverKlient) {
        this.personTjeneste = personTjeneste;
        this.organisasjonTjeneste = organisasjonTjeneste;
        this.minSideArbeidsgiverKlient = minSideArbeidsgiverKlient;
        this.inntektsmeldingSkjemaLenke = inntektsmeldingSkjemaLenke;
    }

    @Override
    public String opprettSak(String grupperingsid, Merkelapp merkelapp, String organisasjonsnumme, String saksTittel, URI lenke) {
        var request = NySakMutationRequest.builder()
            .setGrupperingsid(grupperingsid)
            .setTittel(saksTittel)
            .setVirksomhetsnummer(organisasjonsnumme)
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

    @Override
    public String opprettSak(ForespørselDto forespørselDto) {
        var person = personTjeneste.hentPersonInfoFraAktørId(forespørselDto.aktørId(), forespørselDto.ytelseType());

        var merkelapp = ForespørselTekster.finnMerkelapp(forespørselDto.ytelseType());
        var skjemaUri = getSkjemaUri(forespørselDto);
        var sakstittel = ForespørselTekster.lagSaksTittel(person.mapFulltNavn(), person.fødselsdato());
        var request = NySakMutationRequest.builder()
            .setGrupperingsid(forespørselDto.uuid().toString())
            .setTittel(sakstittel)
            .setVirksomhetsnummer(forespørselDto.arbeidsgiver().orgnr())
            .setMerkelapp(merkelapp.getBeskrivelse())
            .setLenke(skjemaUri.toString())
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

    @Override
    public String opprettOppgave(ForespørselDto forespørselDto) {
        var ytelsetype = forespørselDto.ytelseType();
        var organisasjon = organisasjonTjeneste.finnOrganisasjon(forespørselDto.arbeidsgiver());

        var merkelapp = ForespørselTekster.finnMerkelapp(ytelsetype);
        var skjemaUri = getSkjemaUri(forespørselDto);

        var orgnr = forespørselDto.arbeidsgiver().orgnr();
        var request = NyOppgaveMutationRequest.builder()
            .setNyOppgave(NyOppgaveInput.builder()
                .setMottaker(lagAltinnMottakerInput())
                .setNotifikasjon(NotifikasjonInput.builder()
                    .setMerkelapp(merkelapp.getBeskrivelse())
                    .setTekst(ForespørselTekster.lagOppgaveTekst(ytelsetype))
                    .setLenke(skjemaUri.toString())
                    .build())
                .setMetadata(MetadataInput.builder()
                    .setVirksomhetsnummer(orgnr)
                    .setEksternId(forespørselDto.uuid().toString())
                    .setGrupperingsid(forespørselDto.uuid().toString())
                    .build())
                .setEksterneVarsler(List.of(lagEksternVarselAltinn(ForespørselTekster.lagVarselTekst(ytelsetype, organisasjon), 15)))
                .setPaaminnelse(PaaminnelseInput.builder()
                    .setTidspunkt(PaaminnelseTidspunktInput.builder().setEtterOpprettelse(Duration.ofDays(PÅMINNELSE_ETTER_DAGER).toString()).build())
                    .setEksterneVarsler(List.of(lagPåminnelseVarselAltinn(ForespørselTekster.lagPåminnelseTekst(ytelsetype, organisasjon))))
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


    @Override
    public String opprettOppgave(String grupperingsid,
                                 Merkelapp oppgaveMerkelapp,
                                 String eksternId,
                                 String organisasjonsnumme,
                                 String oppgaveTekst,
                                 String varselTekst,
                                 String påminnelseTekst,
                                 URI oppgaveLenke) {

        var request = NyOppgaveMutationRequest.builder()
            .setNyOppgave(NyOppgaveInput.builder()
                .setMottaker(lagAltinnMottakerInput())
                .setNotifikasjon(NotifikasjonInput.builder()
                    .setMerkelapp(oppgaveMerkelapp.getBeskrivelse())
                    .setTekst(oppgaveTekst)
                    .setLenke(oppgaveLenke.toString())
                    .build())
                .setMetadata(MetadataInput.builder()
                    .setVirksomhetsnummer(organisasjonsnumme)
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

    @Override
    public String sendNyBeskjedMedEksternVarsling(String grupperingsid,
                                                  Merkelapp beskjedMerkelapp,
                                                  String eksternId,
                                                  String organisasjonsnummer,
                                                  String beskjedTekst,
                                                  String varselTekst,
                                                  URI oppgaveLenke) {
        return sendNyBeskjed(grupperingsid, beskjedMerkelapp, organisasjonsnummer, beskjedTekst, Optional.of(varselTekst), oppgaveLenke);
    }

    @Override
    public String sendNyBeskjedMedKvittering(String grupperingsid,
                                Merkelapp beskjedMerkelapp,
                                String eksternId,
                                String organisasjonsnummer,
                                String beskjedTekst,
                                URI lenke) {
        return sendNyBeskjed(grupperingsid, beskjedMerkelapp, organisasjonsnummer, beskjedTekst, Optional.empty(), lenke);
    }


    private String sendNyBeskjed(String grupperingsid,
                                 Merkelapp beskjedMerkelapp,
                                 String organisasjonsnumme,
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
                .setVirksomhetsnummer(organisasjonsnumme)
                .setEksternId(UUID.randomUUID().toString())
                .setGrupperingsid(grupperingsid)
                .build())
            .setEksterneVarsler(varselTekst.map(tekst ->List.of(lagEksternVarselAltinn(tekst, 0))).orElse(List.of()))
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

    @Override
    public String oppgaveUtført(String oppgaveId, OffsetDateTime tidspunkt) {

        var request = OppgaveUtfoertMutationRequest.builder()
            .setId(oppgaveId)
            .setUtfoertTidspunkt(tidspunkt.format(DateTimeFormatter.ISO_DATE_TIME))
            .build();

        var projection = new OppgaveUtfoertResultatResponseProjection().typename()
            .onOppgaveUtfoertVellykket(new OppgaveUtfoertVellykketResponseProjection().id())
            .onUgyldigMerkelapp(new UgyldigMerkelappResponseProjection().feilmelding())
            .onNotifikasjonFinnesIkke(new NotifikasjonFinnesIkkeResponseProjection().feilmelding())
            .onUkjentProdusent(new UkjentProdusentResponseProjection().feilmelding());

        return minSideArbeidsgiverKlient.oppgaveUtført(request, projection);
    }

    @Override
    public String oppgaveUtgått(String oppgaveId, OffsetDateTime tidspunkt) {

        var request = OppgaveUtgaattMutationRequest.builder()
            .setId(oppgaveId)
            .setUtgaattTidspunkt(tidspunkt.format(DateTimeFormatter.ISO_DATE_TIME))
            .build();

        var projection = new OppgaveUtgaattResultatResponseProjection().typename()
            .onOppgaveUtgaattVellykket(new OppgaveUtgaattVellykketResponseProjection().id())
            .onUgyldigMerkelapp(new UgyldigMerkelappResponseProjection().feilmelding())
            .onNotifikasjonFinnesIkke(new NotifikasjonFinnesIkkeResponseProjection().feilmelding())
            .onUkjentProdusent(new UkjentProdusentResponseProjection().feilmelding());

        return minSideArbeidsgiverKlient.oppgaveUtgått(request, projection);
    }

    @Override
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


    @Override
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

    @Override
    public String slettSak(String id) {
        var request = HardDeleteSakMutationRequest.builder().setId(id).build();
        var projection = new HardDeleteSakResultatResponseProjection().typename()
            .onHardDeleteSakVellykket(new HardDeleteSakVellykketResponseProjection().id())
            .onUgyldigMerkelapp(new UgyldigMerkelappResponseProjection().feilmelding())
            .onUkjentProdusent(new UkjentProdusentResponseProjection().feilmelding())
            .onSakFinnesIkke(new SakFinnesIkkeResponseProjection().feilmelding());
        return minSideArbeidsgiverKlient.slettSak(request, projection);
    }

    @Override
    public String sendNyBeskjedMedKvittering(ForespørselDto forespørselDto, UUID inntektsmeldingUuid, String beskjedTekst) {
        var merkelapp = ForespørselTekster.finnMerkelapp(forespørselDto.ytelseType());

        String url = new StringBuilder(inntektsmeldingSkjemaLenke)
            .append("/server/api")
            .append(PdfDokumentRest.INNTEKTSMELDING_FULL_PATH)
            .append("/")
            .append(inntektsmeldingUuid).toString();
        return sendNyBeskjed(forespørselDto.uuid().toString(), merkelapp, forespørselDto.arbeidsgiver().orgnr(), beskjedTekst, Optional.empty(), URI.create(url));
    }

    private URI getSkjemaUri(ForespørselDto forespørselDto) {
        return URI.create(inntektsmeldingSkjemaLenke + "/" + forespørselDto.uuid());
    }

}
