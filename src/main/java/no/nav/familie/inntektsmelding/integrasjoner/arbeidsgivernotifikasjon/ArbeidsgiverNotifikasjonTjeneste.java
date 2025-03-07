package no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.konfig.Environment;

@ApplicationScoped
class ArbeidsgiverNotifikasjonTjeneste implements ArbeidsgiverNotifikasjon {

    static final String SERVICE_CODE = "4936";
    static final String SERVICE_EDITION_CODE = "1";
    static final String SAK_STATUS_TEKST = "";
    static final String SAK_STATUS_TEKST_ARBEIDSGIVERINITIERT = "Mottatt - Se kvittering eller korriger inntektsmelding";
    static final Sendevindu VARSEL_SENDEVINDU = Sendevindu.LOEPENDE;
    static final int PÅMINNELSE_ETTER_DAGER = Environment.current().getProperty("paaminnelse.etter.dager", int.class, 14);

    private ArbeidsgiverNotifikasjonKlient klient;

    @Inject
    public ArbeidsgiverNotifikasjonTjeneste(ArbeidsgiverNotifikasjonKlient klient) {
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
            .setMottakere(List.of(lagAltinnMottakerInput()))
            .build();

        var projection = new NySakResultatResponseProjection().typename()
            .onNySakVellykket(new NySakVellykketResponseProjection().id())
            .onUgyldigMerkelapp(new UgyldigMerkelappResponseProjection().feilmelding())
            .onUgyldigMottaker(new UgyldigMottakerResponseProjection().feilmelding())
            .onDuplikatGrupperingsid(new DuplikatGrupperingsidResponseProjection().feilmelding())
            .onDuplikatGrupperingsidEtterDelete(new DuplikatGrupperingsidEtterDeleteResponseProjection().feilmelding())
            .onUkjentProdusent(new UkjentProdusentResponseProjection().feilmelding())
            .onUkjentRolle(new UkjentRolleResponseProjection().feilmelding());

        return klient.opprettSak(request, projection);
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
    public String opprettNyBeskjedMedEksternVarsling(String grupperingsid,
                                                     Merkelapp beskjedMerkelapp,
                                                     String eksternId,
                                                     String organisasjonsnumme,
                                                     String beskjedTekst,
                                                     String varselTekst,
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
            .setEksterneVarsler(List.of(lagEksternVarselAltinn(varselTekst, 0)))
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

    @Override
    public String opprettMigrertOppgave(String grupperingsid, Merkelapp merkelapp, String eksternId, String orgnr, String oppgaveTekst, URI oppgaveLenke, LocalDate skjæringstidsunkt) {
        var request = NyOppgaveMutationRequest.builder()
            .setNyOppgave(NyOppgaveInput.builder()
                .setMottaker(lagAltinnMottakerInput())
                .setNotifikasjon(NotifikasjonInput.builder()
                    .setMerkelapp(merkelapp.getBeskrivelse())
                    .setTekst(oppgaveTekst)
                    .setLenke(oppgaveLenke.toString())
                    .build())
                .setMetadata(MetadataInput.builder()
                    .setVirksomhetsnummer(orgnr)
                    .setOpprettetTidspunkt(skjæringstidsunkt.minusWeeks(4).atStartOfDay().toString())
                    .setEksternId(eksternId)
                    .setGrupperingsid(grupperingsid)
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

    private static MottakerInput lagAltinnMottakerInput() {
        return MottakerInput.builder()
            .setAltinn(AltinnMottakerInput.builder().setServiceCode(SERVICE_CODE).setServiceEdition(SERVICE_EDITION_CODE).build())
            .build();
    }

    private static EksterntVarselInput lagEksternVarselAltinn(String varselTekst, Integer minutterForsinkelse) {
        return EksterntVarselInput.builder()
            .setAltinntjeneste(EksterntVarselAltinntjenesteInput.builder()
                .setTittel("Nav trenger inntektsmelding")
                .setInnhold(varselTekst)
                .setMottaker(lagAltinnTjenesteMottakerInput())
                .setSendetidspunkt(SendetidspunktInput.builder()
                    .setTidspunkt(LocalDateTime.now().plusMinutes(minutterForsinkelse).toString())
                    .build())
                .build())
            .build();
    }

    private static PaaminnelseEksterntVarselInput lagPåminnelseVarselAltinn(String påminnelseTekst) {
        return PaaminnelseEksterntVarselInput.builder()
            .setAltinntjeneste(PaaminnelseEksterntVarselAltinntjenesteInput.builder()
                .setTittel("Påminnelse: Nav trenger inntektsmelding")
                .setInnhold(påminnelseTekst)
                .setMottaker(lagAltinnTjenesteMottakerInput())
                .setSendevindu(VARSEL_SENDEVINDU)
                .build())
            .build();
    }

    private static AltinntjenesteMottakerInput lagAltinnTjenesteMottakerInput() {
        return AltinntjenesteMottakerInput.builder().setServiceCode(SERVICE_CODE).setServiceEdition(SERVICE_EDITION_CODE).build();
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
