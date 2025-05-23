package no.nav.familie.inntektsmelding.imdialog.tjenester;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.familie.inntektsmelding.integrasjoner.dokgen.FpDokgenTjeneste;

@ApplicationScoped
public class InntektsmeldingTjeneste {
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private InntektsmeldingRepository inntektsmeldingRepository;
    private FpDokgenTjeneste fpDokgenTjeneste;

    InntektsmeldingTjeneste() {
    }

    @Inject
    public InntektsmeldingTjeneste(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                                   InntektsmeldingRepository inntektsmeldingRepository,
                                   FpDokgenTjeneste fpDokgenTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.inntektsmeldingRepository = inntektsmeldingRepository;
        this.fpDokgenTjeneste = fpDokgenTjeneste;
    }

    public InntektsmeldingEntitet hentInntektsmelding(long inntektsmeldingId) {
        return inntektsmeldingRepository.hentInntektsmelding(inntektsmeldingId);
    }

    public List<InntektsmeldingEntitet> hentInntektsmeldinger(UUID forespørselUuid) {
        var forespørsel = forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(
                () -> new IllegalStateException("Prøver å hente data for en forespørsel som ikke finnes, forespørselUUID: " + forespørselUuid));

        return inntektsmeldingRepository.hentInntektsmeldinger(forespørsel.getAktørId(),
            forespørsel.getOrganisasjonsnummer(),
            forespørsel.getFørsteUttaksdato(),
            forespørsel.getYtelseType());
    }

    public byte[] hentPDF(long inntektsmeldingId) {
        var inntektsmeldingEntitet = inntektsmeldingRepository.hentInntektsmelding(inntektsmeldingId);
        var forespørsler = forespørselBehandlingTjeneste.finnForespørsler(inntektsmeldingEntitet.getAktørId(),
            inntektsmeldingEntitet.getYtelsetype(),
            inntektsmeldingEntitet.getArbeidsgiverIdent());
        var forespørselType = forespørsler
            .stream()
            .filter(forespørselEntitet -> forespørselEntitet.getFørsteUttaksdato().equals(inntektsmeldingEntitet.getStartDato()))
            .map(ForespørselEntitet::getForespørselType)
            .findAny()
            .orElseThrow(() -> new IllegalStateException("Forespørseltype ikke funnet for inntektsmeldingId: " + inntektsmeldingId));
        return fpDokgenTjeneste.mapDataOgGenererPdf(inntektsmeldingEntitet, forespørselType);
    }
}
