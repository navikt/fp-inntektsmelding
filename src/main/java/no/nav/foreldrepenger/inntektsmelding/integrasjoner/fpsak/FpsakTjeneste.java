package no.nav.foreldrepenger.inntektsmelding.integrasjoner.fpsak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Saksnummer;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;

import java.util.List;

@ApplicationScoped
public class FpsakTjeneste {

    private FpsakKlient klient;

    public FpsakTjeneste() {
        // CDI
    }

    @Inject
    public FpsakTjeneste(FpsakKlient klient) {
        this.klient = klient;
    }

    public List<FpsakFagsak> henterInfoOmSakIFagsystem(AktørId aktørId, Ytelsetype ytelsetype) {
        var respons = klient.hentSaksoversiktRelevantForInntektsmeldinger(aktørId, ytelsetype);
        return respons.stream().map(this::mapRespons).toList();
    }

    private FpsakFagsak mapRespons(FpsakKlient.InfoOmSakInntektsmeldingResponse r) {
        return new FpsakFagsak(mapStatus(r.statusInntektsmelding()), r.førsteUttaksdato(), r.skjæringstidspunkt(), new Saksnummer(r.saksnummer()));
    }

    private FpsakFagsak.StatusSakInntektsmelding mapStatus(FpsakKlient.StatusSakInntektsmelding status) {
        return switch (status) {
            case ÅPEN_FOR_BEHANDLING -> FpsakFagsak.StatusSakInntektsmelding.ÅPEN_FOR_BEHANDLING;
            case SØKT_FOR_TIDLIG -> FpsakFagsak.StatusSakInntektsmelding.SØKT_FOR_TIDLIG;
            case VENTER_PÅ_SØKNAD -> FpsakFagsak.StatusSakInntektsmelding.VENTER_PÅ_SØKNAD;
            case PAPIRSØKNAD_IKKE_REGISTRERT -> FpsakFagsak.StatusSakInntektsmelding.PAPIRSØKNAD_IKKE_REGISTRERT;
            case INGEN_BEHANDLING -> FpsakFagsak.StatusSakInntektsmelding.INGEN_BEHANDLING;
        };
    }
}
