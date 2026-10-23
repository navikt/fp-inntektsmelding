package no.nav.foreldrepenger.inntektsmelding.integrasjoner.aareg.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record AaregArbeidsforholdDto(
    String arbeidsforholdId,
    Long navArbeidsforholdId,
    OpplysningspliktigArbeidsgiverDto arbeidsgiver,
    AnsettelsesperiodeDto ansettelsesperiode,
    List<ArbeidsavtaleDto> arbeidsavtaler,
    List<PermisjonPermitteringDto> permisjonPermitteringer,
    AaregArbeidsforholdType type
) {
    public enum AaregArbeidsforholdType {
        ORDINÆRT_ARBEIDSFORHOLD("ordinaertArbeidsforhold"),
        MARITIMT_ARBEIDSFORHOLD("maritimtArbeidsforhold"),
        FORENKLET_OPPGJØRSORDNING("forenkletOppgjoersordning"),
        FRILANSER_OPPDRAGSTAKER_MED_MER("frilanserOppdragstakerHonorarPersonerMm"),
        PENSJON_OG_ANDRE_TYPER_YTELSER_UTEN_ANSETTELSESFORHOLD("pensjonOgAndreTyperYtelserUtenAnsettelsesforhold");

        @JsonValue
        private final String aaregKode;

        AaregArbeidsforholdType(String aaregKode) {
            this.aaregKode = aaregKode;
        }

        // Ukjente koder gir null
        @JsonCreator
        public static AaregArbeidsforholdType fraAaregKode(String aaregKode) {
            return Arrays.stream(values())
                .filter(t -> t.aaregKode.equals(aaregKode))
                .findFirst()
                .orElse(null);
        }
    }

    public List<ArbeidsavtaleDto> arbeidsavtaler() {
        return arbeidsavtaler != null ? arbeidsavtaler : List.of();
    }

    public List<PermisjonPermitteringDto> permisjonPermitteringer() {
        return permisjonPermitteringer != null ? permisjonPermitteringer : List.of();
    }
}
