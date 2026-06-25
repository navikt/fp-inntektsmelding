package no.nav.foreldrepenger.inntektsmelding.inntektsmelding;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.lager.ForespørselRepository;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.lager.InntektsmeldingRepository;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.InntektsmeldingStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.foreldrepenger.inntektsmelding.typer.lager.AktørIdEntitet;

@Dependent
public class InntektsmeldingTjeneste {

    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private InntektsmeldingRepository inntektsmeldingRepository;
    private ForespørselRepository forespørselRepository;

    InntektsmeldingTjeneste() {
        // CDI proxy
    }

    @Inject
    public InntektsmeldingTjeneste(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                                   InntektsmeldingRepository inntektsmeldingRepository,
                                   ForespørselRepository forespørselRepository) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.inntektsmeldingRepository = inntektsmeldingRepository;
        this.forespørselRepository = forespørselRepository;
    }

    public InntektsmeldingDto hentInntektsmelding(long inntektsmeldingId) {
        return Optional.of(inntektsmeldingRepository.hent(inntektsmeldingId)).map(InntektsmeldingDtoMapper::mapFraEntitet).orElseThrow();
    }

    public InntektsmeldingDto hentInntektsmelding(UUID inntektsmeldingUuid) {
        return inntektsmeldingRepository.finnInntektsmelding(inntektsmeldingUuid).map(InntektsmeldingDtoMapper::mapFraEntitet).orElse(null);
    }

    public InntektsmeldingDto hentSisteInntektsmeldingForForespørsel(UUID forespørselUuid) {
        var inntekstmeldinger = hentInntektsmeldinger(forespørselUuid);
        return inntekstmeldinger.isEmpty() ? null : inntekstmeldinger.getFirst();
    }

    public List<InntektsmeldingDto> hentInntektsmeldinger(UUID forespørselUuid) {
        var forespørsel = forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(
                () -> new IllegalStateException("Prøver å hente data for en forespørsel som ikke finnes, forespørselUUID: " + forespørselUuid));

        return inntektsmeldingRepository.hentInntektsmeldingerSortertNyesteFørst(new AktørIdEntitet(forespørsel.aktørId().getAktørId()),
                forespørsel.arbeidsgiver().orgnr(),
                forespørsel.førsteUttaksdato(),
                forespørsel.ytelseType())
            .stream().map(InntektsmeldingDtoMapper::mapFraEntitet).toList();
    }

    public Long lagreOverstyrtInntektsmelding(InntektsmeldingDto inntektsmeldingDto) {
        return inntektsmeldingRepository.lagreInntektsmelding(InntektsmeldingDtoMapper.mapTilEntitetUtenKoblingMotForespørsel(inntektsmeldingDto));
    }

    public Long lagreInntektsmelding(InntektsmeldingDto inntektsmeldingDto, UUID forespørselUuid) {
        return inntektsmeldingRepository.lagreInntektsmelding(InntektsmeldingDtoMapper.mapTilEntitet(inntektsmeldingDto, forespørselRepository.hentForespørsel(forespørselUuid).orElseThrow()));
    }

    public List<InntektsmeldingDto> hentInntektsmeldingerFraFilter(String orgnr,
                                                                   AktørIdEntitet aktørId,
                                                                   Ytelsetype ytelseType,
                                                                   LocalDate fom,
                                                                   LocalDate tom,
                                                                   Long fraLoepenr,
                                                                   InntektsmeldingStatus status) {
        return inntektsmeldingRepository.hentInntektsmeldingerFraFilter(orgnr, aktørId, ytelseType, fom, tom, fraLoepenr, status)
            .stream()
            .map(InntektsmeldingDtoMapper::mapFraEntitet)
            .toList();
    }
}
