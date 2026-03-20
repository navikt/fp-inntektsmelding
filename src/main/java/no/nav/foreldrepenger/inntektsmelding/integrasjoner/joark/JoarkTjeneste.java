package no.nav.foreldrepenger.inntektsmelding.integrasjoner.joark;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Saksnummer;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.vedtak.felles.integrasjon.dokarkiv.DokArkiv;
import no.nav.vedtak.felles.integrasjon.dokarkiv.dto.AvsenderMottaker;
import no.nav.vedtak.felles.integrasjon.dokarkiv.dto.Bruker;
import no.nav.vedtak.felles.integrasjon.dokarkiv.dto.DokumentInfoOpprett;
import no.nav.vedtak.felles.integrasjon.dokarkiv.dto.Dokumentvariant;
import no.nav.vedtak.felles.integrasjon.dokarkiv.dto.OpprettJournalpostRequest;
import no.nav.vedtak.felles.integrasjon.dokarkiv.dto.Sak;

@ApplicationScoped
public class JoarkTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(JoarkTjeneste.class);
    // Ved maskinell journalføring skal enhet være satt til 9999. Se https://confluence.adeo.no/display/BOA/opprettJournalpost
    private static final String JOURNALFØRENDE_ENHET = "9999";
    private static final String JOURNALFØRING_TITTEL = "Inntektsmelding";
    // TODO Denne bør nok synkes med avsendersystem i XML
    private static final String KANAL = "NAV_NO";
    private static final String BREVKODE_IM = "4936";
    private static final String TEMA_FOR = "FOR";

    private DokArkiv joarkKlient;
    private OrganisasjonTjeneste organisasjonTjeneste;

    JoarkTjeneste() {
        // CDI proxy
    }

    @Inject
    public JoarkTjeneste(JoarkKlient joarkKlient, OrganisasjonTjeneste organisasjonTjeneste) {
        this.joarkKlient = joarkKlient;
        this.organisasjonTjeneste = organisasjonTjeneste;
    }

    public String journalførInntektsmelding(String xmlAvInntektsmelding,
                                            InntektsmeldingDto inntektsmelding,
                                            byte[] pdf,
                                            Saksnummer fagsystemSaksnummer) {
        var request = opprettRequest(inntektsmelding, xmlAvInntektsmelding, pdf, fagsystemSaksnummer);
        try {
            var response = joarkKlient.opprettJournalpost(request, false);
            // Kan nok fjerne loggingen etter en periode i dev, mest for feilsøking i starten.
            LOG.info("Journalført inntektsmelding fikk journalpostId {}", response.journalpostId());
            return response.journalpostId();
        } catch (Exception e) {
            throw new IllegalStateException("Klarte ikke journalføre innteketsmelding " + e);
        }
    }

    private OpprettJournalpostRequest opprettRequest(InntektsmeldingDto inntektsmeldingDto,
                                                     String inntektsmeldingXml,
                                                     byte[] inntektsmeldingPdf,
                                                     @Nullable Saksnummer saksnummer) {
        var opprettJournalpostRequestBuilder = OpprettJournalpostRequest.nyInngående()
            .medTittel(JOURNALFØRING_TITTEL)
            .medAvsenderMottaker(lagAvsenderBedrift(inntektsmeldingDto.getArbeidsgiver()))
            .medBruker(lagBruker(inntektsmeldingDto.getAktørId()))
            .medBehandlingstema(mapBehandlingTema(inntektsmeldingDto.getYtelse()))
            .medDatoMottatt(inntektsmeldingDto.getInnsendtTidspunkt().toLocalDate())
            .medTema(TEMA_FOR)
            .medEksternReferanseId(Optional.ofNullable(inntektsmeldingDto.getInntektsmeldingUuid())
                .map(UUID::toString)
                .orElse(UUID.randomUUID().toString()))
            .medJournalfoerendeEnhet(JOURNALFØRENDE_ENHET)
            .medKanal(KANAL) //TODO: Bør settes til HR_SYSTEM_API ved maskinell innsending, vurder INNSENDT_NAV_ANSATT ved overstyring også.
            .medDokumenter(lagDokumenter(inntektsmeldingXml, inntektsmeldingPdf));

        if (saksnummer != null && saksnummer.isNotEmpty()) {
            opprettJournalpostRequestBuilder
                .medSak(new Sak(saksnummer.saksnummer(), Fagsystem.FPSAK.getOffisiellKode(), Sak.Sakstype.FAGSAK));
        }
        return opprettJournalpostRequestBuilder.build();
    }

    private List<DokumentInfoOpprett> lagDokumenter(String xmlAvInntektsmelding, byte[] pdf) {
        var dokumentXML = new Dokumentvariant(Dokumentvariant.Variantformat.ORIGINAL, Dokumentvariant.Filtype.XML,
            xmlAvInntektsmelding.getBytes(StandardCharsets.UTF_8));

        var dokumentPDF = new Dokumentvariant(Dokumentvariant.Variantformat.ARKIV, Dokumentvariant.Filtype.PDF, pdf);

        var builder = DokumentInfoOpprett.builder()
            .medTittel(JOURNALFØRING_TITTEL)
            .medBrevkode(BREVKODE_IM)
            .leggTilDokumentvariant(dokumentXML)
            .leggTilDokumentvariant(dokumentPDF);

        return Collections.singletonList(builder.build());
    }

    private String mapBehandlingTema(Ytelsetype ytelsetype) {
        return switch (ytelsetype) {
            case FORELDREPENGER -> Behandlingtema.FORELDREPENGER.getOffisiellKode();
            case SVANGERSKAPSPENGER -> Behandlingtema.SVANGERSKAPSPENGER.getOffisiellKode();
        };
    }

    private Bruker lagBruker(AktørId aktørId) {
        return new Bruker(aktørId.getAktørId(), Bruker.BrukerIdType.AKTOERID);
    }

    private AvsenderMottaker lagAvsenderBedrift(Arbeidsgiver arbeidsgiver) {
        var org = organisasjonTjeneste.finnOrganisasjon(arbeidsgiver);
        return new AvsenderMottaker(org.orgnr(), AvsenderMottaker.AvsenderMottakerIdType.ORGNR, org.navn());
    }
}
