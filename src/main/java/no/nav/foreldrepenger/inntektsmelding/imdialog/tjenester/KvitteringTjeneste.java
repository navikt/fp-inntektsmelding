package no.nav.foreldrepenger.inntektsmelding.imdialog.tjenester;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselDto;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.integrasjoner.dokgen.DokumentGeneratorTjeneste;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;

@ApplicationScoped
public class KvitteringTjeneste {
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private DokumentGeneratorTjeneste dokumentGeneratorTjeneste;

    KvitteringTjeneste() {
    }

    @Inject
    public KvitteringTjeneste(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                              DokumentGeneratorTjeneste dokumentGeneratorTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.dokumentGeneratorTjeneste = dokumentGeneratorTjeneste;
    }

    public byte[] hentPDF(InntektsmeldingDto inntektsmelding) {
        var forespørsler = forespørselBehandlingTjeneste.finnForespørsler(inntektsmelding.getAktørId(),
            Ytelsetype.valueOf(inntektsmelding.getYtelse().name()),
            inntektsmelding.getArbeidsgiver().orgnr());
        var forespørselType = forespørsler
            .stream()
            .filter(forespørselEntitet -> forespørselEntitet.førsteUttaksdato().equals(inntektsmelding.getStartdato()))
            .map(ForespørselDto::forespørselType)
            .findAny()
            .orElseThrow(() -> new IllegalStateException("Forespørseltype ikke funnet for inntektsmeldingUuid: " + inntektsmelding.getInntektsmeldingUuid()));
        return dokumentGeneratorTjeneste.mapDataOgGenererPdf(inntektsmelding, forespørselType);
    }
}
