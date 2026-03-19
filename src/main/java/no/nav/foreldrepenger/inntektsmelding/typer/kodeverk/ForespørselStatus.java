package no.nav.foreldrepenger.inntektsmelding.typer.kodeverk;

import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.lager.DatabaseKode;

public enum ForespørselStatus implements DatabaseKode {
    UNDER_BEHANDLING,
    FERDIG,
    UTGÅTT
}
