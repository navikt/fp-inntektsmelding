package no.nav.familie.inntektsmelding.metrikker;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.konfig.Tid;

public class MetrikkerTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(MetrikkerTjeneste.class);

    private static final String JA = "Ja";
    private static final String NEI = "Nei";

    private MetrikkerTjeneste() {
        // Skjull konstruktor
    }

    private static final String APP_NAME = Environment.current().getNaisAppName().replace("-", "_");

    // Hvor mange dager er det mellom opprettelse og løsning av oppgaven når inntektsmelding sendes inn via eksternt system
    private static final DistributionSummary OPPGAVE_VARIGHET_EKSTERN_TELLER = Metrics.summary(APP_NAME + ".oppgaver.varighet.ekstern");

    // Hvor mange dager er det mellom opprettelse og løsning av oppgaven når inntektsmelding sendes inn via vårt eget skjema
    private static final DistributionSummary OPPGAVE_VARIGHET_INTERN_TELLER = Metrics.summary(APP_NAME + ".oppgaver.varighet.intern");

    // Måler innsending av arbeidsgiverinitiert inntektsmelding
    private static final String ARBEIDSGIVERINITIERT_INNSENDING = APP_NAME + ".arbeidsgiverinitiert.innsending";

    // Måler endring av arbeidsgiverinitiert inntektsmelding
    private static final String ARBEIDSGIVERINITIERT_ENDRING = APP_NAME + ".arbeidsgiverinitiert.endring";

    // Måler opprettelse av oppgaver per ytelse
    private static final String COUNTER_FORESPØRRSEL = APP_NAME + ".oppgaver.opprettet";

    // Måler mottak av inntektsmeldinger per ytelse
    private static final String COUNTER_INNTEKTSMELDING = APP_NAME + ".inntektsmeldinger.mottatt";

    // Måler årsaker til endring av inntekt i inntektsmeldinger innsendt
    private static final String COUNTER_ENDRINGSÅRSAKER = APP_NAME + ".inntektsmeldinger.endringsaarsak";

    // Måler hvor mange oppgaver som lukkes etter at fagsystem melder om en inntektsmelding ftinntektsmelding ikke kjenner til
    private static final String COUNTER_LUKK_EKSTERN = APP_NAME + ".oppgaver.lukk.ekstern";
    private static final String TAG_YTELSE = "ytelse";
    private static final String TAG_AARSAK = "aarsak";

    public static void loggForespørselOpprettet(Ytelsetype ytelsetype) {
        try {
            var tags = new ArrayList<Tag>();
            tags.add(new ImmutableTag(TAG_YTELSE, ytelsetype.name()));
            Metrics.counter(COUNTER_FORESPØRRSEL, tags).increment();
        } catch (Exception e) {
            loggFeil(e, "loggForespørselOpprettet");
        }
    }

    public static void loggForespørselLukkEkstern(ForespørselEntitet forespørsel) {
        try {
            forsøkLoggForespørselLukkEkstern(forespørsel);
        } catch (Exception e) {
            loggFeil(e, "loggForespørselLukkEkstern");
        }
    }

    public static void loggInnsendtInntektsmelding(InntektsmeldingEntitet inntektsmelding) {
        try {
            forsøkLoggInnsendtInntektsmelding(inntektsmelding);
        } catch (Exception e) {
            loggFeil(e, "loggInnsendtInntektsmelding");
        }
    }

    public static void loggForespørselLukkIntern(ForespørselEntitet forespørsel) {
        try {
            OPPGAVE_VARIGHET_INTERN_TELLER.record(finnAntallDagerÅpen(forespørsel));
        } catch (Exception e) {
            loggFeil(e, "loggForespørselLukkIntern");
        }
    }

    private static void forsøkLoggForespørselLukkEkstern(ForespørselEntitet forespørsel) {
        var tags = new ArrayList<Tag>();
        tags.add(new ImmutableTag(TAG_YTELSE, forespørsel.getYtelseType().name()));
        Metrics.counter(COUNTER_LUKK_EKSTERN, tags).increment();
        OPPGAVE_VARIGHET_EKSTERN_TELLER.record(finnAntallDagerÅpen(forespørsel));
    }

    private static long finnAntallDagerÅpen(ForespørselEntitet forespørsel) {
        var opprettetDato = forespørsel.getOpprettetTidspunkt().toLocalDate();
        return ChronoUnit.DAYS.between(opprettetDato, LocalDate.now());
    }

    private static void forsøkLoggInnsendtInntektsmelding(InntektsmeldingEntitet inntektsmelding) {
        var tags = new ArrayList<Tag>();
        var harOppgittRefusjon = inntektsmelding.getMånedRefusjon() != null && inntektsmelding.getMånedRefusjon().compareTo(BigDecimal.ZERO) > 0;
        var harOppgittEndringerIRefusjon = inntektsmelding.getRefusjonsendringer() != null && !inntektsmelding.getRefusjonsendringer().isEmpty();
        var harOppgittOpphørAvRefusjon =
            inntektsmelding.getOpphørsdatoRefusjon() != null && !inntektsmelding.getOpphørsdatoRefusjon().equals(Tid.TIDENES_ENDE);
        var harOppgittNaturalytelse = inntektsmelding.getBorfalteNaturalYtelser() != null && !inntektsmelding.getBorfalteNaturalYtelser().isEmpty();
        var harEndretInntekt = !inntektsmelding.getEndringsårsaker().isEmpty();

        tags.add(new ImmutableTag(TAG_YTELSE, inntektsmelding.getYtelsetype().name()));
        tags.add(new ImmutableTag("har_endret_inntekt", harEndretInntekt ? JA : NEI));
        tags.add(new ImmutableTag("har_oppgitt_refusjon", harOppgittRefusjon ? JA : NEI));
        tags.add(new ImmutableTag("har_oppgitt_endring_i_refusjon", harOppgittEndringerIRefusjon ? JA : NEI));
        tags.add(new ImmutableTag("har_oppgitt_opphoer_av_refusjon", harOppgittOpphørAvRefusjon ? JA : NEI));
        tags.add(new ImmutableTag("har_oppgitt_naturalytelse", harOppgittNaturalytelse ? JA : NEI));
        Metrics.counter(COUNTER_INNTEKTSMELDING, tags).increment();

        if (!inntektsmelding.getEndringsårsaker().isEmpty()) {
            var endringsårsakerTags = new ArrayList<Tag>();
            endringsårsakerTags.add(new ImmutableTag(TAG_YTELSE, inntektsmelding.getYtelsetype().name()));
            endringsårsakerTags.add(new ImmutableTag(TAG_AARSAK, inntektsmelding.getEndringsårsaker().stream()
                .map(en -> en.getÅrsak().name())
                .sorted()
                .collect(Collectors.joining("-"))));
            Metrics.counter(COUNTER_ENDRINGSÅRSAKER, endringsårsakerTags).increment();
        }
    }

    private static void loggFeil(Exception e, String metodekall) {
        String msg = String.format("FPINNTEKTSMELDING_METRIKKER_1: Feil ved generering av metrikker i metode %s, fikk feilmelding %s", metodekall, e);
        LOG.warn(msg);
    }

    public static void logginnsendtArbeidsgiverinitiertIm(InntektsmeldingEntitet imEntitet) {
        try {
            var tags = new ArrayList<Tag>();
            tags.add(new ImmutableTag(TAG_YTELSE, imEntitet.getYtelsetype().name()));
            Metrics.counter(ARBEIDSGIVERINITIERT_INNSENDING, tags).increment();
        } catch (Exception e) {
            loggFeil(e, "logginnsendtArbeidsgiverinitiertIm");
        }
    }

    public static void loggEndretArbeidsgiverinitiertIm(InntektsmeldingEntitet imEntitet) {
        try {
            var tags = new ArrayList<Tag>();
            tags.add(new ImmutableTag(TAG_YTELSE, imEntitet.getYtelsetype().name()));
            Metrics.counter(ARBEIDSGIVERINITIERT_ENDRING, tags).increment();
        } catch (Exception e) {
            loggFeil(e, "loggEndretArbeidsgiverinitiertIm");
        }
    }

}
