package no.nav.foreldrepenger.inntektsmelding.integrasjoner.altinn;

import jakarta.validation.constraints.NotNull;

/**
 * Representerer samme tekst på bokmål, nynorsk og engelsk. Brukes til å bygge flerspråklig innhold
 * (title/summary/content m.m.) som sendes til Dialogporten, som støtter at hvert tekstfelt kan inneholde
 * flere språkvarianter samtidig (konsumenten velger riktig variant basert på brukerens språkinnstilling).
 */
public record FlerspråkligTekst(@NotNull String nb, @NotNull String nn, @NotNull String en) {

    /**
     * Formaterer nb/nn/en-variantene samtidig med samme argumenter (jf. {@link String#formatted}).
     * Brukes for tekstmaler der de samme innsatte verdiene (navn, dato, tall) gjelder uavhengig av språk.
     */
    public FlerspråkligTekst formatted(Object... args) {
        return new FlerspråkligTekst(nb.formatted(args), nn.formatted(args), en.formatted(args));
    }
}
