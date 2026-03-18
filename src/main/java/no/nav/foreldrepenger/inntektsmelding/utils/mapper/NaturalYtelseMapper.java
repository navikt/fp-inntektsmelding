package no.nav.foreldrepenger.inntektsmelding.utils.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.inntektsmelding.inntektsmelding.InntektsmeldingDto;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.NaturalytelseType;
import no.nav.vedtak.konfig.Tid;

public class NaturalYtelseMapper {
    private static final Logger LOG = LoggerFactory.getLogger(NaturalYtelseMapper.class);

    private NaturalYtelseMapper() {
        // static
    }

    public static List<NaturalYtelse> mapNaturalYtelser(List<InntektsmeldingDto.BortfaltNaturalytelse> naturalYtelser) {

        var bortfalteNaturalYtelser = naturalYtelser.stream()
            .map(NaturalYtelseMapper::mapBortfalteNaturalYtelser)
            .toList();

        List<NaturalYtelse> resultat = new ArrayList<>(bortfalteNaturalYtelser);
        LOG.info("Fant {} bortfalte naturalytelser", resultat.size());

        var tilkomneNaturalYtelser = naturalYtelser.stream()
            .filter(naturalytelse -> Tid.tomEllerMax(naturalytelse.tom()).isBefore(Tid.TIDENES_ENDE))
            .map(NaturalYtelseMapper::mapTilkomneNaturalYtelser)
            .toList();

        LOG.info("Utledet {} tilkomne naturalytelser", tilkomneNaturalYtelser.size());

        resultat.addAll(tilkomneNaturalYtelser);
        return resultat;
    }

    public static List<NaturalYtelse> mapNaturalYtelserFraDto(List<InntektsmeldingDto.BortfaltNaturalytelse> naturalytelser) {
        if (naturalytelser == null) {
            return List.of();
        }
        List<NaturalYtelse> resultat = new ArrayList<>();

        var bortfalte = naturalytelser.stream()
            .map(n -> new NaturalYtelse(n.fom(), NaturalytelseType.valueOf(n.naturalytelsetype().name()), n.beløp(), true))
            .toList();
        resultat.addAll(bortfalte);

        var tilkomne = naturalytelser.stream()
            .filter(n -> Tid.tomEllerMax(n.tom()).isBefore(Tid.TIDENES_ENDE))
            .map(n -> new NaturalYtelse(n.tom().plusDays(1), NaturalytelseType.valueOf(n.naturalytelsetype().name()), n.beløp(), false))
            .toList();
        resultat.addAll(tilkomne);

        return resultat;
    }

    private static NaturalYtelse mapBortfalteNaturalYtelser(InntektsmeldingDto.BortfaltNaturalytelse bortfalt) {
        return new NaturalYtelse(
            bortfalt.fom(),
            NaturalytelseType.valueOf(bortfalt.naturalytelsetype().name()),
            bortfalt.beløp(),
            true);
    }

    private static NaturalYtelse mapTilkomneNaturalYtelser(InntektsmeldingDto.BortfaltNaturalytelse tilkommet) {
        return new NaturalYtelse(tilkommet.tom().plusDays(1),
            NaturalytelseType.valueOf(tilkommet.naturalytelsetype().name()),
            tilkommet.beløp(),
            false);

    }

    public record NaturalYtelse(LocalDate fom, NaturalytelseType type, BigDecimal beløp, boolean bortfallt) {
    }

}
