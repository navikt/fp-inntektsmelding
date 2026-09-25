package no.nav.foreldrepenger.inntektsmelding.inntektsmelding;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.inntektsmelding.forespørsel.lager.ForespørselRepository;
import no.nav.foreldrepenger.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.lager.InntektsmeldingRepository;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.InntektsmeldingStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.InntektsmeldingApiStatus;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;
import no.nav.foreldrepenger.inntektsmelding.typer.lager.AktørIdEntitet;

@Dependent
public class InntektsmeldingTjeneste {

    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private InntektsmeldingRepository inntektsmeldingRepository;
    private ForespørselRepository forespørselRepository;
    private static final Set<InntektsmeldingStatus> STATUS_AVVIST_OG_UTDADERT = EnumSet.of(
        InntektsmeldingStatus.AVVIST,
        InntektsmeldingStatus.UTDATERT);

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
        var inntekstmeldinger = hentAktiveInntektsmeldinger(forespørselUuid);
        return inntekstmeldinger.isEmpty() ? null : inntekstmeldinger.getFirst();
    }

    public List<InntektsmeldingDto> hentAktiveInntektsmeldinger(UUID forespørselUuid) {
        var forespørsel = forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid);

        return inntektsmeldingRepository.hentInntektsmeldingerSortertNyesteFørst(new AktørIdEntitet(forespørsel.aktørId().getAktørId()),
                forespørsel.arbeidsgiver().orgnr(),
                forespørsel.førsteUttaksdato(),
                forespørsel.ytelseType())
            .stream()
            .filter(inntektsmelding -> !STATUS_AVVIST_OG_UTDADERT.contains(inntektsmelding.getStatus()))
            .map(InntektsmeldingDtoMapper::mapFraEntitet)
            .toList();
    }

    public List<InntektsmeldingDto> hentAlleInntektsmeldinger(UUID forespørselUuid) {
        var forespørsel = forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid);

        return inntektsmeldingRepository.hentInntektsmeldingerSortertNyesteFørst(new AktørIdEntitet(forespørsel.aktørId().getAktørId()),
                forespørsel.arbeidsgiver().orgnr(),
                forespørsel.førsteUttaksdato(),
                forespørsel.ytelseType())
            .stream()
            .map(InntektsmeldingDtoMapper::mapFraEntitet)
            .toList();
    }

    public Long lagreOverstyrtInntektsmelding(InntektsmeldingDto inntektsmeldingDto) {
        return inntektsmeldingRepository.lagreInntektsmelding(InntektsmeldingDtoMapper.mapTilEntitetUtenKoblingMotForespørsel(inntektsmeldingDto));
    }

    public Long lagreInntektsmelding(InntektsmeldingDto inntektsmeldingDto, UUID forespørselUuid) {
        return inntektsmeldingRepository.lagreInntektsmelding(InntektsmeldingDtoMapper.mapTilEntitet(inntektsmeldingDto, forespørselRepository.hentForespørsel(forespørselUuid).orElseThrow()));
    }

    public void oppdatertStatusTilInntektsmelding(UUID inntektsmeldingUuid, InntektsmeldingStatus inntektsmeldingStatus) {
        inntektsmeldingRepository.oppdaterStatusTilInntektsmelding(inntektsmeldingUuid, inntektsmeldingStatus);
    }

    public List<InntektsmeldingDto> hentInntektsmeldingerFraFilter(String orgnr,
                                                                   AktørIdEntitet aktørId,
                                                                   Ytelsetype ytelseType,
                                                                   LocalDate fom,
                                                                   LocalDate tom,
                                                                   Long fraLoepenr,
                                                                   InntektsmeldingApiStatus status) {
        return inntektsmeldingRepository.hentInntektsmeldingerFraFilter(orgnr, aktørId, ytelseType, fom, tom, fraLoepenr, status)
            .stream()
            .map(InntektsmeldingDtoMapper::mapFraEntitet)
            .toList();
    }
}
