package no.nav.familie.inntektsmelding.inntektsmelding;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class InntektsmeldingDto {
    private final UUID inntektsmeldingUuid;
    private final String aktørId;
    private final Ytelse ytelse;
    private final Arbeidsgiver arbeidsgiver;
    private final Kontaktperson kontaktperson;
    private final LocalDate startdato;
    private final BigDecimal inntekt;
    private final Innsendingsårsak innsendingsårsak; //TODO: finnes ikke i DB ennå
    private final Innsendingstype innsendingstype; //TODO: finnes ikke i DB ennå
    private final LocalDateTime innsendtTidspunkt;
    private final Kildesystem kildesystem;
    private final String opprettetAv;
    private final AvsenderSystem avsenderSystem; //TODO: finnes ikke i DB ennå
    private final BigDecimal månedRefusjon;
    private final LocalDate opphørsdatoRefusjon;
    private final List<SøktRefusjon> søkteRefusjonsperioder;
    private final List<BortfaltNaturalytelse> bortfaltNaturalytelsePerioder;
    private final List<Endringsårsaker> endringAvInntektÅrsaker;

    private InntektsmeldingDto(Builder builder) {
        this.inntektsmeldingUuid = builder.inntektsmeldingUuid;
        this.aktørId = builder.aktørId;
        this.ytelse = builder.ytelse;
        this.arbeidsgiver = builder.arbeidsgiver;
        this.kontaktperson = builder.kontaktperson;
        this.startdato = builder.startdato;
        this.inntekt = builder.inntekt;
        this.innsendingsårsak = builder.innsendingsårsak;
        this.innsendingstype = builder.innsendingstype;
        this.innsendtTidspunkt = builder.innsendtTidspunkt;
        this.kildesystem = builder.kildesystem;
        this.opprettetAv = builder.opprettetAv;
        this.avsenderSystem = builder.avsenderSystem;
        this.månedRefusjon = builder.månedRefusjon;
        this.opphørsdatoRefusjon = builder.opphørsdatoRefusjon;
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

    public String getAktørId() {
        return aktørId;
    }

    public Ytelse getYtelse() {
        return ytelse;
    }

    public Arbeidsgiver getArbeidsgiver() {
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

    public Kildesystem getKildesystem() {
        return kildesystem;
    }

    public String getOpprettetAv() {
        return opprettetAv;
    }

    public AvsenderSystem getAvsenderSystem() {
        return avsenderSystem;
    }

    public List<SøktRefusjon> getSøkteRefusjonsperioder() {
        return søkteRefusjonsperioder;
    }

    public BigDecimal getMånedRefusjon() {
        return månedRefusjon;
    }

    public LocalDate getOpphørsdatoRefusjon() {
        return opphørsdatoRefusjon;
    }

    public List<BortfaltNaturalytelse> getBortfaltNaturalytelsePerioder() {
        return bortfaltNaturalytelsePerioder;
    }

    public List<Endringsårsaker> getEndringAvInntektÅrsaker() {
        return endringAvInntektÅrsaker;
    }

    public static class Builder {

        private UUID inntektsmeldingUuid;
        private String aktørId;
        private Ytelse ytelse;
        private Arbeidsgiver arbeidsgiver;
        private Kontaktperson kontaktperson;
        private LocalDate startdato;
        private BigDecimal inntekt;
        private Innsendingsårsak innsendingsårsak;
        private Innsendingstype innsendingstype;
        private LocalDateTime innsendtTidspunkt;
        private Kildesystem kildesystem;
        private String opprettetAv;
        private AvsenderSystem avsenderSystem;
        private BigDecimal månedRefusjon;
        private LocalDate opphørsdatoRefusjon;
        private List<SøktRefusjon> søkteRefusjonsperioder;
        private List<BortfaltNaturalytelse> bortfaltNaturalytelsePerioder;
        private List<Endringsårsaker> endringAvInntektÅrsaker;

        private Builder() {
        }

        public Builder medInntektsmeldingUuid(UUID inntektsmeldingUuid) {
            this.inntektsmeldingUuid = inntektsmeldingUuid;
            return this;
        }

        public Builder medAktørId(String aktørId) {
            this.aktørId = aktørId;
            return this;
        }

        public Builder medYtelse(Ytelse ytelse) {
            this.ytelse = ytelse;
            return this;
        }

        public Builder medArbeidsgiver(Arbeidsgiver arbeidsgiver) {
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

        public Builder medKildesystem(Kildesystem kildesystem) {
            this.kildesystem = kildesystem;
            return this;
        }

        public Builder medOpprettetAv(String opprettetAv) {
            this.opprettetAv = opprettetAv;
            return this;
        }

        public Builder medAvsenderSystem(AvsenderSystem avsenderSystem) {
            this.avsenderSystem = avsenderSystem;
            return this;
        }

        public Builder medMånedRefusjon(BigDecimal månedRefusjon) {
            this.månedRefusjon = månedRefusjon;
            return this;
        }

        public Builder medOpphørsdatoRefusjon(LocalDate opphørsdatoRefusjon) {
            this.opphørsdatoRefusjon = opphørsdatoRefusjon;
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

    public record Arbeidsgiver(String ident) {

        @Override
        public String toString() {
            return getClass().getSimpleName() + "<" + maskerId() + ">";
        }

        private String maskerId() {
            if (ident == null) {
                return "";
            }
            var length = ident.length();
            if (length <= 4) {
                return "*".repeat(length);
            }
            return "*".repeat(length - 4) + ident.substring(length - 4);
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

    public record SøktRefusjon(LocalDate fom,
                               BigDecimal beløp) {
    }

    public record BortfaltNaturalytelse(LocalDate fom,
                                        LocalDate tom,
                                        Naturalytelsetype naturalytelsetype,
                                        BigDecimal beløp) {
    }

    public record Endringsårsaker(no.nav.familie.inntektsmelding.inntektsmelding.rest.kontrakt.Endringsårsak årsak,
                                  LocalDate fom,
                                  LocalDate tom,
                                  LocalDate bleKjentFom) {
    }

    public record Kontaktperson(
        String telefonnummer,
        String navn
    ) {
    }

    public record AvsenderSystem(
        String navn,
        String versjon
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

    public enum Kildesystem {
        SAKSBEHANDLER,
        ARBEIDSGIVERPORTAL,
        LPS_API
    }

}
