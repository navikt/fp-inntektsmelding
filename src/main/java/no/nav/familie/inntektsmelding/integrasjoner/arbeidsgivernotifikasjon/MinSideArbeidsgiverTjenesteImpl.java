package no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon;

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

import no.nav.foreldrepenger.konfig.Environment;

@ApplicationScoped
class MinSideArbeidsgiverTjenesteImpl implements MinSideArbeidsgiverTjeneste {

    static final String SERVICE_CODE = "4936";
    static final String SERVICE_EDITION_CODE = "1";
    static final String SAK_STATUS_TEKST = "";
    static final String SAK_STATUS_TEKST_ARBEIDSGIVERINITIERT = "Mottatt - Se kvittering eller korriger inntektsmelding";
    static final Sendevindu VARSEL_SENDEVINDU = Sendevindu.LOEPENDE;
    static final int PÅMINNELSE_ETTER_DAGER;
    static final String ALTINN_INNTEKTSMELDING_RESSURS;
    public static final boolean BRUK_ALTINN_TRE_RESSURS_TOGGLE;

    static {
        var ENV = Environment.current();
        ALTINN_INNTEKTSMELDING_RESSURS = ENV.getRequiredProperty("altinn.tre.inntektsmelding.ressurs");
        BRUK_ALTINN_TRE_RESSURS_TOGGLE = ENV.getProperty("bruk.altinn.tre.ressurs.i.fager.toggle", boolean.class, false);
        PÅMINNELSE_ETTER_DAGER = ENV.getProperty("paaminnelse.etter.dager", int.class, 14);
    }

    private MinSideArbeidsgiverKlient klient;

    @Inject
    public MinSideArbeidsgiverTjenesteImpl(MinSideArbeidsgiverKlient klient) {
        this.klient = klient;
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

        return klient.opprettSak(request.build(), projection);
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

        return klient.opprettOppgave(request, projection);
    }

    @Override
    public String sendNyBeskjedMedEksternVarsling(String grupperingsid,
                                                  Merkelapp beskjedMerkelapp,
                                                  String eksternId,
                                                  String organisasjonsnummer,
                                                  String beskjedTekst,
                                                  String varselTekst,
                                                  URI oppgaveLenke) {
        return sendNyBeskjed(grupperingsid, beskjedMerkelapp, eksternId, organisasjonsnummer, beskjedTekst, Optional.of(varselTekst), oppgaveLenke);
    }

    @Override
    public String sendNyBeskjedMedKvittering(String grupperingsid,
                                Merkelapp beskjedMerkelapp,
                                String eksternId,
                                String organisasjonsnummer,
                                String beskjedTekst,
                                URI lenke) {
        return sendNyBeskjed(grupperingsid, beskjedMerkelapp, eksternId, organisasjonsnummer, beskjedTekst, Optional.empty(), lenke);
    }


    private String sendNyBeskjed(String grupperingsid,
                                 Merkelapp beskjedMerkelapp,
                                 String eksternId,
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
        return klient.opprettBeskjedOgVarsling(beskjedRequest, projection);
    }

    private static MottakerInput lagAltinnMottakerInput() {
        var builder = MottakerInput.builder();
            // TODO: Rydd opp etter Altinn 3 er i bruk i prod
            if (BRUK_ALTINN_TRE_RESSURS_TOGGLE) {
                builder.setAltinnRessurs(AltinnRessursMottakerInput.builder().setRessursId(ALTINN_INNTEKTSMELDING_RESSURS).build());
            } else {
                builder.setAltinn(AltinnMottakerInput.builder().setServiceCode(SERVICE_CODE).setServiceEdition(SERVICE_EDITION_CODE).build());
            }
        return builder.build();
    }

    private static EksterntVarselInput lagEksternVarselAltinn(String varselTekst, Integer minutterForsinkelse) {
        var builder = EksterntVarselInput.builder();
        var tittel = "Nav trenger inntektsmelding";
        // TODO: Rydd opp etter Altinn 3 er i bruk i prod
        if (BRUK_ALTINN_TRE_RESSURS_TOGGLE) {
            builder.setAltinnressurs(EksterntVarselAltinnressursInput.builder()
                .setEpostTittel(tittel)
                .setEpostHtmlBody(varselTekst)
                .setSmsTekst("%s. %s".formatted(tittel, varselTekst))
                .setMottaker(lagAltinnRessursMottakerInput())
                .setSendetidspunkt(SendetidspunktInput.builder()
                    .setTidspunkt(LocalDateTime.now().plusMinutes(minutterForsinkelse).toString())
                    .build())
                .build());
        } else {
            builder.setAltinntjeneste(EksterntVarselAltinntjenesteInput.builder()
                .setTittel(tittel)
                .setInnhold(varselTekst)
                .setMottaker(lagAltinnTjenesteMottakerInput())
                .setSendetidspunkt(SendetidspunktInput.builder()
                    .setTidspunkt(LocalDateTime.now().plusMinutes(minutterForsinkelse).toString())
                    .build())
                .build());
        }

        return builder.build();
    }

    private static PaaminnelseEksterntVarselInput lagPåminnelseVarselAltinn(String påminnelseTekst) {
        var builder = PaaminnelseEksterntVarselInput.builder();
        var tittel = "Påminnelse: Nav trenger inntektsmelding";
        // TODO: Rydd opp etter Altinn 3 er i bruk i prod
        if (BRUK_ALTINN_TRE_RESSURS_TOGGLE) {
            builder.setAltinnressurs(PaaminnelseEksterntVarselAltinnressursInput.builder()
                .setEpostTittel(tittel)
                .setEpostHtmlBody(påminnelseTekst)
                .setSmsTekst("%s. %s".formatted(tittel, påminnelseTekst))
                .setMottaker(lagAltinnRessursMottakerInput())
                .setSendevindu(VARSEL_SENDEVINDU)
                .build());
        } else {
            builder.setAltinntjeneste(PaaminnelseEksterntVarselAltinntjenesteInput.builder()
                .setTittel(tittel)
                .setInnhold(påminnelseTekst)
                .setMottaker(lagAltinnTjenesteMottakerInput())
                .setSendevindu(VARSEL_SENDEVINDU)
                .build());
        }

        return builder.build();
    }

    private static AltinntjenesteMottakerInput lagAltinnTjenesteMottakerInput() {
        return AltinntjenesteMottakerInput.builder().setServiceCode(SERVICE_CODE).setServiceEdition(SERVICE_EDITION_CODE).build();
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

        return klient.oppgaveUtført(request, projection);
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

        return klient.oppgaveUtgått(request, projection);
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

        return klient.oppdaterSakStatus(requestBuilder.build(), projection);
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

        return klient.oppdaterSakTilleggsinformasjon(request, projection);
    }

    @Override
    public String slettSak(String id) {
        var request = HardDeleteSakMutationRequest.builder().setId(id).build();
        var projection = new HardDeleteSakResultatResponseProjection().typename()
            .onHardDeleteSakVellykket(new HardDeleteSakVellykketResponseProjection().id())
            .onUgyldigMerkelapp(new UgyldigMerkelappResponseProjection().feilmelding())
            .onUkjentProdusent(new UkjentProdusentResponseProjection().feilmelding())
            .onSakFinnesIkke(new SakFinnesIkkeResponseProjection().feilmelding());
        return klient.slettSak(request, projection);
    }

}
