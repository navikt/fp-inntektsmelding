package no.nav.foreldrepenger.inntektsmelding.integrasjoner.aareg.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record ArbeidsforholdDto(
    String arbeidsforholdId,
    Long navArbeidsforholdId,
    OpplysningspliktigArbeidsgiverDto arbeidsgiver,
    AnsettelsesperiodeDto ansettelsesperiode,
    List<ArbeidsavtaleDto> arbeidsavtaler,
    List<PermisjonPermitteringDto> permisjonPermitteringer,
    String type // (kodeverk: Arbeidsforholdtyper)
) {
    public List<ArbeidsavtaleDto> arbeidsavtaler() {
        return arbeidsavtaler != null ? arbeidsavtaler : List.of();
    }

    public List<PermisjonPermitteringDto> permisjonPermitteringer() {
        return permisjonPermitteringer != null ? permisjonPermitteringer : List.of();
    }
}
