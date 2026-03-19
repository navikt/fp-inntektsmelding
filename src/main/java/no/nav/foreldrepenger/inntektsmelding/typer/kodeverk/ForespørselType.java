package no.nav.foreldrepenger.inntektsmelding.typer.kodeverk;

import no.nav.foreldrepenger.inntektsmelding.typer.kodeverk.lager.DatabaseKode;

public enum ForespørselType implements DatabaseKode {
    ARBEIDSGIVERINITIERT_NYANSATT,
    ARBEIDSGIVERINITIERT_UREGISTRERT,
    BESTILT_AV_FAGSYSTEM
}
