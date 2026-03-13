package no.nav.familie.inntektsmelding.inntektsmelding;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonValue;

public class InntektsmeldingDto {

    private final UUID inntektsmeldingUuid;
    private final UUID forespørselUuid;
    private final String fnr;
    private final Ytelse ytelse;
    private final ArbeidsgiverInformasjonDto arbeidsgiver;
    private final Kontaktperson kontaktperson;
    private final LocalDate startdato;
    private final BigDecimal inntekt;
    private final Innsendingsårsak innsendingsårsak;
    private final Innsendingstype innsendingstype;
    private final LocalDateTime innsendtTidspunkt;
    private final AvsenderSystem avsenderSystem;
    private final List<SøktRefusjon> søkteRefusjonsperioder;
    private final List<BortfaltNaturalytelse> bortfaltNaturalytelsePerioder;
    private final List<Endringsårsaker> endringAvInntektÅrsaker;

    private InntektsmeldingDto(Builder builder) {
        this.inntektsmeldingUuid = builder.inntektsmeldingUuid;
        this.forespørselUuid = builder.forespørselUuid;
        this.fnr = builder.fnr;
        this.ytelse = builder.ytelse;
        this.arbeidsgiver = builder.arbeidsgiver;
        this.kontaktperson = builder.kontaktperson;
        this.startdato = builder.startdato;
        this.inntekt = builder.inntekt;
        this.innsendingsårsak = builder.innsendingsårsak;
        this.innsendingstype = builder.innsendingstype;
        this.innsendtTidspunkt = builder.innsendtTidspunkt;
        this.avsenderSystem = builder.avsenderSystem;
        this.søkteRefusjonsperioder = builder.søkteRefusjonsperioder;
        this.bortfaltNaturalytelsePerioder = builder.bortfaltNaturalytelsePerioder;
        this.endringAvInntektÅrsaker = builder.endringAvInntektÅrsaker;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getInntektsmeldingUuid() {
        return inntektsmeldingUuid;
    }

    public UUID getForespørselUuid() {
        return forespørselUuid;
    }

    public String getFnr() {
        return fnr;
    }

    public Ytelse getYtelse() {
        return ytelse;
    }

    public ArbeidsgiverInformasjonDto getArbeidsgiver() {
        return arbeidsgiver;
    }

    public Kontaktperson getKontaktperson() {
        return kontaktperson;
    }

    public LocalDate getStartdato() {
        return startdato;
    }

    public BigDecimal getInntekt() {
        return inntekt;
    }

    public Innsendingsårsak getInnsendingsårsak() {
        return innsendingsårsak;
    }

    public Innsendingstype getInnsendingstype() {
        return innsendingstype;
    }

    public LocalDateTime getInnsendtTidspunkt() {
        return innsendtTidspunkt;
    }

    public AvsenderSystem getAvsenderSystem() {
        return avsenderSystem;
    }

    public List<SøktRefusjon> getSøkteRefusjonsperioder() {
        return søkteRefusjonsperioder;
    }

    public List<BortfaltNaturalytelse> getBortfaltNaturalytelsePerioder() {
        return bortfaltNaturalytelsePerioder;
    }

    public List<Endringsårsaker> getEndringAvInntektÅrsaker() {
        return endringAvInntektÅrsaker;
    }

    public static class Builder {

        private UUID inntektsmeldingUuid;
        private UUID forespørselUuid;
        private String fnr;
        private Ytelse ytelse;
        private ArbeidsgiverInformasjonDto arbeidsgiver;
        private Kontaktperson kontaktperson;
        private LocalDate startdato;
        private BigDecimal inntekt;
        private Innsendingsårsak innsendingsårsak;
        private Innsendingstype innsendingstype;
        private LocalDateTime innsendtTidspunkt;
        private AvsenderSystem avsenderSystem;
        private List<SøktRefusjon> søkteRefusjonsperioder;
        private List<BortfaltNaturalytelse> bortfaltNaturalytelsePerioder;
        private List<Endringsårsaker> endringAvInntektÅrsaker;

        private Builder() {
        }

        public Builder medInntektsmeldingUuid(UUID inntektsmeldingUuid) {
            this.inntektsmeldingUuid = inntektsmeldingUuid;
            return this;
        }

        public Builder medForespørselUuid(UUID forespørselUuid) {
            this.forespørselUuid = forespørselUuid;
            return this;
        }

        public Builder medFnr(String fnr) {
            this.fnr = fnr;
            return this;
        }

        public Builder medYtelse(Ytelse ytelse) {
            this.ytelse = ytelse;
            return this;
        }

        public Builder medArbeidsgiver(ArbeidsgiverInformasjonDto arbeidsgiver) {
            this.arbeidsgiver = arbeidsgiver;
            return this;
        }

        public Builder medKontaktperson(Kontaktperson kontaktperson) {
            this.kontaktperson = kontaktperson;
            return this;
        }

        public Builder medStartdato(LocalDate startdato) {
            this.startdato = startdato;
            return this;
        }

        public Builder medInntekt(BigDecimal inntekt) {
            this.inntekt = inntekt;
            return this;
        }

        public Builder medInnsendingsårsak(Innsendingsårsak innsendingsårsak) {
            this.innsendingsårsak = innsendingsårsak;
            return this;
        }

        public Builder medInnsendingstype(Innsendingstype innsendingstype) {
            this.innsendingstype = innsendingstype;
            return this;
        }

        public Builder medInnsendtTidspunkt(LocalDateTime innsendtTidspunkt) {
            this.innsendtTidspunkt = innsendtTidspunkt;
            return this;
        }

        public Builder medAvsenderSystem(AvsenderSystem avsenderSystem) {
            this.avsenderSystem = avsenderSystem;
            return this;
        }

        public Builder medSøkteRefusjonsperioder(List<SøktRefusjon> søkteRefusjonsperioder) {
            this.søkteRefusjonsperioder = søkteRefusjonsperioder;
            return this;
        }

        public Builder medBortfaltNaturalytelsePerioder(List<BortfaltNaturalytelse> bortfaltNaturalytelsePerioder) {
            this.bortfaltNaturalytelsePerioder = bortfaltNaturalytelsePerioder;
            return this;
        }

        public Builder medEndringAvInntektÅrsaker(List<Endringsårsaker> endringAvInntektÅrsaker) {
            this.endringAvInntektÅrsaker = endringAvInntektÅrsaker;
            return this;
        }

        public InntektsmeldingDto build() {
            return new InntektsmeldingDto(this);
        }
    }

    record ArbeidsgiverInformasjonDto(@JsonValue @NotNull @Digits(integer = 13, fraction = 0) @Pattern(regexp = REGEXP) String orgnr) {
        private static final String REGEXP = "^(\\d{9}|\\d{13})$";

        @Override
        public String toString() {
            return getClass().getSimpleName() + "<" + maskerId() + ">";
        }

        private String maskerId() {
            if (orgnr == null) {
                return "";
            }
            var length = orgnr.length();
            if (length <= 4) {
                return "*".repeat(length);
            }
            return "*".repeat(length - 4) + orgnr.substring(length - 4);
        }

        public boolean erVirksomhet() {
            return orgnr.length() == 9;
        }

    }

    public enum Endringsårsak {
        PERMITTERING,
        NY_STILLING,
        NY_STILLINGSPROSENT,
        SYKEFRAVÆR,
        BONUS,
        FERIETREKK_ELLER_UTBETALING_AV_FERIEPENGER,
        NYANSATT,
        MANGELFULL_RAPPORTERING_AORDNING,
        INNTEKT_IKKE_RAPPORTERT_ENDA_AORDNING,
        TARIFFENDRING,
        FERIE,
        VARIG_LØNNSENDRING,
        PERMISJON
    }

    public record SøktRefusjon(@NotNull LocalDate fom,
                               @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beløp) {
    }

    public record BortfaltNaturalytelse(@NotNull LocalDate fom,
                                        LocalDate tom,
                                        @NotNull Naturalytelsetype naturalytelsetype,
                                        @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beløp) {
    }

    public record Endringsårsaker(@NotNull @Valid no.nav.familie.inntektsmelding.inntektsmelding.rest.kontrakt.Endringsårsak årsak,
                                  LocalDate fom,
                                  LocalDate tom,
                                  LocalDate bleKjentFom) {
    }

    public record Kontaktperson(
        String tlf,
        String navn
    ) {
    }

    public record AvsenderSystem(
        String systemNavn,
        String systemVersjon
    ) {
    }

    public enum Innsendingsårsak {
        NY,
        ENDRING
    }

    public enum Innsendingstype {
        FORESPURT,
        ARBEIDSGIVER_INITIERT
    }

    public enum Ytelse {
        FORELDREPENGER,
        SVANGERSKAPSPENGER,
    }

    public enum Naturalytelsetype {
        ELEKTRISK_KOMMUNIKASJON,
        AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS,
        LOSJI,
        KOST_DOEGN,
        BESØKSREISER_HJEMMET_ANNET,
        KOSTBESPARELSE_I_HJEMMET,
        RENTEFORDEL_LÅN,
        BIL,
        KOST_DAGER,
        BOLIG,
        SKATTEPLIKTIG_DEL_FORSIKRINGER,
        FRI_TRANSPORT,
        OPSJONER,
        TILSKUDD_BARNEHAGEPLASS,
        ANNET,
        BEDRIFTSBARNEHAGEPLASS,
        YRKEBIL_TJENESTLIGBEHOV_KILOMETER,
        YRKEBIL_TJENESTLIGBEHOV_LISTEPRIS,
        INNBETALING_TIL_UTENLANDSK_PENSJONSORDNING,
    }

}
