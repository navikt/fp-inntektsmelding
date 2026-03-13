package no.nav.familie.inntektsmelding.integrasjoner.fpsak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørId;

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

    public FpsakKlient.InfoOmSakInntektsmeldingResponse henterInfoOmSakIFagsystem(AktørId aktørId, Ytelsetype ytelsetype) {
        return klient.hentInfoOmSak(aktørId, ytelsetype);
    }
}
