package no.nav.foreldrepenger.inntektsmelding.inntektsmelding;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import no.nav.foreldrepenger.inntektsmelding.integrasjoner.person.AktørId;
import no.nav.foreldrepenger.inntektsmelding.typer.domene.Arbeidsgiver;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Endringsårsak;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Kildesystem;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.NaturalytelseType;
import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.Ytelsetype;

public class InntektsmeldingDto {
    private final Long id;
    private final UUID inntektsmeldingUuid;
    private final AktørId aktørId;
    private final Ytelsetype ytelse;
    private final Arbeidsgiver arbeidsgiver;
    private final Kontaktperson kontaktperson;
    private final LocalDate startdato;
    private final BigDecimal månedInntekt;
    private final Innsendingsårsak innsendingsårsak; //TODO: finnes ikke i DB ennå
    private final Innsendingstype innsendingstype; //TODO: finnes ikke i DB ennå
    private final LocalDateTime innsendtTidspunkt;
    private final Kildesystem kildesystem;
    private final AvsenderSystem avsenderSystem; //TODO: finnes ikke i DB ennå
    private final BigDecimal månedRefusjon;
    private final LocalDate opphørsdatoRefusjon;
    private final List<SøktRefusjon> søkteRefusjonsperioder;
    private final List<BortfaltNaturalytelse> bortfaltNaturalytelsePerioder;
    private final List<Endringsårsaker> endringAvInntektÅrsaker;
    private final String opprettetAv;

    private InntektsmeldingDto(Builder builder) {
        this.id = builder.id;
        this.inntektsmeldingUuid = builder.inntektsmeldingUuid;
        this.aktørId = builder.aktørId;
        this.ytelse = builder.ytelse;
        this.arbeidsgiver = builder.arbeidsgiver;
        this.kontaktperson = builder.kontaktperson;
        this.startdato = builder.startdato;
        this.månedInntekt = builder.inntekt;
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

    public Long getId() {
        return id;
    }

    public UUID getInntektsmeldingUuid() {
        return inntektsmeldingUuid;
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public Ytelsetype getYtelse() {
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

    public BigDecimal getMånedInntekt() {
        return månedInntekt;
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
        private Long id;
        private UUID inntektsmeldingUuid;
        private AktørId aktørId;
        private Ytelsetype ytelse;
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

        public Builder medId(Long id) {
            this.id = id;
            return this;
        }

        public Builder medInntektsmeldingUuid(UUID inntektsmeldingUuid) {
            this.inntektsmeldingUuid = inntektsmeldingUuid;
            return this;
        }

        public Builder medAktørId(AktørId aktørId) {
            this.aktørId = aktørId;
            return this;
        }

        public Builder medYtelse(Ytelsetype ytelse) {
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

    public record SøktRefusjon(LocalDate fom,
                               BigDecimal beløp) {
    }

    public record BortfaltNaturalytelse(LocalDate fom,
                                        LocalDate tom,
                                        NaturalytelseType naturalytelsetype,
                                        BigDecimal beløp) {
    }

    public record Endringsårsaker(Endringsårsak årsak,
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

}
