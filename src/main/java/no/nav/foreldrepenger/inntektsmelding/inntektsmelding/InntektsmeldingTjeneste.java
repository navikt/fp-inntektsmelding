package no.nav.foreldrepenger.inntektsmelding.inntektsmelding;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.lager.InntektsmeldingRepository;

@Dependent
public class InntektsmeldingTjeneste {

    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private InntektsmeldingRepository inntektsmeldingRepository;

    InntektsmeldingTjeneste() {
        // CDI proxy
    }

    @Inject
    public InntektsmeldingTjeneste(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                                   InntektsmeldingRepository inntektsmeldingRepository) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.inntektsmeldingRepository = inntektsmeldingRepository;
    }

    public InntektsmeldingDto hentInntektsmelding(long inntektsmeldingId) {
        return Optional.of(inntektsmeldingRepository.hent(inntektsmeldingId)).map(InntektsmeldingDtoMapper::mapFraEntitet).orElseThrow();
    }

    public InntektsmeldingDto hentInntektsmelding(UUID inntektsmeldingUuid) {
        return inntektsmeldingRepository.finnInntektsmelding(inntektsmeldingUuid).map(InntektsmeldingDtoMapper::mapFraEntitet).orElse(null);
    }

    public List<InntektsmeldingDto> hentInntektsmeldinger(UUID forespørselUuid) {
        var forespørsel = forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(
                () -> new IllegalStateException("Prøver å hente data for en forespørsel som ikke finnes, forespørselUUID: " + forespørselUuid));

        return inntektsmeldingRepository.hentInntektsmeldingerSortertNyesteFørst(forespørsel.getAktørId(),
                forespørsel.getOrganisasjonsnummer(),
                forespørsel.getFørsteUttaksdato(),
                forespørsel.getYtelseType())
            .stream().map(InntektsmeldingDtoMapper::mapFraEntitet).toList();
    }

    public Long lagreInntektsmelding(InntektsmeldingDto inntektsmeldingDto) {
        return inntektsmeldingRepository.lagreInntektsmelding(InntektsmeldingDtoMapper.mapTilEntitet(inntektsmeldingDto));
    }

}
