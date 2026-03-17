package no.nav.foreldrepenger.inntektsmelding.imdialog.tjenester;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.dokgen.DokumentGeneratorTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;

@ApplicationScoped
public class KvitteringTjeneste {
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private DokumentGeneratorTjeneste dokumentGeneratorTjeneste;

    KvitteringTjeneste() {
    }

    @Inject
    public KvitteringTjeneste(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                              InntektsmeldingTjeneste inntektsmeldingTjeneste,
                              DokumentGeneratorTjeneste dokumentGeneratorTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.dokumentGeneratorTjeneste = dokumentGeneratorTjeneste;
    }

    public byte[] hentPDF(long inntektsmeldingId) {
        var inntektsmelding = inntektsmeldingTjeneste.hentInntektsmelding(inntektsmeldingId);
        var forespørsler = forespørselBehandlingTjeneste.finnForespørsler(inntektsmelding.getAktørId(),
            Ytelsetype.valueOf(inntektsmelding.getYtelse().name()),
            inntektsmelding.getArbeidsgiver().orgnr());
        var forespørselType = forespørsler
            .stream()
            .filter(forespørselEntitet -> forespørselEntitet.førsteUttaksdato().equals(inntektsmelding.getStartdato()))
            .map(ForespørselDto::forespørselType)
            .findAny()
            .orElseThrow(() -> new IllegalStateException("Forespørseltype ikke funnet for inntektsmeldingId: " + inntektsmeldingId));
        return dokumentGeneratorTjeneste.mapDataOgGenererPdf(inntektsmelding, forespørselType);
    }
}
