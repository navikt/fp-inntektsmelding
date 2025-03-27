package no.nav.familie.inntektsmelding.integrasjoner.aareg;

import no.nav.familie.inntektsmelding.integrasjoner.aareg.dto.AnsettelsesperiodeDto;

public record Arbeidsforhold(String organisasjonsnummer, AnsettelsesperiodeDto ansettelsesperiode) {
}
