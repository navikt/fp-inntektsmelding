package no.nav.foreldrepenger.inntektsmelding.typer.dto;

public enum ForespørselAksjon {
    OPPRETT, // Ny forespørsel
    UTGÅTT, // Forespørsel som ikke trengs/som vi ikke vil ha inn endringer på
    BEHOLD, // Forespørsel som skal være uendret. Brukes for å hindre at åpen (UNDER_BEHANDLING) forespørsel blir satt til utgått.
    GJENOPPRETT // Utgått forespørsel som skal gjenopprettes. Må ha sendt inn IM på denne (status settes til FERDIG).
}
