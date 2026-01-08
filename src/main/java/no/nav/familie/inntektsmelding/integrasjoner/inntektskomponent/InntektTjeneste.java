package no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.typer.dto.MånedslønnStatus;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.exception.TekniskException;

@ApplicationScoped
public class InntektTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(InntektTjeneste.class);
    private static final int DAG_I_MÅNED_RAPPORTERINGSFRIST = 5;
    private InntektskomponentKlient inntektskomponentKlient;

    InntektTjeneste() {
        // CDI
    }

    @Inject
    public InntektTjeneste(InntektskomponentKlient inntektskomponentKlient) {
        this.inntektskomponentKlient = inntektskomponentKlient;
    }

    // Tar inn dagens dato som parameter for å gjøre det enklere å skrive tester
    public Inntektsopplysninger hentInntekt(AktørIdEntitet aktørId, LocalDate skjæringstidspunkt, LocalDate dagensDato, String organisasjonsnummer,
                                            boolean harJobbetHeleBeregningsperioden) {
        // Hvis søker ikke har jobbet hele beregningsperioden, bryr vi oss ikke med å justere innhenting etter rapporteringsfrist
        var antallMånederViBerOm = harJobbetHeleBeregningsperioden ? finnAntallMånederViMåBeOm(skjæringstidspunkt, dagensDato) : 3;
        var fomDato = skjæringstidspunkt.minusMonths(antallMånederViBerOm);
        var tomDato = skjæringstidspunkt.minusMonths(1);
        var request = lagRequest(aktørId, fomDato, tomDato);
        try {
            var respons = inntektskomponentKlient.finnInntekt(request);
            var inntekter = oversettRespons(respons, organisasjonsnummer);
            var alleMåneder = inntekter.size() == antallMånederViBerOm
                              ? inntekter
                              : fyllInnManglendeMåneder(fomDato, antallMånederViBerOm, inntekter);
            var kuttetNedTilTreMndInntekt = fjernOverflødigeMånederOmNødvendig(alleMåneder);
            return beregnSnittOgLeggPåStatus(kuttetNedTilTreMndInntekt, dagensDato, organisasjonsnummer, harJobbetHeleBeregningsperioden);
        } catch (IntegrasjonException e) {
            LOG.warn("Nedetid i inntektskomponenten, returnerer tomme måneder uten snittlønn til frontend. Fikk feil {}", e.getMessage());
            return lagTomRespons(skjæringstidspunkt, organisasjonsnummer);
        }
    }

    private Inntektsopplysninger lagTomRespons(LocalDate skjæringstidspunkt, String organisasjonsnummer) {
        var tommeMåneder = Set.of(1, 2, 3).stream().map(i -> new Inntektsopplysninger.InntektMåned(null,
            YearMonth.from(skjæringstidspunkt.minusMonths(i)),
            MånedslønnStatus.NEDETID_AINNTEKT)).toList();
        return new Inntektsopplysninger(null, organisasjonsnummer, tommeMåneder);
    }

    private Inntektsopplysninger beregnSnittOgLeggPåStatus(List<Månedsinntekt> inntekter, LocalDate dagensDato, String organisasjonsnummer,
                                                           boolean harJobbetHeleBeregningsperioden) {
        var månedsinntekter = inntekter.stream().map(i -> mapInntektMedStatus(i, dagensDato, harJobbetHeleBeregningsperioden)).toList();
        var antallMndMedRapportertInntekt = månedsinntekter.stream().filter(m -> m.beløp() != null).count();
        if (antallMndMedRapportertInntekt > 3) {
            throw new TekniskException("FPINNTEKTSMELDING_INNTEKTKSKOMPONENT_1",
                String.format("Har mappet flere enn 3 måneder med inntekt, ugyldig tilstand. Mappede månedsinntekter var %s", månedsinntekter));
        }
        var totalLønn = månedsinntekter.stream()
            .map(Inntektsopplysninger.InntektMåned::beløp)
            .filter(Objects::nonNull)
            .reduce(BigDecimal::add)
            .orElse(BigDecimal.ZERO)
            .max(BigDecimal.ZERO); // hvis inntekt blir < 0 setter vi den til 0 for å unngå negative tall i inntektsmeldingen

        // Hvis søker ikke har jobbet hele beregningsperioden regner vi kun snitt utifra de månedene med inntekt vi faktisk finner
        var antallMndViSkalRegneSnittFra = harJobbetHeleBeregningsperioden ? 3 : månedsinntekter.stream().filter(b -> b.beløp() != null).count();

        var snittlønn = antallMndViSkalRegneSnittFra == 0
                        ? BigDecimal.ZERO // Nyansatt uten noe rapportert lønn
                        : totalLønn.divide(BigDecimal.valueOf(antallMndViSkalRegneSnittFra), 2, RoundingMode.HALF_EVEN);
        return new Inntektsopplysninger(snittlønn, organisasjonsnummer, månedsinntekter);
    }

    private Inntektsopplysninger.InntektMåned mapInntektMedStatus(Månedsinntekt i, LocalDate dagensDato, boolean harJobbetHeleBeregningsperioden) {
        var erInntektRapportert = i.beløp != null;
        if (erInntektRapportert) {
            return new Inntektsopplysninger.InntektMåned(i.beløp, i.måned, MånedslønnStatus.BRUKT_I_GJENNOMSNITT);
        }
        if (!harJobbetHeleBeregningsperioden) {
            return new Inntektsopplysninger.InntektMåned(i.beløp, i.måned, MånedslønnStatus.IKKE_RAPPORTERT_NYANSATT);
        }
        var skalInntektVæreRapportert = rapporteringsfristErPassert(i.måned.atDay(1), dagensDato);
        return skalInntektVæreRapportert
               ? new Inntektsopplysninger.InntektMåned(i.beløp, i.måned, MånedslønnStatus.IKKE_RAPPORTERT_MEN_BRUKT_I_GJENNOMSNITT)
               : new Inntektsopplysninger.InntektMåned(i.beløp, i.måned, MånedslønnStatus.IKKE_RAPPORTERT_RAPPORTERINGSFRIST_IKKE_PASSERT);
    }

    private int finnAntallMånederViMåBeOm(LocalDate skjæringstidspunkt, LocalDate dagensDato) {
        var beregningsperiodeAntallMnd = 3;
        if (!rapporteringsfristErPassert(skjæringstidspunkt.minusMonths(1), dagensDato)) {
            beregningsperiodeAntallMnd++;
        }
        if (!rapporteringsfristErPassert(skjæringstidspunkt.minusMonths(2), dagensDato)) {
            beregningsperiodeAntallMnd++;
        }
        return beregningsperiodeAntallMnd;
    }

    private boolean rapporteringsfristErPassert(LocalDate dato, LocalDate dagensDato) {
        return dagensDato.isAfter(dato.plusMonths(1).withDayOfMonth(DAG_I_MÅNED_RAPPORTERINGSFRIST));
    }

    private static List<Månedsinntekt> fjernOverflødigeMånederOmNødvendig(List<Månedsinntekt> alleMåneder) {
        // Er alle de tre siste månedene rapportert?
        alleMåneder.sort(Comparator.comparing(Månedsinntekt::måned));
        var treSisteMåneder = alleMåneder.subList(alleMåneder.size() - 3, alleMåneder.size());
        if (treSisteMåneder.stream().noneMatch(i -> i.beløp() == null)) {
            return treSisteMåneder;
        }

        var antallMndMedSattInntekt = alleMåneder.stream().filter(m -> m.beløp != null).toList().size();
        int overflødigeMåneder = antallMndMedSattInntekt > 3 ? antallMndMedSattInntekt - 3 : 0;
        // Vi fant inntekt på flere måneder enn vi trenger, fjerner de eldste som er overflødige
        if (overflødigeMåneder > 0) {
            return alleMåneder.subList(overflødigeMåneder, alleMåneder.size());
        }
        return alleMåneder;
    }

    public static List<Månedsinntekt> fyllInnManglendeMåneder(LocalDate fomDato,
                                                              long månederViBerOm,
                                                              List<Månedsinntekt> inntekter) {
        List<Månedsinntekt> liste = new ArrayList<>(inntekter);
        lagTommeInntekter(fomDato, månederViBerOm).stream()
            .filter(tomInntekt -> inntekter.stream().noneMatch(i -> i.måned.equals(tomInntekt.måned)))
            .forEach(liste::add);
        return liste;
    }

    private static List<Månedsinntekt> lagTommeInntekter(LocalDate fomDato, long månederViBerOm) {
        return Stream.iterate(fomDato.withDayOfMonth(1),
                date -> date.plusMonths(1))
            .limit(månederViBerOm)
            .map(fom -> new Månedsinntekt(
                YearMonth.of(fom.getYear(), fom.getMonth()),
                null))
            .toList();
    }

    private List<Månedsinntekt> oversettRespons(List<InntektskomponentKlient.Inntektsinformasjon> response, String organisasjonsnummer) {

        var månedsinntekter = response.stream()
            .filter(ii -> organisasjonsnummer.equals(ii.underenhet()))
            .flatMap(ii -> Optional.ofNullable(ii.inntektListe()).orElseGet(List::of).stream()
                .filter(i -> "Loennsinntekt".equals(i.type()))
                .filter(i -> i.beloep() != null)
                .map(i -> new Månedsinntekt(ii.maaned(), i.beloep())))
            .collect(Collectors.groupingBy(Månedsinntekt::måned, Collectors.reducing(BigDecimal.ZERO, Månedsinntekt::beløp, BigDecimal::add)));
        var resultat = månedsinntekter.entrySet().stream().map(e -> new Månedsinntekt(e.getKey(), e.getValue())).toList();
        return new ArrayList<>(resultat);
    }

    private record Månedsinntekt(YearMonth måned, BigDecimal beløp) {
    }

    private FinnInntektRequest lagRequest(AktørIdEntitet aktørId, LocalDate fomDato, LocalDate tomDato) {
        var fomÅrMåned = YearMonth.from(fomDato);
        var tomÅrMåned = YearMonth.from(tomDato);

        return new FinnInntektRequest(aktørId.getAktørId(), fomÅrMåned, tomÅrMåned);
    }

}
